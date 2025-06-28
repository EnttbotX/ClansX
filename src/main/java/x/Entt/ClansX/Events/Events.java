package x.Entt.ClansX.Events;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import static x.Entt.ClansX.CX.prefix;

import java.util.*;

public class Events implements Listener {
    private final CX plugin;

    public Events(CX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileHandler fh = plugin.getFH();
        FileConfiguration config = fh.getConfig();
        FileConfiguration data = fh.getData();

        if (!config.getBoolean("welcome-message.enabled")) return;

        String clan = getPlayerClan(player.getName());

        if (clan == null) {
            config.getStringList("welcome-message.no-clan").forEach(
                    msg -> player.sendMessage(MSG.color(msg))
            );
        } else {
            List<String> users = data.getStringList("Clans." + clan + ".Users");
            for (String u : users) {
                Player p = Bukkit.getPlayerExact(u);
                if (p != null) {
                    p.isOnline();
                }
            }

            for (String line : config.getStringList("welcome-message.self-clan")) {
                player.sendMessage(MSG.color(player, line));
            }

            for (String u : users) {
                Player target = Bukkit.getPlayerExact(u);
                if (target != null && target.isOnline() && !target.getName().equalsIgnoreCase(player.getName())) {
                    for (String line : config.getStringList("welcome-message.to-clan")) {
                        target.sendMessage(MSG.color(player, line));
                    }
                }
            }
        }

        List<String> invites = new ArrayList<>();
        if (data.isConfigurationSection("Clans")) {
            for (String id : Objects.requireNonNull(data.getConfigurationSection("Clans")).getKeys(false)) {
                List<String> list = data.getStringList("Clans." + id + ".Invitations");
                if (list.stream().anyMatch(inv -> inv.equalsIgnoreCase(player.getName()))) invites.add(id);
            }
        }

        if (!invites.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&eYou are invited to these clans:"));
            invites.forEach(c -> player.sendMessage(MSG.color("&7- &a" + c + " &7(/cl join " + c + ")")));
        }
    }

    @EventHandler
    public void RegisterPlayerDataOnJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileHandler fh = plugin.getFH();

        FileConfiguration pData = fh.getPStats();
         if(!pData.contains(player.getUniqueId().toString())) {
            pData.set(player.getUniqueId() + ".clanHistory", null);
            pData.set(player.getUniqueId() + ".currentClan", null);
            fh.savePStats();
         }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        Econo econ = CX.getEcon();
        FileHandler fh = plugin.getFH();
        int killReward = fh.getConfig().getInt("economy.earn.kill-enemy");

        if (fh.getConfig().getBoolean("economy.enabled")) {
            Player victim = event.getEntity();
            Player killer = victim.getKiller();

            if (killer != null) {
                econ.deposit(killer, killReward);
                killer.sendMessage(MSG.color(prefix + "&2You Won: &e&l" + killReward));
            }
        }
    }

    private String getPlayerClan(String playerName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        ConfigurationSection clans = data.getConfigurationSection("Clans");
        if (clans != null) {
            for (String clan : clans.getKeys(false)) {
                List<String> users = data.getStringList("Clans." + clan + ".Users");
                if (users.contains(playerName)) return clan;
            }
        }
        return null;
    }
}