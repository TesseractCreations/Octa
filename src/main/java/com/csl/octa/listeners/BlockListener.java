package com.csl.octa.listeners;

import com.csl.octa.managers.BlockManager;
import com.csl.octa.managers.ItemManager;
import com.csl.octa.models.CustomBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class BlockListener implements Listener {
    private final BlockManager blockManager;
    private final ItemManager itemManager;
    private final Plugin plugin;

    private final Set<UUID> fatigueActive = new HashSet<>();
    private final Map<UUID, Location> lastFatigueBlock = new HashMap<>();

    public BlockListener(BlockManager blockManager, ItemManager itemManager, Plugin plugin) {
        this.blockManager = blockManager;
        this.itemManager = itemManager;
        this.plugin = plugin;

        startLookDetection();
    }


    private void startLookDetection() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    RayTraceResult ray = player.rayTraceBlocks(6);

                    if (ray != null && ray.getHitBlock() != null) {
                        Block target = ray.getHitBlock();
                        CustomBlock cb = blockManager.getCustomBlockAt(target);

                        if (cb != null && cb.getHardness() > 0.1) {
                            int amplifier;
                            double hardness = cb.getHardness();

                            if (hardness <= 3.0) {
                                amplifier = 1;
                            } else if (hardness <= 6.0) {
                                amplifier = 2;
                            } else {
                                amplifier = 3;
                            }

                            UUID uuid = player.getUniqueId();
                            Location blockLoc = target.getLocation();

                            boolean isNew = !fatigueActive.contains(uuid);
                            boolean isDifferentBlock = !blockLoc.equals(lastFatigueBlock.get(uuid));

                            if (isNew || isDifferentBlock) {
                                player.sendBlockDamage(blockLoc, 0.0f);

                                lastFatigueBlock.put(uuid, blockLoc);
                            }

                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.MINING_FATIGUE,
                                    25,
                                    amplifier,
                                    false, false, false
                            ));
                            fatigueActive.add(uuid);
                        } else {
                            removeFatigue(player);
                        }
                    } else {
                        removeFatigue(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void removeFatigue(Player player) {
        if (fatigueActive.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            lastFatigueBlock.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String customId = itemManager.getItemId(item);
        if (customId != null) {
            CustomBlock cb = blockManager.getCustomBlock(customId);
            if (cb != null) blockManager.placeBlock(event.getBlock().getLocation(), customId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        CustomBlock cb = blockManager.getCustomBlockAt(event.getBlock());
        if (cb == null) return;

        event.setDropItems(false);
        processBreak(event.getBlock(), cb);

        removeFatigue(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        for (Block b : blocks) {
            CustomBlock cb = blockManager.getCustomBlockAt(b);
            if (cb != null) {
                processBreak(b, cb);
            }
        }
    }

    private void processBreak(Block b, CustomBlock cb) {
        Location dropLoc = b.getLocation().add(0.5, 0.2, 0.5);
        blockManager.removePlacedBlock(b.getLocation());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cb.getDrops() != null) {
                b.getWorld().dropItem(dropLoc, cb.getDrops()).setVelocity(new Vector(0, 0, 0));
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.MUSHROOM_STEM) event.setCancelled(true);
    }
}