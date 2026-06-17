package com.csl.octa.listeners;

import com.csl.octa.managers.BlockManager;
import com.csl.octa.managers.ItemManager;
import com.csl.octa.models.CustomBlock;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class LemonTreeListener implements Listener {

    private final BlockManager blockManager;
    private final ItemManager itemManager;

    public LemonTreeListener(BlockManager blockManager, ItemManager itemManager) {
        this.blockManager = blockManager;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onHarvestLemonTree(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            Player player = e.getPlayer();
            CustomBlock cb = blockManager.getCustomBlockAt(e.getClickedBlock());
            if (cb == blockManager.getCustomBlock("lemon_leaves")) {
                e.setCancelled(true);
                player.give(itemManager.create("lemon"));
                player.getWorld().playSound(
                        Sound.sound(Key.key("minecraft", "block.sweet_berry_bush.pick_berries"), Sound.Source.BLOCK, 1f, 1f),
                        Sound.Emitter.self()

                );
            }
        }
    }

}
