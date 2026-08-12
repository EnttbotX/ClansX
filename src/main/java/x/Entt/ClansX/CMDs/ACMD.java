package x.Entt.ClansX.CMDs;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
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
import x.Entt.ClansX.Utils.Econo;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;

import static x.Entt.ClansX.Utils.MSG.Paths.*;

public class ACMD implements CommandExecutor, TabCompleter {

    private final CX plugin;
    private final FileHandler fh;
    private final Econo econ;

    public ACMD(CX plugin) {
        this.plugin = plugin;
        this.fh = plugin.getFH();
        this.econ = CX.getEcon();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {

        if (args.length == 0) {
            help(sender);
            return true;
        }

        if (sender instanceof Player player && !player.hasPermission("cx.admin")) {
            sender.sendMessage(MSG.send(NO_PERMS));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> reload(sender);

            case "disband" -> {
                if (args.length < 2) {
                    sender.sendMessage(MSG.send(SHOULD_USE, "/clx disband <Clan>"));
                    return true;
                }
                disband(sender, args[1].strip());
            }

            case "reports" -> {
                if (sender instanceof Player p) {
                    reportsGUI(p, 1);
                } else {
                    reportsConsole(sender);
                }
            }

            case "eco" -> economy(sender, args);

            default -> help(sender);
        }

        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage(MSG.sendList(ADMIN_HELP).toArray(new String[0]));
    }

    private void reload(CommandSender s) {
        fh.loadConfig();
        fh.loadMsg();
        econ.reload();
        s.sendMessage(MSG.send(RELOAD_COMPLETE));
    }

    public void disband(CommandSender sender, String clanName) {

        if (clanName == null || clanName.isBlank()) {
            sender.sendMessage(MSG.send(INVALID_CLAN_NAME));
            return;
        }

        if (!fh.clanExists(clanName)) {
            sender.sendMessage(MSG.send(CLAN_NOT_FOUND));
            return;
        }

        FileConfiguration clan = fh.getClan(clanName);
        if (clan == null) {
            sender.sendMessage(MSG.send(CLAN_DATA_CORRUPTED));
            return;
        }

        for (String name : clan.getStringList("users")) {
            Player online = Bukkit.getPlayerExact(name);

            if (online != null) {
                FileConfiguration pd = fh.getPlayerData(online.getUniqueId());
                pd.set("clan", null);
                fh.savePlayerData(online.getUniqueId(), pd);
                online.sendMessage(MSG.send(CLAN_DISBANDED_NOTIFY, "an admin"));
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
                if (!offline.hasPlayedBefore()) continue;

                FileConfiguration pd = fh.getPlayerData(offline.getUniqueId());
                pd.set("clan", null);
                fh.savePlayerData(offline.getUniqueId(), pd);
            }
        }

        plugin.getWarSystem().resolveWarsOnDisband(clanName);
        fh.deleteClan(clanName);
        sender.sendMessage(MSG.send(CLAN_DISBANDED));
    }

    private void reportsConsole(CommandSender s) {

        boolean found = false;

        for (String clan : getClanNames()) {
            FileConfiguration c = fh.getClan(clan);
            if (c == null) continue;

            List<String> reports = c.getStringList("reports");
            if (reports.isEmpty()) continue;

            found = true;

            s.sendMessage(MSG.sendWithoutPrefix(ADMIN_REPORTS_CONSOLE_CLAN_HEADER, clan));
            reports.forEach(r -> s.sendMessage(MSG.sendWithoutPrefix(ADMIN_REPORTS_CONSOLE_LINE, r)));
            s.sendMessage("");
        }

        if (!found)
            s.sendMessage(MSG.send(NO_CLAN_REPORTS));
    }

    public void reportsGUI(Player p, int pg) {
        List<Map.Entry<String, List<String>>> data = new ArrayList<>();

        for (String clan : getClanNames()) {
            FileConfiguration c = fh.getClan(clan);
            if (c == null) continue;

            List<String> r = c.getStringList("reports");
            if (!r.isEmpty())
                data.add(Map.entry(clan, r));
        }

        if (data.isEmpty()) {
            p.sendMessage(MSG.send(NO_CLAN_REPORTS));
            return;
        }

        int perPage = 45;
        int max = (int) Math.ceil((double) data.size() / perPage);
        int page = Math.max(1, Math.min(pg, max));

        Inventory inv = Bukkit.createInventory(null, 54,
                MSG.sendWithoutPrefix(ADMIN_REPORTS_GUI_TITLE, String.valueOf(page), String.valueOf(max)));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, data.size());

        for (int i = start; i < end; i++) {

            var entry = data.get(i);

            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            meta.setDisplayName(MSG.sendWithoutPrefix(ADMIN_REPORTS_GUI_ITEM_NAME, entry.getKey()));

            List<String> lore = new ArrayList<>();
            entry.getValue().forEach(r -> lore.add(MSG.sendWithoutPrefix(ADMIN_REPORTS_GUI_ITEM_LORE_LINE, r)));

            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.addItem(item);
        }

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            if (m != null) {
                m.setDisplayName(MSG.sendWithoutPrefix(UI_PREVIOUS_PAGE));
                prev.setItemMeta(m);
                inv.setItem(45, prev);
            }
        }

