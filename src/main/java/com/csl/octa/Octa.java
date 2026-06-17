package com.csl.octa;

import com.csl.octa.items.drinkable.Beer;
import com.csl.octa.items.drinkable.Mojito;
import com.csl.octa.items.drugs.Meth;
import com.csl.octa.items.edible.Burger;
import com.csl.octa.items.misc.Barley;
import com.csl.octa.items.misc.Lemon;
import com.csl.octa.items.misc.Lime;
import com.csl.octa.items.misc.Mint;
import com.csl.octa.items.weapon.GrapplingHook;
import com.csl.octa.items.weapon.Wither;
import com.csl.octa.listeners.*;
import com.csl.octa.managers.BlockManager;
import com.csl.octa.managers.Commands;
import com.csl.octa.managers.ItemManager;
import com.csl.octa.managers.MapManager;
import com.csl.octa.populators.LemonTreePopulator;
import com.csl.octa.recipes.*;
import com.csl.octa.utils.Warp;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Octa extends JavaPlugin {

    @Override
    public void onLoad() {
        CommandAPI.onLoad(
                new CommandAPIPaperConfig(this)
                        .silentLogs(true)
                        .fallbackToLatestNMS(false)
                        .missingExecutorImplementationMessage("An error occured")
        );
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Warp warp = new Warp(this);
        ItemManager itemManager = new ItemManager(this);
        BlockManager blockManager = new BlockManager(this, itemManager);
        PluginManager pm = getServer().getPluginManager();
        MapManager mapManager = new MapManager(this);

        // Items
        new Mojito(itemManager).register();
        new Lime(itemManager).register();
        new Lemon(itemManager).register();
        new Mint(itemManager).register();
        new Meth(itemManager).register();
        new Barley(itemManager).register();
        new GrapplingHook(itemManager).register();
        new Beer(itemManager).register();
        //new Wither(itemManager).register();
        new Burger(itemManager).register();

        // Recipes
        new LimeRecipe(this, itemManager);
        new MintRecipe(this, itemManager);
        new MojitoRecipe(this, itemManager);
        new MethRecipe(this, itemManager);
        new BarleyRecipe(this, itemManager);
        new BeerRecipe(this, itemManager);
        new GrapplingHookRecipe(this, itemManager);
        new BurgerRecipe(this, itemManager);
        //new WitherRecipe(this, itemManager);

        // Blocks
        blockManager.register("lemon_leaves", Material.PAPER, "tesseract:block/lemon_oak_leaves.png", Component.text("Lemon Oak Leaves"),0.1, itemManager.create("lemon"));

        // Events
        GrapplingHookListener grapplingHookListener = new GrapplingHookListener(this, itemManager);

        pm.registerEvents(new LemonTreeListener(blockManager, itemManager), this);
        pm.registerEvents(grapplingHookListener, this);
        pm.registerEvents(new BlockListener(blockManager, itemManager, this), this);
        //pm.registerEvents(new VoidiumListener(this, itemManager), this);
        pm.registerEvents(new MapListener(this, mapManager), this);

        for (World world : getServer().getWorlds()) {
            world.getPopulators().add(new LemonTreePopulator(blockManager));
        }

        // Commands
        Commands commands = new Commands(this, warp, itemManager, grapplingHookListener, mapManager);
        commands.register();

    }

    /*

    STUFF TO DO.
    Make wither set abilities
    Test maps

     */

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
