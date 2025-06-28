package x.Entt.ClansX.CMDs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import static x.Entt.ClansX.CX.prefix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PECMD implements CommandExecutor, TabCompleter {

    private final CX plugin;
    private static FileHandler fh;
    private final Econo econ;

    public PECMD(CX plugin) {
        this.plugin = plugin;
        this.econ = CX.getEcon();
        fh = plugin.getFH();
    }

    public static void addClanToHistory(OfflinePlayer player, String newClan) {
        String key = player.getUniqueId().toString();
        List<String> history = fh.getPStats().getStringList(key + ".clanHistory");

        if (!history.contains(newClan)) {
            history.add(newClan);
            fh.getPStats().set(key + ".clanHistory", history);
        }

        fh.getPStats().set(key + ".currentClan", newClan);
        fh.savePStats();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE: &f/cxstats <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String key = target.getUniqueId().toString();
        double money = econ.getBalance(target);
        String currentClan = fh.getPStats().getString(key + ".currentClan", "No clan");
        List<String> history = fh.getPStats().getStringList(key + ".clanHistory");

        sender.sendMessage(MSG.color("&6======= &aStatistics of &e" + target.getName() + " &6======="));
        sender.sendMessage(MSG.color("&eMoney: &a" + money));
        sender.sendMessage(MSG.color("&eCurrent Clan: &a" + currentClan));
        sender.sendMessage(MSG.color("&eClan History:"));
        if (history.isEmpty()) {
            sender.sendMessage(MSG.color("&7No history found."));
        } else {
            for (String clan : history) {
                sender.sendMessage(MSG.color("&7- " + clan));
            }
        }
        sender.sendMessage(MSG.color("&6=============================="));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    players.add(p.getName());
                }
            });
            return players;
        }
        return Collections.emptyList();
    }
}