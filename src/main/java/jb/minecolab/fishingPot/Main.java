package jb.minecolab.fishingPot;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Main extends JavaPlugin {

    public static NamespacedKey fishingPotKey;
    private FishTrapManager trapManager;
    private TrapDataHandler trapDataHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fishingPotKey = new NamespacedKey(this, "fishing_pot");

        // Mensaje de inicio en consola con arte ASCII
        getLogger().info("""
               ▄████████  ▄█     ▄████████    ▄█    █▄     ▄█  ███▄▄▄▄      ▄██████▄     ▄███████▄  ▄██████▄      ███     
              ███    ███ ███    ███    ███   ███    ███   ███  ███▀▀▀██▄   ███    ███   ███    ███ ███    ███ ▀█████████▄ 
              ███    █▀  ███▌   ███    █▀    ███    ███   ███▌ ███   ███   ███    █▀    ███    ███ ███    ███    ▀███▀▀██ 
             ▄███▄▄▄     ███▌   ███         ▄███▄▄▄▄███▄▄ ███▌ ███   ███  ▄███          ███    ███ ███    ███     ███   ▀ 
            ▀▀███▀▀▀     ███▌ ▀███████████ ▀▀███▀▀▀▀███▀  ███▌ ███   ███ ▀▀███ ████▄  ▀█████████▀  ███    ███     ███     
              ███        ███           ███   ███    ███   ███  ███   ███   ███    ███   ███        ███    ███     ███     
              ███        ███     ▄█    ███   ███    ███   ███  ███   ███   ███    ███   ███        ███    ███     ███     
              ███        █▀    ▄████████▀    ███    █▀    █▀    ▀█   █▀    ████████▀   ▄████▀       ▀██████▀     ▄████▀   
        """);

        // Inicializar sistemas
        trapManager = new FishTrapManager(this);
        trapDataHandler = new TrapDataHandler(this);

        // Cargar trampas guardadas
        trapDataHandler.loadTraps(trapManager);

        // Empezar sistema de pesca automática
        trapManager.start();

        // Registrar eventos
        Bukkit.getPluginManager().registerEvents(new FishTrapListener(this, trapManager), this);

        // Registrar receta
        registerFishingPotRecipe();

        getLogger().info("[FP] FishingPot habilitado.");
    }

    @Override
    public void onDisable() {
        // Guardar datos de trampas
        trapDataHandler.saveTraps(trapManager);
        getLogger().info("[FP] FishingPot deshabilitado y trampas guardadas.");
    }

    private void registerFishingPotRecipe() {
        ItemStack trap = createFishingPotItem();
        ShapelessRecipe recipe = new ShapelessRecipe(fishingPotKey, trap);
        recipe.addIngredient(Material.COMPOSTER);
        recipe.addIngredient(Material.COBWEB);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack createFishingPotItem() {
        ItemStack item = new ItemStack(Material.COMPOSTER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Fishing Pot");
            meta.setLore(Arrays.asList(
                    "§7Atrapa loot automáticamente",
                    "§eColócala bajo el agua"
            ));
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(fishingPotKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fp") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            trapManager.loadConfig();
            sender.sendMessage("§aFishingPot config recargada.");
            return true;
        }
        return false;
    }
}
