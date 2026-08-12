package x.Entt.ClansX;

import java.io.File;
import java.util.Objects;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import x.Entt.ClansX.CMDs.ACMD;
import x.Entt.ClansX.CMDs.CCMD;
import x.Entt.ClansX.Events.Chat;
import x.Entt.ClansX.Events.Events;
import x.Entt.ClansX.Utils.*;

public class CX extends JavaPlugin {
    public String version = getDescription().getVersion();
    public static String prefix;
    private WarSystem warSystem;
    public CheckName checkName;
    public static Econo econ;
    private Updater updater;
    private Metrics metrics;
    private FileHandler fh;

    @Override
    public void onEnable() {
        version = getDescription().getVersion();

        fh = new FileHandler(this);

        if (needsMigration()) {
            new ConfigMigrator(this, fh).migrateAll();
        } else {
            fh.saveDefaults();
        }

        prefix = fh.getMsg().getString("prefix", "&3&L[Clans&b&lX&3&l] ");

        econ = new Econo(this, fh);

        MSG msg = new MSG(this);

        if (fh.getConfig().getBoolean("economy.enabled", true)) {
            econ.setupEconomy();
        }

        updater = new Updater(this, 114316);
        metrics = new Metrics(this, 20912);
        warSystem = new WarSystem(this);

        checkName = new CheckName(this);

        if (fh.getConfig().getBoolean("updater.enabled")) {
            updater.searchUpdates();
        }

        setupMetrics();
        registerCommands();
        registerEvents();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PAPI(this).registerPlaceholders();
        } else {
            Text.log(prefix + "&cPAPI not found!");
        }

        long purgeInterval = 20L * 60 * 30;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            fh.purgeExpiredTNL();
            fh.purgeExpiredWars();
        }, purgeInterval, purgeInterval);

        fh.CleanUp();

        Text.log(prefix + "&av" + version + " &2Enabled!");
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
        }

        Text.log(prefix + "&av" + getDescription().getVersion() + " &cDisabled");
    }

    private void setupMetrics() {
        FileConfiguration cfg = fh.getConfig();

        int max = cfg.getInt("max-clans", -1);
        String maxClans = (max <= 0) ? "Unlimited" : String.valueOf(max);

        metrics.addCustomChart(new SimplePie("economy_enabled",
                () -> String.valueOf(cfg.getBoolean("economy.enabled", true))));

        metrics.addCustomChart(new SimplePie("economy_system",
                () -> cfg.getString("economy.system", "Unknown")));

        metrics.addCustomChart(new SimplePie("max_clans",
                () -> maxClans));
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("clansx")).setExecutor(new ACMD(this));
        Objects.requireNonNull(getCommand("clans")).setExecutor(new CCMD(this));
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new Events(this), this);
        getServer().getPluginManager().registerEvents(new Chat(this), this);
    }

    private boolean needsMigration() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return false;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        String v = cfg.getString("config-version");

        return v == null || !v.equalsIgnoreCase("1.3.1");
    }

    public static Econo getEcon() {
        return econ;
    }

    public FileHandler getFH() {
        return fh;
    }

    public CheckName getCheckNameTool() {
        return checkName;
    }

    public WarSystem getWarSystem() {
        return warSystem;
    }

    public Updater getUpdater() {
        return updater;
    }

    public String getPlayerClan(Player player) {
        FileConfiguration p = fh.getPlayerData(player.getUniqueId());
        if (p == null) return null;
        return p.getString("clan");
    }
}