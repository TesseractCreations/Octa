package com.csl.octa.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

@SuppressWarnings({"unused", "unchecked"})
public class AnimationUtil {
    private static Plugin plugin;

    public AnimationUtil(Plugin plugin) {
        AnimationUtil.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Lerp helpers
    // ─────────────────────────────────────────────────────────────────────────────

    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static int lerp(int a, int b, double t) { return (int) Math.round(a + (b - a) * t); }
    public static long lerp(long a, long b, double t) { return Math.round(a + (b - a) * t); }

    public static double lerp(double a, double b, double t, Easing e) { return a + (b - a) * (e != null ? e.ease(t) : t); }
    public static float lerp(float a, float b, float t, Easing e) { return a + (b - a) * (float)(e != null ? e.ease(t) : t); }
    public static int lerp(int a, int b, double t, Easing e) { return (int) Math.round(a + (b - a) * (e != null ? e.ease(t) : t)); }
    public static long lerp(long a, long b, double t, Easing e) { return Math.round(a + (b - a) * (e != null ? e.ease(t) : t)); }

    public static <T extends Number> T @NotNull [] lerpFrames(T a, T b, double dur, Easing e) {
        int n = Math.max(2, (int)(dur * 20));
        Number[] r = new Number[n + 1];
        if (e == null) e = Easing.LINEAR;
        for (int i = 0; i <= n; i++) {
            double doubleA = a.doubleValue();
            r[i] = doubleA + (b.doubleValue() - doubleA) * e.ease((double) i / n);
        }
        return (T[]) r;
    }

    public static double[][] lerp(double[] a, double[] b, double dur, Easing e) {
        double dist = 0;
        for (int j = 0; j < a.length; j++) {
            dist += (b[j] - a[j]) * (b[j] - a[j]);
        }
        dist = Math.sqrt(dist);

        int n = Math.max(2, (int)(dur * 20));
        n = Math.max(n, (int)(dist / 10));

        double[][] r = new double[n + 1][a.length];
        if (e == null) e = Easing.LINEAR;

        for (int i = 0; i <= n; i++) {
            double t = e.ease((double) i / n);
            for (int j = 0; j < a.length; j++) {
                r[i][j] = a[j] + (b[j] - a[j]) * t;
            }
        }
        return r;
    }

    public static Location[] lerp(Location a, Location b, double dur, Easing e) {
        double[] start = {a.getX(), a.getY(), a.getZ(), a.getYaw(), a.getPitch()};
        double[] end   = {b.getX(), b.getY(), b.getZ(), b.getYaw(), b.getPitch()};

        double[][] frames = lerp(start, end, dur, e);

        Location[] locations = new Location[frames.length];
        for (int i = 0; i < frames.length; i++) {
            locations[i] = new Location(
                    a.getWorld(),
                    frames[i][0],
                    frames[i][1],
                    frames[i][2],
                    (float) frames[i][3],
                    (float) frames[i][4]
            );
        }
        return locations;
    }

    public static List<Byte> lerpByte(byte a, byte b, double dur, Easing e) {
        int n = Math.max(2, (int)(dur * 20));
        List<Byte> r = new ArrayList<>();
        if (e == null) e = Easing.LINEAR;
        int ai = a & 0xFF, bi = b & 0xFF;
        for (int i = 0; i <= n; i++) r.add((byte)(int)(ai + (bi - ai) * e.ease((double)i / n)));
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helper to resolve viewers safely
    // ─────────────────────────────────────────────────────────────────────────────

    private static Collection<? extends Player> resolveViewers(@Nullable Collection<Player> viewers) {
        return viewers != null ? viewers : Bukkit.getOnlinePlayers();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Standard (non-packet) animate helpers
    // ─────────────────────────────────────────────────────────────────────────────

    public static <T extends Number> BukkitTask animate(Number a, Number b, double dur, Easing e, Consumer<T> fn) {
        return animate(a, b, dur, e, fn, null);
    }

    public static <T extends Number> BukkitTask animate(Number a, Number b, double dur, Easing e, Consumer<T> fn, Runnable done) {
        T[] frames = (T[]) lerpFrames(a, b, dur, e);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i >= frames.length) { t[0].cancel(); if (done != null) done.run(); return; }
                fn.accept(frames[i++]);
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask animate(Location a, Location b, double dur, Easing e, Consumer<Location> fn) {
        return animate(a, b, dur, e, fn, null);
    }

    public static BukkitTask animate(Location a, Location b, double dur, Easing e, Consumer<Location> fn, Runnable done) {
        Location[] frames = lerp(a, b, dur, e);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i >= frames.length) { t[0].cancel(); if (done != null) done.run(); return; }
                fn.accept(frames[i++]);
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask animateEntity(Entity ent, Location a, Location b, double dur, Easing e) {
        return animateEntity(ent, a, b, dur, e, null);
    }

    public static BukkitTask animateEntity(Entity ent, Location a, Location b, double dur,
                                           Easing e, Runnable done) {
        if (e == null) e = Easing.LINEAR;
        return animateEntity(ent, a, b, dur, e::ease, done);
    }

    public static BukkitTask animateEntity(Entity ent, Location a, Location b, double dur,
                                           DoubleUnaryOperator easing, Runnable done) {
        if (easing == null) easing = t -> t;

        final DoubleUnaryOperator e = easing;
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);
        final boolean isPlayer = ent instanceof Player;

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isPlayer) {
                if (!((Player) ent).isOnline()) {
                    t[0].cancel();
                    if (done != null) done.run();
                    return;
                }
            } else if (!ent.isValid()) {
                t[0].cancel();
                if (done != null) done.run();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.applyAsDouble(progress);

            if (progress >= 1.0) {
                if (isPlayer) {
                    ent.teleportAsync(b).thenRun(() -> {
                        t[0].cancel();
                        if (done != null) done.run();
                    });
                } else {
                    ent.teleport(b);
                    t[0].cancel();
                    if (done != null) done.run();
                }
                return;
            }

            double x = a.getX() + (b.getX() - a.getX()) * eased;
            double y = a.getY() + (b.getY() - a.getY()) * eased;
            double z = a.getZ() + (b.getZ() - a.getZ()) * eased;
            float yaw = (float) (a.getYaw() + (b.getYaw() - a.getYaw()) * eased);
            float pitch = (float) (a.getPitch() + (b.getPitch() - a.getPitch()) * eased);

            Location target = new Location(a.getWorld(), x, y, z, yaw, pitch);

            if (isPlayer) {
                nmsTeleport((Player) ent, target);
            } else {
                ent.teleport(target);
            }
        }, 0L, 1L);
        return t[0];
    }

