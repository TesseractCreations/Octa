package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class BurgerRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public BurgerRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        ItemStack burgerResult = itemManager.create("burger");
        if (burgerResult == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "burger");

        ShapedRecipe recipe = new ShapedRecipe(key, burgerResult);

        recipe.shape(
                " B ",
                " S ",
                " B "
        );

        recipe.setIngredient('B', Material.BREAD);
        recipe.setIngredient('S', Material.COOKED_BEEF);

        Bukkit.addRecipe(recipe);
    }
}