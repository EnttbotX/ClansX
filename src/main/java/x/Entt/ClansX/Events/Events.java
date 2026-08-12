package x.Entt.ClansX.Events;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.*;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.*;

import static x.Entt.ClansX.CX.prefix;
import static x.Entt.ClansX.Utils.MSG.Paths.*;

public class Events implements Listener {
    private final CX plugin;
    private final FileHandler fh;

    public Events(CX plugin) {
        this.plugin = plugin;
        this.fh = plugin.getFH();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        FileConfiguration config = fh.getConfig();
        FileConfiguration pData = fh.getPlayerData(player.getUniqueId());

        String clan = plugin.getPlayerClan(player);

        if (config.getBoolean("welcome-message.enabled")) {
            if (clan == null || clan.isEmpty()) {
                config.getStringList("welcome-message.no-clan")
                        .forEach(m -> player.sendMessage(Text.color(m)));
            } else {
                FileConfiguration clanCfg = fh.getClan(clan);
                if (clanCfg != null) {
                    config.getStringList("welcome-message.self-clan")
                            .forEach(m -> player.sendMessage(Text.color(m)));

                    for (String member : clanCfg.getStringList("members")) {
                        Player t = Bukkit.getPlayerExact(member);
                        if (t != null && !t.equals(player)) {
                            config.getStringList("welcome-message.to-clan")
                                    .forEach(m -> t.sendMessage(Text.color(m)));
                        }
                    }
                }
            }
        }

        if (!player.getName().equals(pData.getString("name"))) {
            pData.set("name", player.getName());
            plugin.getFH().savePlayerData(player.getUniqueId(), pData);
        }

        List<String> invites = new ArrayList<>();
        long now = System.currentTimeMillis();
        long expTime = TimeMng.parseToMillis(Objects.requireNonNull(config.getString("clans.invites-expiration-time")));

        File[] files = fh.getClansDir().listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String clanName = f.getName().replace(".yml", "");
                FileConfiguration c = fh.getClan(clanName);
                if (c == null) continue;

                ConfigurationSection inv = c.getConfigurationSection("invitations");
                if (inv == null) continue;

                long time = inv.getLong(player.getName(), -1);
                if (time == -1) continue;

                if (now > time + expTime) {
                    c.set("invitations." + player.getName(), null);
                    fh.saveClan(clanName, c);
                    continue;
                }

                invites.add(c.getString("name", clanName));
            }
        }

        if (config.getBoolean("updater.enabled")) {
            if (player.hasPermission(Objects.requireNonNull(config.getString("updater.advert-permission")))) {
                if (plugin.getUpdater().newUpdateAvailable()) {
                    TextComponent link = new TextComponent(Text.color("&e&l[ DOWNLOAD ]"));
                    link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, plugin.getUpdater().getResourceURL()));

                    player.sendMessage(Text.color(""));
                    player.sendMessage(Text.color("&2======== " + prefix + "&2&l========"));
                    player.sendMessage(Text.color("&eNew Update Available!"));
                    player.sendMessage(Text.color("&eVersion: &f" + plugin.getDescription().getVersion()));
                    player.sendMessage(Text.color("&eNew Version: &f" + plugin.getUpdater().getLatestVersion()));
                    player.sendMessage(Text.color("&eDownload Here:"));
                    player.spigot().sendMessage(link);
                    player.sendMessage(Text.color("&2======== " + prefix + "&2&l========"));
                    player.sendMessage(Text.color(""));
                }
            }
        }

        if (!invites.isEmpty()) {
            player.sendMessage(MSG.send(INVITED_TO_CLANS));
            invites.forEach(c ->
                    player.sendMessage(Text.color(MSG.sendWithoutPrefix(INVITED_LIST_ITEM).replace("$$ph1$$", c)))
            );
        }

        WarSystem.deliverPendingNotifications(fh, player);
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        FileConfiguration pData = fh.getPlayerData(killer.getUniqueId());
        String clan = pData.getString("clan");
        if (clan == null || clan.isEmpty()) return;

        FileConfiguration clanCfg = fh.getClan(clan);
        if (clanCfg == null) return;

        clanCfg.set("kills", clanCfg.getInt("kills") + 1);
        fh.saveClan(clan, clanCfg);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        FileHandler fh = plugin.getFH();
        FileConfiguration config = fh.getConfig();

        String dClan = fh.getPlayerData(dead.getUniqueId()).getString("clan");
        String kClan = fh.getPlayerData(killer.getUniqueId()).getString("clan");
        if (dClan == null || kClan == null || dClan.equals(kClan)) return;

        int killPts   = config.getInt("clans.wars.points.kill", 1);
        int deathPts  = config.getInt("clans.wars.points.death", 0);

        for (String warID : fh.getActiveWars()) {
            FileConfiguration war = fh.getWar(warID);
            if (war == null || !war.getBoolean("active")) continue;

            List<String> off = war.getStringList("clans.offensive");
            List<String> def = war.getStringList("clans.defensive");

            if (!(off.contains(kClan) && def.contains(dClan)) &&
                    !(def.contains(kClan) && off.contains(dClan))) continue;

            String kSide = off.contains(kClan) ? "offensive" : "defensive";
            String dSide = off.contains(dClan) ? "offensive" : "defensive";

            FileConfiguration dClanCfg = fh.getClan(dClan);
            boolean leaderKilled = dClanCfg != null
                    && dead.getName().equalsIgnoreCase(dClanCfg.getString("leader"));

            int bonusKillPts = leaderKilled
                    ? config.getInt("clans.wars.points.leader-killed", killPts)
                    : killPts;
            int bonusDeathPts = leaderKilled
                    ? config.getInt("clans.wars.points.leader-death", deathPts)
                    : deathPts;

            war.set("stats." + kSide + ".kills",  war.getInt("stats." + kSide + ".kills") + 1);
            war.set("stats." + kSide + ".points", war.getInt("stats." + kSide + ".points") + bonusKillPts);
            war.set("stats." + dSide + ".deaths", war.getInt("stats." + dSide + ".deaths") + 1);
            if (bonusDeathPts != 0)
                war.set("stats." + dSide + ".points", war.getInt("stats." + dSide + ".points") - bonusDeathPts);

            fh.saveWar(warID, war);
            return;
        }
    }
}