package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class GrapplingHookRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public GrapplingHookRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "grappling_hook_recipe"), itemManager.create("grappling_hook"));

        recipe.shape(
                "  C",
                " SI",
                "S  "
        );

        recipe.setIngredient('C', Material.IRON_CHAIN);
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('I', Material.IRON_INGOT);

        Bukkit.addRecipe(recipe);
    }
}