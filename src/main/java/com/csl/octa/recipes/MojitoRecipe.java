package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class MojitoRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public MojitoRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        ItemStack mojitoResult = itemManager.create("mojito_drink");
        if (mojitoResult == null) return;

        ItemStack limeItem = itemManager.create("lime");
        ItemStack mintItem = itemManager.create("mint_leaves");

        if (limeItem == null || mintItem == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "mojito_drink");

        ShapelessRecipe recipe = new ShapelessRecipe(key, mojitoResult);

        recipe.addIngredient(new RecipeChoice.ExactChoice(createWaterBottle()));

        recipe.addIngredient(new RecipeChoice.ExactChoice(limeItem));

        recipe.addIngredient(new RecipeChoice.ExactChoice(mintItem));

        recipe.addIngredient(Material.SUGAR);

        Bukkit.addRecipe(recipe);
    }

    private ItemStack createWaterBottle() {
        ItemStack potion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(org.bukkit.potion.PotionType.WATER);
        potion.setItemMeta(meta);
        return potion;
    }
}