package x.Entt.ClansX.CMDs;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import static x.Entt.ClansX.CX.prefix;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class CCMD implements CommandExecutor, TabCompleter {
    private final CX plugin;

    public CCMD(CX plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(prefix + "&cConsole Commands: &f/cls reload."));
            return true;
        } else {
            if (!sender.hasPermission("cx.user")) {
                sender.sendMessage(MSG.color(prefix + "&cYou don't have permissions to use this command"));
                return true;
            }

            String playerName = player.getName();
            String playerClan = this.getPlayerClan(playerName);
            if (args.length < 1) {
                this.help(sender);
                return true;
            } else {
                if (args[0].equalsIgnoreCase("create")) {
                    if (playerClan != null && !playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(prefix + "&cYou are already in a clan."));
                        return true;
                    }

                    this.create(sender, args);
                } else if (args[0].equalsIgnoreCase("disband")) {
                    this.disband(sender, playerClan);
                } else {
                    String playerToInvite;
                    if (args[0].equalsIgnoreCase("report")) {
                        if (args.length < 3) {
                            sender.sendMessage(MSG.color(prefix + "&cUSE: /cls report <clan> <reason>"));
                            return true;
                        }

                        playerToInvite = args[1];
                        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                        this.report(sender, playerToInvite, reason);
                    } else if (args[0].equalsIgnoreCase("list")) {
                        this.list(sender);
                    } else if (args[0].equalsIgnoreCase("join")) {
                        if (args.length != 2) {
                            sender.sendMessage(MSG.color(prefix + "&cUSE: /cls join <clan>"));
                            return true;
                        }

                        playerToInvite = args[1];
                        this.joinClan(sender, playerName, playerToInvite);
//                    } else if (args[0].equalsIgnoreCase("war")) {
//                        this.wars(sender, args, playerClan);
                    } else if (args[0].equalsIgnoreCase("edit")) {
                        this.edit(player, playerClan, args);
                    } else if (args[0].equalsIgnoreCase("kick")) {
                        this.kick(sender, args);
                    } else if (args[0].equalsIgnoreCase("economy")) {
                        this.Economy(player, playerClan, args);
                    } else if (args[0].equalsIgnoreCase("invite")) {
                        if (args.length != 2) {
                            sender.sendMessage(MSG.color(prefix + "&cUse: /cls invite <player>"));
                            return true;
                        }

                        playerToInvite = args[1];
                        this.inviteToClan(sender, playerToInvite);
                    } else if (args[0].equalsIgnoreCase("chat")) {
                        if (playerClan == null || playerClan.isEmpty()) {
                            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                            return true;
                        }

                        this.chat(playerClan, player, Arrays.copyOfRange(args, 1, args.length));
                    } else if (args[0].equalsIgnoreCase("leave")) {
                        this.leave(sender, playerClan);
                    } else if (args[0].equalsIgnoreCase("stats")) {
                        if (playerClan == null || playerClan.isEmpty()) {
                            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
                            return true;
                        }

                        this.stats(sender, playerClan);
                    } else if (args[0].equalsIgnoreCase("resign")) {
                        this.resign(sender, playerClan);
                    }
                }

                return false;
            }
        }
    }

    public void help(CommandSender sender) {
        sender.sendMessage(MSG.color("&6======= &lCLAN COMMANDS &6======="));
        sender.sendMessage(MSG.color("&3&lCREATE: &fCreate a new clan and start your adventure!"));
        sender.sendMessage(MSG.color("&3&lJOIN: &fJoin a public clan and make new friends!"));
        sender.sendMessage(MSG.color("&3&lINVITE: &fInvite a player to become part of your clan!"));
        sender.sendMessage(MSG.color("&3&lLEAVE: &fLeave your current clan respectfully."));
        sender.sendMessage(MSG.color("&3&lDISBAND: &fDisband your clan if necessary."));
        sender.sendMessage(MSG.color("&3&lKICK: &fRemove a player from your clan if needed."));
        sender.sendMessage(MSG.color("&3&lCHAT: &fTalk with your clan members easily."));
        sender.sendMessage(MSG.color("&3&lSTATS: &fView your clan's achievements and stats."));
        sender.sendMessage(MSG.color("&3&lLIST: &fSee all clans available on the server."));
        sender.sendMessage(MSG.color("&3&lREPORT: &fReport a clan for any issues."));
        sender.sendMessage(MSG.color("&3&lEDIT: &fEdit the clan properties."));
        sender.sendMessage(MSG.color("&3&lECONOMY: &fWithdraw/Deposit money from/for the clan."));
        sender.sendMessage(MSG.color("&3&lRESIGN: &fResign from your position as leader gracefully."));
//        sender.sendMessage(MSG.color(" "));
//        sender.sendMessage(MSG.color("&6======= &lWAR COMMANDS &6======="));
//        sender.sendMessage(MSG.color("&4&lDECLARE: &fDeclare war on another clan!"));
//        sender.sendMessage(MSG.color("&4&lPEACE: &fSend a peace request to an enemy clan."));
//        sender.sendMessage(MSG.color("&4&lACCEPT: &fAccept a peace or alliance request."));
//        sender.sendMessage(MSG.color("&4&lDENY: &fDeny a peace or alliance proposal."));
//        sender.sendMessage(MSG.color("&4&lALLIANCE: &fSend an alliance request to another clan."));
        sender.sendMessage(MSG.color("&6======= " + prefix + " &6======="));
    }

    public void kick(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls kick <player>"));
            return;
        }

        String playerName = args[1];
        Player player = (Player) sender;
        FileHandler fh = plugin.getFH();
        String clanName = this.getPlayerClan(player.getName());

        if (clanName == null || clanName.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        FileConfiguration data = fh.getData();
        List<String> users = data.getStringList("Clans." + clanName + ".Users");
        String leader = data.getString("Clans." + clanName + ".Leader");

        if (!player.getName().equalsIgnoreCase(leader)) {
            sender.sendMessage(MSG.color(prefix + "&cOnly the clan leader can expel members."));
            return;
        }

        if (playerName.equalsIgnoreCase(player.getName())) {
            sender.sendMessage(MSG.color(prefix + "&cYou can't expel yourself, use /clans leave."));
            return;
        }

        if (!users.contains(playerName)) {
            sender.sendMessage(MSG.color(prefix + "&cPlayer is not a member of the clan."));
            return;
        }

        users.remove(playerName);
        data.set("Clans." + clanName + ".Users", users);
        fh.saveData();
        sender.sendMessage(MSG.color(prefix + "&2Player &e&l" + playerName + " &2has been expelled from the clan &e&l" + clanName));

        if (users.isEmpty()) {
            data.set("Clans." + clanName, null);
            fh.saveData();
            sender.sendMessage(MSG.color(prefix + "&2The clan is empty, it has been eliminated."));
        }
    }

    public void resign(CommandSender sender, String playerClan) {
        if (playerClan != null && !playerClan.isEmpty()) {
            FileHandler fh = plugin.getFH();
            FileConfiguration data = fh.getData();
            List<String> users = data.getStringList("Clans." + playerClan + ".Users");
            if (!users.isEmpty()) {
                int randomIndex = (new Random()).nextInt(users.size());
                String newLeader = users.get(randomIndex);
                data.set("Clans." + playerClan + ".Leader", newLeader);
                sender.sendMessage(MSG.color(prefix + "&cYou reject your position as clan leader! The new leader is " + newLeader));
            } else {
                data.set("Clans." + playerClan, null);
                sender.sendMessage(MSG.color(prefix + "&cClan deleted! &7(cause: 0 members)."));
            }

        } else {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
        }
    }

