package x.Entt.ClansX.Events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static x.Entt.ClansX.CX.prefix;

public class Wars implements Listener {

    private final CX plugin;

    public enum Screen {
        MAIN,
        DECLARE,
        MY_WARS,
        WAR_DETAIL,
        ALL_WARS,
        JOIN_WAR,
        PROPOSE_PEACE_SELECT,
        OFFERS_SELECT,
        WITHDRAW_SELECT,
        WAR_STATS,
        PEACE_TYPE_SELECT,
        PEACE_MONEY_INPUT,
        PEACE_XP_INPUT
    }

    private final Player player;
    private final FileHandler fh;

    private Screen currentScreen = Screen.MAIN;
    private final List<Screen> screenHistory = new ArrayList<>();

    private String selectedWarID = null;
    private Inventory currentInv = null;
    private boolean navigating = false;

    private long inputAmount = 0;
    private long inputStep = 1;

    private final List<String> selectedTerms = new ArrayList<>();
    private String termTarget = null;

    public Wars(CX plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.fh = plugin.getFH();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        openMain();
    }

    private void navigate(Screen target) {
        screenHistory.add(currentScreen);
        currentScreen = target;
    }

    private void goBack() {
        if (screenHistory.isEmpty()) {
            currentScreen = Screen.MAIN;
            openMain();
            return;
        }
        Screen previous = screenHistory.remove(screenHistory.size() - 1);
        currentScreen = previous;
        reopenScreen(previous);
    }

    private void reopenScreen(Screen screen) {
        switch (screen) {
            case MAIN                  -> openMain();
            case DECLARE               -> openDeclare();
            case MY_WARS               -> openMyWars();
            case WAR_DETAIL            -> { if (selectedWarID != null) openWarDetail(selectedWarID); else openMyWars(); }
            case ALL_WARS              -> openAllWars();
            case JOIN_WAR              -> openJoinWar();
            case PROPOSE_PEACE_SELECT  -> openProposePeaceWarSelect();
            case OFFERS_SELECT         -> openOffersWarSelect();
            case WITHDRAW_SELECT       -> openWithdrawWarSelect();
            case WAR_STATS             -> openWarStats();
            case PEACE_TYPE_SELECT     -> openPeaceTypeSelect();
            case PEACE_MONEY_INPUT     -> openMoneyInput();
            case PEACE_XP_INPUT        -> openXPInput();
        }
    }

    public void openMain() {
        currentScreen = Screen.MAIN;
        screenHistory.clear();

        String pClan = getPlayerClan();
        boolean inClan = pClan != null && !pClan.isEmpty();
        boolean leader = inClan && isLeader();

        Inventory inv = Bukkit.createInventory(null, 54, MSG.color("&8⚔ &c&lWar Manager &8⚔"));

        inv.setItem(10, makeItem(Material.NETHERITE_SWORD,
                "&c&lDeclare War",
                List.of(
                        "&7Challenge another clan to war.",
                        "&7Only the clan leader can do this.",
                        "",
                        leader ? "&aYou can declare war." : "&cRequires leader role."
                )));

        inv.setItem(12, makeItem(Material.SHIELD,
                "&e&lMy Wars",
                List.of(
                        "&7View and manage wars",
                        "&7your clan is currently involved in.",
                        "",
                        inClan ? "&aClick to browse." : "&cYou are not in a clan."
                )));

        inv.setItem(14, makeItem(Material.SPYGLASS,
                "&b&lAll Active Wars",
                List.of(
                        "&7Browse every ongoing war",
                        "&7happening on the server.",
                        "",
                        "&aAlways available."
                )));

        inv.setItem(16, makeItem(Material.ORANGE_BANNER,
                "&6&lJoin a War",
                List.of(
                        "&7Support another clan as an ally.",
                        "&7Choose a side: offensive or defensive.",
                        "",
                        leader ? "&aClick to see available wars." : "&cRequires leader role."
                )));

        inv.setItem(28, makeItem(Material.WHITE_BANNER,
                "&f&lPropose Peace",
                List.of(
                        "&7Open peace negotiations",
                        "&7for one of your active wars.",
                        "",
                        leader ? "&aClick to choose a war." : "&cRequires leader role."
                )));

        inv.setItem(30, makeItem(Material.PAPER,
                "&a&lAccept / Reject Offers",
                List.of(
                        "&7Review incoming peace offers",
                        "&7from enemy clans.",
                        "",
                        leader ? "&aClick to manage offers." : "&cRequires leader role."
                )));

        inv.setItem(32, makeItem(Material.FEATHER,
                "&7&lWithdraw from War",
                List.of(
                        "&7Leave a war your clan joined",
                        "&7as an allied force.",
                        "&8(Cannot withdraw if you started it.)",
                        "",
                        leader ? "&eClick to choose a war." : "&cRequires leader role."
                )));

        inv.setItem(34, makeItem(Material.BOOK,
                "&d&lWar History / Stats",
                List.of(
                        "&7View your clan's war record.",
                        "&7Wins, losses, streaks and kills.",
                        "",
                        inClan ? "&aClick to view stats." : "&cYou are not in a clan."
                )));

        fill(inv);
        open(inv);
    }

