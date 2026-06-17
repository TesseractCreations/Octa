package com.csl.octa.listeners;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ConfettiListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;

    private static final Color[] CONFETTI_COLORS = {
            Color.fromRGB(255, 0, 0),
            Color.fromRGB(255, 105, 0),
            Color.fromRGB(255, 255, 0),
            Color.fromRGB(0, 255, 0),
            Color.fromRGB(0, 200, 255),
            Color.fromRGB(255, 0, 255),
            Color.fromRGB(255, 50, 150),
            Color.fromRGB(0, 255, 200),
            Color.fromRGB(150, 50, 255),
            Color.fromRGB(255, 200, 0),
    };

    public ConfettiListener(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!itemManager.isItem(item, "confetti")) return;

        event.setCancelled(true);
        item.setAmount(item.getAmount() - 1);
        spawnConfetti(player);
    }

    private void spawnConfetti(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Location origin = eyeLoc.clone().add(direction.clone().multiply(0.5));

        player.getWorld().playSound(player.getLocation(), "tesseract:confetti", 1.0f, 1.0f);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ConfettiPiece> pieces = new ArrayList<>();

        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        Vector up = right.clone().crossProduct(direction).normalize();

        int count = 40;

        for (int i = 0; i < count; i++) {
            Color color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.length)];

            double forwardSpeed = 0.3 + random.nextDouble() * 0.4;
            double spreadH = (random.nextDouble() - 0.5) * 0.4;
            double spreadV = (random.nextDouble() - 0.5) * 0.4;

            Vector velocity = direction.clone().multiply(forwardSpeed)
                    .add(right.clone().multiply(spreadH))
                    .add(up.clone().multiply(spreadV));

            Location spawnLoc = origin.clone();
            TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
                td.text(Component.text(" "));
                td.setBillboard(Display.Billboard.CENTER);
                td.setBackgroundColor(color);
                td.setDefaultBackground(false);
                td.setSeeThrough(true);
                td.setShadowed(false);
                td.setGravity(false);

                Transformation transformation = new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(0.4f, 0.4f, 0.4f),
                        new AxisAngle4f(0, 0, 0, 1)
                );
                td.setTransformation(transformation);
            });

            pieces.add(new ConfettiPiece(display, velocity, random.nextFloat() * 100));
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60;
            final double gravity = 0.015;
            final double drag = 0.96;
            final double sway = 0.015;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    for (ConfettiPiece piece : pieces) {
                        piece.display.remove();
                    }
                    cancel();
                    return;
                }

                boolean fadePhase = ticks > maxTicks - 10;

                for (ConfettiPiece piece : pieces) {
                    if (piece.display.isDead()) continue;

                    piece.velocity.setY(piece.velocity.getY() - gravity);
                    piece.velocity.multiply(drag);

                    double sx = Math.sin(ticks * 0.3 + piece.seed) * sway;
                    double sz = Math.cos(ticks * 0.25 + piece.seed * 0.7) * sway;
                    piece.velocity.setX(piece.velocity.getX() + sx);
                    piece.velocity.setZ(piece.velocity.getZ() + sz);

                    Location current = piece.display.getLocation();
                    Location next = current.add(piece.velocity);
                    piece.display.teleport(next);

                    if (fadePhase) {
                        float scale = 0.4f * ((maxTicks - ticks) / 10f);
                        Transformation fadeTransform = new Transformation(
                                new Vector3f(0, 0, 0),
                                new AxisAngle4f(0, 0, 0, 1),
                                new Vector3f(scale, scale, scale),
                                new AxisAngle4f(0, 0, 0, 1)
                        );
                        piece.display.setTransformation(fadeTransform);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static class ConfettiPiece {
        final TextDisplay display;
        final Vector velocity;
        final float seed;

        ConfettiPiece(TextDisplay display, Vector velocity, float seed) {
            this.display = display;
            this.velocity = velocity;
            this.seed = seed;
        }
    }
}