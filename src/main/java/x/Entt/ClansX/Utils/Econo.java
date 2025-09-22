package x.Entt.ClansX.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import x.Entt.ClansX.CX;

public class Econo {
  private final CX plugin;
  
  private String system;
  
  public static Economy vault;
  
  private final Map<UUID, Double> internalBalances = new HashMap<>();
  
  private File balanceFile;
  
  private FileConfiguration balanceCfg;
  
  public Econo(CX plugin) {
    this.plugin = plugin;
  }
  
  public String getSystem() {
    if (this.system == null) {
      FileConfiguration cfg = this.plugin.getConfig();
      this.system = cfg.getString("economy.system", "Vault").toLowerCase();
    } 
    return this.system;
  }
  
  public String reload() {
    if (this.system == null) {
      FileConfiguration cfg = this.plugin.getConfig();
      this.system = cfg.getString("economy.system", "Vault").toLowerCase();
    } 
    return this.system;
  }
  
  public boolean setupEconomy() {
    RegisteredServiceProvider<Economy> provider;
    switch (getSystem()) {
      case "vault":
        provider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        vault = (provider != null) ? (Economy)provider.getProvider() : null;
        if (vault != null)
          return true; 
        Bukkit.getConsoleSender().sendMessage(MSG.color(CX.prefix + "&cVault provider is null, using Internal Econ."));
        this.system = "internal";
        loadInternal();
        return true;
      case "internal":
        loadInternal();
        return true;
    } 
    Bukkit.getConsoleSender().sendMessage(MSG.color(CX.prefix + "&cEconomy system is null!: " + CX.prefix));
    return false;
  }
  
  public double getBalance(OfflinePlayer player) {
    switch (getSystem()) {
      case "vault":
      
      case "internal":
      
    } 
    return 
      
      0.0D;
  }
  
  public void deposit(OfflinePlayer player, double amount) {
    if (amount < 0.0D)
      return; 
    switch (getSystem()) {
      case "vault":
        vault.depositPlayer(player, amount).transactionSuccess();
        return;
      case "internal":
        this.internalBalances.merge(player.getUniqueId(), Double.valueOf(amount), Double::sum);
        saveInternal();
        return;
    } 
  }
  
  public void withdraw(OfflinePlayer player, double amount) {
    UUID id;
    double current;
    if (amount < 0.0D)
      return; 
    switch (getSystem()) {
      case "vault":
        vault.withdrawPlayer(player, amount).transactionSuccess();
        return;
      case "internal":
        id = player.getUniqueId();
        current = ((Double)this.internalBalances.getOrDefault(id, Double.valueOf(0.0D))).doubleValue();
        if (current < amount)
          return; 
        this.internalBalances.put(id, Double.valueOf(current - amount));
        saveInternal();
        return;
    } 
  }
  
  public boolean has(OfflinePlayer player, double amount) {
    if (amount < 0.0D)
      return false; 
    switch (getSystem()) {
      case "vault":
      
      case "internal":
        return 
          
          (((Double)this.internalBalances.getOrDefault(player.getUniqueId(), Double.valueOf(0.0D))).doubleValue() >= amount);
    } 
    return false;
  }
  
  private void loadInternal() {
    this.balanceFile = new File(this.plugin.getDataFolder(), "balances.yml");
    if (!this.balanceFile.exists()) {
      this.balanceFile.getParentFile().mkdirs();
      try {
        this.balanceFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      } 
    } 
    this.balanceCfg = (FileConfiguration)YamlConfiguration.loadConfiguration(this.balanceFile);
    for (String key : this.balanceCfg.getKeys(false)) {
      try {
        UUID id = UUID.fromString(key);
        double money = this.balanceCfg.getDouble(key + ".Money", 0.0D);
        this.internalBalances.put(id, Double.valueOf(money));
      } catch (IllegalArgumentException illegalArgumentException) {}
    } 
  }
  
  private void saveInternal() {
    if (this.balanceCfg == null)
      return; 
    for (String key : this.balanceCfg.getKeys(false))
      this.balanceCfg.set(key, null); 
    for (Map.Entry<UUID, Double> entry : this.internalBalances.entrySet()) {
      UUID uuid = entry.getKey();
      double bal = ((Double)entry.getValue()).doubleValue();
      String name = Bukkit.getOfflinePlayer(uuid).getName();
      if (name == null)
        name = "Unknown"; 
      this.balanceCfg.set(String.valueOf(uuid) + ".Name", name);
      this.balanceCfg.set(String.valueOf(uuid) + ".Money", Double.valueOf(bal));
    } 
    try {
      this.balanceCfg.save(this.balanceFile);
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
}
