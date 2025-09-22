package x.Entt.ClansX.CMDs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;

public class ACMD implements CommandExecutor, TabCompleter {
  private final CX plugin;
  
  public ACMD(CX plugin) {
    this.plugin = plugin;
  }
  
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
    if (!(sender instanceof Player))
      return handleConsole(sender, args); 
    if (!sender.hasPermission("cx.admin")) {
      sender.sendMessage(MSG.color(CX.prefix + "&cYou don't have permission to use this command."));
      return true;
    } 
    if (args.length == 0) {
      help(sender);
      return true;
    } 
    switch (args[0].toLowerCase()) {
      case "reload":
        reload(sender);
        return true;
      case "ban":
        ban(sender, args);
        return true;
      case "unban":
        unban(sender, args);
        return true;
      case "clear":
        clear(sender);
        return true;
      case "reports":
        reports(sender);
        return true;
      case "economy":
        economy(sender, args);
        return true;
    } 
    help(sender);
    return true;
  }
  
  private boolean handleConsole(CommandSender sender, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      reload(sender);
    } else {
      sender.sendMessage(MSG.color(CX.prefix + "&cConsole can only use: &f/clx reload"));
    } 
    return true;
  }
  
  private void help(CommandSender sender) {
    sender.sendMessage(MSG.color("&8\n&3&l=== &b&lClansX Admin Help &3&l===\n&e/clx reports &7- &fShow all clans with reports.\n&e/clx reload &7- &fReload all plugin files.\n&e/clx ban <clan> [reason] &7- &fBan a clan (perm by default).\n&e/clx unban <clan> &7- &fUnban a clan.\n&e/clx clear &7- &cWipe the entire Data.yml (⚠ Use with caution).\n&e/clx economy <player|clan> <name> <set|add|reset> <amount>\n&3&l==============================\n"));
  }
  
  private void reload(CommandSender sender) {
    FileHandler fh = this.plugin.getFH();
    Econo econ = CX.getEcon();
    fh.reloadConfig();
    fh.reloadData();
    fh.reloadPStats();
    econ.reload();
    sender.sendMessage(MSG.color(CX.prefix + "&aPlugin and all files reloaded."));
  }
  
  private void clear(CommandSender sender) {
    FileHandler fh = this.plugin.getFH();
    fh.getData().set("Clans", null);
    fh.saveData();
    sender.sendMessage(MSG.color(CX.prefix + "&cData.yml cleared."));
  }
  
  private void reports(CommandSender sender) {
    FileConfiguration data = this.plugin.getFH().getData();
    if (!data.contains("Clans")) {
      sender.sendMessage(MSG.color(CX.prefix + "&cNo clans found."));
      return;
    } 
    Map<String, List<String>> reported = new HashMap<>();
    for (String clan : ((ConfigurationSection)Objects.<ConfigurationSection>requireNonNull(data.getConfigurationSection("Clans"))).getKeys(false)) {
      List<String> r = data.getStringList("Clans." + clan + ".Reports");
      if (!r.isEmpty())
        reported.put(clan, r); 
    } 
    if (reported.isEmpty()) {
      sender.sendMessage(MSG.color(CX.prefix + "&aNo clans with reports."));
      return;
    } 
    sender.sendMessage(MSG.color("&e--- &6Clan Reports &e---"));
    reported.forEach((clan, reasons) -> {
          sender.sendMessage(MSG.color("&6" + clan + ":"));
          reasons.forEach(());
        });
  }
  
  private void ban(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(MSG.color(CX.prefix + "&cUsage: /clx ban <clan> [reason]"));
      return;
    } 
    String clan = args[1];
    String reason = (args.length >= 3) ? args[2] : "Banned by admin";
    FileConfiguration data = this.plugin.getFH().getData();
    if (!data.contains("Clans." + clan)) {
      sender.sendMessage(MSG.color(CX.prefix + "&cClan '" + CX.prefix + "' doesn't exist."));
      return;
    } 
    List<String> members = data.getStringList("Clans." + clan + ".Users");
    for (String member : members) {
      Player player = Bukkit.getPlayer(member);
      if (player != null) {
        player.kickPlayer(MSG.color("&cYou have been banned from your clan."));
        player.ban(reason, (Date)null, "ClansX", true);
      } 
    } 
    sender.sendMessage(MSG.color(CX.prefix + "&cClan '" + CX.prefix + "' has been banned."));
  }
  
  private void economy(CommandSender sender, String[] args) {
    if (args.length < 5) {
      sender.sendMessage(MSG.color(CX.prefix + "&cUsage: /clx economy <player|clan> <name> <set|add|reset> <amount>"));
      return;
    } 
    String type = args[1].toLowerCase();
    String name = args[2];
    String action = args[3].toLowerCase();
    String amountStr = args[4];
    Econo econ = CX.getEcon();
    double amount = 0.0D;
    if (!action.equals("reset"))
      try {
        amount = Double.parseDouble(amountStr);
        if (amount < 0.0D)
          throw new NumberFormatException(); 
      } catch (NumberFormatException e) {
        sender.sendMessage(MSG.color(CX.prefix + "&cInvalid amount."));
        return;
      }  
    if (type.equals("player")) {
      OfflinePlayer player = Bukkit.getOfflinePlayer(name);
      if (!player.hasPlayedBefore() && !player.isOnline()) {
        sender.sendMessage(MSG.color(CX.prefix + "&cPlayer '" + CX.prefix + "' not found."));
        return;
      } 
      modifyPlayerEcon(sender, econ, player, action, amount);
      return;
    } 
    if (type.equals("clan")) {
      FileConfiguration data = this.plugin.getFH().getData();
      if (!data.contains("Clans." + name)) {
        sender.sendMessage(MSG.color(CX.prefix + "&cClan '" + CX.prefix + "' doesn't exist."));
        return;
      } 
      String path = "Clans." + name + ".Money";
      double current = data.getDouble(path);
      switch (action) {
        case "set":
          data.set(path, Double.valueOf(amount));
          break;
        case "add":
          data.set(path, Double.valueOf(current + amount));
          break;
        case "reset":
          data.set(path, Integer.valueOf(0));
          break;
        default:
          sender.sendMessage(MSG.color(CX.prefix + "&cInvalid action."));
          return;
      } 
      this.plugin.getFH().saveData();
      sender.sendMessage(MSG.color(CX.prefix + "&aClan economy updated: &f" + CX.prefix + " &7-> &f" + name + " &7= &f" + action));
      return;
    } 
    sender.sendMessage(MSG.color(CX.prefix + "&cFirst argument must be 'player' or 'clan'."));
  }
  
  private void modifyPlayerEcon(CommandSender sender, Econo econ, OfflinePlayer p, String action, double amount) {
    double current = econ.getBalance(p);
    switch (action) {
      case "set":
        if (amount > current) {
          econ.deposit(p, amount - current);
        } else {
          econ.withdraw(p, current - amount);
        } 
        sender.sendMessage(MSG.color(p.getName() + "&a economy set to &f" + p.getName()));
        return;
      case "add":
        econ.deposit(p, amount);
        sender.sendMessage(MSG.color("&aAdded &f" + amount + "&a to &f" + p.getName()));
        return;
      case "reset":
        econ.withdraw(p, current);
        sender.sendMessage(MSG.color("&aReset &f" + p.getName() + "&a's balance."));
        return;
    } 
    sender.sendMessage(MSG.color(CX.prefix + "&cInvalid action."));
  }
  
  private void unban(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(MSG.color(CX.prefix + "&cUsage: /clx unban <clan>"));
      return;
    } 
    String clan = args[1];
    FileConfiguration data = this.plugin.getFH().getData();
    if (!data.contains("Clans." + clan)) {
      sender.sendMessage(MSG.color(CX.prefix + "&cClan '" + CX.prefix + "' doesn't exist."));
      return;
    } 
    List<String> members = data.getStringList("Clans." + clan + ".Users");
    for (String member : members)
      Bukkit.getBanList(BanList.Type.NAME).pardon(member); 
    sender.sendMessage(MSG.color(CX.prefix + "&aClan '" + CX.prefix + "' has been unbanned."));
  }
  
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
    if (sender instanceof Player) {
      Player p = (Player)sender;
      if (p.hasPermission("cx.admin")) {
        if (args.length == 1)
          return (List<String>)Stream.<String>of(new String[] { "reload", "ban", "unban", "clear", "reports", "economy" }).filter(sub -> sub.startsWith(args[0].toLowerCase()))
            .collect(Collectors.toList()); 
        String arg0 = args[0].toLowerCase();
        if (arg0.equals("economy")) {
          if (args.length == 2)
            return (List<String>)Stream.<String>of(new String[] { "player", "clan" }).filter(opt -> opt.startsWith(args[1].toLowerCase()))
              .collect(Collectors.toList()); 
          if (args.length == 3) {
            if (args[1].equalsIgnoreCase("player"))
              return (List<String>)Arrays.<OfflinePlayer>stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList()); 
            if (args[1].equalsIgnoreCase("clan"))
              return (List<String>)getClanNames().stream()
                .filter(clan -> clan.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList()); 
            return Collections.emptyList();
          } 
          if (args.length == 4)
            return (List<String>)Stream.<String>of(new String[] { "set", "add", "reset" }).filter(opt -> opt.startsWith(args[3].toLowerCase()))
              .collect(Collectors.toList()); 
          if (args.length == 5 && !args[3].equalsIgnoreCase("reset"))
            return (List<String>)Stream.<String>of(new String[] { "100", "500", "1000", "10000" }).filter(a -> a.startsWith(args[4]))
              .collect(Collectors.toList()); 
          return Collections.emptyList();
        } 
        if ((arg0.equals("ban") || arg0.equals("unban")) && args.length == 2)
          return (List<String>)getClanNames().stream()
            .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList()); 
        if (arg0.equals("ban") && args.length == 3)
          return (List<String>)Stream.<String>of(new String[] { "cheating", "toxicity", "abuse" }).filter(reason -> reason.startsWith(args[2].toLowerCase()))
            .collect(Collectors.toList()); 
        return Collections.emptyList();
      } 
    } 
    return (args.length == 1) ? List.of("reload") : Collections.<String>emptyList();
  }
  
  private List<String> getClanNames() {
    FileConfiguration data = this.plugin.getFH().getData();
    if (data.contains("Clans"))
      return new ArrayList<>(((ConfigurationSection)Objects.<ConfigurationSection>requireNonNull(data.getConfigurationSection("Clans"))).getKeys(false)); 
    return Collections.emptyList();
  }
}
