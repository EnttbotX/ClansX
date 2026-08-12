package x.Entt.ClansX.Utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import x.Entt.ClansX.CX;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileHandler {
    private final CX plugin;

    private final File clansDir;
    private final File playersDir;
    private final File warsDir;

    private final File configFile;
    private final File tnlFile;
    private final File msgFile;

    private FileConfiguration config;
    private FileConfiguration tnl;
    private FileConfiguration messages;

    public FileHandler(CX plugin) {
        this.plugin = plugin;

        File data = plugin.getDataFolder();
        clansDir = new File(data, "Clans");
        playersDir = new File(data, "Players");
        warsDir = new File(data, "Wars");

        configFile = new File(data, "config.yml");
        tnlFile = new File(data, "tnl.yml");
        msgFile = new File(data, "messages.yml");
    }

    public void saveDefaults() {
        plugin.getDataFolder().mkdirs();
        clansDir.mkdirs();
        playersDir.mkdirs();
        warsDir.mkdirs();

        if (!configFile.exists()) plugin.saveResource("config.yml", false);
        if (!tnlFile.exists()) plugin.saveResource("tnl.yml", false);
        if (!msgFile.exists()) plugin.saveResource("messages.yml", false);

        loadConfig();
        loadTNL();
        loadMsg();
        updateClanNames();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        save(config, configFile);
    }

    public FileConfiguration getTNL() {
        return tnl;
    }

    public void loadTNL() {
        tnl = YamlConfiguration.loadConfiguration(tnlFile);
    }

    public void saveTNL() {
        save(tnl, tnlFile);
    }

    public FileConfiguration getMsg() {
        return messages;
    }

    public void loadMsg() {
        messages = YamlConfiguration.loadConfiguration(msgFile);
    }

    public void saveMsg() {
        save(messages, msgFile);
    }

    private FileConfiguration load(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(FileConfiguration cfg, File file) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            cfg.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Can't save file: " + file.getName(), e);
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private boolean validClan(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_-]+$");
    }

    public boolean clanExists(String name) {
        return validClan(name) && new File(clansDir, sanitize(name) + ".yml").exists();
    }

    public FileConfiguration getClan(String name) {
        if (!validClan(name)) {
            return null;
        }

        File file = new File(clansDir, sanitize(name) + ".yml");
        return file.exists() ? load(file) : null;
    }

    public void saveClan(String name, FileConfiguration cfg) {
        if (validClan(name))
            save(cfg, new File(clansDir, sanitize(name) + ".yml"));
    }

    public void deleteClan(String name) {
        if (!validClan(name)) return;

        File file = new File(clansDir, sanitize(name) + ".yml");
        if (file.exists()) file.delete();
    }

    public FileConfiguration newClan(String name) {
        if (!validClan(name)) return null;

        File file = new File(clansDir, sanitize(name) + ".yml");
        if (file.exists()) return null;

        return YamlConfiguration.loadConfiguration(file);
    }

    public List<String> getClanNames() {
        File[] files = clansDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (File f : files) {
            FileConfiguration cfg = load(f);
            String name = cfg.getString("name");
            if (name != null) names.add(name);
            else names.add(f.getName().replace(".yml", ""));
        }
        return names;
    }

    public boolean renameClan(String oldName, String newName) {
        if (!validClan(oldName) || !validClan(newName)) return false;

        File oldFile = new File(clansDir, sanitize(oldName) + ".yml");
        File newFile = new File(clansDir, sanitize(newName) + ".yml");

        if (!oldFile.exists() || newFile.exists()) return false;

        FileConfiguration clan = load(oldFile);
        clan.set("name", newName);
        save(clan, newFile);
        oldFile.delete();

        File[] players = playersDir.listFiles();
        if (players == null) return true;

        for (File f : players) {
            FileConfiguration p = load(f);

            if (oldName.equalsIgnoreCase(p.getString("clan")))
                p.set("clan", newName);

            List<String> history = p.getStringList("history");
            for (int i = 0; i < history.size(); i++)
                if (history.get(i).equalsIgnoreCase(oldName))
                    history.set(i, newName);

            p.set("history", history);
            save(p, f);
        }

        return true;
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        File file = new File(playersDir, uuid + ".yml");

        if (file.exists())
            return load(file);

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return registerPlayer(uuid, op.getName() == null ? "Unknown" : op.getName());
    }

    public FileConfiguration registerPlayer(UUID uuid, String name) {
        File file = new File(playersDir, uuid + ".yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (!file.exists()) {
            data.set("name", name);
            data.set("clan", null);
            data.set("balance", 0);
            data.set("history", new ArrayList<>());
            save(data, file);
        }

        return data;
    }

    public void savePlayerData(UUID uuid, FileConfiguration cfg) {
        save(cfg, new File(playersDir, uuid + ".yml"));
    }

    public FileConfiguration getWar(String id) {
        File file = new File(warsDir, id + ".yml");
        return file.exists() ? load(file) : null;
    }

    public void saveWar(String id, FileConfiguration cfg) {
        save(cfg, new File(warsDir, id + ".yml"));
    }

    public List<String> getActiveWars() {
        List<String> list = new ArrayList<>();
        File[] files = warsDir.listFiles((d, n) -> n.endsWith(".yml"));

        if (files == null) return list;

        for (File f : files) {
            FileConfiguration war = load(f);
            if (war.getBoolean("active"))
                list.add(f.getName().replace(".yml", ""));
        }

        return list;
    }

    public int countActiveWars(String clan) {
        int count = 0;
        File[] files = warsDir.listFiles((d, n) -> n.endsWith(".yml"));

        if (files == null) return 0;

        for (File f : files) {
            FileConfiguration cfg = load(f);

            if (!cfg.getBoolean("active")) continue;

            if (cfg.getStringList("clans.offensive").contains(clan) ||
                    cfg.getStringList("clans.defensive").contains(clan))
                count++;
        }

        return count;
    }

    public void updateClanNames() {
        File[] files = clansDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        int min = config.getInt("clans.names.min-length", 1);
        int max = config.getInt("clans.names.max-length", 0);
        boolean allowUpper = config.getBoolean("clans.names.allow-uppercase", true);

        List<String> blocked = config.getStringList("clans.names.blocked")
                .stream().map(String::toLowerCase).toList();

        for (File file : files) {
            FileConfiguration clan = load(file);
            String oldName = clan.getString("name");

            if (oldName == null) continue;

            String newName = oldName;

            if (!allowUpper) newName = newName.toLowerCase();

            for (String b : blocked)
                if (newName.contains(b))
                    newName = newName.replace(b, "*".repeat(b.length()));

            if (newName.length() < min)
                newName += "_".repeat(min - newName.length());

            if (max > 0 && newName.length() > max)
                newName = newName.substring(0, max);

            if (!oldName.equals(newName))
                renameClan(oldName, newName);
        }
    }

    public void purgeExpiredTNL() {
        if (tnl == null) return;
        long now = System.currentTimeMillis();
        boolean dirty = false;

        var trucesSec = tnl.getConfigurationSection("truces");
        if (trucesSec != null) {
            for (String clanA : new HashSet<>(trucesSec.getKeys(false))) {
                var inner = trucesSec.getConfigurationSection(clanA);
                if (inner == null) continue;
                for (String clanB : new HashSet<>(inner.getKeys(false))) {
                    String path = "truces." + clanA + "." + clanB;
                    Object val = tnl.get(path);
                    if (val instanceof Long expiry && expiry > 0 && now > expiry) {
                        tnl.set(path, null);
                        dirty = true;
                    }
                }
                if (trucesSec.getConfigurationSection(clanA) != null
                        && Objects.requireNonNull(trucesSec.getConfigurationSection(clanA)).getKeys(false).isEmpty()) {
                    tnl.set("truces." + clanA, null);
                    dirty = true;
                }
            }
        }

        var warsSec = tnl.getConfigurationSection("wars");
        if (warsSec != null) {
            for (String warId : new HashSet<>(warsSec.getKeys(false))) {
                long expiry = tnl.getLong("wars." + warId + ".expires", -1);
                if (expiry > 0 && now > expiry) {
                    tnl.set("wars." + warId, null);
                    dirty = true;
                }
            }
        }

        var notifSec = tnl.getConfigurationSection("notifications");
        if (notifSec != null) {
            for (String playerName : new HashSet<>(notifSec.getKeys(false))) {
                long expiry = tnl.getLong("notifications." + playerName + ".expires", -1);
                if (expiry > 0 && now > expiry) {
                    tnl.set("notifications." + playerName, null);
                    dirty = true;
                }
            }
        }

        if (dirty) saveTNL();
    }

    public void purgeExpiredWars() {
        File[] files = warsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        FileConfiguration t = getTNL();
        long now = System.currentTimeMillis();

        for (File f : files) {
            String warId = f.getName().replace(".yml", "");
            long expiry = t.getLong("wars." + warId + ".expires", -1);
            if (expiry > 0 && now > expiry) {
                f.delete();
                t.set("wars." + warId, null);
            }
        }

        saveTNL();
    }

    public void CleanUp() {
        File[] files = playersDir.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".yml")) continue;

            YamlConfiguration data = YamlConfiguration.loadConfiguration(file);

            if (data.contains("clan")) {
                data.set("clan", null);
                save(data, file);
            }
        }
    }

    public File getClansDir() {
        return clansDir;
    }

    public File getPlayersDir() {
        return playersDir;
    }

    public File getWarsDir() {
        return warsDir;
    }
}