    private static void nmsTeleport(Player player, Location loc) {
        ((CraftPlayer) player).getHandle().connection.teleport(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TextDisplay opacity animation
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask animateTDOpacity(TextDisplay td, byte a, byte b, double dur, Easing e) {
        return animateTDOpacity(td, a, b, dur, e, null);
    }

    public static BukkitTask animateTDOpacity(TextDisplay td, byte a, byte b, double dur, Easing e, Runnable done) {
        List<Byte> frames = lerpByte(a, b, dur, e);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i >= frames.size() || !td.isValid()) { t[0].cancel(); if (done != null) done.run(); return; }
                td.setTextOpacity(frames.get(i++));
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask animateTDOpacity(TextDisplay td, int a, int b, double dur, Easing e) {
        return animateTDOpacity(td, (byte) a, (byte) b, dur, e, null);
    }

    public static BukkitTask animateTDOpacity(TextDisplay td, int a, int b, double dur, Easing e, Runnable done) {
        return animateTDOpacity(td, (byte) a, (byte) b, dur, e, done);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Text typing animations
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask animateText(TextDisplay td, String txt, int tpc) {
        return animateText(td, txt, tpc, null);
    }

    public static BukkitTask animateText(TextDisplay td, String txt, int tpc, Runnable done) {
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > txt.length() || !td.isValid()) { t[0].cancel(); if (done != null) done.run(); return; }
                td.text(Component.text(txt.substring(0, i++)));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateText(TextDisplay td, Component comp, int tpc) {
        return animateText(td, comp, tpc, null);
    }

    public static BukkitTask animateText(TextDisplay td, Component comp, int tpc, Runnable done) {
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > plain.length() || !td.isValid()) { t[0].cancel(); if (done != null) done.run(); return; }
                td.text(truncate(comp, i++));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateText(TextDisplay td, String txt, int tpc, String cursor, boolean rmCursor) {
        return animateText(td, txt, tpc, cursor, rmCursor, null);
    }

    public static BukkitTask animateText(TextDisplay td, String txt, int tpc, String cursor, boolean rmCursor, Runnable done) {
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > txt.length() || !td.isValid()) {
                    t[0].cancel();
                    if (rmCursor) td.text(Component.text(txt));
                    if (done != null) done.run();
                    return;
                }
                td.text(Component.text(txt.substring(0, i++) + cursor));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateText(TextDisplay td, Component comp, int tpc, Component cursor, boolean rmCursor) {
        return animateText(td, comp, tpc, cursor, rmCursor, null);
    }

    public static BukkitTask animateText(TextDisplay td, Component comp, int tpc, Component cursor, boolean rmCursor, Runnable done) {
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > plain.length() || !td.isValid()) {
                    t[0].cancel();
                    if (rmCursor) td.text(comp);
                    if (done != null) done.run();
                    return;
                }
                td.text(truncate(comp, i++).append(cursor));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateText(Player plr, String txt, int tpc) {
        return animateText(plr, txt, tpc, null);
    }

    public static BukkitTask animateText(Player plr, String txt, int tpc, Runnable done) {
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > txt.length() || !plr.isOnline()) { t[0].cancel(); if (done != null) done.run(); return; }
                plr.sendMessage(Component.text(txt.substring(0, i++)));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateText(Player plr, Component comp, int tpc) {
        return animateText(plr, comp, tpc, null);
    }

    public static BukkitTask animateText(Player plr, Component comp, int tpc, Runnable done) {
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > plain.length() || !plr.isOnline()) { t[0].cancel(); if (done != null) done.run(); return; }
                plr.sendMessage(truncate(comp, i++));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateActionBar(Player plr, String txt, int tpc) {
        return animateActionBar(plr, txt, tpc, null);
    }

    public static BukkitTask animateActionBar(Player plr, String txt, int tpc, Runnable done) {
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > txt.length() || !plr.isOnline()) { t[0].cancel(); if (done != null) done.run(); return; }
                plr.sendActionBar(Component.text(txt.substring(0, i++)));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateActionBar(Player plr, Component comp, int tpc) {
        return animateActionBar(plr, comp, tpc, null);
    }

    public static BukkitTask animateActionBar(Player plr, Component comp, int tpc, Runnable done) {
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > plain.length() || !plr.isOnline()) { t[0].cancel(); if (done != null) done.run(); return; }
                plr.sendActionBar(truncate(comp, i++));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateText(String txt, int tpc, Consumer<String> fn) {
        return animateText(txt, tpc, fn, null);
    }

    public static BukkitTask animateText(String txt, int tpc, Consumer<String> fn, Runnable done) {
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > txt.length()) { t[0].cancel(); if (done != null) done.run(); return; }
                fn.accept(txt.substring(0, i++));
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask animateTextComponent(Component comp, int tpc, Consumer<Component> fn) {
        return animateTextComponent(comp, tpc, fn, null);
    }

    public static BukkitTask animateTextComponent(Component comp, int tpc, Consumer<Component> fn, Runnable done) {
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > plain.length()) { t[0].cancel(); if (done != null) done.run(); return; }
                fn.accept(truncate(comp, i++));
            }
        }, 0L, tpc);
        return t[0];
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Component truncation
    // ─────────────────────────────────────────────────────────────────────────────