//    public void wars(CommandSender sender, String[] args, String playerClan) {
//        if (args.length < 2) {
//            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /clans wars <declare/peace/alliance/accept/deny> <clan>"));
//            return;
//        }
//
//        if (playerClan == null || playerClan.isEmpty()) {
//            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
//            return;
//        }
//
//        Player player = (Player) sender;
//        String playerName = player.getName();
//        FileHandler fh = plugin.getFH();
//        FileConfiguration data = fh.getData();
//        String leader = data.getString("Clans." + playerClan + ".Leader");
//
//        if (!playerName.equalsIgnoreCase(leader)) {
//            sender.sendMessage(MSG.color(prefix + "&cYou are not the leader of this clan."));
//            return;
//        }
//
//        String action = args[1].toLowerCase();
//        String otherClan;
//
//        switch (action) {
//            case "peace":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to offer peace."));
//                    return;
//                }
//                otherClan = args[2];
//                handlePeaceOffer(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "declare":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to declare war."));
//                    return;
//                }
//                otherClan = args[2];
//                handleWarDeclaration(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "alliance":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to declare an alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceRequest(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "accept":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to accept the alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceAcceptance(sender, playerClan, otherClan, data, fh);
//                break;
//
//            case "deny":
//                if (args.length < 3) {
//                    sender.sendMessage(MSG.color(prefix + "&cYou need to specify a clan name to deny the alliance."));
//                    return;
//                }
//                otherClan = args[2];
//                handleAllianceDenial(sender, playerClan, otherClan, data, fh);
//                break;
//
//            default:
//                sender.sendMessage(MSG.color(prefix + "&cInvalid action. Use: <declare/peace/alliance/accept/deny>"));
//                break;
//        }
//    }
//
//    private void handlePeaceOffer(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&cThe specified clan does not exist."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Enemy");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Enemy", pending);
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2You have offered peace to: &e" + otherClan));
//    }
//
//    private void handleWarDeclaration(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&cThe specified clan does not exist."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Enemy");
//        pending.add(playerClan);
//        data.set("Wars." + otherClan + ".Enemy", pending);
//
//        removePendingAlliance(playerClan, otherClan, data);
//
//        List<String> playerEnemy = data.getStringList("Wars." + playerClan + ".Enemy");
//        playerEnemy.add(otherClan);
//        data.set("Wars." + playerClan + ".Enemy", playerEnemy);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2Your clan has started a war with: &e" + otherClan));
//    }
//
//    private void handleAllianceRequest(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.add(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2Alliance request sent to: &e" + otherClan));
//    }
//
//    private void handleAllianceAcceptance(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&cThe specified clan does not exist."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Alliance");
//        pending.add(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Alliance", pending);
//
//        pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2Now you are allied with: &e" + otherClan));
//    }
//
//    private void handleAllianceDenial(CommandSender sender, String playerClan, String otherClan, FileConfiguration data, FileHandler fh) {
//        if (!data.contains("Clans." + otherClan)) {
//            sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
//            return;
//        }
//
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        fh.saveData();
//        sender.sendMessage(MSG.color(prefix + "&2You rejected the alliance request of: &e" + otherClan));
//    }
//
//    private void removePendingAlliance(String playerClan, String otherClan, FileConfiguration data) {
//        List<String> pending = data.getStringList("Wars." + otherClan + ".Ally.Pending");
//        pending.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Pending", pending);
//
//        List<String> allianceOtherClan = data.getStringList("Wars." + otherClan + ".Ally.Alliance");
//        allianceOtherClan.remove(playerClan);
//        data.set("Wars." + otherClan + ".Ally.Alliance", allianceOtherClan);
//    }

    public void stats(CommandSender sender, String clanName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();

        String path = "Clans." + clanName;

        if (!data.contains(path)) {
            sender.sendMessage(MSG.color(prefix + "&cNo clan found with that name."));
            return;
        }

        String founder = data.getString(path + ".Founder", "???");
        String leader = data.getString(path + ".Leader", "???");
        String privacy = data.getString(path + ".Privacy", "???");
        String money = data.getString(path + ".Money", "???");

        List<String> users = data.getStringList(path + ".Users");
        List<String> invites = data.isList(path + ".Invitations") ? data.getStringList(path + ".Invitations") : List.of();

        sender.sendMessage(MSG.color("&2--------&f&lSTATS&2--------"));
        sender.sendMessage(MSG.color("&2Name: &e&l" + clanName));
        sender.sendMessage(MSG.color("&2Founder: &e&l" + founder));
        sender.sendMessage(MSG.color("&2Leader: &e&l" + leader));
        sender.sendMessage(MSG.color("&2Privacy: &e&l" + privacy));
        sender.sendMessage(MSG.color("&2Money: &e&l$" + money));
        sender.sendMessage(MSG.color("&2Members:"));
        users.forEach(u -> sender.sendMessage(MSG.color("&f- &l" + u)));

        sender.sendMessage(MSG.color(" "));
        sender.sendMessage(MSG.color("&2Invited Members:"));
        invites.forEach(i -> sender.sendMessage(MSG.color("&f- &l" + i)));

        sender.sendMessage(MSG.color("&2-------- " + prefix + "&2--------"));
    }

    private void Economy(Player player, String clan, String[] args) {
        if (args.length != 3) {
            player.sendMessage(MSG.color(prefix + "&cUsage: /cls eco <deposit|withdraw> <amount>"));
            return;
        }

        String playerClan = getPlayerClan(player.getName());

        if (playerClan == null || playerClan.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        String type = args[1];
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

        Econo econ = CX.getEcon();
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();

        String moneyPath = "Clans." + clan + ".Money";
        double clanBalance = data.getDouble(moneyPath);

        if (type.equalsIgnoreCase("deposit")) {
            double playerBalance = econ.getBalance(player);
            if (playerBalance >= amount) {
                econ.withdraw(player, amount);
                data.set(moneyPath, clanBalance + amount);
                fh.saveData();
                player.sendMessage(MSG.color(prefix + "&2Deposited &a$" + amount + " &2to clan."));
            } else {
                player.sendMessage(MSG.color(prefix + "&cNot enough money to deposit."));
            }
        } else if (type.equalsIgnoreCase("withdraw")) {
            if (clanBalance >= amount) {
                econ.deposit(player, amount);
                data.set(moneyPath, clanBalance - amount);
                fh.saveData();
                player.sendMessage(MSG.color(prefix + "&2Withdrew &a$" + amount + " &2from clan."));
            } else {
                player.sendMessage(MSG.color(prefix + "&cClan doesn't have enough money."));
            }
        } else {
            player.sendMessage(MSG.color(prefix + "&cInvalid operation. Use deposit or withdraw."));
        }
    }

    private void inviteToClan(CommandSender sender, String playerToInvite) {
        Player invitedPlayer = this.plugin.getServer().getPlayer(playerToInvite);
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();

        String playerClan = getPlayerClan(sender.getName());
        List<String> invitations = data.getStringList("Clans." + playerClan + ".Invitations");

        Player p = Bukkit.getPlayerExact(playerToInvite);

        if (invitedPlayer != null && invitedPlayer.isOnline()) {
            invitations.add(sender.getName());
            data.set("Clans." + playerClan + ".invitations", invitations);
            fh.saveData();
            sender.sendMessage(MSG.color(prefix + "&2Invitation sent to: &e" + playerToInvite));
            assert p != null;
            p.sendMessage(MSG.color(prefix + "&2You have been invited to the clan: &e" + playerClan));
            p.sendMessage(MSG.color("&8USE: /cls join " + playerClan + " - to join the clan"));
        } else {
            sender.sendMessage(MSG.color(prefix + "&cThis player is not online."));
        }
    }

    public void chat(String clanName, Player player, String[] message) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        List<String> users = data.getStringList("Clans." + clanName + ".Users");

        String playerClan = getPlayerClan(player.getName());

        if (playerClan == null || playerClan.isEmpty()) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        String formattedMessage = String.join(" ", message);

        player.sendMessage(MSG.color("&e" + getPlayerClan(player.getName()) + " &f" + player.getName() + "&f: &7" + formattedMessage));

        for (String userName : users) {
            Player recipient = this.plugin.getServer().getPlayer(userName);
            if (recipient != null && recipient != player) {
                recipient.sendMessage(MSG.color("&e" + getPlayerClan(player.getName()) + " &f" + player.getName() + "&f: &7" + formattedMessage));
            }
        }
    }

    private void leave(CommandSender sender, String playerClan) {
        Player player = (Player) sender;

        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        List<String> users = data.getStringList("Clans." + playerClan + ".Users");
        String playerName = player.getName();
        String leader = data.getString("Clans." + playerClan + ".Leader");

        users.remove(playerName);
        data.set("Clans." + playerClan + ".Users", users);

        if (users.isEmpty()) {
            data.set("Clans." + playerClan, null);
            sender.sendMessage(MSG.color(prefix + "&cClan deleted! &7(cause: 0 members)."));
            fh.saveData();
            return;
        }

        if (playerName.equals(leader)) {
            String newLeader = users.get(new Random().nextInt(users.size()));
            data.set("Clans." + playerClan + ".Leader", newLeader);
            sender.sendMessage(MSG.color(prefix + "&cYou have left the clan. " + newLeader + " is the new leader."));
        } else {
            sender.sendMessage(MSG.color(prefix + "&2You have left the clan."));
        }

        fh.saveData();
    }

    private void joinClan(CommandSender sender, String playerName, String clanToJoin) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();

        String playerClan = getPlayerClan(sender.getName());

        if (playerClan != null) {
            sender.sendMessage(MSG.color(prefix + "&cYou already have a clan."));
            return;
        }

        Player player = (Player) sender;

        List<String> users = data.getStringList("Clans." + clanToJoin + ".Users");

        if (getPlayerClan(getPlayerClan(playerName)) == null) {
            if (Objects.equals(data.getString("Clans." + clanToJoin + ".Privacy"), "Public")) {
                users.add(playerName);
                data.set("Clans." + clanToJoin + ".Users", users);
                fh.saveData();
                PECMD.addClanToHistory(player, clanToJoin);
                sender.sendMessage(MSG.color(prefix + "&2You have joined to:&r &e" + clanToJoin));
            } else {
                if (data.getStringList("Clans." + clanToJoin + ".Invitations").contains(playerName)) {
                    users.add(playerName);
                    data.set("Clans." + clanToJoin + ".Users", users);
                    fh.saveData();
                    PECMD.addClanToHistory(player, clanToJoin);
                    sender.sendMessage(MSG.color(prefix + "&2You have joined to:&r &e" + clanToJoin + "&2 using an invitation."));
                } else {
                    sender.sendMessage(MSG.color(prefix + "&cThis clan is &lPrivate&c."));
                }
            }
        } else {
            sender.sendMessage(MSG.color(prefix + "&cYou are already joined into a clan!"));
        }
    }

    private String getPlayerClan(String playerName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        ConfigurationSection clansSection = data.getConfigurationSection("Clans");
        if (clansSection != null) {

            for (String clan : clansSection.getKeys(false)) {
                List<String> users = data.getStringList("Clans." + clan + ".Users");
                if (users.contains(playerName)) {
                    return clan;
                }
            }
        }

        return null;
    }

    public void list(CommandSender sender) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        ConfigurationSection clansSection = data.getConfigurationSection("Clans");

        if (clansSection == null || clansSection.getKeys(false).isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cThere are no clans on the server."));
            return;
        }

        StringBuilder clansList = new StringBuilder();
        clansList.append(MSG.color(prefix + "&2&lClans:\n"));

        for (String clan : clansSection.getKeys(false)) {
            clansList.append(MSG.color("&c- ")).append(clan).append("\n");
        }

        clansList.append(MSG.color(prefix + "&c--- end >_< ---"));

        sender.sendMessage(clansList.toString());
    }

    private void report(CommandSender sender, String reportedClan, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cPlease provide a valid reason for the report."));
            return;
        }

        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();

        if (!data.contains("Clans." + reportedClan)) {
            sender.sendMessage(MSG.color(prefix + "&cThe reported clan does not exist."));
            return;
        }

        List<String> reports = data.getStringList("Clans." + reportedClan + ".Reports");

        if (reports.contains(reason)) {
            sender.sendMessage(MSG.color(prefix + "&cThis report has already been submitted."));
            return;
        }

        reports.add(reason);
        data.set("Clans." + reportedClan + ".Reports", reports);

        fh.saveData();

        sender.sendMessage(MSG.color(prefix + "&2Clan reported: &e" + reportedClan + ". Reason: " + reason));
    }

    public void edit(Player p, String clanName, String[] args) {
        if (!isLeader(p, clanName)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can perform this action."));
            return;
        }

        if (getPlayerClan(p.getName()) == null) {
            p.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        if (args.length < 3) return;

        String type = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        FileHandler fh = plugin.getFH();
        FileConfiguration d = fh.getData();

        switch (type.toLowerCase()) {
            case "name": {
                if (d.contains("Clans." + value)) {
                    p.sendMessage(MSG.color(prefix + "&cA clan with that name already exists."));
                    return;
                }

                Object clanData = d.get("Clans." + clanName);
                if (clanData == null) {
                    p.sendMessage(MSG.color(prefix + "&cClan data not found."));
                    return;
                }

                boolean success = plugin.getFH().renameClan(p ,clanName, value);
                if (success) {
                    p.sendMessage(MSG.color(prefix + "&aClan name changed to &f" + value));
                } else {
                    p.sendMessage(MSG.color(prefix + "&cFailed to rename clan. Name might already exist."));
                }

                break;
            }

            case "privacy":
                d.set("Clans." + clanName + ".Privacy", value);
                fh.saveData();
                p.sendMessage(MSG.color(prefix + "&3Clan privacy changed to &f" + value));
                break;
        }
    }

    public void disband(CommandSender sender, String playerClan) {
        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        Econo econ = CX.getEcon();
        Player player = (Player) sender;
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        String leader = data.getString("Clans." + playerClan + ".Leader");

        if (!player.getName().equalsIgnoreCase(leader)) {
            sender.sendMessage(MSG.color(prefix + "&cYou are not the leader of this clan."));
            return;
        }

        if (!data.isConfigurationSection("Clans." + playerClan)) {
            sender.sendMessage(MSG.color(prefix + "&cError: Clan data not found."));
            return;
        }

        if (fh.getConfig().getBoolean("config.economy.enabled")) {
            int deleteGain = fh.getConfig().getInt("config.economy.earn.delete-clan");
            econ.deposit(player, deleteGain);
            sender.sendMessage(MSG.color(prefix + "&2The clan was eliminated. You won: &e$" + deleteGain));
        } else {
            sender.sendMessage(MSG.color(prefix + "&2The clan was eliminated."));
        }

        data.set("Clans." + playerClan, null);
        fh.saveData();
    }

    public void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MSG.color(prefix + "&c&lUSE:&f /cls create &5(name of your clan)."));
            return;
        }

        if (!(sender instanceof Player player)) return;

        Econo econ = CX.getEcon();
        FileHandler fh = plugin.getFH();
        String clanName = args[1].toLowerCase();
        FileConfiguration data = fh.getData();
        FileConfiguration config = fh.getConfig();

        if (config.getStringList("names-blocked.blocked").contains(clanName) || data.isConfigurationSection("Clans." + clanName)) {
            sender.sendMessage(MSG.color(prefix + "&cThis name is not allowed or already exists."));
            return;
        }

        int maxClans = config.getInt("max-clans", 0);
        if (maxClans > 0) {
            int currentClans = data.getConfigurationSection("Clans") != null
                    ? Objects.requireNonNull(data.getConfigurationSection("Clans")).getKeys(false).size()
                    : 0;

            if (currentClans >= maxClans) {
                sender.sendMessage(MSG.color(prefix + "&cThe clan limit has been reached (" + maxClans + ")."));
                return;
            }
        }

        if (config.getBoolean("economy.enabled")) {
            int createCost = config.getInt("economy.cost.create-clan");
            if (econ.getBalance(player) < createCost) {
                sender.sendMessage(MSG.color("&cYou don’t have enough money. You need: &2&l$" + createCost));
                return;
            }

            econ.withdraw(player, createCost);
        }

        String playerName = player.getName();
        data.set("Clans." + clanName + ".Founder", playerName);
        data.set("Clans." + clanName + ".Leader", playerName);
        data.set("Clans." + clanName + ".Money", 0);
        data.set("Clans." + clanName + ".Privacy", "Public");
        data.set("Clans." + clanName + ".Users", Collections.singletonList(playerName));
        data.set("Clans." + clanName + ".Invitations", null);

        fh.saveData();
        PECMD.addClanToHistory(player, clanName);
        player.sendMessage(MSG.color(prefix + "&2Your clan &e" + clanName + " &2has been created."));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return args.length == 1 ? List.of("reload") : new ArrayList<>();
        }

        String playerClan = getPlayerClan(player.getName());
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1:
                completions.addAll(List.of(
                        "create", "disband", "report", "list", "join", //"war",
                        "kick", "invite", "chat", "leave", "stats", "resign", "edit", "economy"
                ));
                break;

            case 2:
                switch (args[0].toLowerCase()) {
                    case "join":
                        if (isNotInClan(playerClan)) {
                            completions.addAll(getClanNames());
                        }
                        break;
                    case "invite":
                    case "kick":
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(getOnlinePlayerNames());
                        }
                        break;
