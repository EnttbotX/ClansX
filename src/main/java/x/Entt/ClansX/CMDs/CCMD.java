package x.Entt.ClansX.CMDs;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
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
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import x.Entt.ClansX.Utils.WarSystem;

import static x.Entt.ClansX.CX.prefix;

public class CCMD implements CommandExecutor, TabCompleter {
    private final CX plugin;

    public CCMD(CX plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        String clanToReport, reason;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&cConsole can´t use player commands."));
            return true;
        }

        String playerClan = getPlayerClan(player);
        if (args.length < 1) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create":
                if (!sender.hasPermission("cx.user.create")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to create a clan."));
                    return true;
                }
                if (playerClan != null && !playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(prefix + "&cYou are already in a clan."));
                    return true;
                }
                create(sender, args);
                return true;
            case "disband":
                if (!sender.hasPermission("cx.user.disband")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to disband a clan."));
                    return true;
                }
                disband(sender, playerClan);
                return true;
            case "invite":
                if (!sender.hasPermission("cx.user.invite")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to invite players."));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&cUse: /cls invite <player>"));
                    return true;
                }
                invite(sender, args[1]);
                return true;
            case "kick":
                if (!sender.hasPermission("cx.user.expel")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to expel players."));
                    return true;
                }
                expel(sender, args);
                return true;
            case "join":
                if (!sender.hasPermission("cx.user.join")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to join a clan."));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(prefix + "&cUSE: /cls join <clan>"));
                    return true;
                }
                join(sender, args[1]);
                return true;
            case "leave":
                if (!sender.hasPermission("cx.user.leave")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to leave a clan."));
                    return true;
                }
                leave(sender);
                return true;
            case "chat":
                chat(player);
                return true;
            case "list":
                if (!sender.hasPermission("cx.user.list")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to list clans."));
                    return true;
                }
                list(sender);
                return true;
            case "report":
                if (!sender.hasPermission("cx.user.report")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to report clans."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(MSG.color(prefix + "&cUSE: /cls report <clan> <reason>"));
                    return true;
                }
                clanToReport = args[1];
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                report(sender, clanToReport, reason);
                return true;
            case "edit":
                if (!sender.hasPermission("cx.user.edit")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to edit clan settings."));
                    return true;
                }
                edit(player, playerClan, args);
                return true;
            case "eco":
                if (!sender.hasPermission("cx.user.economy")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to manage clan economy."));
                    return true;
                }
                Economy(player, playerClan, args);
                return true;
            case "stats":
                if (!sender.hasPermission("cx.user.stats")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to view clan stats."));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                    return true;
                }
                stats(sender, playerClan);
                return true;
            case "resign":
                if (!sender.hasPermission("cx.user.resign")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to resign your clan role."));
                    return true;
                }
                resign(player, playerClan);
                return true;
            case "war":
                if (!sender.hasPermission("cx.user.war")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions."));
                    return true;
                }
                war(player, Arrays.copyOfRange(args, 1, args.length));
                return true;
            case "chest":
                if (!sender.hasPermission("cx.user.chest")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to resign your clan role."));
                    return true;
                }
                chest(player, playerClan);
                return true;
            case "relations":
                if (!sender.hasPermission("cx.user.relations")) {
                    sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to resign your clan role."));
                    return true;
                }
                relations(player);
                return true;
            default:
                help(sender);
                break;
        }

        return true;
    }

    public void help(CommandSender sender) {
        sender.sendMessage(MSG.color(""));
        sender.sendMessage(MSG.color("&8&m        &r &3&lClans&b&lX &8— &7Command Reference &8&m        "));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lGeneral"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls create &8<&7name&8>  &8— &fCreate a new clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls join &8<&7clan&8>    &8— &fJoin a public clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls leave            &8— &fLeave your current clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls list             &8— &fBrowse all registered clans"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls chat             &8— &fHow to use clan chat"));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lStats"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls stats            &8— &fView your clan's stats & war history"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls pstats           &8— &fView your personal stats & clan history"));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lManagement &8(Leader only)"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls invite &8<&7player&8>  &8— &fInvite a player to your clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls kick &8<&7player&8>    &8— &fExpel a member from the clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls resign            &8— &fStep down and assign a new leader"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls disband           &8— &fPermanently disband your clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls edit name &8<&7name&8>               &8— &fRename your clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls edit privacy &8<&7public&8|&7private&8>  &8— &fChange clan visibility"));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lEconomy"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls eco deposit &8<&7amount&8>   &8— &fDeposit money into the clan bank"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls eco withdraw &8<&7amount&8>  &8— &fWithdraw money from the clan bank"));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lChest & Relations"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls chest             &8— &fOpen the shared clan chest"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls relations         &8— &fManage allies and enemies (GUI)"));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lWars &8(Leader only)"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war                          &8— &fOpen the War Manager GUI"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war start &8<&7clan&8>              &8— &fDeclare war on a clan"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war join &8<&7warID&8> &8<&7off&8|&7def&8>      &8— &fJoin a war as ally"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war peace &8<&7warID&8> &8<&7terms...&8>   &8— &fSend peace terms"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war accept &8<&7warID&8> &8<&7offerID&8>   &8— &fAccept a peace offer"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war reject &8<&7warID&8> &8<&7offerID&8>   &8— &fReject a peace offer"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls war leave &8<&7warID&8>              &8— &fWithdraw from a war (allies only)"));
        sender.sendMessage(MSG.color(""));

        sender.sendMessage(MSG.color("  &b&lOther"));
        sender.sendMessage(MSG.color("  &8┃ &3/cls report &8<&7clan&8> &8<&7reason...&8>  &8— &fReport a clan to admins"));
        sender.sendMessage(MSG.color(""));
        sender.sendMessage(MSG.color("&8&m                                            "));
        sender.sendMessage(MSG.color(""));
    }

    public void war(Player player, String[] args) {
        String pClan = getPlayerClan(player);

        if (args.length < 1) {
            new x.Entt.ClansX.Events.Wars(plugin, player);
            return;
        }

        if (pClan == null || pClan.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        WarSystem ws = plugin.getWarSystem();

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length != 2) {
                    player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls war start <clan>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.color(prefix + "&cOnly the leader can start a war."));
                    return;
                }
                ws.startWar(player, pClan, args[1]);
            }

            case "join" -> {
                if (args.length != 3 ||
                        (!args[2].equalsIgnoreCase("off") && !args[2].equalsIgnoreCase("def"))) {
                    player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls war join <warID> <off|def>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.color(prefix + "&cOnly the leader can join a war."));
                    return;
                }
                ws.joinToWar(player, args[1], args[2].equalsIgnoreCase("def"));
            }

            case "peace" -> {
                if (args.length < 3) {
                    player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls war peace <warID> <terms...>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.color(prefix + "&cOnly the leader can send peace terms."));
                    return;
                }
                ws.sendPeaceTerms(player, args[1],
                        java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 2, args.length)));
            }

            case "accept" -> {
                if (args.length != 3) {
                    player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls war accept <warID> <offerID>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.color(prefix + "&cOnly the leader can accept peace."));
                    return;
                }
                try {
                    ws.acceptPeace(player, args[1], Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    player.sendMessage(MSG.color(prefix + "&cOfferID must be a number."));
                }
            }

            case "reject" -> {
                if (args.length != 3) {
                    player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls war reject <warID> <offerID>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.color(prefix + "&cOnly the leader can reject peace."));
                    return;
                }
                try {
                    ws.rejectPeace(player, args[1], Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    player.sendMessage(MSG.color(prefix + "&cOfferID must be a number."));
                }
            }

            case "leave" -> {
                if (args.length != 2) {
                    player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls war leave <warID>"));
                    return;
                }
                if (!isLeader(player, pClan)) {
                    player.sendMessage(MSG.color(prefix + "&cOnly the leader can leave a war."));
                    return;
                }
                ws.leaveWar(player, args[1]);
            }

            default -> new x.Entt.ClansX.Events.Wars(plugin, player);
        }
    }

    public void chest(Player player, String clan) {
        FileHandler fh = plugin.getFH();

        String playerClan = getPlayerClan(player);
        if (playerClan == null || !playerClan.equalsIgnoreCase(clan)) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in this clan."));
            return;
        }

        if (!player.hasPermission("cx.user.chest")) {
            player.sendMessage(MSG.color(prefix + "&cYou don't have permission to use the clan chest."));
            return;
        }

        FileConfiguration cfg = fh.getClan(clan);
        if (cfg == null) {
            player.sendMessage(MSG.color(prefix + "&cClan file not found."));
            return;
        }

        Inventory chest = Bukkit.createInventory(null, 54, MSG.color("&8Clan Chest: &e" + clan));

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

    public void expel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        if (args.length != 2) {
            sender.sendMessage(MSG.color(prefix + "&cUSE: /cls kick <player>"));
            return;
        }

        String targetName = args[1];
        FileHandler fh = plugin.getFH();

        String clanName = getPlayerClan(player);
        if (clanName == null) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        List<String> users = clan.getStringList("users");
        String leader = clan.getString("leader");

        if (!player.getName().equalsIgnoreCase(leader)) {
            sender.sendMessage(MSG.color(prefix + "&cOnly the leader can expel members."));
            return;
        }

        if (targetName.equalsIgnoreCase(player.getName())) {
            sender.sendMessage(MSG.color(prefix + "&cYou can't expel yourself."));
            return;
        }

        if (!users.remove(targetName)) {
            sender.sendMessage(MSG.color(prefix + "&cThat player is not in your clan."));
            return;
        }

        clan.set("users", users);
        fh.saveClan(clanName, clan);

        Player target = Bukkit.getPlayerExact(targetName);
        assert target != null;
        FileConfiguration p = fh.getPlayerData(target.getUniqueId());

        p.set("clan", null);
        fh.savePlayerData(target.getUniqueId(), p);

        sender.sendMessage(MSG.color(prefix + "&aPlayer &f" + targetName + " &ahas been expelled from the clan."));
        target.sendMessage(MSG.color(prefix + "&cYou have been expelled from the clan."));
    }

    public void resign(Player player, String clanName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        if (clan == null || !clan.isSet("name")) {
            player.sendMessage(MSG.color(prefix + "&cClan not found."));
            return;
        }

        String leader = clan.getString("leader");
        if (!player.getName().equalsIgnoreCase(leader)) {
            player.sendMessage(MSG.color(prefix + "&cYou are not the leader of this clan."));
            return;
        }

        List<String> users = clan.getStringList("users");

        List<String> otherMembers = new ArrayList<>(users);
        otherMembers.remove(player.getName());

        if (!otherMembers.isEmpty()) {
            String newLeader = otherMembers.get(new Random().nextInt(otherMembers.size()));

            clan.set("leader", newLeader);

            fh.saveClan(clanName, clan);

            player.sendMessage(MSG.color(prefix + "&aYou have resigned as leader. New leader is &e" + newLeader));

            for (String userName : users) {
                Player member = Bukkit.getPlayer(userName);
                if (member != null && member.isOnline()) {
                    member.sendMessage(MSG.color(prefix + "&2" + player.getName() + " is not the leader anymore, the new leader is " + newLeader));
                }
            }

            Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
            if (newLeaderPlayer != null) {
                newLeaderPlayer.sendMessage(MSG.color(prefix + "&aYou are now the leader of clan &e" + clanName));
            }

        } else {
            player.sendMessage(MSG.color(prefix + "&cCannot resign! You are the only member."));
            player.sendMessage(MSG.color(prefix + "&7Use &e/clan disband &7to delete the clan"));
        }
    }

    public void stats(CommandSender sender, String clanName) {
        FileHandler fh = this.plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        if (clan == null || !clan.contains("name")) {
            sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
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

        sender.sendMessage(MSG.color("&6==================== &b&lCLAN STATS &6===================="));
        sender.sendMessage(MSG.color("&eName: &f" + clanName));
        sender.sendMessage(MSG.color("&eFounder: &f" + founder));
        sender.sendMessage(MSG.color("&eLeader: &f" + leader));
        sender.sendMessage(MSG.color("&ePrivacy: &f" + privacy));
        sender.sendMessage(MSG.color("&eBalance: &f$" + balance));

        sender.sendMessage(MSG.color("&eMembers:"));
        if (users.isEmpty()) sender.sendMessage(MSG.color("&7- None"));
        else users.forEach(u -> sender.sendMessage(MSG.color("&7- " + u)));

        sender.sendMessage(MSG.color("&eInvited Members:"));
        if (invites.isEmpty()) sender.sendMessage(MSG.color("&7- None"));
        else invites.forEach(i -> sender.sendMessage(MSG.color("&7- " + i)));

        sender.sendMessage(MSG.color("&eWars:"));
        sender.sendMessage(MSG.color("&7Active: " + (warsActive.isEmpty() ? "None" : String.join(", ", warsActive))));
        sender.sendMessage(MSG.color("&7Won: " + (warsWon.isEmpty() ? "None" : String.join(", ", warsWon))));
        sender.sendMessage(MSG.color("&7Defeated: " + (warsDefeated.isEmpty() ? "None" : String.join(", ", warsDefeated))));
        sender.sendMessage(MSG.color("&7Current Streak: " + streak));
        sender.sendMessage(MSG.color("&7Total Kills: " + kills));

        sender.sendMessage(MSG.color("&6===================================================="));
    }

    private void Economy(Player player, String clanName, String[] args) {
        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls eco <deposit | withdraw> <amount>"));
            return;
        }

        String playerClan = getPlayerClan(player);
        if (playerClan == null || !playerClan.equalsIgnoreCase(clanName)) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in this clan."));
            return;
        }

        String type = args[1].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(MSG.color(prefix + "&cAmount must be a number."));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(MSG.color(prefix + "&cAmount must be greater than 0."));
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
                    player.sendMessage(MSG.color(prefix + "&cNot enough money to deposit."));
                    return;
                }

                econ.withdraw(player, amount);
                clan.set("balance", clanBalance + amount);
                fh.saveClan(clanName, clan);
                player.sendMessage(MSG.color(prefix + "&2Deposited &a$" + amount + "&2 to clan &e" + clanName));
                break;

            case "withdraw":
                if (clanBalance < amount) {
                    player.sendMessage(MSG.color(prefix + "&cClan doesn't have enough money."));
                    return;
                }

                econ.deposit(player, amount);
                clan.set("balance", clanBalance - amount);
                fh.saveClan(clanName, clan);
                player.sendMessage(MSG.color(prefix + "&2Withdrew &a$" + amount + "&2 from clan &e" + clanName));
                break;

            default:
                player.sendMessage(MSG.color(prefix + "&cInvalid operation. Use deposit or withdraw."));
                break;
        }
    }

    private void invite(CommandSender sender, String playerToInvite) {
        if (!(sender instanceof Player player)) return;

        Player invitedPlayer = Bukkit.getPlayerExact(playerToInvite);

        assert invitedPlayer != null;
        if (invitedPlayer.getName().equalsIgnoreCase(player.getName())) {
            player.sendMessage(MSG.color(prefix + "&cYou can´t invite yourself."));
            return;
        }

        if (!invitedPlayer.isOnline()) {
            player.sendMessage(MSG.color(prefix + "&cThis player is not online."));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration pData = fh.getPlayerData(invitedPlayer.getUniqueId());
        if (pData.getString("clan") != null) {
            player.sendMessage(MSG.color(prefix + "&cThis player is already in a clan."));
            return;
        }

        String clanName = getPlayerClan(player);
        if (clanName == null) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        List<String> invitations = clan.getStringList("invitations");
        if (!invitations.contains(invitedPlayer.getName()))
            invitations.add(invitedPlayer.getName());

        clan.set("invitations", invitations);
        fh.saveClan(clanName, clan);

        player.sendMessage(MSG.color(prefix + "&2Invitation sent to: &e" + invitedPlayer.getName()));
        invitedPlayer.sendMessage(MSG.color(prefix + "&2You have been invited to the clan: &e" + clanName));
        invitedPlayer.sendMessage(MSG.color("&8USE: /cls join " + clanName + " &7- to join the clan"));
    }

    public void chat(Player player) {
        String symbol = plugin.getFH().getConfig().getString("clans.messages-to-clan", "@");
        player.sendMessage(MSG.color(prefix + "&eTo talk to your clan, write your messages starting with: &f" + symbol));
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) return;

        FileHandler fh = plugin.getFH();
        FileConfiguration pData = fh.getPlayerData(player.getUniqueId());
        String clanName = pData.getString("clan");

        if (clanName == null) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (!clan.contains("name")) {
            player.sendMessage(MSG.color(prefix + "&cClan data not found."));
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
            player.sendMessage(MSG.color(prefix + "&cYou left the clan. &e" + newLeader + " &cis the new leader."));
        } else if (!player.getName().equalsIgnoreCase(leader)) {
            player.sendMessage(MSG.color(prefix + "&2You left the clan."));
        }

        if (users.isEmpty()) {
            if (fh.getConfig().getBoolean("economy.enabled")) {
                int reward = fh.getConfig().getInt("economy.earn.delete-clan", 0);
                if (reward > 0) {
                    CX.getEcon().deposit(player, reward);
                    sender.sendMessage(MSG.color(prefix + "&2Clan disbanded &7(no members left). You won: &e$" + reward));
                }
            } else {
                sender.sendMessage(MSG.color(prefix + "&2Clan disbanded &7(no members left)"));
            }

            plugin.getWarSystem().resolveWarsOnDisband(clanName);
            fh.deleteClan(clanName);
        } else {
            fh.saveClan(clanName, clan);
        }

        pData.set("clan", null);
        fh.savePlayerData(player.getUniqueId(), pData);
    }

    private void join(CommandSender sender, String clanName) {
        if (!(sender instanceof Player player)) return;

        FileHandler fh = plugin.getFH();

        FileConfiguration pData = fh.getPlayerData(player.getUniqueId());
        String currentClan = pData.getString("clan");

        if (currentClan != null) {
            player.sendMessage(MSG.color(prefix + "&cYou already have a clan."));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (!clan.contains("name")) {
            player.sendMessage(MSG.color(prefix + "&cThat clan does not exist."));
            return;
        }

        String privacy = clan.getString("privacy", "Public");
        List<String> users = clan.getStringList("users");

        if (privacy.equalsIgnoreCase("Private")) {
            List<String> invites = clan.getStringList("invitations");
            if (!invites.contains(player.getName())) {
                player.sendMessage(MSG.color(prefix + "&cThis clan is private."));
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

        player.sendMessage(MSG.color(prefix + "&2You joined &e" + clanName));
    }

    public void list(CommandSender sender) {
        FileHandler fh = plugin.getFH();
        File[] clans = fh.getClansDir().listFiles((d, n) -> n.endsWith(".yml"));

        if (clans == null || clans.length == 0) {
            sender.sendMessage(MSG.color(prefix + "&cNo clans registered."));
            return;
        }

        sender.sendMessage(MSG.color(""));
        sender.sendMessage(MSG.color("&8&m──────── &3&lClansX &8&m────────"));

        for (File file : clans) {
            FileConfiguration c = YamlConfiguration.loadConfiguration(file);

            String name = c.getString("name", file.getName().replace(".yml", ""));
            String leader = c.getString("leader", "Unknown");
            int members = c.getStringList("users").size();
            int kills = c.getInt("kills", 0);

            sender.sendMessage(MSG.color(
                    "&a" + name +
                            " &8┃ &7Leader: &f" + leader +
                            " &8┃ &7Members: &f" + members +
                            " &8┃ &cKills: &f" + kills
            ));
        }

        sender.sendMessage(MSG.color("&8&m────────────────────────"));
        sender.sendMessage(MSG.color(""));
    }

    private void report(CommandSender sender, String reportedClan, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cPlease provide a valid reason."));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(reportedClan);

        if (!clan.contains("name")) {
            sender.sendMessage(MSG.color(prefix + "&cThat clan does not exist."));
            return;
        }

        List<String> reports = clan.getStringList("reports");

        if (reports.contains(reason)) {
            sender.sendMessage(MSG.color(prefix + "&cThis report was already submitted."));
            return;
        }

        reports.add(reason);
        clan.set("reports", reports);
        fh.saveClan(reportedClan, clan);

        sender.sendMessage(MSG.color(
                prefix + "&aClan &f" + reportedClan + " &ahas been reported."
        ));
    }

    public void edit(Player p, String clanName, String[] args) {
        if (clanName == null || clanName.isEmpty()) {
            p.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        if (!isLeader(p, clanName)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can perform this action."));
            return;
        }

        if (args.length < 3) return;

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        String type = args[1].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        switch (type) {
            case "name": {
                String newName = validateClanName(value);
                if (newName == null) {
                    p.sendMessage(MSG.color(prefix + "&cInvalid clan name."));
                    return;
                }

                if (newName.equalsIgnoreCase(clanName)) {
                    p.sendMessage(MSG.color(prefix + "&cYou can't use the same name you're changing."));
                    return;
                }

                if (fh.clanExists(newName)) {
                    p.sendMessage(MSG.color(prefix + "&cA clan with that name already exists."));
                    return;
                }

                if (!fh.renameClan(clanName, newName)) {
                    p.sendMessage(MSG.color(prefix + "&cFailed to rename clan."));
                    return;
                }

                p.sendMessage(MSG.color(prefix + "&aClan renamed to &f" + newName));
                break;
            }

            case "privacy": {
                if (!(value.equalsIgnoreCase("public") || value.equalsIgnoreCase("private"))) {
                    p.sendMessage(MSG.color(prefix + "&cYou &lMUST &cchoose a correct value! &8(public/private)"));
                    return;
                }

                clan.set("privacy", value.toLowerCase());
                fh.saveClan(clanName, clan);
                p.sendMessage(MSG.color(prefix + "&aClan privacy set to &f" + value));
                break;
            }

            default:
                p.sendMessage(MSG.color(prefix + "&cInvalid edit option!"));
        }
    }

    public void relations(Player player) {
        String clan = getPlayerClan(player);

        if (!player.hasPermission("cx.user.relations")) {
            player.sendMessage(MSG.color(prefix + "&cNo permission"));
            return;
        }
        if (!isInClan(clan)) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }
        if (!isLeader(player, clan)) {
            player.sendMessage(MSG.color(prefix + "&cYou are not the leader."));
            return;
        }

        new Relations(plugin, player);
    }

    public void disband(CommandSender sender, String clanName) {
        if (!(sender instanceof Player player)) return;

        if (clanName == null || clanName.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration clan = fh.getClan(clanName);

        if (!player.getName().equalsIgnoreCase(clan.getString("leader"))) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not the leader of this clan."));
            return;
        }

        if (fh.getConfig().getBoolean("economy.enabled")) {
            int reward = fh.getConfig().getInt("economy.earn.delete-clan", 0);
            if (reward > 0) {
                CX.getEcon().deposit(player, reward);
                sender.sendMessage(MSG.color(prefix + "&2Clan disbanded. &famount refunded: &e$" + reward));
            }
        } else {
            sender.sendMessage(MSG.color(prefix + "&2Clan disbanded."));
        }

        List<String> users = clan.getStringList("users");
        for (String name : users) {
            Player p = Bukkit.getPlayerExact(name);
            assert p != null;
            FileConfiguration pd = fh.getPlayerData(p.getUniqueId());
            pd.set("clan", null);
            fh.savePlayerData(p.getUniqueId(), pd);

            p.sendMessage(MSG.color(prefix + "&cYour clan has been disbanded by the Leader."));
        }

        plugin.getWarSystem().resolveWarsOnDisband(clanName);
        fh.deleteClan(clanName);
    }

    public void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        if (args.length < 2) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls create <name>"));
            return;
        }

        String displayName = validateClanName(args[1]);
        if (displayName == null) {
            sender.sendMessage(MSG.color(prefix + "&cInvalid clan name."));
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
                sender.sendMessage(MSG.color(prefix + "&cThe server has reached the maximum number of clans (&e" + maxClans + "&c)."));
                return;
            }
        }

        if (config.getStringList("names-blocked.blocked")
                .stream().map(String::toLowerCase).anyMatch(key::equals)) {
            sender.sendMessage(MSG.color(prefix + "&cThis name is not allowed."));
            return;
        }

        File clanFile = new File(fh.getClansDir(), key + ".yml");
        if (clanFile.exists()) {
            sender.sendMessage(MSG.color(prefix + "&cThis clan already exists."));
            return;
        }

        if (config.getBoolean("economy.enabled")) {
            int cost = config.getInt("economy.cost.create-clan");
            if (econ.getBalance(player) < cost) {
                sender.sendMessage(MSG.color("&cYou need &e$" + cost));
                return;
            }
            econ.withdraw(player, cost);
        }

        FileConfiguration clan = fh.newClan(key);

        if (clan == null) {
            sender.sendMessage("Error creating clan.");
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
        if (!history.contains(key)) history.add(key);
        p.set("history", history);

        fh.savePlayerData(player.getUniqueId(), p);

        player.sendMessage(MSG.color(prefix + "&2Clan &e" + displayName + " &2created."));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command cmd,
                                      @NotNull String label,
                                      String[] args) {

        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            return (args.length == 1) ? List.of("reload") : List.of();
        }

        String playerClan = getPlayerClan(player);
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

    private String getPlayerClan(Player player) {
        FileConfiguration p = plugin.getFH().getPlayerData(player.getUniqueId());
        return p.getString("clan");
    }

    private String validateClanName(String input) {
        FileConfiguration cfg = plugin.getFH().getConfig();

        int min = cfg.getInt("clans.names.min-length", 1);
        int max = cfg.getInt("clans.names.max-length", 0);
        boolean allowUpper = cfg.getBoolean("clans.names.allow-uppercase", true);
        boolean allowNumbers = cfg.getBoolean("clans.names.allow-numbers", true);

        String check = input.toLowerCase();

        for (String b : cfg.getStringList("clans.names.blocked")) {
            if (check.contains(b.toLowerCase())) return null;
        }

        if (!allowNumbers && check.matches(".*\\d.*")) return null;

        if (check.length() < min) return null;
        if (max > 0 && check.length() > max) return null;

        return allowUpper ? input : check;
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