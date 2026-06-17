package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class BeerRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public BeerRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        ShapelessRecipe malted_recipe = new ShapelessRecipe(new NamespacedKey(plugin, "malted_glass"), itemManager.create("malted_glass"));
        malted_recipe.addIngredient(new RecipeChoice.ExactChoice(itemManager.create("barley")));
        malted_recipe.addIngredient(new RecipeChoice.ExactChoice(itemManager.create("empty_glass")));

        Bukkit.addRecipe(malted_recipe);

        // ------------------------ \\

        ShapelessRecipe sweet_malted_recipe = new ShapelessRecipe(new NamespacedKey(plugin, "sweet_malted_glass"), itemManager.create("sweet_malted_glass"));
        sweet_malted_recipe.addIngredient(new RecipeChoice.ExactChoice(itemManager.create("malted_glass")));
        sweet_malted_recipe.addIngredient(Material.SUGAR);

        Bukkit.addRecipe(sweet_malted_recipe);

        // ------------------------ \\

        ShapelessRecipe fermented_recipe = new ShapelessRecipe(new NamespacedKey(plugin, "fermented_glass"), itemManager.create("fermented_glass"));
        fermented_recipe.addIngredient(new RecipeChoice.ExactChoice(itemManager.create("malted_glass")));
        fermented_recipe.addIngredient(Material.SUGAR);

        Bukkit.addRecipe(fermented_recipe);

        // ------------------------ \\

        ShapelessRecipe sweet_fermented_recipe = new ShapelessRecipe(new NamespacedKey(plugin, "sweet_fermented_glass"), itemManager.create("sweet_fermented_glass"));
        sweet_fermented_recipe.addIngredient(new RecipeChoice.ExactChoice(itemManager.create("fermented_glass")));
        sweet_fermented_recipe.addIngredient(Material.SUGAR);

        Bukkit.addRecipe(sweet_fermented_recipe);

        // ------------------------ \\

        ShapelessRecipe empty_glass_recipe = new ShapelessRecipe(new NamespacedKey(plugin, "empty_glass"), itemManager.create("empty_glass"));
        empty_glass_recipe.addIngredient(Material.GLASS_BOTTLE);
        empty_glass_recipe.addIngredient(Material.GLASS);

        Bukkit.addRecipe(empty_glass_recipe);

        // ------------------------ \\

        FurnaceRecipe beer_recipe = new FurnaceRecipe(
                new NamespacedKey(plugin, "beer_drink"),
                itemManager.create("beer_drink"),
                new RecipeChoice.ExactChoice(itemManager.create("fermented_glass")),
                0.1f,
                200
        );

        Bukkit.addRecipe(beer_recipe);

        // ------------------------ \\

        FurnaceRecipe sweet_beer_recipe = new FurnaceRecipe(
                new NamespacedKey(plugin, "sweet_beer_drink"),
                itemManager.create("sweet_beer_drink"),
                new RecipeChoice.ExactChoice(itemManager.create("sweet_fermented_glass")),
                0.1f,
                200
        );

        Bukkit.addRecipe(sweet_beer_recipe);

    }
}