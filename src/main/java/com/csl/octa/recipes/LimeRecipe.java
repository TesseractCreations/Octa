package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

public class LimeRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public LimeRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        ItemStack lemonExample = itemManager.create("lemon");
        if (lemonExample == null) return;

        ItemStack limeResult = itemManager.create("lime");
        if (limeResult == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "lime_from_lemon");

        FurnaceRecipe recipe = new FurnaceRecipe(
                key,
                limeResult,
                new RecipeChoice.ExactChoice(lemonExample),
                0.1f,
                200
        );

        Bukkit.addRecipe(recipe);
    }
}