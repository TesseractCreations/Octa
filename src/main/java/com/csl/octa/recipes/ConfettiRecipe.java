package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfettiRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public ConfettiRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        ItemStack confettiResult = itemManager.create("confetti");
        if (confettiResult == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "confetti");

        ShapelessRecipe recipe = new ShapelessRecipe(key, confettiResult);

        recipe.addIngredient(Material.PAPER);
        recipe.addIngredient(Material.GUNPOWDER);
        recipe.addIngredient(new RecipeChoice.MaterialChoice(
                Material.RED_DYE,
                Material.ORANGE_DYE,
                Material.YELLOW_DYE,
                Material.LIME_DYE,
                Material.LIGHT_BLUE_DYE,
                Material.PINK_DYE,
                Material.PURPLE_DYE
        ));

        Bukkit.addRecipe(recipe);
    }
}