    private void openDeclare() {
        currentScreen = Screen.DECLARE;
        String pClan = getPlayerClan();

        List<String> clans = fh.getClanNames();
        clans.remove(pClan);
        clans.removeIf(c -> plugin.getWarSystem().isAlreadyAtWar(pClan, c)
                || plugin.getWarSystem().isInTruce(pClan, c));

        int size = clampSize(clans.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&c&lDeclare War — Choose Target"));

        for (int i = 0; i < clans.size() && i < size - 9; i++) {
            String clanName = clans.get(i);
            FileConfiguration cfg = fh.getClan(clanName);
            if (cfg == null) continue;

            List<String> lore = new ArrayList<>();
            lore.add("&7Leader: &f" + cfg.getString("leader", "?"));
            lore.add("&7Members: &f" + cfg.getStringList("users").size());
            lore.add("&7Prestige: &6" + cfg.getInt("prestige", 0));
            lore.add("");
            lore.add("&c&lClick to declare war!");

            inv.setItem(i, makeItem(Material.RED_BANNER, "&c" + clanName, lore));
        }

        if (clans.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No available clans",
                    List.of("&8All clans are already at war or in truce with you.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openMyWars() {
        currentScreen = Screen.MY_WARS;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&e&lMy Active Wars"));

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add("&e&lClick to manage this war");

            Material mat = war.getStringList("clans.offensive").contains(pClan)
                    ? Material.ORANGE_BANNER : Material.BLUE_BANNER;

            inv.setItem(i, makeItem(mat, "&e" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No active wars", List.of("&8Your clan is at peace.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openJoinWar() {
        currentScreen = Screen.JOIN_WAR;
        String pClan = getPlayerClan();
        List<String> allWars = fh.getActiveWars();
        List<String> available = new ArrayList<>();

        for (String wid : allWars) {
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;
            List<String> off = war.getStringList("clans.offensive");
            List<String> def = war.getStringList("clans.defensive");
            if (off.contains(pClan) || def.contains(pClan)) continue;
            available.add(wid);
        }

        int size = clampSize(available.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&6&lJoin a War — Choose Side"));

        for (int i = 0; i < available.size() && i < size - 9; i++) {
            String wid = available.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add("&eLeft-click: &cJoin Offensive");
            lore.add("&eRight-click: &aJoin Defensive");

            inv.setItem(i, makeItem(Material.ORANGE_BANNER, "&6" + war.getString("name", wid), lore));
        }

        if (available.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No joinable wars",
                    List.of("&8All wars are already joined or you're in all of them.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openProposePeaceWarSelect() {
        currentScreen = Screen.PROPOSE_PEACE_SELECT;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&f&lPropose Peace — Choose War"));

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add("&eClick to open peace terms.");

            inv.setItem(i, makeItem(Material.WHITE_BANNER, "&f" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No active wars",
                    List.of("&8Your clan has no ongoing wars.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openOffersWarSelect() {
        currentScreen = Screen.OFFERS_SELECT;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&a&lAccept / Reject — Choose War"));

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            boolean hasOffers = war.contains("peace-offers") &&
                    Objects.requireNonNull(war.getConfigurationSection("peace-offers")).getKeys(false)
                            .stream().anyMatch(k ->
                                    "pending".equalsIgnoreCase(war.getString("peace-offers." + k + ".state", ""))
                                            && !war.getString("peace-offers." + k + ".from", "").equalsIgnoreCase(pClan));

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add(hasOffers ? "&aHas pending offers — click to review." : "&7No incoming offers.");

            Material mat = hasOffers ? Material.PAPER : Material.GRAY_DYE;
            inv.setItem(i, makeItem(mat, "&a" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No active wars",
                    List.of("&8Your clan has no ongoing wars.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openWithdrawWarSelect() {
        currentScreen = Screen.WITHDRAW_SELECT;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&7&lWithdraw — Choose War"));

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> off = war.getStringList("clans.offensive");
            List<String> def = war.getStringList("clans.defensive");
            boolean isMain = (!off.isEmpty() && off.get(0).equalsIgnoreCase(pClan))
                    || (!def.isEmpty() && def.get(0).equalsIgnoreCase(pClan));

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add(isMain ? "&cMain clans cannot withdraw." : "&eClick to withdraw from this war.");

            Material mat = isMain ? Material.BARRIER : Material.FEATHER;
            inv.setItem(i, makeItem(mat, "&7" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No active wars",
                    List.of("&8Your clan is not in any wars.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openWarStats() {
        currentScreen = Screen.WAR_STATS;
        String pClan = getPlayerClan();
        if (pClan == null || pClan.isEmpty()) return;

        FileConfiguration clan = fh.getClan(pClan);
        if (clan == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, MSG.color("&d&lWar Stats — &e" + pClan));

        int won      = clan.getStringList("wars.won").size();
        int defeated = clan.getStringList("wars.defeated").size();
        int active   = clan.getStringList("wars.active").size();
        int streak   = clan.getInt("wars.streak", 0);
        int kills    = clan.getInt("kills", 0);

        inv.setItem(10, makeItem(Material.GOLDEN_SWORD,
                "&6&lTotal Kills", List.of("&7" + kills + " kills registered.")));
        inv.setItem(12, makeItem(Material.EMERALD,
                "&a&lWars Won", List.of("&7" + won + " wars won.")));
        inv.setItem(14, makeItem(Material.REDSTONE,
                "&c&lWars Defeated", List.of("&7" + defeated + " wars lost.")));
        inv.setItem(16, makeItem(Material.BLAZE_ROD,
                "&e&lWin Streak", List.of("&7Current streak: &6" + streak)));

        if (active > 0) {
            inv.setItem(4, makeItem(Material.NETHERITE_SWORD,
                    "&c&lActive Wars", List.of("&7Currently in &c" + active + " &7active wars.")));
        }

        inv.setItem(22, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openAllWars() {
        currentScreen = Screen.ALL_WARS;
        List<String> warIDs = fh.getActiveWars();

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.color("&b&lAll Active Wars"));

        String pClan = getPlayerClan();

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            inv.setItem(i, makeItem(Material.RED_BANNER, "&f" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER, "&7No active wars",
                    List.of("&8The server is at peace.")));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openWarDetail(String warID) {
        currentScreen = Screen.WAR_DETAIL;
        selectedWarID = warID;
        String pClan = getPlayerClan();

        FileConfiguration war = fh.getWar(warID);
        if (war == null) { goBack(); return; }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        boolean isParticipant = off.contains(pClan) || def.contains(pClan);
        boolean isMainClan    = (!off.isEmpty() && off.get(0).equalsIgnoreCase(pClan))
                || (!def.isEmpty() && def.get(0).equalsIgnoreCase(pClan));
        boolean leader = isLeader();

        Inventory inv = Bukkit.createInventory(null, 54,
                MSG.color("&c" + war.getString("name", warID)));

        List<String> warInfo = new ArrayList<>();
        warInfo.add("&7Offensive: &c" + String.join(", ", off));
        warInfo.add("&7Defensive: &a" + String.join(", ", def));
        warInfo.add("");
        warInfo.add("&cOffensive stats:");
        warInfo.add("  &7Kills: &f"  + war.getInt("stats.offensive.kills"));
        warInfo.add("  &7Deaths: &f" + war.getInt("stats.offensive.deaths"));
        warInfo.add("  &7Points: &6" + war.getInt("stats.offensive.points"));
        warInfo.add("");
        warInfo.add("&aDefensive stats:");
        warInfo.add("  &7Kills: &f"  + war.getInt("stats.defensive.kills"));
        warInfo.add("  &7Deaths: &f" + war.getInt("stats.defensive.deaths"));
        warInfo.add("  &7Points: &6" + war.getInt("stats.defensive.points"));
        inv.setItem(4, makeItem(Material.BOOK, "&e&lWar Info", warInfo));

        if (isParticipant && war.contains("peace-offers")) {
            int slot = 19;
            for (String k : Objects.requireNonNull(
                    war.getConfigurationSection("peace-offers")).getKeys(false)) {
                String state = war.getString("peace-offers." + k + ".state", "");
                if (!"pending".equalsIgnoreCase(state)) continue;

                String from   = war.getString("peace-offers." + k + ".from", "?");
                List<String> offers = war.getStringList("peace-offers." + k + ".offers");

                List<String> lore = new ArrayList<>();
                lore.add("&7From: &f" + from);
                lore.add("&7Terms:");
                offers.forEach(o -> lore.add("  &e" + formatTermForDisplay(o)));

                if (leader && !from.equalsIgnoreCase(pClan)) {
                    lore.add("");
                    lore.add("&aLeft-click: &2Accept");
                    lore.add("&cRight-click: &4Reject");
                }

                inv.setItem(slot, makeItem(Material.PAPER, "&ePeace Offer #" + k, lore));
                if (++slot > 25) break;
            }
        }

        if (leader && isParticipant) {
            inv.setItem(38, makeItem(Material.WHITE_BANNER, "&f&lPropose Peace",
                    List.of("&7Open the peace terms menu.", "&eClick to choose your terms.")));

            if (!isMainClan) {
                inv.setItem(40, makeItem(Material.FEATHER, "&7&lWithdraw from War",
                        List.of("&7Leave this war (allied clans only).")));
            }
        }

        inv.setItem(49, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openPeaceTypeSelect() {
        if (selectedWarID == null || fh.getWar(selectedWarID) == null) {
            player.sendMessage(MSG.color(prefix + "&cNo war selected. Please try again."));
            openMyWars();
            return;
        }

        if (termTarget == null) {
            recalculateTermTarget();
        }

        currentScreen = Screen.PEACE_TYPE_SELECT;
        FileConfiguration cfg = fh.getConfig();
        boolean multiTerms = cfg.getBoolean("clans.wars.peace-terms.multi-terms", false);

        FileConfiguration war = fh.getWar(selectedWarID);
        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        String pClan = getPlayerClan();
        boolean pIsOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(pClan));
        String opponentClan = pIsOff
                ? (def.isEmpty() ? "?" : def.get(0))
                : (off.isEmpty() ? "?" : off.get(0));

        Inventory inv = Bukkit.createInventory(null, 54,
                MSG.color("&8Peace Terms &7— &fChoose Type"));

        if (multiTerms && !selectedTerms.isEmpty()) {
            List<String> termLore = new ArrayList<>();
            termLore.add("&7Selected so far:");
            selectedTerms.forEach(t -> termLore.add("  &a✔ &f" + formatTermForDisplay(t)));
            termLore.add("");
            termLore.add("&eClick &7GREEN WOOL &eto send all.");
            inv.setItem(4, makeItem(Material.PAPER,
                    "&e&lSelected Terms (" + selectedTerms.size() + ")", termLore));
        }

        int slot = 10;

        if (cfg.getBoolean("clans.wars.peace-terms.white-peace", false)) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Both clans walk away — nobody wins.");
            if (multiTerms) lore.add("&e(Cannot combine with other terms)");
            lore.add("");
            lore.add("&eClick to " + (multiTerms ? "select" : "propose"));
            inv.setItem(slot, makeItem(Material.WHITE_BANNER, "&f&lWhite Peace", lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.money", false)) {
            FileConfiguration opponentCfg = fh.getClan(opponentClan);
            double opponentBalance = opponentCfg != null ? opponentCfg.getDouble("balance", 0) : 0;

            List<String> lore = new ArrayList<>();
            lore.add("&7Demand money from &f" + opponentClan + "&7.");
            lore.add("&7Their current balance: &6$" + String.format("%.2f", opponentBalance));
            lore.add("");
            lore.add("&eClick to set the amount.");
            inv.setItem(slot, makeItem(Material.GOLD_INGOT, "&6&lMoney Demand", lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.xp", false)) {
            FileConfiguration opponentCfg = fh.getClan(opponentClan);
            int opponentPrestige = opponentCfg != null ? opponentCfg.getInt("prestige", 0) : 0;
            int xpPerLevel = cfg.getInt("clans.prestige.acquire-new-prestige", 10000);
            int opponentLevel = xpPerLevel > 0 ? opponentPrestige / xpPerLevel : 0;

            List<String> lore = new ArrayList<>();
            lore.add("&7Demand XP/prestige from &f" + opponentClan + "&7.");
            lore.add("&7Their prestige: &b" + opponentPrestige + " &7(Level &e" + opponentLevel + "&7)");
            lore.add("");
            lore.add("&eClick to set the amount.");
            inv.setItem(slot, makeItem(Material.EXPERIENCE_BOTTLE, "&b&lXP / Prestige Demand", lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.disband", false)) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Demand that &f" + opponentClan + " &7disband.");
            lore.add("&cThis permanently destroys their clan.");
            lore.add("");
            lore.add("&eClick to " + (multiTerms ? "add to terms" : "propose"));
            inv.setItem(slot, makeItem(Material.NETHER_BRICK, "&c&lDemand Disbandment", lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.unify-clans", false)) {
            String mainOff = off.isEmpty() ? "?" : off.get(0);
            String mainDef = def.isEmpty() ? "?" : def.get(0);
            String newName = buildUnifiedName(mainOff, mainDef);

            List<String> lore = new ArrayList<>();
            lore.add("&7Merge both main clans into one.");
            lore.add("&7New clan name: &e" + newName);
            lore.add("&7Prestige, members, and chest are combined.");
            lore.add("");
            lore.add("&eClick to " + (multiTerms ? "add to terms" : "propose"));
            inv.setItem(slot, makeItem(Material.NETHER_STAR, "&d&lUnify Clans", lore));
        }

        if (multiTerms && !selectedTerms.isEmpty()) {
            inv.setItem(48, makeItem(Material.GREEN_WOOL, "&a&lSend Peace Offer",
                    List.of("&7Send all selected terms.", "&aClick to confirm.")));
            inv.setItem(50, makeItem(Material.RED_WOOL, "&c&lClear Selection",
                    List.of("&7Remove all selected terms.")));
        }

        inv.setItem(49, makeItem(Material.ARROW, "&7← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openMoneyInput() {
        currentScreen = Screen.PEACE_MONEY_INPUT;

        FileConfiguration war = fh.getWar(selectedWarID);
        if (war == null) { openMyWars(); return; }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        String pClan = getPlayerClan();
        boolean pIsOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(pClan));
        String opponentClan = pIsOff
                ? (def.isEmpty() ? "?" : def.get(0))
                : (off.isEmpty() ? "?" : off.get(0));

        FileConfiguration opponentCfg = fh.getClan(opponentClan);
        double opponentBalance = opponentCfg != null ? opponentCfg.getDouble("balance", 0) : 0;

        Inventory inv = Bukkit.createInventory(null, 27,
                MSG.color("&6Money Demand &8— &fSet Amount"));

        inv.setItem(3, makeItem(Material.PAPER, "&e&lAmount: &f$" + String.format("%.0f", (double) inputAmount),
                List.of(
                        "&7Opponent: &f" + opponentClan,
                        "&7Their balance: &6$" + String.format("%.2f", opponentBalance),
                        "",
                        "&7Step: &f" + inputStep
                )));

        inv.setItem(4, makeItem(Material.GOLD_INGOT,
                "&b&lOpponent Info",
                List.of(
                        "&7Clan: &f" + opponentClan,
                        "&7Balance: &6$" + String.format("%.2f", opponentBalance)
                )));

        inv.setItem(12, makeItem(Material.LIME_DYE,  "&a▲ Increase", List.of("&7+" + inputStep)));
        inv.setItem(14, makeItem(Material.RED_DYE,   "&c▼ Decrease", List.of("&7-" + inputStep)));

        inv.setItem(0, makeItem(Material.GRAY_DYE, "&7Step: &f" + inputStep,
                List.of("&7Left-click: ÷10", "&7Right-click: ×10")));
        inv.setItem(8, makeItem(Material.GRAY_DYE, "&7Step: &f" + inputStep,
                List.of("&7Left-click: ÷10", "&7Right-click: ×10")));

        inv.setItem(2, makeItem(Material.EMERALD, "&a&lSend",   List.of("&7Propose this money demand.")));
        inv.setItem(6, makeItem(Material.BARRIER,  "&c&lCancel", List.of("&7Go back.")));

        fill(inv);
        open(inv);
    }

    private void openXPInput() {
        currentScreen = Screen.PEACE_XP_INPUT;

        FileConfiguration war = fh.getWar(selectedWarID);
        if (war == null) { openMyWars(); return; }

        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        String pClan = getPlayerClan();
        boolean pIsOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(pClan));
        String opponentClan = pIsOff
                ? (def.isEmpty() ? "?" : def.get(0))
                : (off.isEmpty() ? "?" : off.get(0));

        FileConfiguration opponentCfg = fh.getClan(opponentClan);
        int opponentPrestige = opponentCfg != null ? opponentCfg.getInt("prestige", 0) : 0;

        Inventory inv = Bukkit.createInventory(null, 27,
                MSG.color("&bXP Demand &8— &fSet Amount"));

        inv.setItem(3, makeItem(Material.EXPERIENCE_BOTTLE,
                "&b&lAmount: &f" + inputAmount + " XP",
                List.of(
                        "&7Opponent: &f" + opponentClan,
                        "&7Their prestige: &b" + opponentPrestige,
                        "",
                        "&7Step: &f" + inputStep
                )));

        inv.setItem(4, makeItem(Material.EXPERIENCE_BOTTLE,
                "&b&lOpponent Info",
                List.of(
                        "&7Clan: &f" + opponentClan,
                        "&7Prestige: &b" + opponentPrestige
                )));

        inv.setItem(12, makeItem(Material.LIME_DYE, "&a▲ Increase", List.of("&7+" + inputStep)));
        inv.setItem(14, makeItem(Material.RED_DYE,  "&c▼ Decrease", List.of("&7-" + inputStep)));

        inv.setItem(0, makeItem(Material.GRAY_DYE, "&7Step: &f" + inputStep,
                List.of("&7Left-click: ÷10", "&7Right-click: ×10")));
        inv.setItem(8, makeItem(Material.GRAY_DYE, "&7Step: &f" + inputStep,
                List.of("&7Left-click: ÷10", "&7Right-click: ×10")));

        inv.setItem(2, makeItem(Material.EMERALD, "&a&lSend",   List.of("&7Propose this XP demand.")));
        inv.setItem(6, makeItem(Material.BARRIER,  "&c&lCancel", List.of("&7Go back.")));

        fill(inv);
        open(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.equals(player)) return;
        if (currentInv == null) return;
        if (!e.getView().getTopInventory().equals(currentInv)) return;

        e.setCancelled(true);

        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(currentInv)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        String displayName = clicked.getItemMeta() != null
                ? clicked.getItemMeta().getDisplayName() : "";
        String cleanName = MSG.color(displayName).replaceAll("§.", "").trim();

        if (clicked.getType() == Material.ARROW) {
            goBack();
            return;
        }

        if (currentScreen == Screen.MAIN) {
            String pClan = getPlayerClan();
            boolean inClan = pClan != null && !pClan.isEmpty();

            switch (clicked.getType()) {
                case NETHERITE_SWORD -> {
                    if (!inClan || !isLeader()) { sendNoPermission(inClan); return; }
                    navigate(Screen.MAIN);
                    openDeclare();
                }
                case SHIELD -> {
                    if (!inClan) { player.sendMessage(MSG.color(prefix + "&cYou are not in a clan.")); return; }
                    navigate(Screen.MAIN);
                    openMyWars();
                }
                case SPYGLASS -> {
                    navigate(Screen.MAIN);
                    openAllWars();
                }
                case ORANGE_BANNER -> {
                    if (!inClan || !isLeader()) { sendNoPermission(inClan); return; }
                    navigate(Screen.MAIN);
                    openJoinWar();
                }
                case WHITE_BANNER -> {
                    if (!inClan || !isLeader()) { sendNoPermission(inClan); return; }
                    navigate(Screen.MAIN);
                    openProposePeaceWarSelect();
                }
                case PAPER -> {
                    if (!inClan || !isLeader()) { sendNoPermission(inClan); return; }
                    navigate(Screen.MAIN);
                    openOffersWarSelect();
                }
                case FEATHER -> {
                    if (!inClan || !isLeader()) { sendNoPermission(inClan); return; }
                    navigate(Screen.MAIN);
                    openWithdrawWarSelect();
                }
                case BOOK -> {
                    if (!inClan) { player.sendMessage(MSG.color(prefix + "&cYou are not in a clan.")); return; }
                    navigate(Screen.MAIN);
                    openWarStats();
                }
            }
            return;
        }

        if (currentScreen == Screen.DECLARE && clicked.getType() == Material.RED_BANNER) {
            String pClan = getPlayerClan();
            if (pClan == null) return;
            plugin.getWarSystem().startWar(player, pClan, cleanName);
            player.closeInventory();
            return;
        }

        if (currentScreen == Screen.JOIN_WAR && clicked.getType() == Material.ORANGE_BANNER) {
            String pClan = getPlayerClan();
            List<String> allWars = fh.getActiveWars();
            List<String> available = new ArrayList<>();
            for (String wid : allWars) {
                FileConfiguration war = fh.getWar(wid);
                if (war == null) continue;
                List<String> off = war.getStringList("clans.offensive");
                List<String> def = war.getStringList("clans.defensive");
                if (off.contains(pClan) || def.contains(pClan)) continue;
                available.add(wid);
            }
            int slot = e.getSlot();
            if (slot < available.size()) {
                plugin.getWarSystem().joinToWar(player, available.get(slot), e.isRightClick());
                player.closeInventory();
            }
            return;
        }

        if (currentScreen == Screen.MY_WARS) {
            if (clicked.getType() == Material.ORANGE_BANNER
                    || clicked.getType() == Material.BLUE_BANNER) {
                String pClan = getPlayerClan();
                List<String> warIDs = getWarsOfClan(pClan);
                int slot = e.getSlot();
                if (slot < warIDs.size()) {
                    navigate(Screen.MY_WARS);
                    openWarDetail(warIDs.get(slot));
                }
            }
            return;
        }

        if (currentScreen == Screen.PROPOSE_PEACE_SELECT
                && clicked.getType() == Material.WHITE_BANNER) {
            String pClan = getPlayerClan();
            List<String> warIDs = getWarsOfClan(pClan);
            int slot = e.getSlot();
            if (slot < warIDs.size()) {
                selectedWarID = warIDs.get(slot);
                resetInput();
                selectedTerms.clear();
                recalculateTermTarget();
                navigate(Screen.PROPOSE_PEACE_SELECT);
                openPeaceTypeSelect();
            }
            return;
        }

        if (currentScreen == Screen.OFFERS_SELECT
                && (clicked.getType() == Material.PAPER
                || clicked.getType() == Material.GRAY_DYE)) {
            String pClan = getPlayerClan();
            List<String> warIDs = getWarsOfClan(pClan);
            int slot = e.getSlot();
            if (slot < warIDs.size()) {
                selectedWarID = warIDs.get(slot);
                navigate(Screen.OFFERS_SELECT);
                openWarDetail(selectedWarID);
            }
            return;
        }

        if (currentScreen == Screen.WITHDRAW_SELECT
                && clicked.getType() == Material.FEATHER) {
            String pClan = getPlayerClan();
            List<String> warIDs = getWarsOfClan(pClan);
            int slot = e.getSlot();
            if (slot < warIDs.size()) {
                String wid = warIDs.get(slot);
                FileConfiguration war = fh.getWar(wid);
                if (war == null) return;
                List<String> off = war.getStringList("clans.offensive");
                List<String> def = war.getStringList("clans.defensive");
                boolean isMain = (!off.isEmpty() && off.get(0).equalsIgnoreCase(pClan))
                        || (!def.isEmpty() && def.get(0).equalsIgnoreCase(pClan));
                if (!isMain) {
                    plugin.getWarSystem().leaveWar(player, wid);
                    player.closeInventory();
                } else {
                    player.sendMessage(MSG.color(prefix + "&cMain clans cannot withdraw from their own war."));
                }
            }
            return;
        }

        if (currentScreen == Screen.WAR_DETAIL && selectedWarID != null) {
            FileConfiguration war = fh.getWar(selectedWarID);
            if (war == null) return;
            String pClan = getPlayerClan();

            if (clicked.getType() == Material.WHITE_BANNER) {
                resetInput();
                selectedTerms.clear();
                recalculateTermTarget();
                navigate(Screen.WAR_DETAIL);
                openPeaceTypeSelect();
                return;
            }

            if (clicked.getType() == Material.FEATHER) {
                plugin.getWarSystem().leaveWar(player, selectedWarID);
                player.closeInventory();
                return;
            }

            if (clicked.getType() == Material.PAPER && cleanName.startsWith("Peace Offer #")) {
                try {
                    int offerID = Integer.parseInt(cleanName.replace("Peace Offer #", "").trim());
                    String from = war.getString("peace-offers." + offerID + ".from", "");
                    if (from.equalsIgnoreCase(pClan)) {
                        player.sendMessage(MSG.color(prefix + "&cYou can't accept your own offer."));
                        return;
                    }
                    if (e.isRightClick()) {
                        plugin.getWarSystem().rejectPeace(player, selectedWarID, offerID);
                    } else {
                        plugin.getWarSystem().acceptPeace(player, selectedWarID, offerID);
                    }
                    player.closeInventory();
                } catch (NumberFormatException ignored) {}
            }
            return;
        }

        if (currentScreen == Screen.PEACE_TYPE_SELECT) {
            boolean multiTerms = fh.getConfig().getBoolean("clans.wars.peace-terms.multi-terms", false);

            if (clicked.getType() == Material.GREEN_WOOL) {
                if (!selectedTerms.isEmpty()) {
                    plugin.getWarSystem().sendPeaceTerms(player, selectedWarID, new ArrayList<>(selectedTerms));
                    selectedTerms.clear();
                    player.closeInventory();
                }
                return;
            }

            if (clicked.getType() == Material.RED_WOOL) {
                selectedTerms.clear();
                openPeaceTypeSelect();
                return;
            }

            if (clicked.getType() == Material.WHITE_BANNER) {
                if (multiTerms) {
                    selectedTerms.clear();
                    selectedTerms.add(termTarget + ":white-peace");
                }
                plugin.getWarSystem().sendPeaceTerms(player, selectedWarID,
                        multiTerms ? new ArrayList<>(selectedTerms)
                                : List.of(termTarget + ":white-peace"));
                selectedTerms.clear();
                player.closeInventory();
                return;
            }

            if (clicked.getType() == Material.GOLD_INGOT) {
                resetInput();
                navigate(Screen.PEACE_TYPE_SELECT);
                openMoneyInput();
                return;
            }

            if (clicked.getType() == Material.EXPERIENCE_BOTTLE) {
                resetInput();
                navigate(Screen.PEACE_TYPE_SELECT);
                openXPInput();
                return;
            }

            if (clicked.getType() == Material.NETHER_BRICK) {
                String term = termTarget + ":disband";
                if (multiTerms) {
                    if (!selectedTerms.contains(term)) selectedTerms.add(term);
                    openPeaceTypeSelect();
                } else {
                    plugin.getWarSystem().sendPeaceTerms(player, selectedWarID, List.of(term));
                    player.closeInventory();
                }
                return;
            }

            if (clicked.getType() == Material.NETHER_STAR) {
                String term = termTarget + ":unify-clans";
                if (multiTerms) {
                    if (!selectedTerms.contains(term)) selectedTerms.add(term);
                    openPeaceTypeSelect();
                } else {
                    plugin.getWarSystem().sendPeaceTerms(player, selectedWarID, List.of(term));
                    player.closeInventory();
                }
                return;
            }
        }

        if (currentScreen == Screen.PEACE_MONEY_INPUT) {
            switch (clicked.getType()) {
                case LIME_DYE -> { inputAmount = Math.max(0, inputAmount + inputStep); openMoneyInput(); }
                case RED_DYE  -> { inputAmount = Math.max(0, inputAmount - inputStep); openMoneyInput(); }
                case GRAY_DYE -> {
                    if (e.isRightClick()) inputStep = Math.min(inputStep * 10, 1_000_000_000L);
                    else inputStep = Math.max(1, inputStep / 10);
                    openMoneyInput();
                }
                case EMERALD -> {
                    String term = termTarget + ":money:" + inputAmount;
                    boolean multi = fh.getConfig().getBoolean("clans.wars.peace-terms.multi-terms", false);
                    if (multi) {
                        selectedTerms.removeIf(t -> t.contains(":money:"));
                        selectedTerms.add(term);
                        resetInput();
                        openPeaceTypeSelect();
                    } else {
                        plugin.getWarSystem().sendPeaceTerms(player, selectedWarID, List.of(term));
                        player.closeInventory();
                    }
                }
                case BARRIER -> { resetInput(); openPeaceTypeSelect(); }
                default -> {}
            }
            return;
        }

        if (currentScreen == Screen.PEACE_XP_INPUT) {
            switch (clicked.getType()) {
                case LIME_DYE -> { inputAmount = Math.max(0, inputAmount + inputStep); openXPInput(); }
                case RED_DYE  -> { inputAmount = Math.max(0, inputAmount - inputStep); openXPInput(); }
                case GRAY_DYE -> {
                    if (e.isRightClick()) inputStep = Math.min(inputStep * 10, 1_000_000_000L);
                    else inputStep = Math.max(1, inputStep / 10);
                    openXPInput();
                }
                case EMERALD -> {
                    String term = termTarget + ":xp:" + inputAmount;
                    boolean multi = fh.getConfig().getBoolean("clans.wars.peace-terms.multi-terms", false);
                    if (multi) {
                        selectedTerms.removeIf(t -> t.contains(":xp:"));
                        selectedTerms.add(term);
                        resetInput();
                        openPeaceTypeSelect();
                    } else {
                        plugin.getWarSystem().sendPeaceTerms(player, selectedWarID, List.of(term));
                        player.closeInventory();
                    }
                }
                case BARRIER -> { resetInput(); openPeaceTypeSelect(); }
                default -> {}
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (navigating) return;
        HandlerList.unregisterAll(this);
    }

    private void open(Inventory inv) {
        currentInv = inv;
        navigating = true;
        player.openInventory(inv);
        navigating = false;
    }

    private void resetInput() {
        inputAmount = 0;
        inputStep = 1;
    }

    private void recalculateTermTarget() {
        if (selectedWarID == null) { termTarget = "mdef"; return; }
        FileConfiguration war = fh.getWar(selectedWarID);
        if (war == null) { termTarget = "mdef"; return; }
        String pClan = getPlayerClan();
        List<String> off = war.getStringList("clans.offensive");
        boolean pIsOff = off.stream().anyMatch(c -> c.equalsIgnoreCase(pClan));
        termTarget = pIsOff ? "mdef" : "moff";
    }

    private void sendNoPermission(boolean inClan) {
        if (!inClan) {
            player.sendMessage(MSG.color(prefix + "&cYou are not in a clan."));
        } else {
            player.sendMessage(MSG.color(prefix + "&cOnly the clan leader can do this."));
        }
    }

    private int clampSize(int needed) {
        int size = (int) Math.ceil(needed / 9.0) * 9;
        return Math.min(Math.max(size, 27), 54);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(MSG.color(name));
        List<String> coloredLore = new ArrayList<>();
        lore.forEach(l -> coloredLore.add(MSG.color(l)));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv) {
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack current = inv.getItem(i);
            if (current == null || current.getType() == Material.AIR)
                inv.setItem(i, filler);
        }
    }

    private String getPlayerClan() {
        FileConfiguration pd = fh.getPlayerData(player.getUniqueId());
        return pd != null ? pd.getString("clan") : null;
    }

    private boolean isLeader() {
        String pClan = getPlayerClan();
        if (pClan == null || pClan.isEmpty()) return false;
        FileConfiguration clan = fh.getClan(pClan);
        if (clan == null) return false;
        return player.getName().equalsIgnoreCase(clan.getString("leader"));
    }

    private List<String> getWarsOfClan(String clan) {
        List<String> result = new ArrayList<>();
        if (clan == null) return result;
        for (String wid : fh.getActiveWars()) {
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;
            if (war.getStringList("clans.offensive").contains(clan)
                    || war.getStringList("clans.defensive").contains(clan))
                result.add(wid);
        }
        return result;
    }

    private List<String> buildWarLore(FileConfiguration war, String pClan) {
        List<String> off = war.getStringList("clans.offensive");
        List<String> def = war.getStringList("clans.defensive");
        String side = off.contains(pClan) ? "&cOffensive"
                : def.contains(pClan) ? "&aDefensive" : "&7Observer";

        List<String> lore = new ArrayList<>();
        lore.add("&7Offensive: &c" + String.join(", ", off));
        lore.add("&7Defensive: &a" + String.join(", ", def));
        lore.add("&7Your side: " + side);
        lore.add("&7Score: &c" + war.getInt("stats.offensive.points")
                + " &7vs &a" + war.getInt("stats.defensive.points"));
        return lore;
    }

    private String buildUnifiedName(String offClan, String defClan) {
        String a = offClan.length() >= 3 ? offClan.substring(0, 3) : offClan;
        String b = defClan.length() >= 3 ? defClan.substring(0, 3) : defClan;
        return a + b;
    }

    private String formatTermForDisplay(@NotNull String term) {
        String[] s = term.split(":");
        if (s.length < 2) return term;

        String side = switch (s[0]) {
            case "moff" -> "Offensive";
            case "mdef" -> "Defensive";
            case "off"  -> "All Offensive";
            case "def"  -> "All Defensive";
            default     -> s[0];
        };

        return switch (s[1]) {
            case "white-peace"  -> "White Peace";
            case "money"        -> side + " pays $" + (s.length > 2 ? s[2] : "?");
            case "xp"           -> side + " loses " + (s.length > 2 ? s[2] : "?") + " XP";
            case "disband"      -> side + " disbands";
            case "unify-clans"  -> "Unify both main clans";
            default             -> term;
        };
    }
}