        if (page < max) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            if (m != null) {
                m.setDisplayName(MSG.sendWithoutPrefix(UI_NEXT_PAGE));
                next.setItemMeta(m);
                inv.setItem(53, next);
            }
        }

        Listener l = new Listener() {
            @EventHandler
            public void click(InventoryClickEvent e) {
                if (!e.getInventory().equals(inv)) return;
                e.setCancelled(true);

                if (!(e.getWhoClicked() instanceof Player pl)) return;

                if (e.getSlot() == 45 && page > 1) {
                    HandlerList.unregisterAll(this);
                    reportsGUI(pl, page - 1);
                }

                if (e.getSlot() == 53 && page < max) {
                    HandlerList.unregisterAll(this);
                    reportsGUI(pl, page + 1);
                }
            }

            @EventHandler
            public void close(InventoryCloseEvent e) {
                if (e.getInventory().equals(inv))
                    HandlerList.unregisterAll(this);
            }
        };

        Bukkit.getPluginManager().registerEvents(l, plugin);
        p.openInventory(inv);
    }

    private void economy(CommandSender s, String[] args) {
        if (args.length < 5) {
            s.sendMessage(MSG.send(SHOULD_USE, "/clx eco <player|clan> <name> <set|add|reset> <amount>"));
            return;
        }

        String type = args[1];
        String name = args[2];
        String action = args[3].toLowerCase();
        double amount = action.equals("reset") ? 0 : parseAmount(s, args[4]);
        if (amount < 0) return;

        if (type.equalsIgnoreCase("player")) {

            OfflinePlayer p = Bukkit.getOfflinePlayer(name);

            if (!p.hasPlayedBefore() && !p.isOnline()) {
                s.sendMessage(MSG.send(PLAYER_NOT_FOUND));
                return;
            }

            modifyPlayer(s, p, action, amount);
            return;
        }

        if (type.equalsIgnoreCase("clan")) {

            FileConfiguration c = fh.getClan(name);
            if (c == null) {
                s.sendMessage(MSG.send(CLAN_NOT_FOUND));
                return;
            }

            double current = c.getDouble("Balance");

            switch (action) {
                case "set" -> c.set("Balance", amount);
                case "add" -> c.set("Balance", current + amount);
                case "reset" -> c.set("Balance", 0);
                default -> {
                    s.sendMessage(MSG.send(INVALID_ACTION));
                    return;
                }
            }

            fh.saveClan(name, c);
            s.sendMessage(MSG.send(CLAN_ECONOMY_UPDATED));
        }
    }

    private void modifyPlayer(CommandSender s, OfflinePlayer p, String action, double amount) {

        double current = econ.getBalance(p);

        switch (action) {
            case "set" -> {
                if (amount > current) econ.deposit(p, amount - current);
                else econ.withdraw(p, current - amount);
            }
            case "add" -> econ.deposit(p, amount);
            case "reset" -> econ.withdraw(p, current);
            default -> {
                s.sendMessage(MSG.send(INVALID_ACTION));
                return;
            }
        }

        s.sendMessage(MSG.send(PLAYER_ECONOMY_UPDATED, p.getName()));
    }

    private double parseAmount(CommandSender s, String v) {
        try {
            double d = Double.parseDouble(v);
            if (d < 0) throw new NumberFormatException();
            return d;
        } catch (Exception e) {
            s.sendMessage(MSG.send(INVALID_AMOUNT));
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, String[] a) {

        if (s instanceof Player p && !p.hasPermission("cx.admin"))
            return List.of();

        if (a.length == 1)
            return Stream.of("reload", "disband", "reports", "eco")
                    .filter(x -> x.startsWith(a[0].toLowerCase()))
                    .toList();

        if (a[0].equalsIgnoreCase("eco")) {
            if (a.length == 2) return List.of("player", "clan");
            if (a.length == 3 && a[1].equalsIgnoreCase("clan")) return getClanNames();
            if (a.length == 4) return List.of("set", "add", "reset");
        }

        if (a[0].equalsIgnoreCase("disband") && a.length == 2)
            return getClanNames();

        return List.of();
    }

    private List<String> getClanNames() {
        File[] files = fh.getClansDir().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(f -> f.getName().substring(0, f.getName().length() - 4))
                .collect(Collectors.toList());
    }
}