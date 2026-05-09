package x.Entt.ClansX.Events;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
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

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;
import x.Entt.ClansX.Utils.WarSystem;

import java.util.*;

import static x.Entt.ClansX.CX.prefix;

public class Relations implements Listener {

    private final CX plugin;
    private final Player player;
    private final FileHandler fh;

    public enum Screen { MAIN, CLAN_SELECTOR, REQUESTS, LIST }

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
        Inventory inv = Bukkit.createInventory(null, 27, MSG.color("&8\uD83E\uDD1D &b&lRelations Manager &8\uD83E\uDD1D"));

        inv.setItem(10, makeItem(Material.LIME_WOOL, "&a&lAllies",
                List.of("&7View and manage your", "&7allied clans.", "", "&aClick to open")));

        inv.setItem(13, makeItem(Material.RED_WOOL, "&c&lEnemies",
                List.of("&7View and manage your", "&7enemy clans.", "", "&cClick to open")));

        inv.setItem(16, makeItem(Material.YELLOW_WOOL, "&e&lRequests",
                List.of("&7Incoming alliance requests", "&7from other clans.", "", "&eClick to open")));

        inv.setItem(21, makeItem(Material.EMERALD, "&a&lSend Ally Request",
                List.of("&7Choose a clan to send", "&7an alliance request to.", "", "&aClick to select")));

        inv.setItem(23, makeItem(Material.NETHER_STAR, "&c&lAdd Enemy",
                List.of("&7Mark another clan", "&7as your enemy.", "", "&cClick to select")));

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
            player.sendMessage(MSG.color(prefix + "&cNo available clans"));
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
                ? "&8\uD83E\uDD1D &a&lSelect Clan — Ally"
                : "&8⚔ &c&lSelect Clan — Enemy";

        Inventory inv = Bukkit.createInventory(null, 54, MSG.color(title));

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
            inv.setItem(45, makeItem(Material.ARROW, "&7← Previous", List.of("&7Page " + page)));

        if (page < maxPage - 1)
            inv.setItem(53, makeItem(Material.ARROW, "&7Next →", List.of("&7Page " + (page + 2))));

        inv.setItem(49, makeItem(Material.BARRIER, "&c← Back", List.of()));

