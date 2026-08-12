package x.Entt.ClansX.Utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

import x.Entt.ClansX.CX;

import static x.Entt.ClansX.CX.econ;
import static x.Entt.ClansX.Utils.MSG.Paths.*;

public class WarSystem {

    private final CX plugin;

    public WarSystem(CX plugin) {
        this.plugin = plugin;
    }

    private boolean isNotPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MSG.send(CONSOLE));
            return true;
        }
        return false;
    }

    private boolean isNotLeader(Player player) {
        FileHandler fh = plugin.getFH();
        String clan = plugin.getPlayerClan(player);

        if (clan == null) {
            return true;
        }

        FileConfiguration cfg = fh.getClan(clan);
        return !player.getName().equals(cfg.getString("leader"));
    }

    private boolean alreadyAtWar(String clanA, String clanB) {
        FileHandler fh = plugin.getFH();
        File[] files = fh.getWarsDir().listFiles((d, n) -> n.endsWith(".yml"));

        if (files == null) {
            return false;
        }

        for (File f : files) {
            FileConfiguration war = YamlConfiguration.loadConfiguration(f);
            if (!war.getBoolean("active")) continue;

            List<String> off = war.getStringList("clans.offensive");
            List<String> def = war.getStringList("clans.defensive");

            boolean aOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(clanA));
            boolean bDef = def.stream().anyMatch(c -> c.equalsIgnoreCase(clanB));
            boolean bOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(clanB));
            boolean aDef = def.stream().anyMatch(c -> c.equalsIgnoreCase(clanA));

            if ((aOff && bDef) || (bOff && aDef)) return true;
        }
        return false;
    }

    private boolean inTruce(String clanA, String clanB) {
        FileConfiguration tnl = plugin.getFH().getTNL();
        long now = System.currentTimeMillis();

        String keyAB = "truces." + clanA.toLowerCase() + "." + clanB.toLowerCase();
        String keyBA = "truces." + clanB.toLowerCase() + "." + clanA.toLowerCase();

        if (tnl.contains(keyAB) && tnl.getLong(keyAB) > now) return true;
        return tnl.contains(keyBA) && tnl.getLong(keyBA) > now;
    }

    private void addWarToClan(FileHandler fh, String clan, String warID) {
        FileConfiguration cfg = fh.getClan(clan);
        if (cfg == null) return;

        List<String> active = cfg.getStringList("wars.active");
        active.add(warID);
        cfg.set("wars.active", active);
        fh.saveClan(clan, cfg);
    }

    private void removeWarFromClan(FileHandler fh, String clan, String warID) {
        FileConfiguration cfg = fh.getClan(clan);
        if (cfg == null) return;

        List<String> active = cfg.getStringList("wars.active");
        active.remove(warID);
        cfg.set("wars.active", active);
        fh.saveClan(clan, cfg);
    }

    private void removeWarFromClans(FileHandler fh, Collection<String> clans, String warID) {
        for (String clan : clans) {
            removeWarFromClan(fh, clan, warID);
        }
    }

    private void notifyClan(FileHandler fh, String clan, String message) {
        FileConfiguration cfg = fh.getClan(clan);
        if (cfg == null) return;

        for (String member : cfg.getStringList("users")) {
            Player target = Bukkit.getPlayerExact(member);
            if (target != null) {
                target.sendMessage(message);
            } else {
                queueNotification(fh, member, message);
            }
        }
    }

    private void notifyClans(FileHandler fh, Collection<String> clans, String message) {
        for (String clan : clans) {
            notifyClan(fh, clan, message);
        }
    }

    private void notifyWarParticipants(FileHandler fh, List<String> off, List<String> def, String message) {
        List<String> allClans = new ArrayList<>(off);
        allClans.addAll(def);
        notifyClans(fh, allClans, message);
    }

    private void createTruce(FileHandler fh, String clanA, String clanB) {
        FileConfiguration cache = fh.getTNL();
        cache.set("truces." + clanA.toLowerCase() + "." + clanB.toLowerCase(),
                TimeMng.fromNow(fh.getConfig().getString("clans.wars.duration-of-the-truces", "5d")));
        fh.saveTNL();
    }

    private void WarExpirationDate(FileHandler fh, String warID, String time) {
        FileConfiguration cache = fh.getTNL();

        if (time.equalsIgnoreCase("-1")) {
            cache.set("wars." + warID + ".expiration", -1);
            fh.saveTNL();
            return;
        }

        cache.set("wars." + warID + ".expiration", TimeMng.fromNow(time));
        fh.saveTNL();
    }

    private void removeInfluence(FileConfiguration war, String clan) {
        war.set("influences." + clan, null);
    }

    private void recordWarResult(FileConfiguration cfg, boolean victory, String warName) {
        if (victory) {
            cfg.set("wars.won", addToList(cfg.getStringList("wars.won"), warName));
            cfg.set("wars.streak", cfg.getInt("wars.streak", 0) + 1);
        } else {
            cfg.set("wars.defeated", addToList(cfg.getStringList("wars.defeated"), warName));
            cfg.set("wars.streak", 0);
        }
    }

    private void updateClanXP(FileHandler fh, String clan, String warID, String warName,
                              boolean victory, int xpDelta) {
        FileConfiguration cfg = fh.getClan(clan);
        if (cfg == null) return;

        int prestige = cfg.getInt("prestige", 0);
        int xp = cfg.getInt("xp", 0) + xpDelta;
        int xpPerLevel = fh.getConfig().getInt("clans.prestige.acquire-new-prestige", 10000);

        if (xp >= xpPerLevel) {
            int gainedPrestige = xp / xpPerLevel;
            prestige += gainedPrestige;
            xp %= xpPerLevel;
        }

        while (xp < 0 && prestige > 0) {
            prestige--;
            xp += xpPerLevel;
        }
        if (xp < 0) xp = 0;

        cfg.set("prestige", prestige);
        cfg.set("xp", xp);

        List<String> active = cfg.getStringList("wars.active");
        active.remove(warID);
        cfg.set("wars.active", active);

        recordWarResult(cfg, victory, warName);

        fh.saveClan(clan, cfg);
    }

    private void applyWarPrestige(String warID, FileConfiguration war, List<String> off, List<String> def, FileHandler fh) {
        if (!fh.getConfig().getBoolean("clans.prestige.enabled", true)) return;

        int offPoints = war.getInt("stats.offensive.points", 0);
        int defPoints = war.getInt("stats.defensive.points", 0);

        int xpWon = fh.getConfig().getInt("clans.prestige.calc.wars-won", 1000);
        int xpDefeated = fh.getConfig().getInt("clans.prestige.calc.wars-defeated", -1000);

        List<String> winners = offPoints >= defPoints ? off : def;
        List<String> losers = offPoints >= defPoints ? def : off;

        String warName = war.getString("name", warID);

        for (String c : winners) {
            updateClanXP(fh, c, warID, warName, true, xpWon);
        }
        for (String c : losers) {
            updateClanXP(fh, c, warID, warName, false, xpDefeated);
        }
    }

    private void finishWar(FileHandler fh, String warID, FileConfiguration war,
                           List<String> off, List<String> def, String broadcastMessage) {
        war.set("active", false);
        fh.saveWar(warID, war);

        List<String> allClans = new ArrayList<>(off);
        allClans.addAll(def);
        removeWarFromClans(fh, allClans, warID);

        applyWarPrestige(warID, war, off, def, fh);

        if (!off.isEmpty() && !def.isEmpty()) {
            createTruce(fh, off.get(0), def.get(0));
        }

        if (broadcastMessage != null) {
            notifyWarParticipants(fh, off, def, broadcastMessage);
        }
    }

    public void startWar(CommandSender sender, String mainOff, String mainDef) {
        if (isNotPlayer(sender)) return;
        Player player = (Player) sender;

        FileHandler fh = plugin.getFH();
        String pClan = plugin.getPlayerClan(player);

        if (pClan == null || pClan.isEmpty()) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        FileConfiguration plClan = fh.getClan(pClan);

        if (isNotLeader(player)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        if (mainOff.equalsIgnoreCase(mainDef)) {
            player.sendMessage(MSG.send(SELF_DECLARATION));
            return;
        }

        if (!fh.clanExists(mainDef)) {
            player.sendMessage(MSG.send(UNEXISTENT_CLAN).replace("$$ph1$$", mainDef));
            return;
        }

        if (alreadyAtWar(mainOff, mainDef)) {
            player.sendMessage(MSG.send(ALREADY_AT_WAR).replace("$$ph1$$", mainDef));
            return;
        }

        if (inTruce(mainOff, mainDef)) {
            player.sendMessage(MSG.send(IN_TRUCE).replace("$$ph1$$", mainDef));
            return;
        }

        int maxWars = fh.getConfig().getInt("clans.wars.max-wars-per-clan", 3);
        if (fh.countActiveWars(mainOff) >= maxWars) {
            player.sendMessage(MSG.send(MAX_WARS));
            return;
        }

        if (fh.getConfig().getBoolean("economy.enabled", false)) {
            int cost = fh.getConfig().getInt("economy.cost.start-war", 0);

            if (cost > 0 && econ.getBalance(player) < cost) {
                player.sendMessage(MSG.send(NO_BALANCE_WAR).replace("$$ph1$$", String.valueOf(cost)));
                return;
            }

            if (cost > 0) {
                econ.withdraw(player, cost);
            }
        }

        String warID = mainDef.toLowerCase() + "_" + System.currentTimeMillis() + "_" + mainOff.toLowerCase();
        File file = new File(fh.getWarsDir(), warID + ".yml");
        FileConfiguration war = YamlConfiguration.loadConfiguration(file);

        String name = getRandomWarName()
                .replace("$defender_clan$", mainDef)
                .replace("$offensive_clan$", mainOff);

        war.set("name", name);
        war.set("active", true);
        war.set("clans.offensive", new ArrayList<>(List.of(mainOff)));
        war.set("clans.defensive", new ArrayList<>(List.of(mainDef)));

        war.set("stats.offensive.kills", 0);
        war.set("stats.offensive.deaths", 0);
        war.set("stats.offensive.points", 0);
        war.set("stats.defensive.kills", 0);
        war.set("stats.defensive.deaths", 0);
        war.set("stats.defensive.points", 0);

        war.createSection("peace-offers");

        fh.saveWar(warID, war);

        FileConfiguration defClan = fh.getClan(mainDef);

        List<String> defUsers = defClan.getStringList("users");
        List<String> offUsers = plClan.getStringList("users");

        addWarToClan(fh, mainDef, warID);

        for (String member : defUsers) {
            Player target = Bukkit.getPlayerExact(member);

            if (target != null) {
                target.sendMessage(Text.color(""));
                target.sendMessage(MSG.send(RECEIVED_WAR));
                target.sendMessage(MSG.sendWithoutPrefix(RECEIVED_WAR_INFO).replace("$$ph1$$", mainOff));
                target.sendMessage(MSG.sendWithoutPrefix(WAR_NAME).replace("$$ph1$$", name));
                target.sendMessage(Text.color(""));
            } else {
                queueNotification(fh, member, MSG.sendWithoutPrefix(RECEIVED_WAR_INFO).replace("$$ph1$$", mainOff));
            }
        }

        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!defUsers.contains(pl.getName())
                    && !offUsers.contains(pl.getName())) {
                pl.sendMessage(MSG.send(WAR_NEWS)
                        .replace("$$ph1$$", mainOff)
                        .replace("$$ph2$$", mainDef));
            }
        }

        addWarToClan(fh, mainOff, warID);

        FileConfiguration cache = fh.getTNL();

        String maxDuration = fh.getConfig().getString("clans.wars.max-duration", "-1");

        WarExpirationDate(fh, warID, maxDuration);
        cache.set("wars." + warID + ".started", System.currentTimeMillis());
        cache.set("wars." + warID + ".ask-peace-until",
                TimeMng.fromNow(fh.getConfig().getString("clans.wars.ask-for-peace-duration", "15h")));
        fh.saveTNL();

        for (String member : offUsers) {
            if (member.equalsIgnoreCase(player.getName())) {
                continue;
            }

            Player target = Bukkit.getPlayerExact(member);

            if (target != null) {
                target.sendMessage(Text.color(""));
                target.sendMessage(MSG.send(WAR_DECLARATION).replace("$$ph1$$", mainDef));
                target.sendMessage(Text.color(""));
            } else {
                queueNotification(
                        fh,
                        member,
                        MSG.sendWithoutPrefix(WAR_DECLARATION).replace("$$ph1$$", mainDef)
                );
            }
        }

        player.sendMessage(Text.color(""));
        player.sendMessage(MSG.send(WAR_DECLARED));
        player.sendMessage(MSG.sendWithoutPrefix(WAR_NAME).replace("$$ph1$$", name));
        player.sendMessage(MSG.sendWithoutPrefix(WAR_SIDES).replace("$$ph1$$", mainOff).replace("$$ph2$$", mainDef));
        player.sendMessage(MSG.sendWithoutPrefix(WAR_ID).replace("$$ph1$$", warID));
        player.sendMessage(Text.color(""));
    }

    public void joinToWar(CommandSender sender, String warID, boolean defensiveSide) {
        if (isNotPlayer(sender)) return;
        Player player = (Player) sender;

        FileHandler fh = plugin.getFH();
        String pClan = plugin.getPlayerClan(player);

        if (pClan == null || pClan.isEmpty()) {
            player.sendMessage(MSG.send(NOT_IN_CLAN));
            return;
        }

        if (isNotLeader(player)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            player.sendMessage(MSG.send(WAR_NOT_FOUND));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");

        boolean alreadyJoined = off.stream().anyMatch(c -> c.equalsIgnoreCase(pClan))
                || def.stream().anyMatch(c -> c.equalsIgnoreCase(pClan));

        if (alreadyJoined) {
            player.sendMessage(MSG.send(ALREADY_JOINED_WAR));
            return;
        }

        if (!fh.getConfig().getBoolean("clans.wars.influences.enabled")) {
            player.sendMessage(MSG.send(INFLUENCES_DISABLED));
            return;
        }

        if (fh.countActiveWars(pClan) >= fh.getConfig().getInt("clans.wars.max-wars-per-clan", 3)) {
            player.sendMessage(MSG.send(TOO_MANY_WARS));
            return;
        }

        int prestige = fh.getClan(pClan).getInt("prestige");
        int minPrestige = fh.getConfig().getInt("clans.wars.influences.min-prestige", 10000);
        if (prestige < minPrestige) {
            player.sendMessage(MSG.send(NOT_ENOUGH_PRESTIGE)
                    .replace("$$ph1$$", String.valueOf(minPrestige))
                    .replace("$$ph2$$", String.valueOf(prestige)));
            return;
        }

        if (defensiveSide) def.add(pClan);
        else off.add(pClan);

        war.set("clans.offensive", off);
        war.set("clans.defensive", def);
        war.set("influences." + pClan, defensiveSide ? "defensive" : "offensive");

        fh.saveWar(warID, war);

        player.sendMessage(MSG.send(JOINED_WAR)
                .replace("$$ph1$$", Objects.requireNonNull(war.getString("name")))
                .replace("$$ph2$$", defensiveSide ? "defensive" : "offensive"));
    }

    public void sendPeaceTerms(CommandSender sender, String warID, List<String> rawTerms) {
        if (isNotPlayer(sender)) return;
        Player player = (Player) sender;

        FileHandler fh = plugin.getFH();
        String clan = plugin.getPlayerClan(player);

        if (clan == null) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        if (isNotLeader(player)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            player.sendMessage(MSG.send(WAR_NOT_FOUND));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        if (!off.contains(clan) && !def.contains(clan)) {
            player.sendMessage(MSG.send(NOT_IN_WAR));
            return;
        }

        FileConfiguration cache = fh.getTNL();
        long until = cache.getLong("wars." + warID + ".ask-peace-until");
        if (System.currentTimeMillis() > until) {
            player.sendMessage(MSG.send(PEACE_WINDOW_EXPIRED));
            return;
        }

        List<String> parsed = parsePeaceTerms(player, fh, rawTerms);
        if (parsed == null) return;

        ConfigurationSection sec = war.getConfigurationSection("peace-offers");
        if (sec == null) sec = war.createSection("peace-offers");
        int id = sec.getKeys(false).size() + 1;

        war.set("peace-offers." + id + ".state", "pending");
        war.set("peace-offers." + id + ".from", clan);
        war.set("peace-offers." + id + ".offers", parsed);

        fh.saveWar(warID, war);

        player.sendMessage(MSG.send(PEACE_OFFER_SENT)
                .replace("$$ph1$$", String.valueOf(id))
                .replace("$$ph2$$", Objects.requireNonNull(war.getString("name"))));

        List<String> targets = off.contains(clan) ? def : off;
        String notif = MSG.send(PEACE_OFFER_RECEIVED)
                .replace("$$ph1$$", clan)
                .replace("$$ph2$$", String.valueOf(id))
                .replace("$$ph3$$", Objects.requireNonNull(war.getString("name")))
                + "\n" + MSG.sendWithoutPrefix(PEACE_OFFER_USE_ACCEPT)
                .replace("$$ph1$$", warID)
                .replace("$$ph2$$", String.valueOf(id));
        for (String c : targets) {
            notifyClan(fh, c, notif);
        }
    }

    private List<String> parsePeaceTerms(Player player, FileHandler fh, List<String> rawTerms) {
        List<String> parsed = new ArrayList<>();
        boolean whitePeace = false;

        for (String t : rawTerms) {
            String[] s = t.split(":");
            if (s.length < 2 || s.length > 3) {
                player.sendMessage(MSG.send(INVALID_TERM_FORMAT).replace("$$ph1$$", t));
                return null;
            }

            String side = s[0];
            String type = s[1];

            if (!List.of("off", "def", "moff", "mdef").contains(side)) {
                player.sendMessage(MSG.send(INVALID_SIDE).replace("$$ph1$$", side));
                return null;
            }
            if (!fh.getConfig().getBoolean("clans.wars.peace-terms." + type, false)) {
                player.sendMessage(MSG.send(TERM_NOT_ENABLED).replace("$$ph1$$", type));
                return null;
            }

            if (type.equals("white-peace")) {
                whitePeace = true;
                parsed.add(side + ":white-peace");
                continue;
            }

            if (s.length < 3) {
                player.sendMessage(MSG.send(TERM_REQUIRES_AMOUNT).replace("$$ph1$$", type));
                return null;
            }

            int amount;
            try {
                amount = Integer.parseInt(s[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(MSG.send(AMOUNT_NOT_NUMBER).replace("$$ph1$$", t));
                return null;
            }
            parsed.add(side + ":" + type + ":" + amount);
        }

        if (whitePeace && parsed.size() > 1) {
            player.sendMessage(MSG.send(WHITE_PEACE_COMBINED));
            return null;
        }

        return parsed;
    }

    public void acceptPeace(CommandSender sender, String warID, int id) {
        if (isNotPlayer(sender)) return;
        Player player = (Player) sender;

        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(player.getUniqueId()).getString("clan");

        if (clan == null) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        if (isNotLeader(player)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            player.sendMessage(MSG.send(WAR_NOT_FOUND));
            return;
        }

        String base = "peace-offers." + id;
        if (!war.contains(base)) {
            player.sendMessage(MSG.send(PEACE_OFFER_NOT_FOUND).replace("$$ph1$$", String.valueOf(id)));
            return;
        }
        if (!"pending".equalsIgnoreCase(war.getString(base + ".state"))) {
            player.sendMessage(MSG.send(PEACE_OFFER_NOT_PENDING));
            return;
        }

        String from = war.getString(base + ".from");
        if (clan.equalsIgnoreCase(from)) {
            player.sendMessage(MSG.send(CANNOT_ACCEPT_OWN_OFFER));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        List<String> offers = war.getStringList(base + ".offers");

        String failedClan = validatePeaceOffer(fh, offers, off, def);
        if (failedClan != null) {
            player.sendMessage(MSG.send(BELOW_MIN_BALANCE).replace("$$ph1$$", failedClan));
            return;
        }

        applyPeaceOffer(fh, offers, off, def);

        for (String k : Objects.requireNonNull(war.getConfigurationSection("peace-offers")).getKeys(false)) {
            war.set("peace-offers." + k + ".state", k.equals(String.valueOf(id)) ? "accepted" : "expired");
        }
        fh.saveWar(warID, war);

        String warName = war.getString("name", warID);
        finishWar(fh, warID, war, off, def,
                MSG.send(PEACE_ACCEPTED_BROADCAST).replace("$$ph1$$", warName));

        player.sendMessage(MSG.send(PEACE_ACCEPTED_SELF).replace("$$ph1$$", String.valueOf(id)));
    }

    private String validatePeaceOffer(FileHandler fh, List<String> offers, List<String> off, List<String> def) {
        double minBal = fh.getConfig().getDouble("clans.balance.min", -10000);

        for (String o : offers) {
            String[] s = o.split(":");
            if (s[1].equals("white-peace")) break;
            if (!s[1].equals("money")) continue;

            int amount = Integer.parseInt(s[2]);
            Collection<String> targets = resolveTargets(s[0], off, def);
            if (targets == null) continue;

            for (String c : targets) {
                FileConfiguration cfg = fh.getClan(c);
                if (cfg == null) continue;
                double newBalance = cfg.getDouble("balance", 0) + amount;
                if (newBalance < minBal) {
                    return c;
                }
            }
        }
        return null;
    }

    private void applyPeaceOffer(FileHandler fh, List<String> offers, List<String> off, List<String> def) {
        for (String o : offers) {
            String[] s = o.split(":");
            if (s[1].equals("white-peace")) break;

            int amount = Integer.parseInt(s[2]);
            Collection<String> targets = resolveTargets(s[0], off, def);
            if (targets == null) continue;

            for (String c : targets) {
                FileConfiguration cfg = fh.getClan(c);
                if (cfg == null) continue;

                if (s[1].equals("money")) {
                    cfg.set("balance", cfg.getDouble("balance", 0) + amount);
                }
                if (s[1].equals("prestige")) {
                    cfg.set("prestige", cfg.getInt("prestige", 0) + amount);
                }
                fh.saveClan(c, cfg);
            }
        }
    }

    private Collection<String> resolveTargets(String side, List<String> off, List<String> def) {
        return switch (side) {
            case "off" -> off;
            case "def" -> def;
            case "moff" -> List.of(off.get(0));
            case "mdef" -> List.of(def.get(0));
            default -> null;
        };
    }

    public void resolveWarsOnDisband(String disbandedClan) {
        FileHandler fh = plugin.getFH();

        for (String warID : fh.getActiveWars()) {
            FileConfiguration war = fh.getWar(warID);
            if (war == null || !war.getBoolean("active")) continue;

            List<String> off = war.getStringList("clans.offensive");
            List<String> def = war.getStringList("clans.defensive");

            boolean inOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(disbandedClan));
            boolean inDef = def.stream().anyMatch(c -> c.equalsIgnoreCase(disbandedClan));

            if (!inOff && !inDef) continue;

            off.removeIf(c -> c.equalsIgnoreCase(disbandedClan));
            def.removeIf(c -> c.equalsIgnoreCase(disbandedClan));

            String warName = war.getString("name", warID);

            war.set("clans.offensive", off);
            war.set("clans.defensive", def);
            fh.saveWar(warID, war);

            notifyWarParticipants(fh, inOff ? List.of() : off, inOff ? def : List.of(),
                    MSG.send(WAR_FORFEIT)
                            .replace("$$ph1$$", disbandedClan)
                            .replace("$$ph2$$", warName));

            finishWar(fh, warID, war, off, def, null);
        }
    }

    private List<String> addToList(List<String> list, String value) {
        List<String> result = new ArrayList<>(list);
        if (value != null && !value.isEmpty()) result.add(value);
        return result;
    }

    public void rejectPeace(CommandSender sender, String warID, int id) {
        if (!(sender instanceof Player player)) return;

        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(player.getUniqueId()).getString("clan");

        if (clan == null) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        if (isNotLeader(player)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            player.sendMessage(MSG.send(WAR_NOT_FOUND));
            return;
        }

        String base = "peace-offers." + id;
        if (!war.contains(base)) {
            player.sendMessage(MSG.send(PEACE_OFFER_NOT_FOUND).replace("$$ph1$$", String.valueOf(id)));
            return;
        }

        if (!"pending".equalsIgnoreCase(war.getString(base + ".state"))) {
            player.sendMessage(MSG.send(PEACE_OFFER_NOT_PENDING));
            return;
        }

        String from = war.getString(base + ".from");
        if (clan.equalsIgnoreCase(from)) {
            player.sendMessage(MSG.send(CANNOT_REJECT_OWN_OFFER));
            return;
        }

        war.set(base + ".state", "rejected");
        fh.saveWar(warID, war);

        player.sendMessage(MSG.send(PEACE_OFFER_REJECTED).replace("$$ph1$$", String.valueOf(id)));

        if (from != null) {
            notifyClan(fh, from, MSG.send(PEACE_OFFER_REJECTED_NOTIFY)
                    .replace("$$ph1$$", String.valueOf(id))
                    .replace("$$ph2$$", Objects.requireNonNull(war.getString("name"))));
        }
    }

    public void leaveWar(CommandSender sender, String warID) {
        if (isNotPlayer(sender)) return;
        Player player = (Player) sender;

        FileHandler fh = plugin.getFH();
        String clan = plugin.getPlayerClan(player);
        if (clan == null || isNotLeader(player)) {
            player.sendMessage(MSG.send(NOT_LEADER));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            player.sendMessage(MSG.send(WAR_NOT_FOUND));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");

        if (off.get(0).equalsIgnoreCase(clan) || def.get(0).equalsIgnoreCase(clan)) {
            player.sendMessage(MSG.send(MAIN_CLANS_CANNOT_LEAVE));
            return;
        }

        boolean alreadyJoined = off.stream().anyMatch(c -> c.equalsIgnoreCase(clan))
                || def.stream().anyMatch(c -> c.equalsIgnoreCase(clan));

        if (alreadyJoined) {
            player.sendMessage(MSG.send(NOT_IN_WAR));
            return;
        }

        off.removeIf(c -> c.equalsIgnoreCase(clan));
        def.removeIf(c -> c.equalsIgnoreCase(clan));

        war.set("clans.offensive", off);
        war.set("clans.defensive", def);
        removeInfluence(war, clan);

        fh.saveWar(warID, war);

        removeWarFromClan(fh, clan, warID);

        player.sendMessage(MSG.send(WAR_LEFT).replace("$$ph1$$", Objects.requireNonNull(war.getString("name"))));
    }

    public String getRandomWarName() {
        List<String> names = plugin.getFH().getConfig().getStringList("clans.wars.names");
        if (names.isEmpty()) return "War";
        return names.get(new Random().nextInt(names.size()));
    }

    public boolean isAlreadyAtWar(String clanA, String clanB) {
        return alreadyAtWar(clanA, clanB);
    }

    public boolean isInTruce(String clanA, String clanB) {
        return inTruce(clanA, clanB);
    }

    public static void queueNotification(FileHandler fh, String playerName, String message) {
        FileConfiguration tnl = fh.getTNL();
        String base = "notifications." + playerName.toLowerCase();
        List<String> pending = tnl.getStringList(base + ".messages");
        pending.add(message);
        tnl.set(base + ".messages", pending);
        tnl.set(base + ".expires", TimeMng.fromNow("30d"));
        fh.saveTNL();
    }

    public static void deliverPendingNotifications(FileHandler fh, Player player) {
        FileConfiguration tnl = fh.getTNL();
        String base = "notifications." + player.getName().toLowerCase();
        if (!tnl.isConfigurationSection(base)) return;

        List<String> messages = tnl.getStringList(base + ".messages");
        if (messages.isEmpty()) return;

        player.sendMessage(MSG.send(PENDING_NOTIFICATIONS_HEADER));
        for (String msg : messages) {
            for (String line : msg.split("\n"))
                player.sendMessage(Text.color("&7" + line));
            player.sendMessage("");
        }

        tnl.set(base, null);
        fh.saveTNL();
    }
}