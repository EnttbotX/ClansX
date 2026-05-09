package x.Entt.ClansX;

import java.io.File;
import java.util.Objects;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
    public static Econo econ;
    private Updater updater;
    private Metrics metrics;
    private FileHandler fh;

    @Override
    public void onEnable() {
        version = getDescription().getVersion();

        fh = new FileHandler(this);

        if (needsMigration())
            new ConfigMigrator(this, fh).migrateAll();
        else
            fh.saveDefaults();

        prefix = fh.getConfig().getString("prefix", "&3&L[Clans&b&lX&3&l] ");

        econ = new Econo(this, fh);

        if (fh.getConfig().getBoolean("economy.enabled", true))
            econ.setupEconomy();

        updater = new Updater(this, 114316);
        metrics = new Metrics(this, 20912);
        warSystem = new WarSystem(this);

        setupMetrics();
        registerCommands();
        registerEvents();
        searchUpdates();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            new PAPI(this).registerPlaceholders();
        else
            MSG.log(prefix + "&cPAPI not found!");

        long purgeInterval = 20L * 60 * 30;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            fh.purgeExpiredTNL();
            fh.purgeExpiredWars();
        }, purgeInterval, purgeInterval);

        MSG.log(prefix + "&av" + version + " &2Enabled!");
    }

    @Override
    public void onDisable() {
        if (metrics != null)
            metrics.shutdown();

        MSG.log(prefix + "&av" + getDescription().getVersion() + " &cDisabled");
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
        if (!configFile.exists()) return false;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        String v = cfg.getString("config-version");

        return v == null || !v.equalsIgnoreCase("1.3.0");
    }

    public void searchUpdates() {
        String latest = updater.getLatestVersion();
        if (latest == null) return;

        if (compareVersions(version, latest) >= 0) return;

        String url = updater.getResourceURL();

        TextComponent link = new TextComponent(MSG.color("&e&l[ DOWNLOAD UPDATE ]"));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        MSG.log("");
        MSG.log("&2&l======== " + prefix + "&2&l========");
        MSG.log("&e&lUPDATE AVAILABLE");
        MSG.log("&e&lVersion: &f" + version);
        MSG.log("&e&lNew Version: &f" + latest);
        MSG.log("&e&lDownload Link:");
        MSG.log(url);
        MSG.log("&2&l======== " + prefix + "&2&l========");
        MSG.log("");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("if.admin")) {
                p.sendMessage(MSG.color(prefix + "&e&lA new plugin update is available!"));
                p.spigot().sendMessage(link);
            }
        }
    }

    private int compareVersions(String current, String latest) {
        String[] c = current.split("\\.");
        String[] l = latest.split("\\.");

        int max = Math.max(c.length, l.length);

        for (int i = 0; i < max; i++) {
            int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
            int lv = i < l.length ? Integer.parseInt(l[i]) : 0;

            if (cv < lv) return -1;
            if (cv > lv) return 1;
        }

        return 0;
    }

    public static Econo getEcon() {
        return econ;
    }

    public FileHandler getFH() {
        return fh;
    }

    public WarSystem getWarSystem() {
        return warSystem;
    }

    public String getPlayerClan(Player player) {
        FileConfiguration p = fh.getPlayerData(player.getUniqueId());
        if (p == null) return null;
        return p.getString("clan");
    }
}