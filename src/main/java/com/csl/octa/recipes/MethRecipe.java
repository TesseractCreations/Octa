package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

public class MethRecipe {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public MethRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        register();
    }

    private void register() {

        FurnaceRecipe streetMeth = new FurnaceRecipe(
                new NamespacedKey(plugin, "street_meth"),
                itemManager.create("street_meth"),
                Material.AMETHYST_SHARD,
                10f,
                200
        );
        Bukkit.addRecipe(streetMeth);

        FurnaceRecipe doubleWashMeth = new FurnaceRecipe(
                new NamespacedKey(plugin, "double_wash_meth"),
                itemManager.create("double_wash_meth"),
                new RecipeChoice.ExactChoice(itemManager.create("street_meth")),
                15f,
                400
        );
        Bukkit.addRecipe(doubleWashMeth);

        FurnaceRecipe glassMeth = new FurnaceRecipe(
                new NamespacedKey(plugin, "glass_meth"),
                itemManager.create("glass_meth"),
                new RecipeChoice.ExactChoice(itemManager.create("double_wash_meth")),
                20f,
                600
        );
        Bukkit.addRecipe(glassMeth);

        FurnaceRecipe blueStuffMeth = new FurnaceRecipe(
                new NamespacedKey(plugin, "blue_stuff_meth"),
                itemManager.create("blue_stuff_meth"),
                new RecipeChoice.ExactChoice(itemManager.create("glass_meth")),
                25f,
                800
        );
        Bukkit.addRecipe(blueStuffMeth);

        FurnaceRecipe heisenbergMeth = new FurnaceRecipe(
                new NamespacedKey(plugin, "heisenberg_meth"),
                itemManager.create("heisenberg_meth"),
                new RecipeChoice.ExactChoice(itemManager.create("blue_stuff_meth")),
                30f,
                1000
        );
        Bukkit.addRecipe(heisenbergMeth);
    }
}