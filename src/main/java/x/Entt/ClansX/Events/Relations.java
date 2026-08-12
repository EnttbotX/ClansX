package x.Entt.ClansX.Events;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import x.Entt.ClansX.Utils.Text;
import x.Entt.ClansX.Utils.WarSystem;

import static x.Entt.ClansX.CX.prefix;
import static x.Entt.ClansX.Utils.MSG.Paths.*;

public class Relations implements Listener {

    private final CX plugin;
    private final Player player;
    private final FileHandler fh;

    public enum Screen {MAIN, CLAN_SELECTOR, REQUESTS, LIST}

    private Screen currentScreen = Screen.MAIN;
    private String selectorMode = "ALLY";
    private int selectorPage = 0;
    private String listType = "allies";
    private Inventory currentInv = null;
    private boolean navigating = false;

    public Relations(CX plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.fh = plugin.getFH();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        openMain();
    }

    public void openMain() {
        currentScreen = Screen.MAIN;
        Inventory inv = Bukkit.createInventory(null, 27, Text.color(MSG.sendWithoutPrefix(RGUI_MAIN_TITLE)));

        inv.setItem(10, makeItem(Material.LIME_WOOL, RGUI_MAIN_ALLIES_NAME, RGUI_MAIN_ALLIES_LORE));
        inv.setItem(13, makeItem(Material.RED_WOOL, RGUI_MAIN_ENEMIES_NAME, RGUI_MAIN_ENEMIES_LORE));
        inv.setItem(16, makeItem(Material.YELLOW_WOOL, RGUI_MAIN_REQUESTS_NAME, RGUI_MAIN_REQUESTS_LORE));
        inv.setItem(21, makeItem(Material.EMERALD, RGUI_MAIN_SEND_ALLY_NAME, RGUI_MAIN_SEND_ALLY_LORE));
        inv.setItem(23, makeItem(Material.NETHER_STAR, RGUI_MAIN_ADD_ENEMY_NAME, RGUI_MAIN_ADD_ENEMY_LORE));

        fill(inv);
        open(inv);
    }

    private void openClanSelector(String mode, int page) {
        currentScreen = Screen.CLAN_SELECTOR;
        selectorMode = mode;
        selectorPage = page;

        String playerClan = getPlayerClan();
        List<String> clans = fh.getClanNames();
        clans.removeIf(c -> c.equalsIgnoreCase(playerClan));

        if (clans.isEmpty()) {
            player.sendMessage(MSG.send(REL_NO_AVAILABLE_CLANS));
            openMain();
            return;
        }

        clans.sort(Comparator.comparingInt(c -> {
            FileConfiguration cfg = fh.getClan(c);
            return cfg != null ? -cfg.getInt("prestige") : 0;
        }));

        int perPage = 45;
        int maxPage = (int) Math.ceil((double) clans.size() / perPage);
        if (page < 0) page = selectorPage = 0;
        if (page >= maxPage) page = selectorPage = maxPage - 1;

        int start = page * perPage;
        int end = Math.min(start + perPage, clans.size());

        String title = mode.equals("ALLY")
                ? MSG.sendWithoutPrefix(RGUI_SELECTOR_TITLE_ALLY)
                : MSG.sendWithoutPrefix(RGUI_SELECTOR_TITLE_ENEMY);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        for (int i = start; i < end; i++) {
            String clan = clans.get(i);
            FileConfiguration c = fh.getClan(clan);
            if (c == null) continue;

            String founder = c.getString("founder", "Unknown");
            int prestige = c.getInt("prestige", 0);
            int members = c.getStringList("users").size();

            inv.setItem(i - start, makeClanHead(clan, founder, prestige, members));
        }

        if (page > 0)
            inv.setItem(45, makeItem(Material.ARROW, UI_PREVIOUS_PAGE,
                    MSG.sendList(RGUI_SELECTOR_PAGE_LORE, String.valueOf(page))));

        if (page < maxPage - 1)
            inv.setItem(53, makeItem(Material.ARROW, UI_NEXT_PAGE,
                    MSG.sendList(RGUI_SELECTOR_PAGE_LORE, String.valueOf(page + 2))));

        inv.setItem(49, makeItem(Material.BARRIER, UI_BACK, List.of()));

        fill(inv);
        open(inv);
    }

