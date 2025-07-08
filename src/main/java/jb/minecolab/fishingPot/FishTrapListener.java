package jb.minecolab.fishingPot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class FishTrapListener implements Listener {
    private final FishTrapManager manager;

    public FishTrapListener(Main plugin, FishTrapManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item != null && item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer()
                        .has(Main.fishingPotKey, PersistentDataType.BYTE)) {

            Block placed = e.getBlockPlaced();
            if (placed.getRelative(BlockFace.UP).getType() != Material.WATER) {
                e.getPlayer().sendMessage("§cDebes colocarla bajo el agua.");
                e.setCancelled(true);
                return;
            }
            manager.createTrapAt(placed.getLocation());
            e.getPlayer().sendMessage("§aFishing Pot colocada correctamente.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b != null && b.getType() == Material.COMPOSTER &&
                manager.isFishingPot(b.getLocation())) {

            e.setCancelled(true);
            e.getPlayer().openInventory(manager.getInventory(b.getLocation()));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() == Material.COMPOSTER &&
                manager.isFishingPot(b.getLocation())) {

            manager.removeTrap(b.getLocation(), "[rompido]");
            e.getPlayer().sendMessage("§cFishing Pot destruida y contenido dropeado.");
        }
    }
}
