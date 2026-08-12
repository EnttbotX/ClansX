package x.Entt.ClansX.Utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import x.Entt.ClansX.CX;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConfigMigrator {

    private final CX plugin;
    private final FileHandler fh;

    private FileConfiguration oldConfig;
    private FileConfiguration oldClans;
    private FileConfiguration oldPlayers;
    private FileConfiguration oldBalances;

    private File oldConfigFile;

    public ConfigMigrator(CX plugin, FileHandler fh) {
        this.plugin = plugin;
        this.fh = fh;
    }

    public void migrateAll() {
        File folder = plugin.getDataFolder();
        folder.mkdirs();

        backupAndLoadOldData();

        fh.saveDefaults();

        migrateConfig();
        migrateClans();
        migratePlayers();

        cleanup();
        deleteOldFiles();
    }

    private void backupAndLoadOldData() {
        File folder = plugin.getDataFolder();

        File config = new File(folder, "config.yml");
        if (config.exists()) {
            oldConfigFile = new File(folder, "config.old.yml");
            config.renameTo(oldConfigFile);
            oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile);
        }

        File fClans = new File(folder, "data.yml");
        File fPlayers = new File(folder, "playerData.yml");
        File fBalances = new File(folder, "balances.yml");

        if (fClans.exists()) oldClans = YamlConfiguration.loadConfiguration(fClans);
        if (fPlayers.exists()) oldPlayers = YamlConfiguration.loadConfiguration(fPlayers);
        if (fBalances.exists()) oldBalances = YamlConfiguration.loadConfiguration(fBalances);
    }

    private void migrateConfig() {
        if (oldConfig == null) return;

        FileHandler fh = plugin.getFH();

        FileConfiguration cfg = fh.getConfig();
        FileConfiguration msg = fh.getMsg();

        if (oldConfig.getString("config-version") != null && Objects.requireNonNull(oldConfig.getString("config-version")).equalsIgnoreCase("1.3.0")) {
            for (String key : oldConfig.getKeys(true)) {
                if (key.equals("prefix") || key.equals("config-version") || key.equals("welcome-message")) continue;

                cfg.set(key, oldConfig.get(key));
            }

            cfg.set("welcome-message", oldConfig.getBoolean("welcome-message.enabled", false));
            msg.set("prefix", oldConfig.getString("prefix", "&3&L[Clans&b&lX&3&l] "));

            List<String> noClan = oldConfig.getStringList("welcome-message.no-clan");
            if (noClan != null && !noClan.isEmpty()) {
                msg.set("welcome_message.no_clan", noClan);
            }

            List<String> self = oldConfig.getStringList("welcome-message.self-clan");
            if (self != null && !self.isEmpty()) {
                msg.set("welcome_message.self", self);
            }

            List<String> to_clan = oldConfig.getStringList("welcome-message.to-clan");
            if (to_clan != null && !to_clan.isEmpty()) {
                msg.set("welcome_message.to_clan", to_clan);
            }

            cfg.set("clans.wars.max-duration", "7d");
            cfg.set("config-version", plugin.version);
        } else {
            msg.set("prefix", oldConfig.getString("prefix", "&3&L[Clans&b&lX&3&l] "));

            cfg.set("economy.enabled", oldConfig.getBoolean("economy.enabled", true));
            cfg.set("economy.system", oldConfig.getString("economy.system", "Vault"));
            cfg.set("economy.cost.create-clan", oldConfig.getInt("economy.cost.create-clan", 5));
            cfg.set("economy.cost.start-war", 0);
            cfg.set("economy.earn.delete-clan", oldConfig.getInt("economy.earn.delete-clan", 1));

            cfg.set("clans.wars", true);
            cfg.set("clans.max-clans", oldConfig.getInt("max-clans", 0));
            cfg.set("clans.messages-to-clan", "@");
            cfg.set("clans.names.max-length", 0);
            cfg.set("clans.names.blocked", oldConfig.getStringList("names-blocked.blocked"));
        }

        fh.saveConfig();
        fh.saveMsg();
    }

    private void migrateClans() {
        if (oldClans == null) {
            return;
        }

        if (!oldClans.isConfigurationSection("Clans")) {
            return;
        }

        if (oldConfig.getString("config-version") != null && Objects.requireNonNull(oldConfig.getString("config-version")).equalsIgnoreCase("1.3.0")) {
            return;
        }

        for (String clan : Objects.requireNonNull(oldClans.getConfigurationSection("Clans")).getKeys(false)) {
            String b = "Clans." + clan + ".";

            FileConfiguration n = fh.getClan(clan);
            n.getKeys(false).forEach(k -> n.set(k, null));

            n.set("name", clan);
            n.set("founder", oldClans.getString(b + "Founder"));
            n.set("leader", oldClans.getString(b + "Leader"));
            n.set("privacy", oldClans.getString(b + "Privacy", "Public"));
            n.set("balance", oldClans.getDouble(b + "Money", 0));

            n.set("users", oldClans.getStringList(b + "Users"));

            n.set("invitations", null);
            n.set("reports", null);

            n.set("wars.streak", 0);
            n.set("wars.active", null);
            n.set("wars.won", 0);
            n.set("wars.defeated", 0);

            n.set("clanChest", null);

            fh.saveClan(clan, n);
        }
    }

    private void migratePlayers() {
        if (oldPlayers == null) {
            return;
        }

        if (oldConfig.getString("config-version") != null && Objects.requireNonNull(oldConfig.getString("config-version")).equalsIgnoreCase("1.3.0")) {
            return;
        }

        for (String key : oldPlayers.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Player Key is not a valid UUID: " + key);
                continue;
            }

            FileConfiguration n = fh.getPlayerData(uuid);
            n.getKeys(false).forEach(k -> n.set(k, null));

            String name = null;

            if (oldBalances != null) {
                name = oldBalances.getString(key + ".Name");
            }

            n.set("name", name);
            n.set("clan", oldPlayers.getString(key + ".currentClan"));
            n.set("history", oldPlayers.getStringList(key + ".clanHistory"));

            double balance = oldBalances != null ? oldBalances.getDouble(key + ".Money", 0) : 0;

            n.set("balance", balance);
            fh.savePlayerData(uuid, n);
        }
    }

    private void cleanup() {
        try {
            oldConfigFile.delete();
        } catch (Exception e) {
            Text.log("&cNothing to delete in the migration process.");
        }
    }

    private void deleteOldFiles() {
        delete("data.yml");
        delete("playerData.yml");
        delete("balances.yml");
    }

    private void delete(String name) {
        File f = new File(plugin.getDataFolder(), name);

        if (f.exists()) {
            f.delete();
        }
    }
}