    private void openRequests() {
        currentScreen = Screen.REQUESTS;

        String clan = getPlayerClan();
        FileConfiguration c = fh.getClan(clan);
        List<String> requests = c != null ? c.getStringList("relations.requests") : List.of();

        Inventory inv = Bukkit.createInventory(null, 54, MSG.sendWithoutPrefix(RGUI_REQUESTS_TITLE));

        if (requests.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER, RGUI_REQUESTS_EMPTY_NAME, RGUI_REQUESTS_EMPTY_LORE));
        } else {
            for (int i = 0; i < requests.size() && i < 45; i++) {
                String requester = requests.get(i);
                FileConfiguration rc = fh.getClan(requester);
                int prestige = rc != null ? rc.getInt("prestige", 0) : 0;
                int members = rc != null ? rc.getStringList("users").size() : 0;

                inv.setItem(i, makeItem(Material.YELLOW_WOOL, "&e&l" + requester,
                        MSG.sendList(RGUI_REQUESTS_ITEM_LORE, String.valueOf(prestige), String.valueOf(members))));
            }
        }

        inv.setItem(49, makeItem(Material.BARRIER, UI_BACK, List.of()));
        fill(inv);
        open(inv);
    }

    private void openList(String type) {
        currentScreen = Screen.LIST;
        listType = type;

        String clan = getPlayerClan();
        FileConfiguration c = fh.getClan(clan);
        List<String> list = c != null ? c.getStringList("relations." + type) : List.of();

        boolean allies = type.equals("allies");
        String title = allies
                ? MSG.sendWithoutPrefix(RGUI_LIST_TITLE_ALLIES)
                : MSG.sendWithoutPrefix(RGUI_LIST_TITLE_ENEMIES);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        Material mat = allies ? Material.LIME_WOOL : Material.RED_WOOL;
        String removeHint = MSG.sendWithoutPrefix(allies ? RGUI_LIST_REMOVE_HINT_ALLY : RGUI_LIST_REMOVE_HINT_ENEMY);
        String nameColor = allies ? "&a&l" : "&c&l";

        if (list.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER, RGUI_LIST_EMPTY_NAME,
                    MSG.sendList(RGUI_LIST_EMPTY_LORE, type)));
        } else {
            for (int i = 0; i < list.size() && i < 45; i++) {
                String entry = list.get(i);
                FileConfiguration ec = fh.getClan(entry);
                int prestige = ec != null ? ec.getInt("prestige", 0) : 0;
                int members = ec != null ? ec.getStringList("users").size() : 0;

                inv.setItem(i, makeItem(mat, nameColor + entry,
                        MSG.sendList(RGUI_LIST_ITEM_LORE,
                                String.valueOf(prestige), String.valueOf(members), removeHint)));
            }
        }

        inv.setItem(49, makeItem(Material.BARRIER, UI_BACK, List.of()));
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

        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(currentInv)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.BARRIER) {
            if (currentScreen == Screen.MAIN) {
                player.closeInventory();
            } else {
                openMain();
            }
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            int slot = e.getSlot();
            if (slot == 45) {
                openClanSelector(selectorMode, selectorPage - 1);
            } else if (slot == 53) {
                openClanSelector(selectorMode, selectorPage + 1);
            }
            return;
        }

        switch (currentScreen) {
            case MAIN -> handleMain(clicked);
            case CLAN_SELECTOR -> handleSelector(clicked);
            case REQUESTS -> handleRequests(clicked, e.isLeftClick(), e.isRightClick());
            case LIST -> handleList(clicked, e.isLeftClick());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (navigating) return;
        HandlerList.unregisterAll(this);
    }

    private void handleMain(ItemStack clicked) {
        switch (clicked.getType()) {
            case LIME_WOOL -> openList("allies");
            case RED_WOOL -> openList("enemies");
            case YELLOW_WOOL -> openRequests();
            case EMERALD -> openClanSelector("ALLY", 0);
            case NETHER_STAR -> openClanSelector("ENEMY", 0);
        }
    }

    private void handleSelector(ItemStack clicked) {
        if (clicked.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String clanName = org.bukkit.ChatColor.stripColor(meta.getDisplayName()).trim();

        executeRelation(selectorMode, clanName);
        player.closeInventory();
    }

    private void handleRequests(ItemStack clicked, boolean left, boolean right) {
        if (clicked.getType() != Material.YELLOW_WOOL) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String requesterClan = org.bukkit.ChatColor.stripColor(meta.getDisplayName()).trim();

        String clan = getPlayerClan();
        FileConfiguration c = fh.getClan(clan);
        FileConfiguration tClan = fh.getClan(requesterClan);
        if (c == null || tClan == null) return;

        List<String> requests = c.getStringList("relations.requests");
        if (!requests.contains(requesterClan)) return;

        if (left) {
            List<String> allies = c.getStringList("relations.allies");
            List<String> tallies = tClan.getStringList("relations.allies");

            allies.add(requesterClan);
            tallies.add(clan);
            requests.remove(requesterClan);

            c.set("relations.allies", allies);
            c.set("relations.requests", requests);
            tClan.set("relations.allies", tallies);

            fh.saveClan(clan, c);
            fh.saveClan(requesterClan, tClan);

            broadcastClan(clan, MSG.sendWithoutPrefix(REL_ESTABLISHED_ALLY_BROADCAST, requesterClan));
            broadcastClan(requesterClan, MSG.sendWithoutPrefix(REL_ESTABLISHED_ALLY_BROADCAST, clan));

            for (String member : tClan.getStringList("users"))
                if (Bukkit.getPlayerExact(member) == null)
                    WarSystem.queueNotification(fh, member,
                            MSG.sendWithoutPrefix(REL_ALLY_OFFLINE_NOTIFY, clan, requesterClan));

            for (String member : c.getStringList("users"))
                if (Bukkit.getPlayerExact(member) == null)
                    WarSystem.queueNotification(fh, member,
                            MSG.sendWithoutPrefix(REL_ALLY_OFFLINE_NOTIFY, clan, requesterClan));

            openRequests();

        } else if (right) {
            requests.remove(requesterClan);
            c.set("relations.requests", requests);
            fh.saveClan(clan, c);
            player.sendMessage(MSG.send(REL_REQUEST_REJECTED));
            openRequests();
        }
    }

    private void handleList(ItemStack clicked, boolean left) {
        if (!left) return;
        if (clicked.getType() != Material.LIME_WOOL && clicked.getType() != Material.RED_WOOL) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String targetClan = org.bukkit.ChatColor.stripColor(meta.getDisplayName()).trim();

        String clan = getPlayerClan();
        FileConfiguration pClan = fh.getClan(clan);
        FileConfiguration tClan = fh.getClan(targetClan);
        if (pClan == null || tClan == null) return;

        if (listType.equals("allies")) {
            List<String> allies = pClan.getStringList("relations.allies");
            List<String> tallies = tClan.getStringList("relations.allies");
            allies.remove(targetClan);
            tallies.remove(clan);
            pClan.set("relations.allies", allies);
            tClan.set("relations.allies", tallies);
            fh.saveClan(clan, pClan);
            fh.saveClan(targetClan, tClan);
            broadcastClan(clan, MSG.sendWithoutPrefix(REL_REMOVED_ALLY_BROADCAST, targetClan));
            broadcastClan(targetClan, MSG.sendWithoutPrefix(REL_REMOVED_ALLY_BROADCAST, clan));
        } else {
            List<String> enemies = pClan.getStringList("relations.enemies");
            List<String> tenemies = tClan.getStringList("relations.enemies");
            enemies.remove(targetClan);
            tenemies.remove(clan);
            pClan.set("relations.enemies", enemies);
            tClan.set("relations.enemies", tenemies);
            fh.saveClan(clan, pClan);
            fh.saveClan(targetClan, tClan);
            broadcastClan(clan, MSG.sendWithoutPrefix(REL_REMOVED_ENEMY_BROADCAST, targetClan));
            broadcastClan(targetClan, MSG.sendWithoutPrefix(REL_REMOVED_ENEMY_BROADCAST, clan));
        }

        openList(listType);
    }

    private void executeRelation(String mode, String targetClan) {
        String clan = getPlayerClan();

        if (!fh.clanExists(targetClan)) {
            player.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }
        if (targetClan.equalsIgnoreCase(clan)) {
            player.sendMessage(MSG.send(REL_INVALID_TARGET));
            return;
        }

        FileConfiguration pClan = fh.getClan(clan);
        FileConfiguration tClan = fh.getClan(targetClan);
        if (pClan == null || tClan == null) return;

        if (mode.equals("ALLY")) {
            if (pClan.getStringList("relations.enemies").contains(targetClan)) {
                player.sendMessage(MSG.send(REL_IS_RELATION, "enemies"));
                return;
            }
            if (pClan.getStringList("relations.allies").contains(targetClan)) {
                player.sendMessage(MSG.send(REL_ALREADY_RELATION, "allies"));
                return;
            }

            int maxAlliances = fh.getConfig().getInt("clans.max-alliances", 0);
            if (maxAlliances > 0 && pClan.getStringList("relations.allies").size() >= maxAlliances) {
                player.sendMessage(MSG.send(REL_MAX_ALLIANCES, String.valueOf(maxAlliances)));
                return;
            }

            List<String> requests = tClan.getStringList("relations.requests");
            if (requests.contains(clan)) {
                player.sendMessage(MSG.send(REL_REQUEST_ALREADY_SENT));
                return;
            }

            int maxRequests = fh.getConfig().getInt("clans.max-ally-requests", 0);
            if (maxRequests > 0 && requests.size() >= maxRequests) {
                player.sendMessage(MSG.send(REL_REQUEST_INBOX_FULL, String.valueOf(maxRequests)));
                return;
            }

            requests.add(clan);
            tClan.set("relations.requests", requests);
            fh.saveClan(targetClan, tClan);

            player.sendMessage(MSG.send(REL_REQUEST_SENT));

            Player leader = getLeaderOnline(targetClan);
            if (leader != null) {
                leader.sendMessage(Text.color(prefix));
                leader.sendMessage(MSG.sendWithoutPrefix(REL_NEW_REQUEST_ONLINE));
                TextComponent button = new TextComponent(MSG.sendWithoutPrefix(REL_NEW_REQUEST_BUTTON));
                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cls relations"));
                leader.spigot().sendMessage(button);
            } else {
                String leaderName = tClan.getString("leader");
                if (leaderName != null)
                    WarSystem.queueNotification(fh, leaderName,
                            MSG.sendWithoutPrefix(REL_NEW_REQUEST_OFFLINE, clan, targetClan));
            }

        } else if (mode.equals("ENEMY")) {
            if (pClan.getStringList("relations.allies").contains(targetClan)) {
                player.sendMessage(MSG.send(REL_IS_RELATION, "allies"));
                return;
            }

            List<String> enemies = pClan.getStringList("relations.enemies");
            List<String> tenemies = tClan.getStringList("relations.enemies");

            if (enemies.contains(targetClan)) {
                player.sendMessage(MSG.send(REL_ALREADY_RELATION, "enemy"));
                return;
            }

            enemies.add(targetClan);
            tenemies.add(clan);
            pClan.set("relations.enemies", enemies);
            tClan.set("relations.enemies", tenemies);
            fh.saveClan(clan, pClan);
            fh.saveClan(targetClan, tClan);

            broadcastClan(clan, MSG.sendWithoutPrefix(REL_ESTABLISHED_ENEMY_BROADCAST, targetClan));
            broadcastClan(targetClan, MSG.sendWithoutPrefix(REL_ESTABLISHED_ENEMY_BROADCAST, clan));

            for (String member : tClan.getStringList("users"))
                if (Bukkit.getPlayerExact(member) == null)
                    WarSystem.queueNotification(fh, member,
                            MSG.sendWithoutPrefix(REL_ENEMY_OFFLINE_NOTIFY, clan, targetClan));
        }
    }

    private ItemStack makeClanHead(String clan, String founder, int prestige, int members) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        OfflinePlayer op = Bukkit.getPlayer(founder);

        if (op != null) {
            if (op.hasPlayedBefore()) {
                meta.setOwningPlayer(op);
            }

            FileConfiguration cfg = fh.getClan(clan);
            String displayName = cfg != null ? cfg.getString("name", clan) : clan;

            meta.setDisplayName(Text.color("&e&l" + displayName));
            meta.setLore(MSG.sendList(RGUI_CLAN_HEAD_LORE,
                    founder, String.valueOf(prestige), String.valueOf(members)));
        } else {
            String displayName = "UNKNOWN";

            meta.setDisplayName(Text.color("&e&l" + displayName));
            meta.setLore(List.of(MSG.send(REL_CLAN_HEAD_ERROR)));
        }

        head.setItemMeta(meta);
        return head;
    }

    private ItemStack makeItem(Material mat, MSG.Paths namePath, MSG.Paths lorePath) {
        return makeItem(mat, MSG.sendWithoutPrefix(namePath), MSG.sendList(lorePath));
    }

    private ItemStack makeItem(Material mat, MSG.Paths namePath, List<String> lore) {
        return makeItem(mat, MSG.sendWithoutPrefix(namePath), lore);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(Text.color(name));
        List<String> colored = new ArrayList<>();
        lore.forEach(l -> colored.add(Text.color(l)));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv) {
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType() == Material.AIR)
                inv.setItem(i, filler);
        }
    }

    private void open(Inventory inv) {
        currentInv = inv;
        navigating = true;
        player.openInventory(inv);
        navigating = false;
    }

    private String getPlayerClan() {
        FileConfiguration pd = fh.getPlayerData(player.getUniqueId());
        return pd != null ? pd.getString("clan") : null;
    }

    private Player getLeaderOnline(String clan) {
        FileConfiguration c = fh.getClan(clan);
        if (c == null) return null;
        String leader = c.getString("leader");
        if (leader == null) return null;
        Player p = Bukkit.getPlayerExact(leader);
        return p != null && p.isOnline() ? p : null;
    }

    private void broadcastClan(String clan, String msg) {
        for (Player p : Bukkit.getOnlinePlayers())
            if (clan.equalsIgnoreCase(plugin.getPlayerClan(p)))
                p.sendMessage(Text.color(prefix + msg));
    }
}