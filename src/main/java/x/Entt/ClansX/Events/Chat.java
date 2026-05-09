package x.Entt.ClansX.Events;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;

import java.util.List;

public class Chat implements Listener {
    private final CX plugin;

    public Chat(CX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        FileHandler fh = plugin.getFH();
        FileConfiguration pData = fh.getPlayerData(player.getUniqueId());

        String clanName = pData.getString("clan");
        if (clanName == null || clanName.isEmpty()) return;

        String symbol = fh.getConfig().getString("clans.messages-to-clan", "@");
        if (symbol.isEmpty()) symbol = "@";

        String message = e.getMessage();
        if (!message.startsWith(symbol)) return;

        e.setCancelled(true);
        message = message.substring(symbol.length()).trim();

        FileConfiguration clan = fh.getClan(clanName);
        if (clan == null) return;

        List<String> users = clan.getStringList("users");
        if (users.isEmpty()) return;

        String role = clan.getString("leader", "").equals(player.getName()) ? "Leader" : "User";

        String format = fh.getConfig().getString("clans.chat-format",
                "&e&l%cx_clan_name% &c[$role$] &f&l%player_name%&r: $message$"
        );

        String chatMessage = MSG.color(format
                .replace("%cx_clan_name%", clanName)
                .replace("$role$", role)
                .replace("%player_name%", player.getName())
                .replace("$message$", message)
        );

        for (String userName : users) {
            Player recipient = Bukkit.getPlayerExact(userName);
            if (recipient != null) recipient.sendMessage(chatMessage);
        }
    }
}