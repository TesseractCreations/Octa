package com.csl.octa.utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Warp implements Listener {

    private static JavaPlugin plugin = null;
    private static final Map<UUID, WarpSession> activeWarps = new ConcurrentHashMap<>();

    public Warp(JavaPlugin plugin) {
        Warp.plugin = plugin;
        new AnimationUtil(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!activeWarps.containsKey(player.getUniqueId())) return;

        WarpSession session = activeWarps.get(player.getUniqueId());
        if (session == null) return;

        if (session.animating) {
            e.setCancelled(true);
            return;
        }

        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        cancelWarp(player);
    }

    private static void cancelWarp(Player player) {
        WarpSession session = activeWarps.remove(player.getUniqueId());
        if (session == null) return;

        if (session.animationTask != null && !session.animationTask.isCancelled()) {
            session.animationTask.cancel();
        }
        if (session.freezeTask != null && !session.freezeTask.isCancelled()) {
            session.freezeTask.cancel();
        }
        if (session.delayedTask != null) {
            try { Bukkit.getScheduler().cancelTask(session.delayedTask); } catch (Exception ignored) {}
        }

        player.clearTitle();
        player.setGameMode(session.originalMode);
        player.teleport(session.originalLocation);

        player.sendActionBar(Component.text("Warp cancelled — you moved!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private static void registerWarp(Player player, GameMode originalMode, Location originalLocation) {
        WarpSession session = new WarpSession(originalMode, originalLocation);
        activeWarps.put(player.getUniqueId(), session);
    }

    private static void setAnimationTask(Player player, BukkitTask task) {
        WarpSession session = activeWarps.get(player.getUniqueId());
        if (session != null) session.animationTask = task;
    }

    private static void setFreezeTask(Player player, BukkitTask task) {
        WarpSession session = activeWarps.get(player.getUniqueId());
        if (session != null) session.freezeTask = task;
    }

    private static void setDelayedTask(Player player, int taskId) {
        WarpSession session = activeWarps.get(player.getUniqueId());
        if (session != null) session.delayedTask = taskId;
    }

    private static void setAnimating(Player player, boolean animating) {
        WarpSession session = activeWarps.get(player.getUniqueId());
        if (session != null) session.animating = animating;
    }

    private static boolean isWarping(Player player) {
        return activeWarps.containsKey(player.getUniqueId());
    }

    private static void finishWarp(Player player) {
        WarpSession session = activeWarps.remove(player.getUniqueId());
        if (session != null) {
            if (session.freezeTask != null && !session.freezeTask.isCancelled()) {
                session.freezeTask.cancel();
            }
        }
    }

    private static BukkitTask startFreeze(Player player, Location loc) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isWarping(player)) {
                    this.cancel();
                    return;
                }
                player.teleport(loc);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private static void animateCamera(Player player, Location from, Location to, double duration, Easing easing, Runnable done) {
        setAnimating(player, true);

        BukkitTask freezeTask = startFreeze(player, from);
        setFreezeTask(player, freezeTask);

        final long startTime = System.currentTimeMillis();
        final long dur = (long) (duration * 1000);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isWarping(player)) {
                    this.cancel();
                    if (freezeTask != null && !freezeTask.isCancelled()) freezeTask.cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                double progress = Math.min(1.0, (double) elapsed / dur);
                double eased = easing.ease(progress);

                double x = from.getX() + (to.getX() - from.getX()) * eased;
                double y = from.getY() + (to.getY() - from.getY()) * eased;
                double z = from.getZ() + (to.getZ() - from.getZ()) * eased;
                float yaw = (float) (from.getYaw() + (to.getYaw() - from.getYaw()) * eased);
                float pitch = (float) (from.getPitch() + (to.getPitch() - from.getPitch()) * eased);

                Location current = new Location(from.getWorld(), x, y, z, yaw, pitch);

                WarpSession session = activeWarps.get(player.getUniqueId());
                if (session != null && session.freezeTask != null && !session.freezeTask.isCancelled()) {
                    session.freezeTask.cancel();
                }
                BukkitTask newFreeze = startFreeze(player, current);
                setFreezeTask(player, newFreeze);

                player.teleport(current);

                if (progress >= 1.0) {
                    this.cancel();
                    if (newFreeze != null && !newFreeze.isCancelled()) newFreeze.cancel();
                    setAnimating(player, false);
                    if (done != null) done.run();
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        setAnimationTask(player, task);
    }

    public static void warpPlayer(Player player, Location from, Location to) {
        if (player == null || !player.isOnline() || from == null || to == null) return;
        if (from.getWorld() == null || to.getWorld() == null) return;

        GameMode originalMode = player.getGameMode();
        registerWarp(player, originalMode, from.clone());

        Location start = from.clone();
        start.setYaw(player.getLocation().getYaw());
        start.setPitch(player.getLocation().getPitch());

        Location startBack = start.clone().add(start.getDirection().normalize().multiply(-5));
        startBack.setYaw(start.getYaw());
        startBack.setPitch(start.getPitch());

        Location end = to.clone();
        Location endBack = end.clone().add(end.getDirection().normalize().multiply(-5));
        endBack.setYaw(end.getYaw());
        endBack.setPitch(end.getPitch());

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(start);

        Component titleText = Component.text("\uD001")
                .style(Style.style().font(Key.key("tesseract", "ui")).build());

        player.showTitle(Title.title(
                titleText,
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(1000),
                        Duration.ofMillis(500)
                )
        ));

        animateCamera(player, start, startBack, 0.5, Easing.EASE_IN_CIRC, () -> {
            if (!isWarping(player)) return;

            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead() || !isWarping(player)) {
                    finishWarp(player);
                    player.setGameMode(originalMode);
                    return;
                }

                player.teleport(endBack);

                animateCamera(player, endBack, end, 0.5, Easing.EASE_OUT_CIRC, () -> {
                    if (player.isOnline()) {
                        player.teleport(end);
                        player.setGameMode(originalMode);
                    }
                    finishWarp(player);
                });
            }, 20L).getTaskId();
            setDelayedTask(player, taskId);
        });
    }

    public static void warpPlayerSpawn(Player player, Location from, Location to) {
        if (player == null || !player.isOnline() || from == null || to == null) return;
        if (from.getWorld() == null || to.getWorld() == null) return;

        GameMode originalMode = player.getGameMode();
        registerWarp(player, originalMode, from.clone());

        Location start = from.clone();
        start.setYaw(player.getLocation().getYaw());
        start.setPitch(player.getLocation().getPitch());

        Location startBack = start.clone().add(start.getDirection().normalize().multiply(-5));
        startBack.setYaw(start.getYaw());
        startBack.setPitch(start.getPitch());

        Location end = to.clone();
        Location endBack = end.clone().add(end.getDirection().normalize().multiply(-5));
        endBack.setYaw(end.getYaw());
        endBack.setPitch(end.getPitch());

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(start);

        Component titleText = Component.text("\uD001")
                .style(Style.style().font(Key.key("tesseract", "ui")).build());

        player.showTitle(Title.title(
                titleText,
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(1500),
                        Duration.ofMillis(500)
                )
        ));

        animateCamera(player, start, startBack, 0.5, Easing.EASE_IN_CIRC, () -> {
            if (!isWarping(player)) return;

            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead() || !isWarping(player)) {
                    finishWarp(player);
                    player.setGameMode(originalMode);
                    return;
                }

                player.teleport(endBack);

                animateCamera(player, endBack, end, 0.5, Easing.EASE_OUT_CIRC, () -> {
                    if (player.isOnline()) {
                        player.teleport(end);
                        player.setGameMode(originalMode);
                    }
                    finishWarp(player);
                });
            }, 30L).getTaskId();
            setDelayedTask(player, taskId);
        });
    }

    public static void warpPlayerTpa(Player player, Location from, Location to) {
        if (player == null || !player.isOnline() || from == null || to == null) return;
        if (from.getWorld() == null || to.getWorld() == null) return;

        GameMode originalMode = player.getGameMode();
        registerWarp(player, originalMode, from.clone());

        Location groundLoc = from.clone();
        World world = groundLoc.getWorld();
        if (world == null) return;

        int groundY = world.getHighestBlockYAt(groundLoc);
        if (groundLoc.getBlockY() > groundY + 1) {
            groundLoc.setY(groundY + 1);
        }

        groundLoc.setYaw(from.getYaw());
        groundLoc.setPitch(from.getPitch());

        player.setGameMode(GameMode.SPECTATOR);

        animateCamera(player, from, groundLoc, 0.3, Easing.EASE_IN_QUAD, () -> {
            if (!player.isOnline() || player.isDead() || !isWarping(player)) {
                finishWarp(player);
                player.setGameMode(originalMode);
                return;
            }

            Component titleText = Component.text("\uD001")
                    .style(Style.style().font(Key.key("tesseract", "ui")).build());

            player.showTitle(Title.title(
                    titleText,
                    Component.empty(),
                    Title.Times.times(
                            Duration.ofMillis(100),
                            Duration.ofMillis(200),
                            Duration.ofMillis(100)
                    )
            ));

            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead() || !isWarping(player)) {
                    finishWarp(player);
                    player.setGameMode(originalMode);
                    return;
                }

                player.teleport(to);
                player.setGameMode(originalMode);
                finishWarp(player);

                player.playSound(Sound.sound(
                        Key.key("minecraft", "entity.firework_rocket.blast"),
                        Sound.Source.PLAYER, 1.0f, 1.2f
                ));
                player.playSound(Sound.sound(
                        Key.key("minecraft", "entity.firework_rocket.twinkle"),
                        Sound.Source.PLAYER, 0.6f, 1.5f
                ));

                World destWorld = to.getWorld();
                if (destWorld != null) {
                    Location particleLoc = to.clone().add(0, 0.5, 0);
                    destWorld.spawnParticle(Particle.FIREWORK, particleLoc, 30, 0.3, 0.5, 0.3, 0.05);
                    destWorld.spawnParticle(Particle.POOF, particleLoc, 15, 0.2, 0.3, 0.2, 0.02);
                    destWorld.spawnParticle(Particle.FLASH, particleLoc, 1, 0, 0, 0, 0);
                    destWorld.spawnParticle(Particle.END_ROD, particleLoc, 10, 0.3, 0.5, 0.3, 0.03);
                }
            }, 4L).getTaskId();
            setDelayedTask(player, taskId);
        });
    }

    public void cleanup() {
        for (UUID uuid : activeWarps.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                cancelWarp(player);
            }
        }
        activeWarps.clear();
    }

    private static class WarpSession {
        final GameMode originalMode;
        final Location originalLocation;
        BukkitTask animationTask;
        BukkitTask freezeTask;
        Integer delayedTask;
        boolean animating = false;

        WarpSession(GameMode originalMode, Location originalLocation) {
            this.originalMode = originalMode;
            this.originalLocation = originalLocation;
        }
    }
}