        fill(inv);
        open(inv);
    }

    private void openRequests() {
        currentScreen = Screen.REQUESTS;

        String clan = getPlayerClan();
        FileConfiguration c = fh.getClan(clan);
        List<String> requests = c != null ? c.getStringList("relations.requests") : List.of();

        Inventory inv = Bukkit.createInventory(null, 54, MSG.color("&8\uD83D\uDCE8 &e&lAlliance Requests"));

        if (requests.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER, "&7No pending requests",
                    List.of("&8No clans have sent", "&8alliance requests.")));
        } else {
            for (int i = 0; i < requests.size() && i < 45; i++) {
                String requester = requests.get(i);
                FileConfiguration rc = fh.getClan(requester);
                int prestige = rc != null ? rc.getInt("prestige", 0) : 0;
                int members = rc != null ? rc.getStringList("users").size() : 0;

                inv.setItem(i, makeItem(Material.YELLOW_WOOL, "&e&l" + requester,
                        List.of(
                                "&7Prestige: &6" + prestige,
                                "&7Members: &f" + members,
                                "",
                                "&aLeft-click: Accept",
                                "&cRight-click: Reject"
                        )));
            }
        }

        inv.setItem(49, makeItem(Material.BARRIER, "&c← Back", List.of()));
        fill(inv);
        open(inv);
    }

    private void openList(String type) {
        currentScreen = Screen.LIST;
        listType = type;

        String clan = getPlayerClan();
        FileConfiguration c = fh.getClan(clan);
        List<String> list = c != null ? c.getStringList("relations." + type) : List.of();

        String title = type.equals("allies")
                ? "&8\uD83E\uDD1D &a&lYour Allies"
                : "&8⚔ &c&lYour Enemies";

        Inventory inv = Bukkit.createInventory(null, 54, MSG.color(title));

        Material mat = type.equals("allies") ? Material.LIME_WOOL : Material.RED_WOOL;
        String removeHint = type.equals("allies") ? "&cLeft-click: Remove ally" : "&cLeft-click: Remove enemy";
        String nameColor = type.equals("allies") ? "&a&l" : "&c&l";

        if (list.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER, "&7Empty", List.of("&8No " + type + " yet.")));
        } else {
            for (int i = 0; i < list.size() && i < 45; i++) {
                String entry = list.get(i);
                FileConfiguration ec = fh.getClan(entry);
                int prestige = ec != null ? ec.getInt("prestige", 0) : 0;
                int members = ec != null ? ec.getStringList("users").size() : 0;

                inv.setItem(i, makeItem(mat, nameColor + entry,
                        List.of(
                                "&7Prestige: &6" + prestige,
                                "&7Members: &f" + members,
                                "",
                                removeHint
                        )));
            }
        }

        inv.setItem(49, makeItem(Material.BARRIER, "&c← Back", List.of()));
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
            ItemMeta m = clicked.getItemMeta();
            String dn = m != null ? m.getDisplayName() : "";
            if (dn.contains("Previous") || dn.contains("←")) {
                openClanSelector(selectorMode, selectorPage - 1);
            } else {
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

            broadcastClan(clan, "&aNow allied with &f" + requesterClan);
            broadcastClan(requesterClan, "&aNow allied with &f" + clan);

            for (String member : tClan.getStringList("users"))
                if (Bukkit.getPlayerExact(member) == null)
                    WarSystem.queueNotification(fh, member,
                            "&aClan &f" + clan + " &aand &f" + requesterClan + " &aare now allies!");

            for (String member : c.getStringList("users"))
                if (Bukkit.getPlayerExact(member) == null)
                    WarSystem.queueNotification(fh, member,
                            "&aClan &f" + clan + " &aand &f" + requesterClan + " &aare now allies!");

            openRequests();

        } else if (right) {
            requests.remove(requesterClan);
            c.set("relations.requests", requests);
            fh.saveClan(clan, c);
            player.sendMessage(MSG.color(prefix + "&cRequest rejected"));
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
            broadcastClan(clan, "&cAlliance removed with &f" + targetClan);
            broadcastClan(targetClan, "&cAlliance removed with &f" + clan);
        } else {
            List<String> enemies = pClan.getStringList("relations.enemies");
            List<String> tenemies = tClan.getStringList("relations.enemies");
            enemies.remove(targetClan);
            tenemies.remove(clan);
            pClan.set("relations.enemies", enemies);
            tClan.set("relations.enemies", tenemies);
            fh.saveClan(clan, pClan);
            fh.saveClan(targetClan, tClan);
            broadcastClan(clan, "&aNo longer enemy of &f" + targetClan);
            broadcastClan(targetClan, "&aNo longer enemy of &f" + clan);
        }

        openList(listType);
    }

    private void executeRelation(String mode, String targetClan) {
        String clan = getPlayerClan();

        if (!fh.clanExists(targetClan)) {
            player.sendMessage(MSG.color(prefix + "&cClan not found"));
            return;
        }
        if (targetClan.equalsIgnoreCase(clan)) {
            player.sendMessage(MSG.color(prefix + "&cInvalid target"));
            return;
        }

        FileConfiguration pClan = fh.getClan(clan);
        FileConfiguration tClan = fh.getClan(targetClan);
        if (pClan == null || tClan == null) return;

        if (mode.equals("ALLY")) {
            if (pClan.getStringList("relations.enemies").contains(targetClan)) {
                player.sendMessage(MSG.color(prefix + "&cYou are enemies"));
                return;
            }
            if (pClan.getStringList("relations.allies").contains(targetClan)) {
                player.sendMessage(MSG.color(prefix + "&cAlready allies"));
                return;
            }

            int maxAlliances = fh.getConfig().getInt("clans.max-alliances", 0);
            if (maxAlliances > 0 && pClan.getStringList("relations.allies").size() >= maxAlliances) {
                player.sendMessage(MSG.color(prefix + "&cYou have reached the max number of alliances (&e" + maxAlliances + "&c)."));
                return;
            }

            List<String> requests = tClan.getStringList("relations.requests");
            if (requests.contains(clan)) {
                player.sendMessage(MSG.color(prefix + "&cAlready sent"));
                return;
            }

            int maxRequests = fh.getConfig().getInt("clans.max-ally-requests", 0);
            if (maxRequests > 0 && requests.size() >= maxRequests) {
                player.sendMessage(MSG.color(prefix + "&cThat clan's request inbox is full (&e" + maxRequests + "&c max)."));
                return;
            }

            requests.add(clan);
            tClan.set("relations.requests", requests);
            fh.saveClan(targetClan, tClan);

            player.sendMessage(MSG.color(prefix + "&aRequest sent"));

            Player leader = getLeaderOnline(targetClan);
            if (leader != null) {
                leader.sendMessage(MSG.color(prefix));
                leader.sendMessage(MSG.color("&3| &eYou received a new alliance request."));
                TextComponent button = new TextComponent(MSG.color("&3| &a[OPEN MENU]"));
                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cls relations"));
                button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(MSG.color("&aClick to open")).create()));
                leader.spigot().sendMessage(button);
            } else {
                String leaderName = tClan.getString("leader");
                if (leaderName != null)
                    WarSystem.queueNotification(fh, leaderName,
                            "&eClan &f" + clan + " &esent an alliance request to &f" + targetClan
                                    + "&e. Use &a/cls relations &eto manage it.");
            }

        } else if (mode.equals("ENEMY")) {
            if (pClan.getStringList("relations.allies").contains(targetClan)) {
                player.sendMessage(MSG.color(prefix + "&cYou are allies"));
                return;
            }

            List<String> enemies = pClan.getStringList("relations.enemies");
            List<String> tenemies = tClan.getStringList("relations.enemies");

            if (enemies.contains(targetClan)) {
                player.sendMessage(MSG.color(prefix + "&cAlready enemy"));
                return;
            }

            enemies.add(targetClan);
            tenemies.add(clan);
            pClan.set("relations.enemies", enemies);
            tClan.set("relations.enemies", tenemies);
            fh.saveClan(clan, pClan);
            fh.saveClan(targetClan, tClan);

            broadcastClan(clan, "&cNow enemy of &f" + targetClan);
            broadcastClan(targetClan, "&cNow enemy of &f" + clan);

            for (String member : tClan.getStringList("users"))
                if (Bukkit.getPlayerExact(member) == null)
                    WarSystem.queueNotification(fh, member,
                            "&cClan &f" + clan + " &chas marked &f" + targetClan + " &cas enemy.");
        }
    }

    private ItemStack makeClanHead(String clan, String founder, int prestige, int members) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        OfflinePlayer op = Bukkit.getOfflinePlayer(founder);
        if (op.hasPlayedBefore()) meta.setOwningPlayer(op);

        FileConfiguration cfg = fh.getClan(clan);
        String displayName = cfg != null ? cfg.getString("name", clan) : clan;

        meta.setDisplayName(MSG.color("&e&l" + displayName));
        meta.setLore(MSG.colorList(List.of(
                "&7Founder: &f" + founder,
                "&7Prestige: &6" + prestige,
                "&7Members: &f" + members,
                "",
                "&aClick to select"
        )));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(MSG.color(name));
        List<String> colored = new ArrayList<>();
        lore.forEach(l -> colored.add(MSG.color(l)));
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
                p.sendMessage(MSG.color(prefix + msg));
    }
}