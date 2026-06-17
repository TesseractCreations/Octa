package com.csl.octa.listeners;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GrapplingHookListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, GrappleSession> activeSessions = new ConcurrentHashMap<>();

    private static final double HOOK_SPEED = 2.0;
    private static final double HOOK_RETURN_SPEED = 3.5;
    private static final double MAX_RANGE = 30.0;
    private static final double DISMANTLE_DISTANCE = 1.0;
    private static final double ENTITY_MIN_DISTANCE = 1.5;
    private static final double ENTITY_PULL_STRENGTH = 0.4;
    private static final double CHAIN_LINK_LENGTH = 0.5;
    private static final double CHAIN_LINK_THICKNESS = 0.5;
    private static final double ROPE_ADJUST_STEP = 0.5;
    private static final double MIN_ROPE_LENGTH = 0.4;
    private static final double MAX_ROPE_LENGTH = 40.0;
    private static final double MIN_DISTANCE = 1.5;
    private static final int IK_ITERATIONS = 15;

    private static final double GRAVITY = 0.08;
    private static final double PASSIVE_PULL = 0.15;
    private static final double PASSIVE_SHORTEN_RATE = 0.08;
    private static final double REEL_SPEED = 0.35;
    private static final double REEL_SHORTEN_RATE = 0.2;
    private static final double MAX_SWING_SPEED = 2.5;
    private static final double AIR_FRICTION = 0.995;
    private static final double PLAYER_PUSH_FORCE = 0.14;

    public GrapplingHookListener(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onUseGrappling(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (!itemManager.isItem(held, "grappling_hook")) return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        e.setCancelled(true);

        if (activeSessions.containsKey(player.getUniqueId())) {
            GrappleSession session = activeSessions.get(player.getUniqueId());
            if (session.state != GrappleState.RETURNING) {
                if (session.state == GrappleState.ATTACHED) {
                    player.setVelocity(player.getVelocity().multiply(1.2));
                }
                startReturn(session);
            }
            return;
        }

        launchHook(player);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player player = e.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        GrappleSession session = activeSessions.get(player.getUniqueId());
        if (session.state != GrappleState.ATTACHED) return;

        session.reeling = e.isSneaking();
    }

    @EventHandler
    public void onScroll(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();

        if (!activeSessions.containsKey(player.getUniqueId())) return;

        GrappleSession session = activeSessions.get(player.getUniqueId());
        if (session.state != GrappleState.ATTACHED) return;
        if (!player.isSneaking()) return;

        ItemStack oldItem = player.getInventory().getItem(e.getPreviousSlot());
        if (!itemManager.isItem(oldItem, "grappling_hook")) return;

        e.setCancelled(true);

        int delta = e.getNewSlot() - e.getPreviousSlot();
        if (delta > 4) delta -= 9;
        if (delta < -4) delta += 9;

        if (delta > 0) {
            session.ropeLength = Math.min(MAX_ROPE_LENGTH, session.ropeLength + ROPE_ADJUST_STEP);
        } else if (delta < 0) {
            session.ropeLength = Math.max(MIN_ROPE_LENGTH, session.ropeLength - ROPE_ADJUST_STEP);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        forceRemove(e.getPlayer());
    }

    private void launchHook(Player player) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        float hookYaw = eyeLocation.getYaw();
        float hookPitch = eyeLocation.getPitch();

        ItemDisplay hookDisplay = world.spawn(eyeLocation, ItemDisplay.class, display -> {
            display.setItemStack(itemManager.create("grappling_hook"));
            Quaternionf rotation = new Quaternionf();
            rotation.rotateY((float) Math.toRadians(-hookYaw));
            rotation.rotateX((float) Math.toRadians(hookPitch));
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    rotation,
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()
            ));
            display.setBillboard(Display.Billboard.FIXED);
            display.setGravity(false);
        });

        GrappleSession session = new GrappleSession(player, hookDisplay, direction, hookYaw, hookPitch);
        activeSessions.put(player.getUniqueId(), session);

        session.activeTask = new BukkitRunnable() {
            final Location hookLoc = eyeLocation.clone();
            double traveled = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                GrappleSession current = activeSessions.get(player.getUniqueId());
                if (current == null || current.state == GrappleState.RETURNING) {
                    this.cancel();
                    return;
                }

                if (!isHoldingGrapple(player)) {
                    startReturn(current);
                    this.cancel();
                    return;
                }

                hookLoc.add(direction.clone().multiply(HOOK_SPEED));
                traveled += HOOK_SPEED;
                hookDisplay.teleport(hookLoc);

                updateChain(session);

                RayTraceResult entityHit = world.rayTraceEntities(
                        hookLoc, direction, HOOK_SPEED, 1.0,
                        entity -> entity instanceof LivingEntity
                                && !entity.getUniqueId().equals(player.getUniqueId())
                                && !(entity instanceof ItemDisplay)
                );

                if (entityHit != null && entityHit.getHitEntity() != null) {
                    session.state = GrappleState.PULLING_ENTITY;
                    session.hookedEntity = (LivingEntity) entityHit.getHitEntity();
                    session.anchorPoint = entityHit.getHitEntity().getLocation();
                    startEntityPull(session);
                    this.cancel();
                    return;
                }

                RayTraceResult blockHit = world.rayTraceBlocks(
                        hookLoc.clone().subtract(direction.clone().multiply(HOOK_SPEED)),
                        direction, HOOK_SPEED,
                        FluidCollisionMode.NEVER, true
                );

                if (blockHit != null && blockHit.getHitBlock() != null) {
                    session.state = GrappleState.ATTACHED;
                    session.anchorPoint = blockHit.getHitPosition().toLocation(world);
                    hookDisplay.teleport(session.anchorPoint);
                    session.ropeLength = player.getEyeLocation().distance(session.anchorPoint);
                    startRopePhysics(session);
                    this.cancel();
                    return;
                }

                if (traveled >= MAX_RANGE) {
                    startReturn(session);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void startRopePhysics(GrappleSession session) {
        session.activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = session.player;

                if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                GrappleSession current = activeSessions.get(player.getUniqueId());
                if (current == null || current.state == GrappleState.RETURNING) {
                    this.cancel();
                    return;
                }

                if (!isHoldingGrapple(player)) {
                    startReturn(current);
                    this.cancel();
                    return;
                }

                simulateRope(session);
                updateChain(session);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void simulateRope(GrappleSession session) {
        Player player = session.player;
        Vector hookPos = session.anchorPoint.toVector();
        Vector playerPos = player.getLocation().toVector().add(new Vector(0, 1.0, 0));

        Vector toHook = hookPos.clone().subtract(playerPos);
        double dist = toHook.length();
        Vector ropeDir = toHook.clone().normalize();

        Vector vel = player.getVelocity().clone();

        vel.setY(vel.getY() - GRAVITY);
        vel.multiply(AIR_FRICTION);

        Vector lookDir = player.getLocation().getDirection().normalize();
        float forward = 0;
        float strafe = 0;

        if (!player.isOnGround()) {
            org.bukkit.craftbukkit.entity.CraftPlayer cp = (org.bukkit.craftbukkit.entity.CraftPlayer) player;
            forward = cp.getHandle().zza;
            strafe = cp.getHandle().xxa;
        }

        if (forward != 0 || strafe != 0) {
            Vector right = lookDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            Vector flatLook = new Vector(lookDir.getX(), 0, lookDir.getZ()).normalize();

            Vector moveDir = new Vector(0, 0, 0);

            if (forward != 0) {
                moveDir.add(flatLook.clone().multiply(forward > 0 ? 1 : -1));
            }
            if (strafe != 0) {
                moveDir.add(right.clone().multiply(strafe > 0 ? -1 : 1));
            }

            if (moveDir.lengthSquared() > 0) {
                moveDir.normalize();

                Vector tangent = moveDir.clone().subtract(ropeDir.clone().multiply(moveDir.dot(ropeDir)));

                if (tangent.lengthSquared() > 0.0001) {
                    tangent.normalize();

                    double currentSpeed = vel.length();
                    double speedBonus = 1.0 + (currentSpeed * 0.5);

                    double dotWithVel = 0;
                    if (vel.lengthSquared() > 0.001) {
                        dotWithVel = tangent.dot(vel.clone().normalize());
                    }
                    double alignmentBonus = 1.0 + Math.max(0, dotWithVel) * 1.5;

                    vel.add(tangent.multiply(PLAYER_PUSH_FORCE * speedBonus * alignmentBonus));
                }
            }
        }

        if (session.reeling) {
            session.ropeLength = Math.max(MIN_ROPE_LENGTH, session.ropeLength - REEL_SHORTEN_RATE);
            vel.add(ropeDir.clone().multiply(REEL_SPEED));
        } else {
            session.ropeLength = Math.max(MIN_ROPE_LENGTH, session.ropeLength - PASSIVE_SHORTEN_RATE);
            if (dist > MIN_DISTANCE) {
                vel.add(ropeDir.clone().multiply(PASSIVE_PULL));
            }
        }

        if (dist > session.ropeLength) {
            double radialVel = vel.dot(ropeDir);

            if (radialVel < 0) {
                vel.subtract(ropeDir.clone().multiply(radialVel));
            }

            double overshoot = dist - session.ropeLength;
            vel.add(ropeDir.clone().multiply(overshoot * 0.3));

            Vector correctedPos = hookPos.clone().subtract(ropeDir.clone().multiply(session.ropeLength));
            Vector posCorrection = correctedPos.subtract(playerPos).multiply(0.1);
            vel.add(posCorrection);
        }

        if (dist <= MIN_DISTANCE) {
            double radialVel = vel.dot(ropeDir);
            if (radialVel > 0) {
                vel.subtract(ropeDir.clone().multiply(radialVel * 0.8));
            }
        }

        if (vel.length() > MAX_SWING_SPEED) {
            vel.normalize().multiply(MAX_SWING_SPEED);
        }

        player.setVelocity(vel);
        player.setFallDistance(0);
    }

    private void startEntityPull(GrappleSession session) {
        session.activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = session.player;

                if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                GrappleSession current = activeSessions.get(player.getUniqueId());
                if (current == null || current.state == GrappleState.RETURNING) {
                    this.cancel();
                    return;
                }

                if (!isHoldingGrapple(player)) {
                    startReturn(current);
                    this.cancel();
                    return;
                }

                LivingEntity target = session.hookedEntity;

                if (target == null || target.isDead()) {
                    startReturn(session);
                    this.cancel();
                    return;
                }

                double distance = player.getLocation().distance(target.getLocation());

                if (distance <= ENTITY_MIN_DISTANCE) {
                    startReturn(session);
                    this.cancel();
                    return;
                }

                session.hookDisplay.teleport(target.getLocation().add(0, target.getHeight() / 2, 0));

                Vector pullDirection = player.getLocation().toVector()
                        .subtract(target.getLocation().toVector())
                        .normalize();

                Vector targetVel = target.getVelocity();

                double awayVel = -targetVel.dot(pullDirection);
                if (awayVel > 0) {
                    targetVel.add(pullDirection.clone().multiply(awayVel));
                }

                targetVel.add(pullDirection.clone().multiply(ENTITY_PULL_STRENGTH));

                if (targetVel.length() > 2.0) {
                    targetVel.normalize().multiply(2.0);
                }

                target.setVelocity(targetVel);
                target.setFallDistance(0);

                updateChain(session);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void startReturn(GrappleSession session) {
        if (session.state == GrappleState.RETURNING) return;
        session.state = GrappleState.RETURNING;
        session.reeling = false;

        if (session.activeTask != null && !session.activeTask.isCancelled()) {
            session.activeTask.cancel();
        }

        session.activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = session.player;

                if (!player.isOnline()) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                if (!activeSessions.containsKey(player.getUniqueId())) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                if (!session.hookDisplay.isValid()) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                Location hookLoc = session.hookDisplay.getLocation();
                Location playerLoc = player.getLocation().add(0, 1.2, 0);

                Vector toPlayer = playerLoc.toVector().subtract(hookLoc.toVector());
                double distance = toPlayer.length();

                if (distance < DISMANTLE_DISTANCE) {
                    forceRemove(player);
                    this.cancel();
                    return;
                }

                Vector returnDir = toPlayer.normalize().multiply(Math.min(HOOK_RETURN_SPEED, distance));
                session.hookDisplay.teleport(hookLoc.add(returnDir));

                updateChain(session);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void updateChain(GrappleSession session) {
        Player player = session.player;
        if (!player.isOnline()) return;
        if (!session.hookDisplay.isValid()) return;

        Location start = player.getLocation().add(0, 1.2, 0);
        Location end = session.hookDisplay.getLocation();

        double totalLength = start.distance(end);
        if (totalLength < 0.1) {
            removeAllChainSegments(session);
            return;
        }

        int segmentCount = Math.max(1, (int) Math.ceil(totalLength / CHAIN_LINK_LENGTH));

        while (session.chainSegments.size() < segmentCount) {
            ItemDisplay segment = player.getWorld().spawn(start, ItemDisplay.class, display -> {
                display.setItemStack(createChainItem());
                display.setBillboard(Display.Billboard.FIXED);
                display.setGravity(false);
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(1);
            });
            session.chainSegments.add(segment);
        }

        while (session.chainSegments.size() > segmentCount) {
            ItemDisplay removed = session.chainSegments.remove(session.chainSegments.size() - 1);
            removed.remove();
        }

        List<Vector> points = solveFABRIK(start.toVector(), end.toVector(), segmentCount);

        for (int i = 0; i < session.chainSegments.size() && i < points.size() - 1; i++) {
            ItemDisplay segment = session.chainSegments.get(i);

            if (!segment.isValid()) continue;

            Vector pointA = points.get(i);
            Vector pointB = points.get(i + 1);

            Vector midpoint = pointA.clone().add(pointB).multiply(0.5);
            Location segmentLoc = midpoint.toLocation(player.getWorld());

            Vector segDir = pointB.clone().subtract(pointA);
            float segLength = (float) segDir.length();

            if (segLength < 0.001f) continue;

            Vector3f dir = new Vector3f(
                    (float) segDir.getX(),
                    (float) segDir.getY(),
                    (float) segDir.getZ()
            ).normalize();

            Quaternionf rotation = alignYAxis(dir);

            float scaleLength = segLength + (float) (CHAIN_LINK_LENGTH * 0.3);

            segment.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    rotation,
                    new Vector3f((float) CHAIN_LINK_THICKNESS, scaleLength, (float) CHAIN_LINK_THICKNESS),
                    new Quaternionf()
            ));

            segment.teleport(segmentLoc);
        }
    }

    private Quaternionf alignYAxis(Vector3f target) {
        Vector3f from = new Vector3f(0, 1, 0);
        Vector3f to = new Vector3f(target).normalize();

        float dot = from.dot(to);

        if (dot > 0.9999f) {
            return new Quaternionf();
        }

        if (dot < -0.9999f) {
            return new Quaternionf().rotateZ((float) Math.PI);
        }

        Vector3f axis = new Vector3f();
        from.cross(to, axis);
        axis.normalize();

        float angle = (float) Math.acos(Math.max(-1.0f, Math.min(1.0f, dot)));

        return new Quaternionf().rotateAxis(angle, axis);
    }

    private List<Vector> solveFABRIK(Vector start, Vector end, int segments) {
        double totalDist = start.distance(end);
        double segLen = totalDist / segments;

        List<Vector> points = new ArrayList<>();
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            Vector point = start.clone().add(end.clone().subtract(start).multiply(t));
            points.add(point);
        }

        double sag = Math.min(totalDist * 0.05, 0.8);
        for (int i = 1; i < points.size() - 1; i++) {
            double t = (double) i / (points.size() - 1);
            double catenary = sag * (4.0 * t * (1.0 - t));
            points.get(i).setY(points.get(i).getY() - catenary);
        }

        for (int iteration = 0; iteration < IK_ITERATIONS; iteration++) {
            points.set(points.size() - 1, end.clone());
            for (int i = points.size() - 2; i >= 0; i--) {
                Vector dir = points.get(i).clone().subtract(points.get(i + 1));
                if (dir.length() > 0.0001) {
                    points.set(i, points.get(i + 1).clone().add(dir.normalize().multiply(segLen)));
                }
            }

            points.set(0, start.clone());
            for (int i = 1; i <= segments; i++) {
                Vector dir = points.get(i).clone().subtract(points.get(i - 1));
                if (dir.length() > 0.0001) {
                    points.set(i, points.get(i - 1).clone().add(dir.normalize().multiply(segLen)));
                }
            }
        }

        return points;
    }

    private void removeAllChainSegments(GrappleSession session) {
        for (ItemDisplay chain : session.chainSegments) {
            if (chain.isValid()) chain.remove();
        }
        session.chainSegments.clear();
    }

    private boolean isHoldingGrapple(Player player) {
        return itemManager.isItem(player.getInventory().getItemInMainHand(), "grappling_hook");
    }

    private ItemStack createChainItem() {
        ItemStack chainItem = itemManager.create("chain_link");
        if (chainItem != null) return chainItem;
        return new ItemStack(Material.IRON_CHAIN);
    }

    private void forceRemove(Player player) {
        GrappleSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        if (session.activeTask != null && !session.activeTask.isCancelled()) {
            session.activeTask.cancel();
        }

        if (session.hookDisplay.isValid()) {
            session.hookDisplay.remove();
        }

        removeAllChainSegments(session);
    }

    public void debugInfo(Player player) {
        if (!activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(mm.deserialize("<red>No active grapple session."));
            return;
        }

        GrappleSession session = activeSessions.get(player.getUniqueId());
        Location hookPos = session.hookDisplay.getLocation();
        Location eyePos = player.getEyeLocation();

        Vector vecToHook = hookPos.toVector().subtract(eyePos.toVector());
        Vector unit = vecToHook.clone().normalize();
        double dist = vecToHook.length();

        Vector vel = player.getVelocity();
        double vRadial = vel.dot(unit);
        Vector vTangential = vel.clone().subtract(unit.clone().multiply(vRadial));

        player.sendMessage(mm.deserialize("<dark_gray>----------- <gold>Grapple Debug</gold> -----------"));
        player.sendMessage(mm.deserialize("<gray>State: <white>" + session.state));
        player.sendMessage(mm.deserialize("<gray>Rope Length: <white>" + String.format("%.2f", session.ropeLength)));
        player.sendMessage(mm.deserialize("<gray>Distance: <white>" + String.format("%.2f", dist)));
        player.sendMessage(mm.deserialize("<gray>Overshoot: <white>" + String.format("%.4f", Math.max(0, dist - session.ropeLength))));
        player.sendMessage(mm.deserialize("<gray>Reeling: <white>" + session.reeling));
        player.sendMessage(mm.deserialize("<gray>vRadial: <white>" + String.format("%.4f", vRadial)));
        player.sendMessage(mm.deserialize("<gray>vTangential: <white>" + String.format("%.4f", vTangential.length())));
        player.sendMessage(mm.deserialize("<gray>Speed: <white>" + String.format("%.4f", vel.length())));
        player.sendMessage(mm.deserialize("<gray>Chains: <white>" + session.chainSegments.size()));
        player.sendMessage(mm.deserialize("<dark_gray>-----------------------------------"));
    }

    public void cleanup() {
        for (UUID uuid : new HashSet<>(activeSessions.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                forceRemove(player);
            } else {
                GrappleSession session = activeSessions.remove(uuid);
                if (session != null) {
                    if (session.activeTask != null && !session.activeTask.isCancelled()) {
                        session.activeTask.cancel();
                    }
                    if (session.hookDisplay.isValid()) session.hookDisplay.remove();
                    session.chainSegments.forEach(s -> { if (s.isValid()) s.remove(); });
                }
            }
        }
    }

    private enum GrappleState {
        LAUNCHING,
        ATTACHED,
        PULLING_ENTITY,
        RETURNING
    }

    private static class GrappleSession {
        final Player player;
        final ItemDisplay hookDisplay;
        final Vector launchDirection;
        final float hookYaw;
        final float hookPitch;
        final List<ItemDisplay> chainSegments = new ArrayList<>();
        GrappleState state = GrappleState.LAUNCHING;
        Location anchorPoint;
        LivingEntity hookedEntity;
        double ropeLength = MAX_ROPE_LENGTH;
        boolean reeling = false;
        BukkitTask activeTask;

        GrappleSession(Player player, ItemDisplay hookDisplay, Vector launchDirection, float hookYaw, float hookPitch) {
            this.player = player;
            this.hookDisplay = hookDisplay;
            this.launchDirection = launchDirection;
            this.hookYaw = hookYaw;
            this.hookPitch = hookPitch;
        }
    }
}