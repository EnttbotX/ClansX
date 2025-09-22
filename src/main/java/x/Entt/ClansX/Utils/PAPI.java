package x.Entt.ClansX.Utils;

import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import x.Entt.ClansX.CX;

public class PAPI extends PlaceholderExpansion {
  private final CX plugin;
  
  private final FileConfiguration data;
  
  public PAPI(CX plugin) {
    this.plugin = plugin;
    this.data = plugin.getFH().getData();
  }
  
  @NotNull
  public String getIdentifier() {
    return "cx";
  }
  
  @NotNull
  public String getAuthor() {
    return String.join(", ", this.plugin.getDescription().getAuthors());
  }
  
  @NotNull
  public String getVersion() {
    return this.plugin.getDescription().getVersion();
  }
  
  public boolean persist() {
    return true;
  }
  
  public boolean canRegister() {
    return true;
  }
  
  public String onPlaceholderRequest(Player player, @NotNull String identifier) {
    List<String> users;
    long online;
    if (player == null)
      return "no player"; 
    Econo econ = CX.getEcon();
    String clanName = getPlayerClan(player.getName());
    if (clanName == null)
      return "no clan"; 
    switch (identifier.toLowerCase()) {
      case "prefix":
        return CX.prefix;
      case "player_money":
        return String.valueOf(econ.getBalance((OfflinePlayer)player));
      case "clan_leader":
        return this.data.getString("Clans." + clanName + ".Leader", "N/A");
      case "clan_founder":
        return this.data.getString("Clans." + clanName + ".Founder", "N/A");
      case "clan_name":
        return clanName;
      case "clan_money":
        return this.data.getString("Clans." + clanName + ".Money");
      case "clan_membercount":
        users = this.data.getStringList("Clans." + clanName + ".Users");
        return Integer.toString(users.size());
      case "clan_membercount_online":
        users = this.data.getStringList("Clans." + clanName + ".Users");
        online = Bukkit.getOnlinePlayers().stream().filter(p -> users.contains(p.getName())).count();
        return Long.toString(online);
      case "clan_membercount_offline":
        users = this.data.getStringList("Clans." + clanName + ".Users");
        online = Bukkit.getOnlinePlayers().stream().filter(p -> users.contains(p.getName())).count();
        return Long.toString(users.size() - online);
    } 
    return "&5&lEnttbot&d&lX";
  }
  
  private String getPlayerClan(String playerName) {
    if (playerName == null)
      return null; 
    playerName = playerName.trim().toLowerCase();
    ConfigurationSection clans = this.data.getConfigurationSection("Clans");
    if (clans == null)
      return null; 
    for (String clan : clans.getKeys(false)) {
      List<String> users = this.data.getStringList("Clans." + clan + ".Users");
      for (String user : users) {
        if (user != null && user.trim().toLowerCase().equals(playerName))
          return clan; 
      } 
    } 
    return null;
  }
  
  public void registerPlaceholders() {
    if (!isRegistered()) {
      if (!register()) {
        this.plugin.getLogger().warning("Failed to register ClansX placeholders.");
      } else {
        this.plugin.getLogger().info("Successfully registered ClansX placeholders.");
      } 
    } else {
      this.plugin.getLogger().info("ClansX placeholders already registered.");
    } 
  }
}
