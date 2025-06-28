package x.Entt.ClansX.Utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.entity.Player;
import x.Entt.ClansX.CX;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class FileHandler {
    private final CX plugin;
    private FileConfiguration dataFileConfig = null;
    private final File dataFile;
    private FileConfiguration configFileConfig = null;
    private final File configFile;
    private FileConfiguration pDataFile = null;
    private final File playerDataFile;

    public FileHandler(CX plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.playerDataFile = new File(plugin.getDataFolder(), "playerData.yml");
        this.configFile = new File(plugin.getDataFolder(), "config.yml");

        saveDefaults();
    }

    public void saveDefaults() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        configFileConfig = YamlConfiguration.loadConfiguration(configFile);

        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        dataFileConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (!playerDataFile.exists()) {
            plugin.saveResource("playerData.yml", false);
        }
        pDataFile = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void saveData() {
        try {
            if (dataFileConfig != null) {
                dataFileConfig.save(dataFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    public void loadData() {
        dataFileConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void reloadData() {
        loadData();
    }

    public FileConfiguration getData() {
        if (dataFileConfig == null) {
            loadData();
        }
        return dataFileConfig;
    }

    public void savePStats() {
        try {
            if (playerDataFile != null) {
                pDataFile.save(playerDataFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    public void loadPStats() {
        pDataFile = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void reloadPStats() {
        loadPStats();
    }

    public FileConfiguration getPStats() {
        if (playerDataFile == null) {
            loadData();
        }
        return pDataFile;
    }

    public void saveConfig() {
        try {
            if (configFileConfig != null) {
                configFileConfig.save(configFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
        }
    }

    public void loadConfig() {
        configFileConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public FileConfiguration getConfig() {
        if (configFileConfig == null) {
            loadConfig();
        }
        return configFileConfig;
    }


    public boolean renameClan(UUID uuid, String oldName, String newName) {
        FileConfiguration data = getData();
        FileConfiguration pData = getPStats();
        if (!data.contains("Clans." + oldName) || data.contains("Clans." + newName)) return false;

        data.set("Clans." + newName, data.get("Clans." + oldName));
        data.set("Clans." + oldName, null);

        String key = uuid.toString();
        if (oldName.equalsIgnoreCase(pData.getString(key + ".currentClan")))
            pData.set(key + ".currentClan", newName);

        List<String> history = pData.getStringList(key + ".clanHistory");
        for (int i = 0; i < history.size(); i++)
            if (oldName.equalsIgnoreCase(history.get(i)))
                history.set(i, newName);
        pData.set(key + ".clanHistory", history);

        saveData();
        savePStats();
        return true;
    }

    public boolean renameClan(Player player, String oldName, String newName) {
        return renameClan(player.getUniqueId(), oldName, newName);
    }
}