//                    case "war":
//                        completions.addAll(List.of("declare", "peace", "alliance", "accept", "deny"));
//                        break;
                    case "economy":
                        completions.addAll(List.of("deposit", "withdraw"));
                        break;
                    case "report":
                        completions.addAll(getClanNames());
                        break;
                    case "edit":
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(List.of("name", "privacy"));
                        }
                        break;
                }
                break;

            case 3:
            /*
            if (args[0].equalsIgnoreCase("war")) {
                switch (args[1].toLowerCase()) {
                    case "declare":
                    case "peace":
                    case "alliance":
                    case "accept":
                    case "deny":
                        if (isInClan(playerClan) && isLeader(player, playerClan)) {
                            completions.addAll(getClanNames());
                        }
                        break;
                }
            }
            */
                break;
        }

        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean isInClan(String clan) {
        return clan != null && !clan.isEmpty();
    }

    private boolean isNotInClan(String clan) {
        return !isInClan(clan);
    }

    private boolean isLeader(Player player, String clanName) {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        String leader = data.getString("Clans." + clanName + ".Leader");
        return player.getName().equalsIgnoreCase(leader);
    }

    private List<String> getClanNames() {
        FileHandler fh = plugin.getFH();
        FileConfiguration data = fh.getData();
        return new ArrayList<>(Objects.requireNonNull(data.getConfigurationSection("Clans")).getKeys(false));
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}