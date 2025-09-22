package x.Entt.ClansX.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import x.Entt.ClansX.CX;

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
    if (!this.configFile.exists())
      this.plugin.saveResource("config.yml", false); 
    this.configFileConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.configFile);
    if (!this.dataFile.exists())
      this.plugin.saveResource("data.yml", false); 
    this.dataFileConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.dataFile);
    if (!this.playerDataFile.exists())
      this.plugin.saveResource("playerData.yml", false); 
    this.pDataFile = (FileConfiguration)YamlConfiguration.loadConfiguration(this.playerDataFile);
  }
  
  public void saveData() {
    try {
      if (this.dataFileConfig != null)
        this.dataFileConfig.save(this.dataFile); 
    } catch (IOException e) {
      this.plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
    } 
  }
  
  public void loadData() {
    this.dataFileConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.dataFile);
  }
  
  public void reloadData() {
    loadData();
  }
  
  public FileConfiguration getData() {
    if (this.dataFileConfig == null)
      loadData(); 
    return this.dataFileConfig;
  }
  
  public void savePStats() {
    try {
      if (this.playerDataFile != null)
        this.pDataFile.save(this.playerDataFile); 
    } catch (IOException e) {
      this.plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
    } 
  }
  
  public void loadPStats() {
    this.pDataFile = (FileConfiguration)YamlConfiguration.loadConfiguration(this.playerDataFile);
  }
  
  public void reloadPStats() {
    loadPStats();
  }
  
  public FileConfiguration getPStats() {
    if (this.playerDataFile == null)
      loadData(); 
    return this.pDataFile;
  }
  
  public void saveConfig() {
    try {
      if (this.configFileConfig != null)
        this.configFileConfig.save(this.configFile); 
    } catch (IOException e) {
      this.plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
    } 
  }
  
  public void loadConfig() {
    this.configFileConfig = (FileConfiguration)YamlConfiguration.loadConfiguration(this.configFile);
  }
  
  public void reloadConfig() {
    loadConfig();
  }
  
  public FileConfiguration getConfig() {
    if (this.configFileConfig == null)
      loadConfig(); 
    return this.configFileConfig;
  }
  
  public boolean renameClan(UUID uuid, String oldName, String newName) {
    FileConfiguration data = getData();
    FileConfiguration pData = getPStats();
    if (!data.contains("Clans." + oldName) || data.contains("Clans." + newName))
      return false; 
    data.set("Clans." + newName, data.get("Clans." + oldName));
    data.set("Clans." + oldName, null);
    String key = uuid.toString();
    if (oldName.equalsIgnoreCase(pData.getString(key + ".currentClan")))
      pData.set(key + ".currentClan", newName); 
    List<String> history = pData.getStringList(key + ".clanHistory");
    for (int i = 0; i < history.size(); i++) {
      if (oldName.equalsIgnoreCase(history.get(i)))
        history.set(i, newName); 
    } 
    pData.set(key + ".clanHistory", history);
    saveData();
    savePStats();
    return true;
  }
  
  public boolean renameClan(Player player, String oldName, String newName) {
    return renameClan(player.getUniqueId(), oldName, newName);
  }
}
