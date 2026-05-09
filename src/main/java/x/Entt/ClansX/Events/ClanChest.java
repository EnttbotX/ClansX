package x.Entt.ClansX.Events;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import x.Entt.ClansX.CX;
import x.Entt.ClansX.Utils.FileHandler;
import x.Entt.ClansX.Utils.MSG;

import java.util.ArrayList;
import java.util.List;

public class ClanChest implements Listener {
    private final CX plugin;
    private final Player owner;
    private final String clan;
    private final Inventory chest;

    public ClanChest(CX plugin, Player owner, String clan, Inventory chest) {
        this.plugin = plugin;
        this.owner = owner;
        this.clan = clan;
        this.chest = chest;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTopInventory().equals(chest)) return;

        if (e.getRawSlot() >= chest.getSize()) return;

        if (!owner.hasPermission("cx.user.chest")) {
            e.setCancelled(true);
            owner.sendMessage(MSG.color(CX.prefix + "&cYou lost permission to modify the clan chest."));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!e.getView().getTopInventory().equals(chest)) return;

        if (!owner.hasPermission("cx.user.chest")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getView().getTopInventory().equals(chest)) return;

        FileHandler fh = plugin.getFH();
        FileConfiguration cfg = fh.getClan(clan);

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : chest.getContents()) {
            if (item != null) items.add(item.clone());
        }

        cfg.set("clanChest", items);
        fh.saveClan(clan, cfg);

        HandlerList.unregisterAll(this);
    }
}