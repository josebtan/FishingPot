package jb.minecolab.fishingPot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TrapDataHandler {

    private final JavaPlugin plugin;
    private File file;
    private YamlConfiguration data;

    public TrapDataHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "traps.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear traps.yml");
                return;
            }
        }

        data = YamlConfiguration.loadConfiguration(file);
    }

    public void saveTraps(FishTrapManager manager) {
        data.set("traps", null); // Limpiar antes de guardar

        int index = 0;
        for (Map.Entry<Location, Inventory> entry : manager.getTrapInventories().entrySet()) {
            Location loc = entry.getKey();
            Inventory inv = entry.getValue();
            String path = "traps." + index++;

            data.set(path + ".world", loc.getWorld().getName());
            data.set(path + ".x", loc.getBlockX());
            data.set(path + ".y", loc.getBlockY());
            data.set(path + ".z", loc.getBlockZ());
            data.set(path + ".contents", inv.getContents());
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar traps.yml");
        }
    }

    public void loadTraps(FishTrapManager manager) {
        if (!data.contains("traps")) return;

        for (String key : data.getConfigurationSection("traps").getKeys(false)) {
            String path = "traps." + key;

            String world = data.getString(path + ".world");
            int x = data.getInt(path + ".x");
            int y = data.getInt(path + ".y");
            int z = data.getInt(path + ".z");

            Location loc = new Location(Bukkit.getWorld(world), x, y, z);

            if (loc.getBlock().getType() == Material.COMPOSTER) {
                manager.createTrapAt(loc);

                Inventory inv = manager.getInventory(loc);
                List<?> rawItems = data.getList(path + ".contents");
                if (inv != null && rawItems != null) {
                    inv.setContents(rawItems.toArray(new ItemStack[0]));
                }
            } else {
                plugin.getLogger().warning("[FP] Trampa ignorada, no hay composter en: " + loc);
            }
        }
    }
}
