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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import x.Entt.ClansX.Utils.Text;

import static x.Entt.ClansX.CX.prefix;
import static x.Entt.ClansX.Utils.MSG.Paths.*;

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

        Inventory inv = Bukkit.createInventory(null, 54, MSG.sendWithoutPrefix(WGUI_MAIN_TITLE));

        inv.setItem(10, makeItem(Material.NETHERITE_SWORD,
                MSG.sendWithoutPrefix(WGUI_MAIN_DECLARE_NAME),
                MSG.sendList(WGUI_MAIN_DECLARE_LORE, leader
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_DECLARE_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_DECLARE_CANNOT))));

        inv.setItem(12, makeItem(Material.SHIELD,
                MSG.sendWithoutPrefix(WGUI_MAIN_MYWARS_NAME),
                MSG.sendList(WGUI_MAIN_MYWARS_LORE, inClan
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_MYWARS_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_MYWARS_CANNOT))));

        inv.setItem(14, makeItem(Material.SPYGLASS,
                MSG.sendWithoutPrefix(WGUI_MAIN_ALLWARS_NAME),
                MSG.sendList(WGUI_MAIN_ALLWARS_LORE)));

        inv.setItem(16, makeItem(Material.ORANGE_BANNER,
                MSG.sendWithoutPrefix(WGUI_MAIN_JOIN_NAME),
                MSG.sendList(WGUI_MAIN_JOIN_LORE, leader
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_JOIN_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_JOIN_CANNOT))));

        inv.setItem(28, makeItem(Material.WHITE_BANNER,
                MSG.sendWithoutPrefix(WGUI_MAIN_PEACE_NAME),
                MSG.sendList(WGUI_MAIN_PEACE_LORE, leader
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_PEACE_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_PEACE_CANNOT))));

        inv.setItem(30, makeItem(Material.PAPER,
                MSG.sendWithoutPrefix(WGUI_MAIN_OFFERS_NAME),
                MSG.sendList(WGUI_MAIN_OFFERS_LORE, leader
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_OFFERS_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_OFFERS_CANNOT))));

        inv.setItem(32, makeItem(Material.FEATHER,
                MSG.sendWithoutPrefix(WGUI_MAIN_WITHDRAW_NAME),
                MSG.sendList(WGUI_MAIN_WITHDRAW_LORE, leader
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_WITHDRAW_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_WITHDRAW_CANNOT))));

        inv.setItem(34, makeItem(Material.BOOK,
                MSG.sendWithoutPrefix(WGUI_MAIN_STATS_NAME),
                MSG.sendList(WGUI_MAIN_STATS_LORE, inClan
                        ? MSG.sendWithoutPrefix(WGUI_MAIN_STATS_CAN)
                        : MSG.sendWithoutPrefix(WGUI_MAIN_STATS_CANNOT))));

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
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_DECLARE_TITLE));

        for (int i = 0; i < clans.size() && i < size - 9; i++) {
            String clanName = clans.get(i);
            FileConfiguration cfg = fh.getClan(clanName);
            if (cfg == null) continue;

            List<String> lore = MSG.sendList(WGUI_DECLARE_TARGET_LORE,
                    cfg.getString("leader", "?"),
                    String.valueOf(cfg.getStringList("users").size()),
                    String.valueOf(cfg.getInt("prestige", 0)));

            inv.setItem(i, makeItem(Material.RED_BANNER, "&c" + clanName, lore));
        }

        if (clans.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_DECLARE_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_DECLARE_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openMyWars() {
        currentScreen = Screen.MY_WARS;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_MYWARS_TITLE));

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_MYWARS_MANAGE_HINT));

            Material mat = war.getStringList("clans.offensive").contains(pClan)
                    ? Material.ORANGE_BANNER : Material.BLUE_BANNER;

            inv.setItem(i, makeItem(mat, "&e" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_MYWARS_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_MYWARS_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
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
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_JOIN_TITLE));

        for (int i = 0; i < available.size() && i < size - 9; i++) {
            String wid = available.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_JOIN_LEFT_CLICK));
            lore.add(MSG.sendWithoutPrefix(WGUI_JOIN_RIGHT_CLICK));

            inv.setItem(i, makeItem(Material.ORANGE_BANNER, "&6" + war.getString("name", wid), lore));
        }

        if (available.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_JOIN_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_JOIN_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openProposePeaceWarSelect() {
        currentScreen = Screen.PROPOSE_PEACE_SELECT;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_PEACESELECT_TITLE));

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACESELECT_HINT));

            inv.setItem(i, makeItem(Material.WHITE_BANNER, "&f" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_PEACESELECT_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_PEACESELECT_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openOffersWarSelect() {
        currentScreen = Screen.OFFERS_SELECT;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_OFFERSSELECT_TITLE));

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
            lore.add(hasOffers
                    ? MSG.sendWithoutPrefix(WGUI_OFFERSSELECT_HAS_OFFERS)
                    : MSG.sendWithoutPrefix(WGUI_OFFERSSELECT_NO_OFFERS));

            Material mat = hasOffers ? Material.PAPER : Material.GRAY_DYE;
            inv.setItem(i, makeItem(mat, "&a" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_OFFERSSELECT_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_OFFERSSELECT_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openWithdrawWarSelect() {
        currentScreen = Screen.WITHDRAW_SELECT;
        String pClan = getPlayerClan();
        List<String> warIDs = getWarsOfClan(pClan);

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_WITHDRAWSELECT_TITLE));

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
            lore.add(isMain
                    ? MSG.sendWithoutPrefix(WGUI_WITHDRAWSELECT_MAIN_CANNOT)
                    : MSG.sendWithoutPrefix(WGUI_WITHDRAWSELECT_CAN_WITHDRAW));

            Material mat = isMain ? Material.BARRIER : Material.FEATHER;
            inv.setItem(i, makeItem(mat, "&7" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_WITHDRAWSELECT_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_WITHDRAWSELECT_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openWarStats() {
        currentScreen = Screen.WAR_STATS;
        String pClan = getPlayerClan();
        if (pClan == null || pClan.isEmpty()) return;

        FileConfiguration clan = fh.getClan(pClan);
        if (clan == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, MSG.sendWithoutPrefix(WGUI_STATS_TITLE, pClan));

        int won      = clan.getStringList("wars.won").size();
        int defeated = clan.getStringList("wars.defeated").size();
        int active   = clan.getStringList("wars.active").size();
        int streak   = clan.getInt("wars.streak", 0);
        int kills    = clan.getInt("kills", 0);

        inv.setItem(10, makeItem(Material.GOLDEN_SWORD,
                MSG.sendWithoutPrefix(WGUI_STATS_KILLS_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_STATS_KILLS_LORE, String.valueOf(kills)))));
        inv.setItem(12, makeItem(Material.EMERALD,
                MSG.sendWithoutPrefix(WGUI_STATS_WON_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_STATS_WON_LORE, String.valueOf(won)))));
        inv.setItem(14, makeItem(Material.REDSTONE,
                MSG.sendWithoutPrefix(WGUI_STATS_DEFEATED_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_STATS_DEFEATED_LORE, String.valueOf(defeated)))));
        inv.setItem(16, makeItem(Material.BLAZE_ROD,
                MSG.sendWithoutPrefix(WGUI_STATS_STREAK_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_STATS_STREAK_LORE, String.valueOf(streak)))));

        if (active > 0) {
            inv.setItem(4, makeItem(Material.NETHERITE_SWORD,
                    MSG.sendWithoutPrefix(WGUI_STATS_ACTIVE_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_STATS_ACTIVE_LORE, String.valueOf(active)))));
        }

        inv.setItem(22, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openAllWars() {
        currentScreen = Screen.ALL_WARS;
        List<String> warIDs = fh.getActiveWars();

        int size = clampSize(warIDs.size() + 9);
        Inventory inv = Bukkit.createInventory(null, size, MSG.sendWithoutPrefix(WGUI_ALLWARS_TITLE));

        String pClan = getPlayerClan();

        for (int i = 0; i < warIDs.size() && i < size - 9; i++) {
            String wid = warIDs.get(i);
            FileConfiguration war = fh.getWar(wid);
            if (war == null) continue;

            List<String> lore = buildWarLore(war, pClan);
            inv.setItem(i, makeItem(Material.RED_BANNER, "&f" + war.getString("name", wid), lore));
        }

        if (warIDs.isEmpty()) {
            inv.setItem(13, makeItem(Material.BARRIER,
                    MSG.sendWithoutPrefix(WGUI_ALLWARS_EMPTY_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_ALLWARS_EMPTY_LORE))));
        }

        inv.setItem(size - 5, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
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

        Inventory inv = Bukkit.createInventory(null, 54, Text.color("&c" + war.getString("name", warID)));

        List<String> warInfo = new ArrayList<>();
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_OFFENSIVE, String.join(", ", off)));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_DEFENSIVE, String.join(", ", def)));
        warInfo.add("");
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_OFF_HEADER));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_KILLS, String.valueOf(war.getInt("stats.offensive.kills"))));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_DEATHS, String.valueOf(war.getInt("stats.offensive.deaths"))));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_POINTS, String.valueOf(war.getInt("stats.offensive.points"))));
        warInfo.add("");
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_DEF_HEADER));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_KILLS, String.valueOf(war.getInt("stats.defensive.kills"))));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_DEATHS, String.valueOf(war.getInt("stats.defensive.deaths"))));
        warInfo.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_POINTS, String.valueOf(war.getInt("stats.defensive.points"))));
        inv.setItem(4, makeItem(Material.BOOK, MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_NAME), warInfo));

        if (isParticipant && war.contains("peace-offers")) {
            int slot = 19;
            for (String k : Objects.requireNonNull(
                    war.getConfigurationSection("peace-offers")).getKeys(false)) {
                String state = war.getString("peace-offers." + k + ".state", "");
                if (!"pending".equalsIgnoreCase(state)) continue;

                String from   = war.getString("peace-offers." + k + ".from", "?");
                List<String> offers = war.getStringList("peace-offers." + k + ".offers");

                List<String> lore = new ArrayList<>();
                lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_OFFER_FROM, from));
                lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_OFFER_TERMS_HEADER));
                offers.forEach(o -> lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_OFFER_TERM_LINE, formatTermForDisplay(o))));

                if (leader && !from.equalsIgnoreCase(pClan)) {
                    lore.add("");
                    lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_OFFER_ACCEPT));
                    lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_OFFER_REJECT));
                }

                inv.setItem(slot, makeItem(Material.PAPER, MSG.sendWithoutPrefix(WGUI_DETAIL_OFFER_NAME, k), lore));
                if (++slot > 25) break;
            }
        }

        if (leader && isParticipant) {
            inv.setItem(38, makeItem(Material.WHITE_BANNER,
                    MSG.sendWithoutPrefix(WGUI_DETAIL_PEACE_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_DETAIL_PEACE_LORE1), MSG.sendWithoutPrefix(WGUI_DETAIL_PEACE_LORE2))));

            if (!isMainClan) {
                inv.setItem(40, makeItem(Material.FEATHER,
                        MSG.sendWithoutPrefix(WGUI_DETAIL_WITHDRAW_NAME),
                        List.of(MSG.sendWithoutPrefix(WGUI_DETAIL_WITHDRAW_LORE))));
            }
        }

        inv.setItem(49, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
        fill(inv);
        open(inv);
    }

    private void openPeaceTypeSelect() {
        if (selectedWarID == null || fh.getWar(selectedWarID) == null) {
            player.sendMessage(MSG.send(WGUI_NO_WAR_SELECTED));
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

        Inventory inv = Bukkit.createInventory(null, 54, MSG.sendWithoutPrefix(WGUI_PEACETYPE_TITLE));

        if (multiTerms && !selectedTerms.isEmpty()) {
            List<String> termLore = new ArrayList<>();
            termLore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_SELECTED_HEADER));
            selectedTerms.forEach(t -> termLore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_SELECTED_LINE, formatTermForDisplay(t))));
            termLore.add("");
            termLore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_SELECTED_HINT));
            inv.setItem(4, makeItem(Material.PAPER,
                    MSG.sendWithoutPrefix(WGUI_PEACETYPE_SELECTED_NAME, String.valueOf(selectedTerms.size())), termLore));
        }

        int slot = 10;

        if (cfg.getBoolean("clans.wars.peace-terms.white-peace", false)) {
            List<String> lore = new ArrayList<>();
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_WHITEPEACE_LORE));
            if (multiTerms) lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_WHITEPEACE_MULTI));
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_WHITEPEACE_CLICK,
                    multiTerms ? MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLICK_SELECT) : MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLICK_PROPOSE)));
            inv.setItem(slot, makeItem(Material.WHITE_BANNER, MSG.sendWithoutPrefix(WGUI_PEACETYPE_WHITEPEACE_NAME), lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.money", false)) {
            FileConfiguration opponentCfg = fh.getClan(opponentClan);
            double opponentBalance = opponentCfg != null ? opponentCfg.getDouble("balance", 0) : 0;

            List<String> lore = new ArrayList<>();
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_MONEY_LORE1, opponentClan));
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_MONEY_LORE2, String.format("%.2f", opponentBalance)));
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_MONEY_CLICK));
            inv.setItem(slot, makeItem(Material.GOLD_INGOT, MSG.sendWithoutPrefix(WGUI_PEACETYPE_MONEY_NAME), lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.xp", false)) {
            FileConfiguration opponentCfg = fh.getClan(opponentClan);
            int opponentPrestige = opponentCfg != null ? opponentCfg.getInt("prestige", 0) : 0;
            int xpPerLevel = cfg.getInt("clans.prestige.acquire-new-prestige", 10000);
            int opponentLevel = xpPerLevel > 0 ? opponentPrestige / xpPerLevel : 0;

            List<String> lore = new ArrayList<>();
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_XP_LORE1, opponentClan));
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_XP_LORE2, String.valueOf(opponentPrestige), String.valueOf(opponentLevel)));
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_XP_CLICK));
            inv.setItem(slot, makeItem(Material.EXPERIENCE_BOTTLE, MSG.sendWithoutPrefix(WGUI_PEACETYPE_XP_NAME), lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.disband", false)) {
            List<String> lore = new ArrayList<>();
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_DISBAND_LORE1, opponentClan));
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_DISBAND_LORE2));
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_DISBAND_CLICK,
                    multiTerms ? MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLICK_ADD) : MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLICK_PROPOSE)));
            inv.setItem(slot, makeItem(Material.NETHER_BRICK, MSG.sendWithoutPrefix(WGUI_PEACETYPE_DISBAND_NAME), lore));
            slot += 2;
        }

        if (cfg.getBoolean("clans.wars.peace-terms.unify-clans", false)) {
            String mainOff = off.isEmpty() ? "?" : off.get(0);
            String mainDef = def.isEmpty() ? "?" : def.get(0);
            String newName = buildUnifiedName(mainOff, mainDef);

            List<String> lore = new ArrayList<>();
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_UNIFY_LORE1));
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_UNIFY_LORE2, newName));
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_UNIFY_LORE3));
            lore.add("");
            lore.add(MSG.sendWithoutPrefix(WGUI_PEACETYPE_UNIFY_CLICK,
                    multiTerms ? MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLICK_ADD) : MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLICK_PROPOSE)));
            inv.setItem(slot, makeItem(Material.NETHER_STAR, MSG.sendWithoutPrefix(WGUI_PEACETYPE_UNIFY_NAME), lore));
        }

        if (multiTerms && !selectedTerms.isEmpty()) {
            inv.setItem(48, makeItem(Material.GREEN_WOOL,
                    MSG.sendWithoutPrefix(WGUI_PEACETYPE_SEND_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_PEACETYPE_SEND_LORE))));
            inv.setItem(50, makeItem(Material.RED_WOOL,
                    MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLEAR_NAME),
                    List.of(MSG.sendWithoutPrefix(WGUI_PEACETYPE_CLEAR_LORE))));
        }

        inv.setItem(49, makeItem(Material.ARROW, MSG.sendWithoutPrefix(WGUI_COMMON_BACK), List.of()));
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

        Inventory inv = Bukkit.createInventory(null, 27, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_TITLE));

        inv.setItem(3, makeItem(Material.PAPER,
                MSG.sendWithoutPrefix(WGUI_MONEYINPUT_AMOUNT_NAME, String.format("%.0f", (double) inputAmount)),
                List.of(
                        MSG.sendWithoutPrefix(WGUI_MONEYINPUT_AMOUNT_OPPONENT, opponentClan),
                        MSG.sendWithoutPrefix(WGUI_MONEYINPUT_AMOUNT_BALANCE, String.format("%.2f", opponentBalance)),
                        "",
                        MSG.sendWithoutPrefix(WGUI_MONEYINPUT_AMOUNT_STEP, String.valueOf(inputStep))
                )));

        inv.setItem(4, makeItem(Material.GOLD_INGOT,
                MSG.sendWithoutPrefix(WGUI_MONEYINPUT_OPPONENT_NAME),
                List.of(
                        MSG.sendWithoutPrefix(WGUI_MONEYINPUT_OPPONENT_CLAN, opponentClan),
                        MSG.sendWithoutPrefix(WGUI_MONEYINPUT_OPPONENT_BALANCE, String.format("%.2f", opponentBalance))
                )));

        inv.setItem(12, makeItem(Material.LIME_DYE, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_INCREASE_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_MONEYINPUT_INCREASE_LORE, String.valueOf(inputStep)))));
        inv.setItem(14, makeItem(Material.RED_DYE, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_DECREASE_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_MONEYINPUT_DECREASE_LORE, String.valueOf(inputStep)))));

        inv.setItem(0, makeItem(Material.GRAY_DYE, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_STEP_NAME, String.valueOf(inputStep)),
                List.of(MSG.sendWithoutPrefix(WGUI_MONEYINPUT_STEP_LEFT), MSG.sendWithoutPrefix(WGUI_MONEYINPUT_STEP_RIGHT))));
        inv.setItem(8, makeItem(Material.GRAY_DYE, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_STEP_NAME, String.valueOf(inputStep)),
                List.of(MSG.sendWithoutPrefix(WGUI_MONEYINPUT_STEP_LEFT), MSG.sendWithoutPrefix(WGUI_MONEYINPUT_STEP_RIGHT))));

        inv.setItem(2, makeItem(Material.EMERALD, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_SEND_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_MONEYINPUT_SEND_LORE))));
        inv.setItem(6, makeItem(Material.BARRIER, MSG.sendWithoutPrefix(WGUI_MONEYINPUT_CANCEL_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_MONEYINPUT_CANCEL_LORE))));

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

        Inventory inv = Bukkit.createInventory(null, 27, MSG.sendWithoutPrefix(WGUI_XPINPUT_TITLE));

        inv.setItem(3, makeItem(Material.EXPERIENCE_BOTTLE,
                MSG.sendWithoutPrefix(WGUI_XPINPUT_AMOUNT_NAME, String.valueOf(inputAmount)),
                List.of(
                        MSG.sendWithoutPrefix(WGUI_XPINPUT_AMOUNT_OPPONENT, opponentClan),
                        MSG.sendWithoutPrefix(WGUI_XPINPUT_AMOUNT_PRESTIGE, String.valueOf(opponentPrestige)),
                        "",
                        MSG.sendWithoutPrefix(WGUI_XPINPUT_AMOUNT_STEP, String.valueOf(inputStep))
                )));

        inv.setItem(4, makeItem(Material.EXPERIENCE_BOTTLE,
                MSG.sendWithoutPrefix(WGUI_XPINPUT_OPPONENT_NAME),
                List.of(
                        MSG.sendWithoutPrefix(WGUI_XPINPUT_OPPONENT_CLAN, opponentClan),
                        MSG.sendWithoutPrefix(WGUI_XPINPUT_OPPONENT_PRESTIGE, String.valueOf(opponentPrestige))
                )));

        inv.setItem(12, makeItem(Material.LIME_DYE, MSG.sendWithoutPrefix(WGUI_XPINPUT_INCREASE_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_XPINPUT_INCREASE_LORE, String.valueOf(inputStep)))));
        inv.setItem(14, makeItem(Material.RED_DYE, MSG.sendWithoutPrefix(WGUI_XPINPUT_DECREASE_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_XPINPUT_DECREASE_LORE, String.valueOf(inputStep)))));

        inv.setItem(0, makeItem(Material.GRAY_DYE, MSG.sendWithoutPrefix(WGUI_XPINPUT_STEP_NAME, String.valueOf(inputStep)),
                List.of(MSG.sendWithoutPrefix(WGUI_XPINPUT_STEP_LEFT), MSG.sendWithoutPrefix(WGUI_XPINPUT_STEP_RIGHT))));
        inv.setItem(8, makeItem(Material.GRAY_DYE, MSG.sendWithoutPrefix(WGUI_XPINPUT_STEP_NAME, String.valueOf(inputStep)),
                List.of(MSG.sendWithoutPrefix(WGUI_XPINPUT_STEP_LEFT), MSG.sendWithoutPrefix(WGUI_XPINPUT_STEP_RIGHT))));

        inv.setItem(2, makeItem(Material.EMERALD, MSG.sendWithoutPrefix(WGUI_XPINPUT_SEND_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_XPINPUT_SEND_LORE))));
        inv.setItem(6, makeItem(Material.BARRIER, MSG.sendWithoutPrefix(WGUI_XPINPUT_CANCEL_NAME),
                List.of(MSG.sendWithoutPrefix(WGUI_XPINPUT_CANCEL_LORE))));

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
        String cleanName = Text.color(displayName).replaceAll("§.", "").trim();

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
                    if (!inClan) { player.sendMessage(MSG.send(NOT_IN_CLAN)); return; }
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
                    if (!inClan) { player.sendMessage(MSG.send(NOT_IN_CLAN)); return; }
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
                    player.sendMessage(MSG.send(WGUI_MAIN_CANNOT_WITHDRAW));
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
                        player.sendMessage(MSG.send(WGUI_CANNOT_ACCEPT_OWN));
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
            player.sendMessage(MSG.send(NOT_IN_CLAN));
        } else {
            player.sendMessage(MSG.send(NOT_LEADER));
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
        meta.setDisplayName(Text.color(name));
        List<String> coloredLore = new ArrayList<>();
        lore.forEach(l -> coloredLore.add(Text.color(l)));
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
        String side = off.contains(pClan) ? MSG.sendWithoutPrefix(WGUI_WARLORE_SIDE_OFFENSIVE)
                : def.contains(pClan) ? MSG.sendWithoutPrefix(WGUI_WARLORE_SIDE_DEFENSIVE)
                : MSG.sendWithoutPrefix(WGUI_WARLORE_SIDE_OBSERVER);

        List<String> lore = new ArrayList<>();
        lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_OFFENSIVE, String.join(", ", off)));
        lore.add(MSG.sendWithoutPrefix(WGUI_DETAIL_INFO_DEFENSIVE, String.join(", ", def)));
        lore.add(side);
        lore.add(MSG.sendWithoutPrefix(WGUI_WARLORE_SCORE,
                String.valueOf(war.getInt("stats.offensive.points")),
                String.valueOf(war.getInt("stats.defensive.points"))));
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
            case "moff" -> MSG.sendWithoutPrefix(WGUI_TERM_SIDE_MOFF);
            case "mdef" -> MSG.sendWithoutPrefix(WGUI_TERM_SIDE_MDEF);
            case "off"  -> MSG.sendWithoutPrefix(WGUI_TERM_SIDE_OFF);
            case "def"  -> MSG.sendWithoutPrefix(WGUI_TERM_SIDE_DEF);
            default     -> s[0];
        };

        return switch (s[1]) {
            case "white-peace"  -> MSG.sendWithoutPrefix(WGUI_TERM_WHITEPEACE);
            case "money"        -> MSG.sendWithoutPrefix(WGUI_TERM_MONEY, side, s.length > 2 ? s[2] : "?");
            case "xp"           -> MSG.sendWithoutPrefix(WGUI_TERM_XP, side, s.length > 2 ? s[2] : "?");
            case "disband"      -> MSG.sendWithoutPrefix(WGUI_TERM_DISBAND, side);
            case "unify-clans"  -> MSG.sendWithoutPrefix(WGUI_TERM_UNIFY);
            default             -> term;
        };
    }
}