    public static Component truncate(Component comp, int len) {
        if (len <= 0) return Component.empty();
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        if (len >= plain.length()) return comp;
        return truncateRec(comp, new int[]{len});
    }

    private static Component truncateRec(Component comp, int[] rem) {
        if (rem[0] <= 0) return Component.empty();

        String content = "";
        if (comp instanceof net.kyori.adventure.text.TextComponent tc) content = tc.content();

        Component res;
        if (content.length() <= rem[0]) {
            rem[0] -= content.length();
            res = comp.children(new ArrayList<>());
        } else {
            String cut = content.substring(0, rem[0]);
            rem[0] = 0;
            net.kyori.adventure.text.TextComponent tc = (net.kyori.adventure.text.TextComponent) comp;
            res = tc.content(cut).children(new ArrayList<>());
            return res;
        }

        List<Component> kids = new ArrayList<>();
        for (Component child : comp.children()) {
            if (rem[0] <= 0) break;
            kids.add(truncateRec(child, rem));
        }
        return res.children(kids);
    }

    public static void cancel(BukkitTask task) {
        if (task != null && !task.isCancelled()) task.cancel();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // PACKET-BASED ANIMATION METHODS (PacketEvents)
    // ═════════════════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate entity position (client-side only)
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimateEntity(Entity entity, Location from, Location to, double dur,
                                                 @Nullable Easing easing, @Nullable Collection<Player> viewers,
                                                 @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;
        return packetAnimateEntity(entity, from, to, dur, easing::ease, viewers, done);
    }

    public static BukkitTask packetAnimateEntity(Entity entity, Location from, Location to, double dur,
                                                 @Nullable Easing easing, @Nullable Collection<Player> viewers) {
        return packetAnimateEntity(entity, from, to, dur, easing, viewers, null);
    }

    public static BukkitTask packetAnimateEntity(Entity entity, Location from, Location to, double dur,
                                                 @Nullable Easing easing) {
        return packetAnimateEntity(entity, from, to, dur, easing, null, null);
    }

    public static BukkitTask packetAnimateEntity(Entity entity, Location from, Location to, double dur,
                                                 @Nullable DoubleUnaryOperator easing,
                                                 @Nullable Collection<Player> viewers,
                                                 @Nullable Runnable done) {
        if (easing == null) easing = t -> t;

        final DoubleUnaryOperator e = easing;
        final int entityId = entity.getEntityId();
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);

        final double[] lastPos = {from.getX(), from.getY(), from.getZ()};

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.applyAsDouble(progress);

            double x = from.getX() + (to.getX() - from.getX()) * eased;
            double y = from.getY() + (to.getY() - from.getY()) * eased;
            double z = from.getZ() + (to.getZ() - from.getZ()) * eased;
            float yaw = (float) (from.getYaw() + (to.getYaw() - from.getYaw()) * eased);
            float pitch = (float) (from.getPitch() + (to.getPitch() - from.getPitch()) * eased);

            double dx = x - lastPos[0];
            double dy = y - lastPos[1];
            double dz = z - lastPos[2];

            boolean useRelMove = Math.abs(dx) < 8 && Math.abs(dy) < 8 && Math.abs(dz) < 8;

            Collection<? extends Player> targets = resolveViewers(viewers);

            if (useRelMove) {
                WrapperPlayServerEntityRelativeMoveAndRotation packet =
                        new WrapperPlayServerEntityRelativeMoveAndRotation(
                                entityId, dx, dy, dz, yaw, pitch, entity.isOnGround()
                        );
                for (Player viewer : targets) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
                    }
                }
            } else {
                sendPacketTeleport(entityId, x, y, z, yaw, pitch, entity.isOnGround(), targets);
            }

            WrapperPlayServerEntityHeadLook headPacket = new WrapperPlayServerEntityHeadLook(entityId, yaw);
            for (Player viewer : targets) {
                if (viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, headPacket);
                }
            }

            lastPos[0] = x;
            lastPos[1] = y;
            lastPos[2] = z;

            if (progress >= 1.0) {
                t[0].cancel();
                if (done != null) done.run();
            }
        }, 0L, 1L);
        return t[0];
    }

    private static void sendPacketTeleport(int entityId, double x, double y, double z,
                                           float yaw, float pitch, boolean onGround,
                                           Collection<? extends Player> viewers) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new com.github.retrooper.packetevents.protocol.world.Location(x, y, z, yaw, pitch),
                onGround
        );
        for (Player viewer : viewers) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate entity head rotation
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimateHeadRotation(Entity entity, float fromYaw, float toYaw, double dur,
                                                       @Nullable Easing easing,
                                                       @Nullable Collection<Player> viewers,
                                                       @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;
        final Easing e = easing;
        final int entityId = entity.getEntityId();
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.ease(progress);

            float yaw = (float) (fromYaw + (toYaw - fromYaw) * eased);

            WrapperPlayServerEntityHeadLook packet = new WrapperPlayServerEntityHeadLook(entityId, yaw);
            WrapperPlayServerEntityRotation rotPacket = new WrapperPlayServerEntityRotation(
                    entityId, yaw, entity.getLocation().getPitch(), entity.isOnGround()
            );

            Collection<? extends Player> targets = resolveViewers(viewers);
            for (Player viewer : targets) {
                if (viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, rotPacket);
                }
            }

            if (progress >= 1.0) {
                t[0].cancel();
                if (done != null) done.run();
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask packetAnimateHeadRotation(Entity entity, float fromYaw, float toYaw, double dur,
                                                       @Nullable Easing easing,
                                                       @Nullable Collection<Player> viewers) {
        return packetAnimateHeadRotation(entity, fromYaw, toYaw, dur, easing, viewers, null);
    }

    public static BukkitTask packetAnimateHeadRotation(Entity entity, float fromYaw, float toYaw, double dur,
                                                       @Nullable Easing easing) {
        return packetAnimateHeadRotation(entity, fromYaw, toYaw, dur, easing, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate float metadata
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimateMetadataFloat(Entity entity, int metadataIndex,
                                                        float from, float to, double dur,
                                                        @Nullable Easing easing,
                                                        @Nullable Collection<Player> viewers,
                                                        @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;
        final Easing e = easing;
        final int entityId = entity.getEntityId();
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid()) {
                t[0].cancel();
                if (done != null) done.run();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.ease(progress);

            float value = (float) (from + (to - from) * eased);

            List<EntityData<?>> metadata = new ArrayList<>();
            metadata.add(new EntityData<>(metadataIndex, EntityDataTypes.FLOAT, value));

            WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);

            Collection<? extends Player> targets = resolveViewers(viewers);
            for (Player viewer : targets) {
                if (viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
                }
            }

            if (progress >= 1.0) {
                t[0].cancel();
                if (done != null) done.run();
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask packetAnimateMetadataFloat(Entity entity, int metadataIndex,
                                                        float from, float to, double dur,
                                                        @Nullable Easing easing,
                                                        @Nullable Collection<Player> viewers) {
        return packetAnimateMetadataFloat(entity, metadataIndex, from, to, dur, easing, viewers, null);
    }

    public static BukkitTask packetAnimateMetadataFloat(Entity entity, int metadataIndex,
                                                        float from, float to, double dur,
                                                        @Nullable Easing easing) {
        return packetAnimateMetadataFloat(entity, metadataIndex, from, to, dur, easing, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate byte metadata
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimateMetadataByte(Entity entity, int metadataIndex,
                                                       byte from, byte to, double dur,
                                                       @Nullable Easing easing,
                                                       @Nullable Collection<Player> viewers,
                                                       @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;
        final Easing e = easing;
        final int entityId = entity.getEntityId();
        List<Byte> frames = lerpByte(from, to, dur, e);

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i >= frames.size() || !entity.isValid()) {
                    t[0].cancel();
                    if (done != null) done.run();
                    return;
                }

                List<EntityData<?>> metadata = new ArrayList<>();
                metadata.add(new EntityData<>(metadataIndex, EntityDataTypes.BYTE, frames.get(i++)));

                WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);

                Collection<? extends Player> targets = resolveViewers(viewers);
                for (Player viewer : targets) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
                    }
                }
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask packetAnimateMetadataByte(Entity entity, int metadataIndex,
                                                       byte from, byte to, double dur,
                                                       @Nullable Easing easing,
                                                       @Nullable Collection<Player> viewers) {
        return packetAnimateMetadataByte(entity, metadataIndex, from, to, dur, easing, viewers, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate entity velocity
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimateVelocity(Entity entity, double[] fromVel, double[] toVel,
                                                   double dur, @Nullable Easing easing,
                                                   @Nullable Collection<Player> viewers,
                                                   @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;
        final Easing e = easing;
        final int entityId = entity.getEntityId();
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid()) {
                t[0].cancel();
                if (done != null) done.run();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.ease(progress);

            double vx = fromVel[0] + (toVel[0] - fromVel[0]) * eased;
            double vy = fromVel[1] + (toVel[1] - fromVel[1]) * eased;
            double vz = fromVel[2] + (toVel[2] - fromVel[2]) * eased;

            WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(
                    entityId, new com.github.retrooper.packetevents.util.Vector3d(vx, vy, vz)
            );

            Collection<? extends Player> targets = resolveViewers(viewers);
            for (Player viewer : targets) {
                if (viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
                }
            }

            if (progress >= 1.0) {
                t[0].cancel();
                if (done != null) done.run();
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask packetAnimateVelocity(Entity entity, double[] fromVel, double[] toVel,
                                                   double dur, @Nullable Easing easing,
                                                   @Nullable Collection<Player> viewers) {
        return packetAnimateVelocity(entity, fromVel, toVel, dur, easing, viewers, null);
    }

    public static BukkitTask packetAnimateVelocity(Entity entity, double[] fromVel, double[] toVel,
                                                   double dur, @Nullable Easing easing) {
        return packetAnimateVelocity(entity, fromVel, toVel, dur, easing, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Send entity animation (swing arm, hurt, etc.)
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetEntityAnimation(Entity entity, int animation, @Nullable Collection<Player> viewers) {
        int entityId = entity.getEntityId();
        WrapperPlayServerEntityAnimation.EntityAnimationType[] types =
                WrapperPlayServerEntityAnimation.EntityAnimationType.values();
        if (animation < 0 || animation >= types.length) return;

        WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(entityId, types[animation]);

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    public static void packetEntityAnimation(Entity entity, int animation) {
        packetEntityAnimation(entity, animation, null);
    }

    public static BukkitTask packetAnimateEntityRepeating(Entity entity, int animation, double dur,
                                                          long intervalTicks,
                                                          @Nullable Collection<Player> viewers,
                                                          @Nullable Runnable done) {
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid()) {
                t[0].cancel();
                if (done != null) done.run();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= duration) {
                t[0].cancel();
                if (done != null) done.run();
                return;
            }

            packetEntityAnimation(entity, animation, viewers);
        }, 0L, intervalTicks);
        return t[0];
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Entity status / event
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetEntityStatus(Entity entity, int status, @Nullable Collection<Player> viewers) {
        int entityId = entity.getEntityId();
        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(entityId, status);

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    public static void packetEntityStatus(Entity entity, int status) {
        packetEntityStatus(entity, status, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Spawn fake entity, animate it, then remove
    // ─────────────────────────────────────────────────────────────────────────────

    public static int packetSpawnAndAnimate(com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType,
                                            Location from, Location to, double dur,
                                            @Nullable Easing easing,
                                            @Nullable List<EntityData<?>> metadata,
                                            @NotNull Collection<Player> viewers,
                                            boolean autoRemove,
                                            @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;

        int fakeEntityId = -(int)(Math.random() * Integer.MAX_VALUE);
        UUID fakeUUID = UUID.randomUUID();

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                fakeEntityId, fakeUUID, entityType,
                new com.github.retrooper.packetevents.protocol.world.Location(
                        from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch()
                ),
                from.getYaw(), 0, null
        );

        for (Player viewer : viewers) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawnPacket);
            }
        }

        if (metadata != null && !metadata.isEmpty()) {
            WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(fakeEntityId, metadata);
            for (Player viewer : viewers) {
                if (viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metaPacket);
                }
            }
        }

        final Easing e = easing;
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);
        final double[] lastPos = {from.getX(), from.getY(), from.getZ()};

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.ease(progress);

            double x = from.getX() + (to.getX() - from.getX()) * eased;
            double y = from.getY() + (to.getY() - from.getY()) * eased;
            double z = from.getZ() + (to.getZ() - from.getZ()) * eased;
            float yaw = (float) (from.getYaw() + (to.getYaw() - from.getYaw()) * eased);
            float pitch = (float) (from.getPitch() + (to.getPitch() - from.getPitch()) * eased);

            double dx = x - lastPos[0];
            double dy = y - lastPos[1];
            double dz = z - lastPos[2];

            boolean useRelMove = Math.abs(dx) < 8 && Math.abs(dy) < 8 && Math.abs(dz) < 8;

            if (useRelMove) {
                WrapperPlayServerEntityRelativeMoveAndRotation movePacket =
                        new WrapperPlayServerEntityRelativeMoveAndRotation(
                                fakeEntityId, dx, dy, dz, yaw, pitch, false
                        );
                for (Player viewer : viewers) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, movePacket);
                    }
                }
            } else {
                WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                        fakeEntityId,
                        new com.github.retrooper.packetevents.protocol.world.Location(x, y, z, yaw, pitch),
                        false
                );
                for (Player viewer : viewers) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleportPacket);
                    }
                }
            }

            lastPos[0] = x;
            lastPos[1] = y;
            lastPos[2] = z;

            if (progress >= 1.0) {
                t[0].cancel();
                if (autoRemove) {
                    packetDestroyEntity(fakeEntityId, viewers);
                }
                if (done != null) done.run();
            }
        }, 0L, 1L);

        return fakeEntityId;
    }

    public static int packetSpawnAndAnimate(com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType,
                                            Location from, Location to, double dur,
                                            @Nullable Easing easing,
                                            @NotNull Collection<Player> viewers) {
        return packetSpawnAndAnimate(entityType, from, to, dur, easing, null, viewers, true, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Destroy entity
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetDestroyEntity(int entityId, @Nullable Collection<? extends Player> viewers) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);

        Collection<? extends Player> targets = viewers != null ? viewers : Bukkit.getOnlinePlayers();
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    public static void packetDestroyEntity(int entityId) {
        packetDestroyEntity(entityId, null);
    }

    public static void packetDestroyEntity(Entity entity, @Nullable Collection<Player> viewers) {
        packetDestroyEntity(entity.getEntityId(), viewers);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Send entity metadata directly
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetSendMetadata(int entityId, List<EntityData<?>> metadata,
                                          @Nullable Collection<Player> viewers) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    public static void packetSendMetadata(Entity entity, List<EntityData<?>> metadata,
                                          @Nullable Collection<Player> viewers) {
        packetSendMetadata(entity.getEntityId(), metadata, viewers);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate entity custom name (typewriter)
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimateCustomName(Entity entity, Component text, int tpc,
                                                     @Nullable Collection<Player> viewers,
                                                     @Nullable Runnable done) {
        return packetAnimateCustomName(entity.getEntityId(), text, tpc, viewers, done);
    }

    public static BukkitTask packetAnimateCustomName(int entityId, Component text, int tpc,
                                                     @Nullable Collection<Player> viewers,
                                                     @Nullable Runnable done) {
        String plain = PlainTextComponentSerializer.plainText().serialize(text);
        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int i = 0;
            public void run() {
                if (i > plain.length()) {
                    t[0].cancel();
                    if (done != null) done.run();
                    return;
                }

                Component truncated = truncate(text, i++);

                List<EntityData<?>> metadata = new ArrayList<>();
                metadata.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                        Optional.of(truncated)));
                metadata.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));

                WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);

                Collection<? extends Player> targets = resolveViewers(viewers);
                for (Player viewer : targets) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
                    }
                }
            }
        }, 0L, tpc);
        return t[0];
    }

    public static BukkitTask packetAnimateCustomName(Entity entity, Component text, int tpc,
                                                     @Nullable Collection<Player> viewers) {
        return packetAnimateCustomName(entity, text, tpc, viewers, null);
    }

    public static BukkitTask packetAnimateCustomName(Entity entity, Component text, int tpc) {
        return packetAnimateCustomName(entity, text, tpc, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Animate entity along a multi-point path
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetAnimatePath(int entityId, List<Location> waypoints,
                                         List<Double> segmentDur, @Nullable Easing easing,
                                         @Nullable Collection<Player> viewers,
                                         @Nullable Runnable done) {
        if (waypoints.size() < 2) {
            if (done != null) done.run();
            return;
        }
        animatePathSegment(entityId, waypoints, segmentDur, easing, viewers, done, 0);
    }

    public static void packetAnimatePath(Entity entity, List<Location> waypoints,
                                         List<Double> segmentDur, @Nullable Easing easing,
                                         @Nullable Collection<Player> viewers,
                                         @Nullable Runnable done) {
        packetAnimatePath(entity.getEntityId(), waypoints, segmentDur, easing, viewers, done);
    }

    public static void packetAnimatePath(Entity entity, List<Location> waypoints,
                                         double segmentDur, @Nullable Easing easing,
                                         @Nullable Collection<Player> viewers,
                                         @Nullable Runnable done) {
        List<Double> durations = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) durations.add(segmentDur);
        packetAnimatePath(entity.getEntityId(), waypoints, durations, easing, viewers, done);
    }

    private static void animatePathSegment(int entityId, List<Location> waypoints,
                                           List<Double> segmentDur, @Nullable Easing easing,
                                           @Nullable Collection<Player> viewers,
                                           @Nullable Runnable done, int segmentIndex) {
        if (segmentIndex >= waypoints.size() - 1) {
            if (done != null) done.run();
            return;
        }

        Location from = waypoints.get(segmentIndex);
        Location to = waypoints.get(segmentIndex + 1);
        double dur = segmentIndex < segmentDur.size() ? segmentDur.get(segmentIndex) : 1.0;

        Easing e = easing != null ? easing : Easing.LINEAR;
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);
        final double[] lastPos = {from.getX(), from.getY(), from.getZ()};

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.ease(progress);

            double x = from.getX() + (to.getX() - from.getX()) * eased;
            double y = from.getY() + (to.getY() - from.getY()) * eased;
            double z = from.getZ() + (to.getZ() - from.getZ()) * eased;
            float yaw = (float) (from.getYaw() + (to.getYaw() - from.getYaw()) * eased);
            float pitch = (float) (from.getPitch() + (to.getPitch() - from.getPitch()) * eased);

            double dx = x - lastPos[0];
            double dy = y - lastPos[1];
            double dz = z - lastPos[2];

            boolean useRelMove = Math.abs(dx) < 8 && Math.abs(dy) < 8 && Math.abs(dz) < 8;

            Collection<? extends Player> targets = resolveViewers(viewers);

            if (useRelMove) {
                WrapperPlayServerEntityRelativeMoveAndRotation movePacket =
                        new WrapperPlayServerEntityRelativeMoveAndRotation(
                                entityId, dx, dy, dz, yaw, pitch, false
                        );
                for (Player viewer : targets) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, movePacket);
                    }
                }
            } else {
                WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                        entityId,
                        new com.github.retrooper.packetevents.protocol.world.Location(x, y, z, yaw, pitch),
                        false
                );
                for (Player viewer : targets) {
                    if (viewer.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleportPacket);
                    }
                }
            }

            lastPos[0] = x;
            lastPos[1] = y;
            lastPos[2] = z;

            if (progress >= 1.0) {
                t[0].cancel();
                animatePathSegment(entityId, waypoints, segmentDur, e, viewers, done, segmentIndex + 1);
            }
        }, 0L, 1L);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Teleport entity (single packet, instant)
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetTeleportEntity(Entity entity, Location loc, @Nullable Collection<Player> viewers) {
        packetTeleportEntity(entity.getEntityId(), loc, viewers);
    }

    public static void packetTeleportEntity(int entityId, Location loc, @Nullable Collection<Player> viewers) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new com.github.retrooper.packetevents.protocol.world.Location(
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
                ),
                false
        );

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Set entity velocity (single packet)
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetSetVelocity(Entity entity, double vx, double vy, double vz,
                                         @Nullable Collection<Player> viewers) {
        packetSetVelocity(entity.getEntityId(), vx, vy, vz, viewers);
    }

    public static void packetSetVelocity(int entityId, double vx, double vy, double vz,
                                         @Nullable Collection<Player> viewers) {
        WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(
                entityId, new com.github.retrooper.packetevents.util.Vector3d(vx, vy, vz)
        );

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Generic animation driver
    // ─────────────────────────────────────────────────────────────────────────────

    public static BukkitTask packetAnimate(int entityId, double dur, @Nullable Easing easing,
                                           java.util.function.BiConsumer<Integer, Double> tickFn,
                                           @Nullable Runnable done) {
        if (easing == null) easing = Easing.LINEAR;
        final Easing e = easing;
        final long startTime = System.currentTimeMillis();
        final long duration = (long) (dur * 1000);

        final BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double progress = Math.min(1.0, (double) elapsed / duration);
            double eased = e.ease(progress);

            tickFn.accept(entityId, eased);

            if (progress >= 1.0) {
                t[0].cancel();
                if (done != null) done.run();
            }
        }, 0L, 1L);
        return t[0];
    }

    public static BukkitTask packetAnimate(Entity entity, double dur, @Nullable Easing easing,
                                           java.util.function.BiConsumer<Integer, Double> tickFn,
                                           @Nullable Runnable done) {
        return packetAnimate(entity.getEntityId(), dur, easing, tickFn, done);
    }

    public static BukkitTask packetAnimate(Entity entity, double dur, @Nullable Easing easing,
                                           java.util.function.BiConsumer<Integer, Double> tickFn) {
        return packetAnimate(entity.getEntityId(), dur, easing, tickFn, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Set entity equipment
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetSetEquipment(Entity entity, List<Equipment> equipment,
                                          @Nullable Collection<Player> viewers) {
        packetSetEquipment(entity.getEntityId(), equipment, viewers);
    }

    public static void packetSetEquipment(int entityId, List<Equipment> equipment,
                                          @Nullable Collection<Player> viewers) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(entityId, equipment);

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Packet: Set passengers
    // ─────────────────────────────────────────────────────────────────────────────

    public static void packetSetPassengers(int vehicleEntityId, int[] passengerIds,
                                           @Nullable Collection<Player> viewers) {
        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(vehicleEntityId, passengerIds);

        Collection<? extends Player> targets = resolveViewers(viewers);
        for (Player viewer : targets) {
            if (viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }

    public static void packetSetPassengers(Entity vehicle, int[] passengerIds,
                                           @Nullable Collection<Player> viewers) {
        packetSetPassengers(vehicle.getEntityId(), passengerIds, viewers);
    }
}