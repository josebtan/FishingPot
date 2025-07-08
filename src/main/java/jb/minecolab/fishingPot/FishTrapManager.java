package jb.minecolab.fishingPot;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FishTrapManager {

    private final JavaPlugin plugin;
    private final Map<Location, Inventory> trapInventories = new HashMap<>();
    private final Map<Location, Entity> visuals = new HashMap<>();
    private final Random random = new Random();

    // Configuraciones de loot
    private double fishChance, junkChance, treasureChance, nothingChance;
    private int avgIntervalTicks;

    // Loot por tipo
    private final WeightedRandom<ItemStack> fishTable = new WeightedRandom<>();
    private final WeightedRandom<ItemStack> junkTable = new WeightedRandom<>();
    private final WeightedRandom<ItemStack> treasureTable = new WeightedRandom<>();

    public FishTrapManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        double avgSec = plugin.getConfig().getDouble("average-interval-seconds", 10.0);
        avgIntervalTicks = (int) (avgSec * 20);

        fishChance     = plugin.getConfig().getDouble("categories.fish", 0.70);
        junkChance     = plugin.getConfig().getDouble("categories.junk", 0.10);
        treasureChance = plugin.getConfig().getDouble("categories.treasure", 0.05);
        nothingChance  = 1.0 - (fishChance + junkChance + treasureChance);

        fishTable.clear(); junkTable.clear(); treasureTable.clear();
        loadSection("fish", fishTable);
        loadSection("junk", junkTable);
        loadSection("treasure", treasureTable);
    }

    private void loadSection(String path, WeightedRandom<ItemStack> table) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            var entry = sec.get(key);
            if (entry instanceof ConfigurationSection sub) {
                double weight = sub.getDouble("weight", 0);
                if ("ENCHANTED_BOOK".equals(key)) {
                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                    ConfigurationSection enchants = sub.getConfigurationSection("enchantments");
                    if (enchants != null) {
                        for (String enchKey : enchants.getKeys(false)) {
                            Enchantment enchantment = Enchantment.getByName(enchKey.toUpperCase());
                            int level = enchants.getInt(enchKey, 1);
                            if (enchantment != null && meta != null) {
                                meta.addStoredEnchant(enchantment, level, true);
                            }
                        }
                        book.setItemMeta(meta);
                    }
                    table.add(weight, book);
                } else {
                    Material mat = Material.getMaterial(key.toUpperCase());
                    if (mat != null) table.add(weight, new ItemStack(mat));
                }
            } else {
                Material mat = Material.getMaterial(key.toUpperCase());
                double weight = sec.getDouble(key);
                if (mat != null) table.add(weight, new ItemStack(mat));
            }
        }
    }

    public void start() {
        trapInventories.keySet().forEach(this::scheduleNextCatch);

        // Cada 10 segundos: asegurar visuales
        new BukkitRunnable() {
            @Override
            public void run() {
                trapInventories.keySet().forEach(FishTrapManager.this::ensureVisual);
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    private void scheduleNextCatch(Location loc) {
        int min = (int)(avgIntervalTicks * 0.8);
        int max = (int)(avgIntervalTicks * 1.2);
        int delay = min + random.nextInt(max - min + 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                attemptCatch(loc);
                scheduleNextCatch(loc); // volver a programar siguiente intento
            }
        }.runTaskLater(plugin, delay);
    }

    private void attemptCatch(Location loc) {
        Block b = loc.getBlock();
        if (b.getType() != Material.COMPOSTER || b.getRelative(BlockFace.UP).getType() != Material.WATER) {
            removeTrap(loc, "bloque inválido");
            return;
        }

        ensureVisual(loc);
        b.getWorld().spawnParticle(Particle.WATER_BUBBLE, loc.clone().add(0.5, 1.0, 0.5), 10, 0.3, 0.2, 0.3);

        double r = random.nextDouble();
        ItemStack drop = null;

        if (r < fishChance) drop = fishTable.next(random);
        else if (r < fishChance + junkChance) drop = junkTable.next(random);
        else if (r < fishChance + junkChance + treasureChance) drop = treasureTable.next(random);

        if (drop != null) {
            trapInventories.get(loc).addItem(drop);
            b.getWorld().playSound(loc, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 1f);
        }
    }

    private void ensureVisual(Location loc) {
        Entity e = visuals.get(loc);
        if (e == null || !e.isValid()) {
            FallingBlock fb = loc.getWorld().spawnFallingBlock(loc.clone().add(0.5, 0.1, 0.5), Material.COBWEB.createBlockData());
            fb.setGravity(false);
            fb.setDropItem(false);
            visuals.put(loc, fb);
        }
    }

    public void createTrapAt(Location loc) {
        trapInventories.computeIfAbsent(loc, l -> {
            Inventory inv = Bukkit.createInventory(null, 27, "Fishing Pot");
            scheduleNextCatch(l);
            return inv;
        });
        ensureVisual(loc);
    }

    public void removeTrap(Location loc, String reason) {
        Inventory inv = trapInventories.remove(loc);
        if (inv != null) {
            inv.forEach(item -> {
                if (item != null)
                    loc.getWorld().dropItemNaturally(loc, item);
            });
        }

        Entity visual = visuals.remove(loc);
        if (visual != null && !visual.isDead()) visual.remove();
    }

    public Map<Location, Inventory> getTrapInventories() {
        return Collections.unmodifiableMap(trapInventories);
    }

    public Inventory getInventory(Location loc) {
        return trapInventories.get(loc);
    }

    public boolean isFishingPot(Location loc) {
        return trapInventories.containsKey(loc);
    }

    // Utilidad para loot aleatorio con peso
    private static class WeightedRandom<T> {
        final NavigableMap<Double, T> map = new TreeMap<>();
        double total = 0;

        void add(double weight, T item) {
            if (weight <= 0) return;
            total += weight;
            map.put(total, item);
        }

        T next(Random rnd) {
            return map.higherEntry(rnd.nextDouble() * total).getValue();
        }

        void clear() {
            map.clear();
            total = 0;
        }
    }
}
