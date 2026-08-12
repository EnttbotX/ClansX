package x.Entt.ClansX.CMDs;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Events.ClanChest;
import x.Entt.ClansX.Events.Relations;
import x.Entt.ClansX.Events.Wars;
import x.Entt.ClansX.Utils.*;

import static x.Entt.ClansX.CX.econ;
import static x.Entt.ClansX.Utils.MSG.Paths.*;

public class CCMD implements CommandExecutor, TabCompleter {
    private final CX plugin;

    public CCMD(CX plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        String clanToReport, reason;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.send(CONSOLE));
            return true;
        }

        String playerClan = plugin.getPlayerClan(player);
        if (args.length < 1) {
            help(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("cx.user.create")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                if (playerClan != null && !playerClan.isEmpty()) {
                    player.sendMessage(MSG.send(ALREADY_IN_CLAN));
                    return true;
                }
                create(player, args);
                return true;
            case "disband":
                if (!sender.hasPermission("cx.user.disband")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                disband(player, playerClan);
                return true;
            case "invite":
                if (!sender.hasPermission("cx.user.invite")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls invite <player>"));
                    return true;
                }
                invite(player, args[1]);
                return true;
            case "kick":
                if (!sender.hasPermission("cx.user.expel")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                expel(player, args);
                return true;
            case "join":
                if (!sender.hasPermission("cx.user.join")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls join <clan>"));
                    return true;
                }
                join(player, args[1]);
                return true;
            case "leave":
                if (!sender.hasPermission("cx.user.leave")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                leave(player);
                return true;
            case "list":
                if (!sender.hasPermission("cx.user.list")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                list(player);
                return true;
            case "report":
                if (!sender.hasPermission("cx.user.report")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls report <clan> <reason>"));
                    return true;
                }
                clanToReport = args[1];
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                report(player, clanToReport, reason);
                return true;
            case "edit":
                if (!sender.hasPermission("cx.user.edit")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                edit(player, playerClan, args);
                return true;
            case "eco":
                if (!sender.hasPermission("cx.user.economy")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                economy(player, playerClan, args);
                return true;
            case "stats":
                if (!sender.hasPermission("cx.user.stats")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.send(NOT_IN_CLAN));
                    return true;
                }
                stats(player, playerClan);
                return true;
            case "resign":
                if (!sender.hasPermission("cx.user.resign")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                resign(player, playerClan);
                return true;
            case "war":
                if (!sender.hasPermission("cx.user.war")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                war(player, Arrays.copyOfRange(args, 1, args.length));
                return true;
            case "chest":
                if (!sender.hasPermission("cx.user.chest")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                chest(player, playerClan);
                return true;
            case "relations":
                if (!sender.hasPermission("cx.user.relations")) {
                    player.sendMessage(MSG.send(NO_PERMS));
                    return true;
                }
                relations(player);
                return true;
            default:
                help(player);
                break;
        }

        return true;
    }

    public void help(Player player) {
        player.sendMessage(MSG.sendList(USERS_HELP).toArray(new String[0]));
    }

    public void war(Player player, String[] args) {
        String pClan = plugin.getPlayerClan(player);

        if (args.length < 1) {
            new Wars(plugin, player);
            return;
        }

        if (pClan == null || pClan.isEmpty()) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        WarSystem ws = plugin.getWarSystem();

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length != 2) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls war start <clan>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.send(NOT_LEADER));
                    return;
                }
                ws.startWar(player, pClan, args[1]);
            }

            case "join" -> {
                if (args.length != 3 ||
                        (!args[2].equalsIgnoreCase("off") && !args[2].equalsIgnoreCase("def"))) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls war join <warID> <off|def>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.send(NOT_LEADER));
                    return;
                }
                ws.joinToWar(player, args[1], args[2].equalsIgnoreCase("def"));
            }

            case "peace" -> {
                if (args.length < 3) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls war peace <warID> <terms...>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.send(NOT_LEADER));
                    return;
                }
                ws.sendPeaceTerms(player, args[1],
                        java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 2, args.length)));
            }

            case "accept" -> {
                if (args.length != 3) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls war accept <warID> <offerID>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.send(NOT_LEADER));
                    return;
                }
                try {
                    ws.acceptPeace(player, args[1], Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    player.sendMessage(MSG.send(OFFER_ID_NOT_NUMBER));
                }
            }

            case "reject" -> {
                if (args.length != 3) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls war reject <warID> <offerID>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.send(NOT_LEADER));
                    return;
                }
                try {
                    ws.rejectPeace(player, args[1], Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    player.sendMessage(MSG.send(OFFER_ID_NOT_NUMBER));
                }
            }

            case "leave" -> {
                if (args.length != 2) {
                    player.sendMessage(MSG.send(SHOULD_USE, "/cls war leave <warID>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.send(NOT_LEADER));
                    return;
                }
                ws.leaveWar(player, args[1]);
            }

            default -> new x.Entt.ClansX.Events.Wars(plugin, player);
        }
    }

    public void chest(Player player, String clan) {
        FileHandler fh = plugin.getFH();

        String playerClan = plugin.getPlayerClan(player);
        if (playerClan == null || !playerClan.equalsIgnoreCase(clan)) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        if (!player.hasPermission("cx.user.chest")) {
            player.sendMessage(MSG.send(NO_PERMS));
            return;
        }

        FileConfiguration cfg = fh.getClan(clan);
        if (cfg == null) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        Inventory chest = Bukkit.createInventory(null, 54, Text.color("&8Clan Chest: &e" + clan));

        List<?> raw = cfg.getList("clanChest");
        if (raw != null) {
            for (int i = 0; i < raw.size() && i < chest.getSize(); i++) {
                if (raw.get(i) instanceof ItemStack item) {
                    chest.setItem(i, item);
                }
            }
        }

        Bukkit.getPluginManager().registerEvents(
                new ClanChest(plugin, player, clan, chest),
                plugin
        );

        player.openInventory(chest);
    }

    public void expel(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(MSG.send(SHOULD_USE, "/cls kick <player>"));
            return;
        }

        String targetName = args[1];
        FileHandler fh = plugin.getFH();

        String clanName = plugin.getPlayerClan(player);
        if (clanName == null || clanName.isEmpty()) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (clan == null) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        String leader = clan.getString("leader");

        if (!player.getName().equalsIgnoreCase(leader)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target.getName() == null) {
            player.sendMessage(MSG.send(PLAYER_NOT_FOUND));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MSG.send(CANNOT_EXPEL_SELF));
            return;
        }

        List<String> users = clan.getStringList("users");

        String targetUser = users.stream()
                .filter(name -> name.equalsIgnoreCase(target.getName()))
                .findFirst()
                .orElse(null);

        if (targetUser == null) {
            player.sendMessage(MSG.send(PLAYER_NOT_IN_CLAN));
            return;
        }

        users.remove(targetUser);
        clan.set("users", users);
        fh.saveClan(clanName, clan);

        FileConfiguration pData = fh.getPlayerData(target.getUniqueId());
        pData.set("clan", null);
        fh.savePlayerData(target.getUniqueId(), pData);

        player.sendMessage(MSG.send(PLAYER_EXPELLED, target.getName()));

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            onlineTarget.sendMessage(MSG.send(YOU_WERE_EXPELLED));
        }
    }

    public void resign(Player player, String clanName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        if (clan == null || !clan.isSet("name")) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        String leader = clan.getString("leader");

        if (!player.getName().equalsIgnoreCase(leader)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        List<String> users = clan.getStringList("users");

        List<String> otherMembers = users.stream()
                .filter(name -> !name.equalsIgnoreCase(player.getName()))
                .toList();

        if (otherMembers.isEmpty()) {
            player.sendMessage(MSG.send(CANNOT_RESIGN));
            return;
        }

        String newLeaderName = otherMembers.get(new Random().nextInt(otherMembers.size()));
        OfflinePlayer newLeader = Bukkit.getOfflinePlayer(newLeaderName);

        clan.set("leader", newLeader.getName());
        fh.saveClan(clanName, clan);

        player.sendMessage(MSG.send(RESIGN, newLeader.getName()));

        for (String userName : users) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(userName);
            Player onlineMember = member.getPlayer();

            if (onlineMember != null && !onlineMember.getUniqueId().equals(newLeader.getUniqueId())) {
                onlineMember.sendMessage(MSG.send(NEW_LEADER, player.getName(), newLeader.getName()));
            }
        }

        Player onlineNewLeader = newLeader.getPlayer();

        if (onlineNewLeader != null) {
            onlineNewLeader.sendMessage(MSG.send(NEW_LEADER_ADVICE, clanName));
        }
    }

    public void stats(Player player, String clanName) {
        FileHandler fh = this.plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        if (clan == null || !clan.contains("name")) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        String founder = clan.getString("founder", "???");
        String leader = clan.getString("leader", "???");
        String privacy = clan.getString("privacy", "Public");
        double balance = clan.getDouble("balance", 0.0);

        List<String> users = clan.getStringList("users");
        List<String> invites = clan.getStringList("invitations");

        List<String> warsActive = clan.getStringList("wars.active");
        List<String> warsWon = clan.getStringList("wars.won");
        List<String> warsDefeated = clan.getStringList("wars.defeated");
        int streak = clan.getInt("wars.streak", 0);
        int kills = clan.getInt("kills", 0);

        String none = MSG.sendWithoutPrefix(STATS_NONE);
        String listItem = MSG.sendWithoutPrefix(STATS_LIST_ITEM);

        player.sendMessage(MSG.sendWithoutPrefix(STATS_HEADER));
        player.sendMessage(MSG.sendWithoutPrefix(STATS_NAME) + clanName);
        player.sendMessage(MSG.sendWithoutPrefix(STATS_FOUNDER) + founder);
        player.sendMessage(MSG.sendWithoutPrefix(STATS_LEADER) + leader);
        player.sendMessage(MSG.sendWithoutPrefix(STATS_PRIVACY) + privacy);
        player.sendMessage(MSG.sendWithoutPrefix(STATS_BALANCE) + balance);

        player.sendMessage(MSG.sendWithoutPrefix(STATS_MEMBERS));
        if (users.isEmpty()) player.sendMessage(Text.color(listItem + none));
        else users.forEach(u -> player.sendMessage(Text.color(listItem + u)));

        player.sendMessage(MSG.sendWithoutPrefix(STATS_INVITED));
        if (invites.isEmpty()) player.sendMessage(Text.color(listItem + none));
        else invites.forEach(i -> player.sendMessage(Text.color(listItem + i)));

        player.sendMessage(MSG.sendWithoutPrefix(STATS_WARS));
        player.sendMessage(MSG.sendWithoutPrefix(STATS_ACTIVE_WARS) + (warsActive.isEmpty() ? none : String.join(", ", warsActive)));
        player.sendMessage(MSG.sendWithoutPrefix(STATS_WON_WARS) + (warsWon.isEmpty() ? none : String.join(", ", warsWon)));
        player.sendMessage(MSG.sendWithoutPrefix(STATS_DEF_WARS) + (warsDefeated.isEmpty() ? none : String.join(", ", warsDefeated)));
        player.sendMessage(MSG.sendWithoutPrefix(STATS_STREAK) + streak);
        player.sendMessage(MSG.sendWithoutPrefix(STATS_TOTAL_KILLS) + kills);

        player.sendMessage(MSG.send(STATS_FOOTER));
    }

    private void economy(Player player, String clanName, String[] args) {
        if (args.length != 3) {
            player.sendMessage(MSG.send(SHOULD_USE, "/cls eco <deposit | withdraw> <amount>"));
            return;
        }

        String playerClan = plugin.getPlayerClan(player);
        if (playerClan == null || !playerClan.equalsIgnoreCase(clanName)) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        String type = args[1].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(MSG.send(INVALID_AMOUNT));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(MSG.send(AMOUNT_MUST_BE_POSITIVE));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);
        Econo econ = CX.getEcon();
        double clanBalance = clan.getDouble("balance");

        switch (type) {
            case "deposit":
                double playerBalance = econ.getBalance(player);
                if (playerBalance < amount) {
                    player.sendMessage(MSG.send(NOT_ENOUGH_MONEY));
                    return;
                }

                econ.withdraw(player, amount);
                clan.set("balance", clanBalance + amount);
                fh.saveClan(clanName, clan);
                player.sendMessage(MSG.send(DEPOSITED, String.valueOf(amount), clanName));
                break;

            case "withdraw":
                if (clanBalance < amount) {
                    player.sendMessage(MSG.send(CLAN_NOT_ENOUGH_MONEY));
                    return;
                }

                econ.deposit(player, amount);
                clan.set("balance", clanBalance - amount);
                fh.saveClan(clanName, clan);
                player.sendMessage(MSG.send(WITHDREW, String.valueOf(amount), clanName));
                break;

            default:
                player.sendMessage(MSG.send(INVALID_OPERATION));
                break;
        }
    }

    private void invite(Player player, String playerToInvite) {
        String clanName = plugin.getPlayerClan(player);
        if (clanName == null || clanName.isEmpty()) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        OfflinePlayer invitedPlayer = Bukkit.getOfflinePlayer(playerToInvite);

        if (!invitedPlayer.hasPlayedBefore() && !invitedPlayer.isOnline()) {
            player.sendMessage(MSG.send(PLAYER_NOT_FOUND));
            return;
        }

        if (invitedPlayer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MSG.send(CANNOT_INVITE_SELF));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration pData = fh.getPlayerData(invitedPlayer.getUniqueId());

        if (pData.getString("clan") != null) {
            player.sendMessage(MSG.send(PLAYER_ALREADY_IN_CLAN));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (clan == null) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        List<String> invitations = pData.getStringList("invitations");

        if (!invitations.stream().anyMatch(name -> name.equalsIgnoreCase(invitedPlayer.getName()))) {
            invitations.add(invitedPlayer.getName());
        }

        pData.set("invitations", invitations);
        fh.savePlayerData(invitedPlayer.getUniqueId(), pData);

        player.sendMessage(MSG.send(INVITATION_SENT, invitedPlayer.getName()));

        Player onlinePlayer = invitedPlayer.getPlayer();
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(MSG.send(INVITED_TO_CLAN, clanName));
            onlinePlayer.sendMessage(Text.color(MSG.sendWithoutPrefix(HINT) + "/cls join " + clanName));
        }
    }

    private void leave(Player player) {
        FileHandler fh = plugin.getFH();
        FileConfiguration pData = fh.getPlayerData(player.getUniqueId());
        String clanName = pData.getString("clan");

        if (clanName == null) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (!clan.contains("name")) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            pData.set("clan", null);
            fh.savePlayerData(player.getUniqueId(), pData);
            return;
        }

        List<String> users = clan.getStringList("users");
        users.remove(player.getName());
        clan.set("users", users);

        String leader = clan.getString("leader");
        if (player.getName().equalsIgnoreCase(leader) && !users.isEmpty()) {
            String newLeader = users.get(new Random().nextInt(users.size()));
            clan.set("leader", newLeader);
            player.sendMessage(MSG.send(LEFT_CLAN_NEW_LEADER, newLeader));
        } else if (!player.getName().equalsIgnoreCase(leader)) {
            player.sendMessage(MSG.send(LEFT_CLAN));
        }

        if (users.isEmpty()) {
            if (fh.getConfig().getBoolean("economy.enabled")) {
                int reward = fh.getConfig().getInt("economy.earn.delete-clan", 0);
                if (reward > 0) {
                    CX.getEcon().deposit(player, reward);
                    player.sendMessage(MSG.send(CLAN_DISBANDED_NO_MEMBERS_REWARD, String.valueOf(reward)));
                }
            } else {
                player.sendMessage(MSG.send(CLAN_DISBANDED_NO_MEMBERS));
            }

            plugin.getWarSystem().resolveWarsOnDisband(clanName);
            fh.deleteClan(clanName);
        } else {
            fh.saveClan(clanName, clan);
        }

        pData.set("clan", null);
        fh.savePlayerData(player.getUniqueId(), pData);
    }

    private void join(Player player, String clanName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration pData = fh.getPlayerData(player.getUniqueId());

        String currentClan = pData.getString("clan");

        if (currentClan != null) {
            player.sendMessage(MSG.send(ALREADY_IN_CLAN));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (!clan.contains("name")) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        String privacy = clan.getString("privacy", "Public");
        List<String> users = clan.getStringList("users");

        if (privacy.equalsIgnoreCase("Private")) {
            List<String> invites = clan.getStringList("invitations");
            if (!invites.contains(player.getName())) {
                player.sendMessage(MSG.send(CLAN_IS_PRIVATE));
                return;
            }
            invites.remove(player.getName());
            clan.set("invitations", invites);
        }

        users.add(player.getName());
        clan.set("users", users);
        fh.saveClan(clanName, clan);

        pData.set("clan", clanName);

        List<String> history = pData.getStringList("history");
        if (!history.contains(clanName))
            history.add(clanName);

        pData.set("history", history);
        fh.savePlayerData(player.getUniqueId(), pData);

        player.sendMessage(MSG.send(JOINED_CLAN, clanName));
    }

    public void list(Player player) {
        FileHandler fh = plugin.getFH();
        File[] clans = fh.getClansDir().listFiles((d, n) -> n.endsWith(".yml"));

        if (clans == null || clans.length == 0) {
            player.sendMessage(MSG.send(NO_CLANS_REGISTERED));
            return;
        }

        player.sendMessage(Text.color(""));
        player.sendMessage(MSG.sendWithoutPrefix(LIST_HEADER));

        for (File file : clans) {
            FileConfiguration c = YamlConfiguration.loadConfiguration(file);

            String name = c.getString("name", file.getName().replace(".yml", ""));
            String leader = c.getString("leader", "Unknown");
            int members = c.getStringList("users").size();
            int kills = c.getInt("kills", 0);

            player.sendMessage(MSG.sendWithoutPrefix(LIST_ITEM_FORMAT,
                    name, leader, String.valueOf(members), String.valueOf(kills)));
        }

        player.sendMessage(MSG.sendWithoutPrefix(LIST_FOOTER));
        player.sendMessage(Text.color(""));
    }

    private void report(Player player, String reportedClan, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            player.sendMessage(MSG.send(INVALID_REASON));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(reportedClan);

        if (!clan.contains("name")) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        List<String> reports = clan.getStringList("reports");

        if (reports.contains(reason)) {
            player.sendMessage(MSG.send(REPORT_ALREADY_SUBMITTED));
            return;
        }

        reports.add(reason);
        clan.set("reports", reports);
        fh.saveClan(reportedClan, clan);

        player.sendMessage(MSG.send(CLAN_REPORTED, reportedClan));
    }

    public void edit(Player p, String clanName, String[] args) {
        if (clanName == null || clanName.isEmpty()) {
            p.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        if (!isLeader(p, clanName)) {
            p.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        if (args.length < 3) return;

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        String type = args[1].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        switch (type) {
            case "name": {
                CheckName checker = plugin.getCheckNameTool();
                String newName = checker.validateClanName(value);

                if (newName == null) {
                    p.sendMessage(MSG.send(INVALID_CLAN_NAME));
                    return;
                }

                if (newName.equalsIgnoreCase(clanName)) {
                    p.sendMessage(MSG.send(SAME_NAME));
                    return;
                }

                if (fh.clanExists(newName)) {
                    p.sendMessage(MSG.send(CLAN_NAME_TAKEN));
                    return;
                }

                if (!fh.renameClan(clanName, newName)) {
                    p.sendMessage(MSG.send(RENAME_FAILED));
                    return;
                }

                p.sendMessage(MSG.send(CLAN_RENAMED, newName));
                break;
            }

            case "privacy": {
                if (!(value.equalsIgnoreCase("public") || value.equalsIgnoreCase("private"))) {
                    p.sendMessage(MSG.send(INVALID_PRIVACY_VALUE));
                    return;
                }

                clan.set("privacy", value.toLowerCase());
                fh.saveClan(clanName, clan);
                p.sendMessage(MSG.send(PRIVACY_UPDATED, value));
                break;
            }

            default:
                p.sendMessage(MSG.send(INVALID_EDIT_OPTION));
        }
    }

    public void relations(Player player) {
        String clan = plugin.getPlayerClan(player);

        if (!player.hasPermission("cx.user.relations")) {
            player.sendMessage(MSG.send(NO_PERMS));
            return;
        }
        if (!isInClan(clan)) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }
        if (!isLeader(player, clan)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        new Relations(plugin, player);
    }

    public void disband(Player player, String clanName) {
        if (clanName == null || clanName.isEmpty()) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        if (clan == null) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        if (!player.getName().equalsIgnoreCase(clan.getString("leader"))) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        if (fh.getConfig().getBoolean("economy.enabled")) {
            int reward = fh.getConfig().getInt("economy.earn.delete-clan", 0);
            if (reward > 0) {
                econ.deposit(player, reward);
                player.sendMessage(MSG.send(CLAN_DISBANDED_REWARD, String.valueOf(reward)));
            } else {
                player.sendMessage(MSG.send(CLAN_DISBANDED));
            }
        } else {
            player.sendMessage(MSG.send(CLAN_DISBANDED));
        }

        List<String> users = clan.getStringList("users");

        for (String name : users) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            FileConfiguration pd = fh.getPlayerData(offlinePlayer.getUniqueId());

            pd.set("clan", null);
            fh.savePlayerData(offlinePlayer.getUniqueId(), pd);

            Player onlinePlayer = offlinePlayer.getPlayer();

            if (onlinePlayer != null && !onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                onlinePlayer.sendMessage(MSG.send(CLAN_DISBANDED_NOTIFY, player.getName()));
            }
        }

        plugin.getWarSystem().resolveWarsOnDisband(clanName);
        fh.deleteClan(clanName);
    }

    public void create(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MSG.send(SHOULD_USE, "/cls create <name>"));
            return;
        }

        CheckName checker = plugin.getCheckNameTool();
        String displayName = checker.validateClanName(args[1]);
        if (displayName == null) {
            player.sendMessage(MSG.send(INVALID_CLAN_NAME));
            return;
        }

        String key = displayName.toLowerCase();

        FileHandler fh = plugin.getFH();
        FileConfiguration config = fh.getConfig();
        Econo econ = CX.getEcon();

        int maxClans = config.getInt("clans.max-clans", 0);
        if (maxClans > 0) {
            File[] existing = fh.getClansDir().listFiles((d, n) -> n.endsWith(".yml"));
            int count = existing == null ? 0 : existing.length;
            if (count >= maxClans) {
                player.sendMessage(MSG.send(MAX_CLANS_REACHED, String.valueOf(maxClans)));
                return;
            }
        }

        if (config.getStringList("names-blocked.blocked")
                .stream().map(String::toLowerCase).anyMatch(key::equals)) {
            player.sendMessage(MSG.send(NAME_NOT_ALLOWED));
            return;
        }

        File clanFile = new File(fh.getClansDir(), key + ".yml");
        if (clanFile.exists()) {
            player.sendMessage(MSG.send(CLAN_NAME_TAKEN));
            return;
        }

        if (config.getBoolean("economy.enabled")) {
            int cost = config.getInt("economy.cost.create-clan");
            if (econ.getBalance(player) < cost) {
                player.sendMessage(MSG.send(INSUFFICIENT_FUNDS, String.valueOf(cost)));
                return;
            }
            econ.withdraw(player, cost);
        }

        FileConfiguration clan = fh.newClan(key);

        if (clan == null) {
            player.sendMessage(MSG.send(CLAN_CREATION_ERROR));
            return;
        }

        clan.set("name", displayName);
        clan.set("key", key);
        clan.set("founder", player.getName());
        clan.set("leader", player.getName());
        clan.set("privacy", "Public");
        clan.set("balance", 0);
        clan.set("kills", 0);
        clan.set("prestige", 0);
        clan.set("users", List.of(player.getName()));
        clan.set("invitations", List.of());
        clan.set("relations.allies", List.of());
        clan.set("relations.enemies", List.of());
        clan.set("relations.requests", List.of());
        clan.set("wars.active", List.of());
        clan.set("wars.won", List.of());
        clan.set("wars.defeated", List.of());
        clan.set("wars.streak", 0);
        clan.set("reports", List.of());
        clan.set("clanChest", List.of());

        fh.saveClan(key, clan);

        FileConfiguration p = fh.getPlayerData(player.getUniqueId());
        p.set("name", player.getName());
        p.set("clan", displayName);

        List<String> history = p.getStringList("history");
        if (!history.contains(key)) {
            history.add(key);
        }

        p.set("history", history);

        fh.savePlayerData(player.getUniqueId(), p);

        player.sendMessage(MSG.send(CLAN_CREATED, displayName));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return null;
        }

        String playerClan = plugin.getPlayerClan(player);
        List<String> completions = new ArrayList<>();

        switch (args.length) {

            case 1 -> completions.addAll(List.of(
                    "create", "disband", "report", "list", "join", "kick",
                    "invite", "leave", "stats", "resign", "edit",
                    "eco", "chest", "relations", "war"
            ));

            case 2 -> {
                switch (args[0].toLowerCase()) {

                    case "create" -> {
                        if (!isInClan(playerClan))
                            completions.add("<name>");
                    }

                    case "join" -> {
                        if (!isInClan(playerClan))
                            completions.addAll(getClanNames());
                    }

                    case "invite" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan))
                            completions.addAll(getOnlinePlayerNames());
                    }

                    case "kick" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan))
                            completions.addAll(getClanMembers(playerClan, player));
                    }

                    case "eco" -> {
                        if (isInClan(playerClan))
                            completions.addAll(List.of("deposit", "withdraw"));
                    }

                    case "report", "stats" -> completions.addAll(getClanNames());

                    case "edit" -> {
                        if (isInClan(playerClan) && isLeader(player, playerClan))
                            completions.addAll(List.of("name", "privacy"));
                    }

                    case "war" -> completions.addAll(List.of(
                            "start", "join", "peace", "accept", "reject", "leave"
                    ));
                }
            }

            case 3 -> {
                switch (args[0].toLowerCase()) {

                    case "edit" -> {
                        switch (args[1].toLowerCase()) {
                            case "name" -> completions.add("<name>");
                            case "privacy" -> completions.addAll(List.of("public", "private"));
                        }
                    }

                    case "eco" -> {
                        if (isInClan(playerClan))
                            completions.add("<amount>");
                    }

                    case "report" -> completions.add("<reason...>");

                    case "war" -> {
                        switch (args[1].toLowerCase()) {

                            case "start" -> {
                                if (isLeader(player, playerClan)) {
                                    List<String> clans = getClanNames();
                                    clans.remove(playerClan);
                                    completions.addAll(clans);
                                }
                            }

                            case "join" -> {
                                if (isLeader(player, playerClan))
                                    completions.addAll(getAllActiveWarIDs());
                            }

                            case "peace", "leave" -> {
                                if (isLeader(player, playerClan))
                                    completions.addAll(getWarsOfTheClan(playerClan));
                            }

                            case "accept", "reject" -> {
                                if (isLeader(player, playerClan))
                                    completions.addAll(getWarsWithPendingOffers(playerClan));
                            }
                        }
                    }
                }
            }

            case 4 -> {
                if (args[0].equalsIgnoreCase("war")) {
                    switch (args[1].toLowerCase()) {
                        case "join" -> completions.addAll(List.of("off", "def"));

                        case "accept", "reject" -> {
                            if (isLeader(player, playerClan))
                                completions.addAll(getPendingOfferIDs(args[2], playerClan));
                        }

                        case "peace" -> completions.add("<terms...>");
                    }
                }
            }
        }

        String last = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(last))
                .collect(Collectors.toList());
    }

    private boolean isInClan(String clan) {
        return (clan != null && !clan.isEmpty());
    }

    private boolean isLeader(Player player, String clanName) {
        if (clanName == null || clanName.isEmpty()) return false;
        FileConfiguration clan = plugin.getFH().getClan(clanName);
        if (clan == null) return false;
        return player.getName().equalsIgnoreCase(clan.getString("leader"));
    }

    private List<String> getClanNames() {
        File dir = plugin.getFH().getClansDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(f -> f.getName().replace(".yml", ""))
                .collect(Collectors.toList());
    }

    private List<String> getClanMembers(String clanName, Player exclude) {
        if (clanName == null) return List.of();
        FileConfiguration clan = plugin.getFH().getClan(clanName);
        if (clan == null) return List.of();
        return clan.getStringList("users").stream()
                .filter(u -> !u.equalsIgnoreCase(exclude.getName()))
                .collect(Collectors.toList());
    }

    private List<String> getAllActiveWarIDs() {
        return new ArrayList<>(plugin.getFH().getActiveWars());
    }

    private List<String> getWarsWithPendingOffers(String clan) {
        FileHandler fh = plugin.getFH();
        List<String> result = new ArrayList<>();
        for (String warID : fh.getActiveWars()) {
            FileConfiguration war = fh.getWar(warID);
            if (war == null) continue;
            List<String> off = war.getStringList("clans.offensive");
            List<String> def = war.getStringList("clans.defensive");
            if (!off.contains(clan) && !def.contains(clan)) continue;
            if (war.contains("peace-offers")) result.add(warID);
        }
        return result;
    }

    private List<String> getPendingOfferIDs(String warID, String clan) {
        FileHandler fh = plugin.getFH();
        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.contains("peace-offers")) return List.of();

        List<String> ids = new ArrayList<>();
        for (String k : Objects.requireNonNull(war.getConfigurationSection("peace-offers")).getKeys(false)) {
            String state = war.getString("peace-offers." + k + ".state", "");
            String from = war.getString("peace-offers." + k + ".from", "");
            if ("pending".equalsIgnoreCase(state) && !from.equalsIgnoreCase(clan))
                ids.add(k);
        }
        return ids;
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public List<String> getWarsOfTheClan(String clan) {
        FileHandler fh = plugin.getFH();
        String target = clan.toLowerCase();
        List<String> wars = new ArrayList<>();

        for (String warCfg : fh.getActiveWars()) {
            String name = warCfg.replace(".yml", "").toLowerCase();
            String[] parts = name.split("_");

            if (parts.length < 3) continue;
            String defPart = parts[0];
            String offPart = parts[parts.length - 1];

            if (defPart.equals(target) || offPart.equals(target)) {
                wars.add(warCfg);
            }
        }

        return wars;
    }
}