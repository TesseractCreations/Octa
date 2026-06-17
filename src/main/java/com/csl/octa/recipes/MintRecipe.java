package com.csl.octa.recipes;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MintRecipe implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    public MintRecipe(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onGrindstoneInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType() == Material.GRINDSTONE) {
                if (heldItem.getType() == Material.SPRUCE_SAPLING) {
                    e.setCancelled(true);
                    player.getWorld().playSound(
                            Sound.sound(Key.key("minecraft", "block.grindstone.use"), Sound.Source.BLOCK, 1f, 1f),
                            Sound.Emitter.self()

                    );
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    if (heldItem.getAmount() <= 0) {
                        player.getInventory().setItemInMainHand(itemManager.create("mint_leaves"));
                    } else {
                        player.give(itemManager.create("mint_leaves"));
                    }
                }
            }
        }
    }
}