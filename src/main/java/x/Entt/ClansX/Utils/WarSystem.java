package x.Entt.ClansX.Utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import x.Entt.ClansX.CX;

import java.io.File;
import java.util.*;

import static x.Entt.ClansX.CX.prefix;

public class WarSystem {

    private final CX plugin;

    public WarSystem(CX plugin) {
        this.plugin = plugin;
    }

    private boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MSG.color(prefix + "&cOnly players can use this command."));
            return false;
        }
        return true;
    }

    private boolean isLeader(Player p) {
        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (clan == null) return false;
        FileConfiguration cfg = fh.getClan(clan);
        return p.getName().equalsIgnoreCase(cfg.getString("leader"));
    }

    private boolean alreadyAtWar(String clanA, String clanB) {
        FileHandler fh = plugin.getFH();
        File[] files = fh.getWarsDir().listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return false;

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

    public void startWar(CommandSender sender, String mainOff, String mainDef) {
        if (!isPlayer(sender)) return;
        Player p = (Player) sender;

        FileHandler fh = plugin.getFH();

        String pClan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (pClan == null || pClan.isEmpty()) {
            p.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }

        if (!isLeader(p)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can declare war."));
            return;
        }

        if (mainOff.equalsIgnoreCase(mainDef)) {
            p.sendMessage(MSG.color(prefix + "&cYou cannot declare war on your own clan."));
            return;
        }

        if (!fh.clanExists(mainDef)) {
            p.sendMessage(MSG.color(prefix + "&cThe clan &e" + mainDef + " &cdoes not exist."));
            return;
        }

        if (alreadyAtWar(mainOff, mainDef)) {
            p.sendMessage(MSG.color(prefix + "&cYour clan is already at war with &e" + mainDef + "&c."));
            return;
        }

        if (inTruce(mainOff, mainDef)) {
            p.sendMessage(MSG.color(prefix + "&cYou are in a truce with &e" + mainDef + "&c. Wait before declaring war again."));
            return;
        }

        int maxWars = fh.getConfig().getInt("clans.wars.max-wars-per-clan", 3);
        if (fh.countActiveWars(mainOff) >= maxWars) {
            p.sendMessage(MSG.color(prefix + "&cYour clan is already in &e" + maxWars + " &cactive wars (max reached)."));
            return;
        }

        if (fh.getConfig().getBoolean("economy.enabled", false)) {
            int cost = fh.getConfig().getInt("economy.cost.start-war", 0);
            if (cost > 0 && CX.getEcon().getBalance(p) < cost) {
                p.sendMessage(MSG.color(prefix + "&cYou need &e$" + cost + " &cto declare war."));
                return;
            }
            if (cost > 0) CX.getEcon().withdraw(p, cost);
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

        FileConfiguration cache = fh.getTNL();
        cache.set("wars." + warID + ".started", System.currentTimeMillis());
        cache.set("wars." + warID + ".ask-peace-until",
                TimeMng.fromNow(fh.getConfig().getString("clans.wars.ask-for-peace-duration", "15h")));
        fh.saveTNL();

        p.sendMessage(MSG.color(""));
        p.sendMessage(MSG.color(prefix + "&c&lWAR DECLARED!"));
        p.sendMessage(MSG.color("&7War name: &e" + name));
        p.sendMessage(MSG.color("&7Offensive: &c" + mainOff + "  &7vs  Defensive: &a" + mainDef));
        p.sendMessage(MSG.color("&7War ID: &f" + warID));
        p.sendMessage(MSG.color(""));

        FileConfiguration defClan = fh.getClan(mainDef);
        if (defClan != null) {
            for (String member : defClan.getStringList("users")) {
                Player target = Bukkit.getPlayerExact(member);
                if (target != null) {
                    target.sendMessage(MSG.color(""));
                    target.sendMessage(MSG.color(prefix + "&c&lWAR DECLARED AGAINST YOUR CLAN!"));
                    target.sendMessage(MSG.color("&7The clan &c" + mainOff + " &7has declared war on &a" + mainDef + "&7!"));
                    target.sendMessage(MSG.color("&7War: &e" + name));
                    target.sendMessage(MSG.color(""));
                } else {
                    queueNotification(fh, member, "&c&lWAR DECLARED AGAINST YOUR CLAN!\n&7The clan &c"
                            + mainOff + " &7has declared war on &a" + mainDef + "&7!\n&7War: &e" + name);
                }
            }
        }
    }

    public void joinToWar(CommandSender sender, String warID, boolean defensiveSide) {
        if (!isPlayer(sender)) return;
        Player p = (Player) sender;

        FileHandler fh = plugin.getFH();
        String pClan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (pClan == null || pClan.isEmpty()) {
            p.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
            return;
        }
        if (!isLeader(p)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the leader can join a war."));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            p.sendMessage(MSG.color(prefix + "&cWar not found or is no longer active."));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");

        if (off.contains(pClan) || def.contains(pClan)) {
            p.sendMessage(MSG.color(prefix + "&cYour clan is already participating in this war."));
            return;
        }

        if (!fh.getConfig().getBoolean("clans.wars.influences.enabled")) {
            p.sendMessage(MSG.color(prefix + "&cExternal clans cannot join this war (influences disabled)."));
            return;
        }

        if (fh.countActiveWars(pClan) >= fh.getConfig().getInt("clans.wars.max-wars-per-clan", 3)) {
            p.sendMessage(MSG.color(prefix + "&cYour clan is already in too many wars."));
            return;
        }

        int prestige = fh.getClan(pClan).getInt("prestige");
        int minPrestige = fh.getConfig().getInt("clans.wars.influences.min-prestige", 10000);
        if (prestige < minPrestige) {
            p.sendMessage(MSG.color(prefix + "&cNot enough prestige. Required: &e" + minPrestige + " &c| Current: &e" + prestige));
            return;
        }

        if (defensiveSide) def.add(pClan);
        else off.add(pClan);

        war.set("clans.offensive", off);
        war.set("clans.defensive", def);
        war.set("influences." + pClan, defensiveSide ? "defensive" : "offensive");

        fh.saveWar(warID, war);

        p.sendMessage(MSG.color(prefix + "&aYou joined the war &e" + war.getString("name") + " &aon the &f"
                + (defensiveSide ? "defensive" : "offensive") + " &aside."));
    }

    public void sendPeaceTerms(CommandSender sender, String warID, List<String> rawTerms) {
        if (!isPlayer(sender)) return;
        Player p = (Player) sender;

        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (clan == null || !isLeader(p)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can send peace terms."));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            p.sendMessage(MSG.color(prefix + "&cWar not found or is no longer active."));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        if (!off.contains(clan) && !def.contains(clan)) {
            p.sendMessage(MSG.color(prefix + "&cYour clan is not part of this war."));
            return;
        }

        FileConfiguration cache = fh.getTNL();
        long until = cache.getLong("wars." + warID + ".ask-peace-until");
        if (System.currentTimeMillis() > until) {
            p.sendMessage(MSG.color(prefix + "&cThe peace request window has expired for this war."));
            return;
        }

        List<String> parsed = new ArrayList<>();
        boolean whitePeace = false;

        for (String t : rawTerms) {
            String[] s = t.split(":");
            if (s.length < 2 || s.length > 3) {
                p.sendMessage(MSG.color(prefix + "&cInvalid term format: &e" + t + "&c. Use <side>:<type>[:<amount>]"));
                return;
            }

            String side = s[0];
            String type = s[1];

            if (!List.of("off", "def", "moff", "mdef").contains(side)) {
                p.sendMessage(MSG.color(prefix + "&cInvalid side: &e" + side + "&c. Use off/def/moff/mdef"));
                return;
            }
            if (!fh.getConfig().getBoolean("clans.wars.peace-terms." + type, false)) {
                p.sendMessage(MSG.color(prefix + "&cPeace term '&e" + type + "&c' is not enabled in config."));
                return;
            }

            if (type.equals("white-peace")) {
                whitePeace = true;
                parsed.add(side + ":white-peace");
                continue;
            }

            if (s.length < 3) {
                p.sendMessage(MSG.color(prefix + "&cTerm &e" + type + " &crequires an amount."));
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(s[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(MSG.color(prefix + "&cAmount must be a number in: &e" + t));
                return;
            }
            parsed.add(side + ":" + type + ":" + amount);
        }

        if (whitePeace && parsed.size() > 1) {
            p.sendMessage(MSG.color(prefix + "&cWhite peace cannot be combined with other terms."));
            return;
        }

        ConfigurationSection sec = war.getConfigurationSection("peace-offers");
        if (sec == null) sec = war.createSection("peace-offers");
        int id = sec.getKeys(false).size() + 1;

        war.set("peace-offers." + id + ".state", "pending");
        war.set("peace-offers." + id + ".from", clan);
        war.set("peace-offers." + id + ".offers", parsed);

        fh.saveWar(warID, war);

        p.sendMessage(MSG.color(prefix + "&aPeace offer #" + id + " sent for war &e" + war.getString("name") + "&a."));

        List<String> targets = off.contains(clan) ? def : off;
        for (String c : targets) {
            FileConfiguration cfg = fh.getClan(c);
            if (cfg == null) continue;
            for (String member : cfg.getStringList("users")) {
                Player mp = Bukkit.getPlayerExact(member);
                if (mp != null) {
                    mp.sendMessage(MSG.color(prefix + "&eClan &f" + clan + " &ehas sent a peace offer (ID: &f" + id + "&e) for war &f" + war.getString("name")));
                    mp.sendMessage(MSG.color("&7Use &a/cls war accept &f" + warID + " " + id + " &7to accept."));
                }
            }
        }
    }

    public void acceptPeace(CommandSender sender, String warID, int id) {
        if (!isPlayer(sender)) return;
        Player p = (Player) sender;

        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (clan == null || !isLeader(p)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can accept peace."));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            p.sendMessage(MSG.color(prefix + "&cWar not found or is no longer active."));
            return;
        }

        String base = "peace-offers." + id;
        if (!war.contains(base)) {
            p.sendMessage(MSG.color(prefix + "&cPeace offer #" + id + " not found."));
            return;
        }
        if (!"pending".equalsIgnoreCase(war.getString(base + ".state"))) {
            p.sendMessage(MSG.color(prefix + "&cThis offer is no longer pending."));
            return;
        }

        String from = war.getString(base + ".from");
        if (clan.equalsIgnoreCase(from)) {
            p.sendMessage(MSG.color(prefix + "&cYou can't accept your own peace offer."));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");

        List<String> offers = war.getStringList(base + ".offers");

        for (String o : offers) {
            String[] s = o.split(":");
            if (s[1].equals("white-peace")) break;

            int amount = Integer.parseInt(s[2]);
            Collection<String> targets;

            switch (s[0]) {
                case "off" -> targets = off;
                case "def" -> targets = def;
                case "moff" -> targets = List.of(off.get(0));
                case "mdef" -> targets = List.of(def.get(0));
                default -> {
                    continue;
                }
            }

            for (String c : targets) {
                FileConfiguration cfg = fh.getClan(c);
                if (s[1].equals("money")) {
                    double newBalance = cfg.getDouble("balance", 0) + amount;
                    double minBal = fh.getConfig().getDouble("clans.balance.min", -10000);
                    if (newBalance < minBal) {
                        p.sendMessage(MSG.color(prefix + "&cClan &e" + c + " &cwould go below minimum balance. Can't accept."));
                        return;
                    }
                    cfg.set("balance", newBalance);
                }
                if (s[1].equals("prestige")) {
                    cfg.set("prestige", cfg.getInt("prestige", 0) + amount);
                }
                fh.saveClan(c, cfg);
            }
        }

        for (String k : Objects.requireNonNull(war.getConfigurationSection("peace-offers")).getKeys(false)) {
            war.set("peace-offers." + k + ".state", k.equals(String.valueOf(id)) ? "accepted" : "expired");
        }

        war.set("active", false);
        fh.saveWar(warID, war);

        applyWarPrestige(war, off, def, fh);

        FileConfiguration cache = fh.getTNL();
        cache.set("truces." + off.get(0).toLowerCase() + "." + def.get(0).toLowerCase(),
                TimeMng.fromNow(fh.getConfig().getString("clans.wars.duration-of-the-truces", "5d")));
        cache.set("wars." + warID + ".expires", TimeMng.fromNow("1w"));
        fh.saveTNL();

        String warName = war.getString("name", warID);
        broadcastToWarParticipants(war, off, def, fh,
                MSG.color(prefix + "&aPeace accepted! War &e" + warName + " &ahas ended."));

        p.sendMessage(MSG.color(prefix + "&aPeace offer #" + id + " accepted. The war is over."));
    }

    private void applyWarPrestige(FileConfiguration war, List<String> off, List<String> def, FileHandler fh) {
        if (!fh.getConfig().getBoolean("clans.prestige.enabled", true)) return;

        int offPoints = war.getInt("stats.offensive.points", 0);
        int defPoints = war.getInt("stats.defensive.points", 0);

        int xpWon = fh.getConfig().getInt("clans.prestige.calc.wars-won", 1000);
        int xpDefeated = fh.getConfig().getInt("clans.prestige.calc.wars-defeated", -1000);
        int xpPerLevel = fh.getConfig().getInt("clans.prestige.acquire-new-prestige", 10000);

        List<String> winners = offPoints >= defPoints ? off : def;
        List<String> losers = offPoints >= defPoints ? def : off;

        for (String c : winners) {
            FileConfiguration cfg = fh.getClan(c);
            if (cfg == null) continue;
            int newPrestige = cfg.getInt("prestige", 0) + xpWon;
            cfg.set("prestige", newPrestige);
            cfg.set("prestige-level", newPrestige / xpPerLevel);
            cfg.set("wars.won", addToList(cfg.getStringList("wars.won"), war.getString("name", "")));
            cfg.set("wars.streak", cfg.getInt("wars.streak", 0) + 1);
            fh.saveClan(c, cfg);
        }

        for (String c : losers) {
            FileConfiguration cfg = fh.getClan(c);
            if (cfg == null) continue;
            int newPrestige = Math.max(0, cfg.getInt("prestige", 0) + xpDefeated);
            cfg.set("prestige", newPrestige);
            cfg.set("prestige-level", newPrestige / xpPerLevel);
            cfg.set("wars.defeated", addToList(cfg.getStringList("wars.defeated"), war.getString("name", "")));
            cfg.set("wars.streak", 0);
            fh.saveClan(c, cfg);
        }
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

            List<String> survivors = inOff ? def : off;
            List<String> losers   = inOff ? off : def;

            for (String c : survivors) {
                FileConfiguration cfg = fh.getClan(c);
                if (cfg == null) continue;
                for (String member : cfg.getStringList("users")) {
                    Player mp = Bukkit.getPlayerExact(member);
                    if (mp != null) {
                        mp.sendMessage(MSG.color(prefix + "&aClan &e" + disbandedClan
                                + " &adisbanded! Your clan won war &e" + warName + " &aby forfeit."));
                    } else {
                        queueNotification(fh, member, "&aClan &f" + disbandedClan
                                + " &adisbanded. Your clan won war &f" + warName + " &aby forfeit.");
                    }
                }
            }

            if (fh.getConfig().getBoolean("clans.prestige.enabled", true)) {
                int xpWon      = fh.getConfig().getInt("clans.prestige.calc.wars-won", 1000);
                int xpPerLevel = fh.getConfig().getInt("clans.prestige.acquire-new-prestige", 10000);

                for (String c : survivors) {
                    FileConfiguration cfg = fh.getClan(c);
                    if (cfg == null) continue;
                    int newPrestige = cfg.getInt("prestige", 0) + xpWon;
                    cfg.set("prestige", newPrestige);
                    cfg.set("prestige-level", newPrestige / xpPerLevel);
                    cfg.set("wars.won", addToList(cfg.getStringList("wars.won"), warName));
                    cfg.set("wars.streak", cfg.getInt("wars.streak", 0) + 1);
                    fh.saveClan(c, cfg);
                }
            }

            war.set("active", false);
            war.set("clans.offensive", off);
            war.set("clans.defensive", def);
            fh.saveWar(warID, war);

            if (!off.isEmpty() && !def.isEmpty()) {
                FileConfiguration cache = fh.getTNL();
                cache.set("truces." + off.get(0).toLowerCase() + "." + def.get(0).toLowerCase(),
                        TimeMng.fromNow(fh.getConfig().getString("clans.wars.duration-of-the-truces", "5d")));
                cache.set("wars." + warID + ".expires", TimeMng.fromNow("1w"));
                fh.saveTNL();
            }
        }
    }

    private List<String> addToList(List<String> list, String value) {
        List<String> result = new ArrayList<>(list);
        if (value != null && !value.isEmpty()) result.add(value);
        return result;
    }

    private void broadcastToWarParticipants(FileConfiguration war, List<String> off, List<String> def,
                                            FileHandler fh, String message) {
        List<String> allClans = new ArrayList<>(off);
        allClans.addAll(def);
        for (String c : allClans) {
            FileConfiguration cfg = fh.getClan(c);
            if (cfg == null) continue;
            for (String member : cfg.getStringList("users")) {
                Player mp = Bukkit.getPlayerExact(member);
                if (mp != null) mp.sendMessage(message);
            }
        }
    }

    public void rejectPeace(CommandSender sender, String warID, int id) {
        if (!(sender instanceof Player p)) return;

        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (clan == null || !isLeader(p)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can reject peace."));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            p.sendMessage(MSG.color(prefix + "&cWar not found or is no longer active."));
            return;
        }

        String base = "peace-offers." + id;
        if (!war.contains(base)) {
            p.sendMessage(MSG.color(prefix + "&cPeace offer #" + id + " not found."));
            return;
        }

        if (!"pending".equalsIgnoreCase(war.getString(base + ".state"))) {
            p.sendMessage(MSG.color(prefix + "&cThis offer is no longer pending."));
            return;
        }

        String from = war.getString(base + ".from");
        if (clan.equalsIgnoreCase(from)) {
            p.sendMessage(MSG.color(prefix + "&cYou can't reject your own peace offer."));
            return;
        }

        war.set(base + ".state", "rejected");
        fh.saveWar(warID, war);

        p.sendMessage(MSG.color(prefix + "&cPeace offer #" + id + " rejected."));

        if (from != null) {
            FileConfiguration fromClan = fh.getClan(from);
            if (fromClan != null) {
                for (String member : fromClan.getStringList("users")) {
                    Player mp = Bukkit.getPlayerExact(member);
                    if (mp != null) {
                        mp.sendMessage(MSG.color(prefix + "&cYour peace offer #" + id
                                + " for war &e" + war.getString("name") + " &cwas rejected."));
                    }
                }
            }
        }
    }

    public void leaveWar(CommandSender sender, String warID) {
        if (!isPlayer(sender)) return;
        Player p = (Player) sender;

        FileHandler fh = plugin.getFH();
        String clan = fh.getPlayerData(p.getUniqueId()).getString("clan");
        if (clan == null || !isLeader(p)) {
            p.sendMessage(MSG.color(prefix + "&cOnly the clan leader can leave a war."));
            return;
        }

        FileConfiguration war = fh.getWar(warID);
        if (war == null || !war.getBoolean("active")) {
            p.sendMessage(MSG.color(prefix + "&cWar not found or is no longer active."));
            return;
        }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");

        if (off.get(0).equalsIgnoreCase(clan) || def.get(0).equalsIgnoreCase(clan)) {
            p.sendMessage(MSG.color(prefix + "&cThe main warring clans cannot leave. Send a peace offer instead."));
            return;
        }

        if (!off.contains(clan) && !def.contains(clan)) {
            p.sendMessage(MSG.color(prefix + "&cYour clan is not part of this war."));
            return;
        }

        off.remove(clan);
        def.remove(clan);
        war.set("clans.offensive", off);
        war.set("clans.defensive", def);
        war.set("influences." + clan, null);

        fh.saveWar(warID, war);
        p.sendMessage(MSG.color(prefix + "&aYour clan has withdrawn from war &e" + war.getString("name") + "&a."));
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

        player.sendMessage(MSG.color(prefix + "&e&lYou have pending notifications:"));
        for (String msg : messages) {
            for (String line : msg.split("\n"))
                player.sendMessage(MSG.color("&7" + line));
            player.sendMessage("");
        }

        tnl.set(base, null);
        fh.saveTNL();
    }
}