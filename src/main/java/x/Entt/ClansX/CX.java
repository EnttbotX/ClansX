package x.Entt.ClansX;

import java.util.Objects;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import x.Entt.ClansX.CMDs.ACMD;
import x.Entt.ClansX.CMDs.CCMD;
import x.Entt.ClansX.CMDs.PECMD;
import x.Entt.ClansX.Events.Events;
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import x.Entt.ClansX.Utils.Metrics;
import x.Entt.ClansX.Utils.PAPI;
import x.Entt.ClansX.Utils.Updater;

public class CX extends JavaPlugin {
  public String version = getDescription().getVersion();
  
  public static String prefix;
  
  public static Econo econ;
  
  private Updater updater;
  
  private Metrics metrics;
  
  private FileHandler fh;
  
  public void onEnable() {
    saveDefaultConfig();
    prefix = MSG.color(getConfig().getString("prefix", "&3&L[Clans&b&lX&3&l] "));
    this.fh = new FileHandler(this);
    this.updater = new Updater(this, 114316);
    this.metrics = new Metrics((Plugin)this, 20912);
    econ = new Econo(this);
    if (getConfig().getBoolean("economy.enabled", true) && 
      !econ.setupEconomy()) {
      getLogger().severe("Can´t load the economy system.");
      this.fh.getConfig().set("economy.enabled", Boolean.valueOf(false));
      this.fh.saveConfig();
      getLogger().severe("Economy system disabled.");
      return;
    } 
    this.fh.saveDefaults();
    setupMetrics();
    registerCommands();
    registerEvents();
    searchUpdates();
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
      (new PAPI(this)).registerPlaceholders(); 
    Bukkit.getConsoleSender().sendMessage(MSG.color("&av" + getDescription().getVersion() + " &2Enabled!"));
  }
  
  public void onDisable() {
    Bukkit.getConsoleSender().sendMessage(MSG.color("&av" + getDescription().getVersion() + " &cDisabled"));
  }
  
  private void setupMetrics() {
    int max = getConfig().getInt("max-clans", -1);
    String maxClans = (max <= 0) ? "Unlimited" : String.valueOf(max);
    this.metrics.addCustomChart((Metrics.CustomChart)new Metrics.SimplePie("economy_enabled", () -> String.valueOf(getConfig().getBoolean("economy.enabled", true))));
    this.metrics.addCustomChart((Metrics.CustomChart)new Metrics.SimplePie("economy_system", () -> getConfig().getString("economy.system", "Unknown")));
    this.metrics.addCustomChart((Metrics.CustomChart)new Metrics.SimplePie("max_clans", () -> maxClans));
  }
  
  private void registerCommands() {
    ((PluginCommand)Objects.<PluginCommand>requireNonNull(getCommand("clansx"))).setExecutor((CommandExecutor)new ACMD(this));
    ((PluginCommand)Objects.<PluginCommand>requireNonNull(getCommand("clans"))).setExecutor((CommandExecutor)new CCMD(this));
    ((PluginCommand)Objects.<PluginCommand>requireNonNull(getCommand("cxstats"))).setExecutor((CommandExecutor)new PECMD(this));
  }
  
  private void registerEvents() {
    getServer().getPluginManager().registerEvents((Listener)new Events(this), (Plugin)this);
  }
  
  public void searchUpdates() {
    String downloadUrl = "https://www.spigotmc.org/resources/clansx-the-best-clan-system-1-8-1-21.114316/";
    TextComponent link = new TextComponent(MSG.color("&e&lClick here to download the update!"));
    link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));
    boolean updateAvailable = false;
    String latestVersion = "unknown";
    try {
      this.updater = new Updater(this, 114316);
      updateAvailable = this.updater.isUpdateAvailable();
      latestVersion = this.updater.getLatestVersion();
    } catch (Exception e) {
      Bukkit.getConsoleSender().sendMessage(MSG.color("&cError checking for updates: " + e.getMessage()));
    } 
    if (updateAvailable) {
      Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============= " + prefix + "&2&l============="));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&6&lNEW VERSION AVAILABLE!"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lCurrent Version: &f" + this.version));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lLatest Version: &f" + latestVersion));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lDownload it here: &f" + downloadUrl));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============= " + prefix + "&2&l============="));
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.hasPermission("cx.admin")) {
          player.sendMessage(MSG.color(prefix + "&e&lA new plugin update is available!"));
          player.spigot().sendMessage((BaseComponent)link);
        } 
      } 
    } 
  }
  
  public static Econo getEcon() {
    return econ;
  }
  
  public FileHandler getFH() {
    return this.fh;
  }
}
