package com.thefallersgames.bettermanhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating and managing GUI elements.
 */
public class GuiUtil {

    /**
     * Creates a custom ItemStack with display name and lore.
     *
     * @param material The material of the item
     * @param name The display name of the item
     * @param lore The lore (description) of the item
     * @return The created ItemStack
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            
            if (lore != null && lore.length > 0) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            
            // Add all possible item flags to hide all attributes and make it clear this is a UI item
            meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_POTION_EFFECTS
            );
            
            // meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an inventory GUI with a title and size.
     *
     * @param title The title of the GUI
     * @param size The size of the GUI (must be a multiple of 9)
     * @return The created inventory
     */
    public static Inventory createGui(String title, int size) {
        return Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', title));
    }
    
    /**
     * Fills the empty slots of an inventory with a placeholder item.
     *
     * @param inventory The inventory to fill
     * @param material The material to use as placeholder
     */
    public static void fillEmptySlots(Inventory inventory, Material material) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createItem(material, " "));
            }
        }
    }
    
    /**
     * Creates a bordered inventory GUI with a title, size, and border material.
     *
     * @param title The title of the GUI
     * @param size The size of the GUI (must be a multiple of 9)
     * @param borderMaterial The material to use for the border
     * @return The created inventory with a border
     */
    public static Inventory createBorderedGui(String title, int size, Material borderMaterial) {
        Inventory inv = createGui(title, size);
        
        // Create border
        for (int i = 0; i < size; i++) {
            // Top and bottom rows
            if (i < 9 || i >= size - 9) {
                inv.setItem(i, createItem(borderMaterial, " "));
            }
            // Left and right columns
            else if (i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, createItem(borderMaterial, " "));
            }
        }
        
        return inv;
    }
} 