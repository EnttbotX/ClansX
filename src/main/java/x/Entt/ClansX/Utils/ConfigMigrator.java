package x.Entt.ClansX.Utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import x.Entt.ClansX.CX;

import java.io.File;
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

        FileConfiguration cfg = plugin.getConfig();

        cfg.set("config-version", "1.3.0");
        cfg.set("prefix", oldConfig.getString("prefix"));

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

        plugin.saveConfig();
    }

    private void migrateClans() {
        if (oldClans == null) return;

        if (!oldClans.isConfigurationSection("Clans")) return;

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
        if (oldPlayers == null) return;

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

            double balance = oldBalances != null
                    ? oldBalances.getDouble(key + ".Money", 0)
                    : 0;

            n.set("balance", balance);

            fh.savePlayerData(uuid, n);
        }
    }

    private void cleanup() {
        if (oldConfigFile != null && oldConfigFile.exists()) {
            oldConfigFile.delete();
        }
    }

    private void deleteOldFiles() {
        delete("data.yml");
        delete("playerData.yml");
        delete("balances.yml");
    }

    private void delete(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (f.exists()) f.delete();
    }
}