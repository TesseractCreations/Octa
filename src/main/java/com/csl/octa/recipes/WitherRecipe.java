package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class WitherRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public WitherRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {
        RecipeChoice.ExactChoice voidium = new RecipeChoice.ExactChoice(itemManager.create("voidium"));

        ShapedRecipe helmet = new ShapedRecipe(new NamespacedKey(plugin, "wither_mask"), itemManager.create("wither_mask"));
        helmet.shape(
                "VVV",
                "V V",
                "   "
        );
        helmet.setIngredient('V', voidium);
        Bukkit.addRecipe(helmet);

        ShapedRecipe chestplate = new ShapedRecipe(new NamespacedKey(plugin, "wither_chestplate"), itemManager.create("wither_chestplate"));
        chestplate.shape(
                "V V",
                "VVV",
                "VVV"
        );
        chestplate.setIngredient('V', voidium);
        Bukkit.addRecipe(chestplate);

        ShapedRecipe leggings = new ShapedRecipe(new NamespacedKey(plugin, "wither_leggings"), itemManager.create("wither_leggings"));
        leggings.shape(
                "VVV",
                "V V",
                "V V"
        );
        leggings.setIngredient('V', voidium);
        Bukkit.addRecipe(leggings);

        ShapedRecipe boots = new ShapedRecipe(new NamespacedKey(plugin, "wither_boots"), itemManager.create("wither_boots"));
        boots.shape(
                "   ",
                "V V",
                "V V"
        );
        boots.setIngredient('V', voidium);
        Bukkit.addRecipe(boots);

        ShapedRecipe sword = new ShapedRecipe(new NamespacedKey(plugin, "wither_sword"), itemManager.create("wither_sword"));
        sword.shape(
                " V ",
                " V ",
                " S "
        );
        sword.setIngredient('V', voidium);
        sword.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(sword);

        ShapedRecipe pickaxe = new ShapedRecipe(new NamespacedKey(plugin, "wither_pickaxe"), itemManager.create("wither_pickaxe"));
        pickaxe.shape(
                "VVV",
                " S ",
                " S "
        );
        pickaxe.setIngredient('V', voidium);
        pickaxe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(pickaxe);

        ShapedRecipe axe = new ShapedRecipe(new NamespacedKey(plugin, "wither_axe"), itemManager.create("wither_axe"));
        axe.shape(
                "VV ",
                "VS ",
                " S "
        );
        axe.setIngredient('V', voidium);
        axe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(axe);
    }
}