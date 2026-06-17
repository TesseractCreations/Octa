package com.csl.octa.listeners;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoidiumListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Random random = new Random();

    private final Map<UUID, Long> dashCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> doubleJumpCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dodgeCooldowns = new ConcurrentHashMap<>();

    private final Set<UUID> noFall = ConcurrentHashMap.newKeySet();

    private final Set<UUID> usedDoubleJump = ConcurrentHashMap.newKeySet();
    private final Set<UUID> wasOnGround = ConcurrentHashMap.newKeySet();

    private static final long DASH_COOLDOWN_MS = 15_000;
    private static final long DOUBLE_JUMP_COOLDOWN_MS = 5_000;
    private static final long DODGE_COOLDOWN_MS = 25_000;
    private static final double DASH_HORIZONTAL = 2.5;
    private static final double DASH_VERTICAL_DIVISOR = 2.0;
    private static final double DOUBLE_JUMP_POWER = 0.75;
    private static final double DODGE_CHANCE = 0.45;

    private static final String BOOTS_OFF = "\uD002";
    private static final String BOOTS_ON = "\uD003";
    private static final String LEGS_OFF = "\uD004";
    private static final String LEGS_ON = "\uD005";
    private static final String CHEST_OFF = "\uD006";
    private static final String CHEST_ON = "\uD007";
    private static final String HELMET_OFF = "\uD008";
    private static final String HELMET_ON = "\uD009";

    public VoidiumListener(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        startActionBarTask();
        startGroundCheckTask();
    }

    // -------------------------------------------------------------------------
    // Action bar
    // -------------------------------------------------------------------------

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean hasBoots  = itemManager.isItem(player.getInventory().getBoots(), "wither_boots");
                    boolean hasLegs   = itemManager.isItem(player.getInventory().getLeggings(), "wither_leggings");
                    boolean hasChest  = itemManager.isItem(player.getInventory().getChestplate(), "wither_chestplate");
                    boolean hasHelmet = itemManager.isItem(player.getInventory().getHelmet(), "wither_mask");

                    if (!hasBoots && !hasLegs && !hasChest && !hasHelmet) continue;

                    long now = System.currentTimeMillis();
                    StringBuilder bar = new StringBuilder();

                    if (hasBoots) {
                        Long lastJump = doubleJumpCooldowns.get(player.getUniqueId());
                        boolean ready = lastJump == null || (now - lastJump) >= DOUBLE_JUMP_COOLDOWN_MS;
                        bar.append(ready ? BOOTS_ON : BOOTS_OFF);
                    }

                    if (hasLegs) {
                        Long lastDash = dashCooldowns.get(player.getUniqueId());
                        boolean ready = lastDash == null || (now - lastDash) >= DASH_COOLDOWN_MS;
                        bar.append(ready ? LEGS_ON : LEGS_OFF);
                    }

                    if (hasChest) {
                        bar.append(CHEST_ON);
                    }

                    if (hasHelmet) {
                        Long lastDodge = dodgeCooldowns.get(player.getUniqueId());
                        boolean ready = lastDodge == null || (now - lastDodge) >= DODGE_COOLDOWN_MS;
                        bar.append(ready ? HELMET_ON : HELMET_OFF);
                    }

                    player.sendActionBar(mm.deserialize("<font:tesseract:ui>" + bar + "</font>"));
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private void startGroundCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;

                    UUID id = player.getUniqueId();
                    ItemStack boots = player.getInventory().getBoots();
                    boolean hasBoots = itemManager.isItem(boots, "wither_boots");

                    if (!hasBoots) {
                        if (player.getAllowFlight()) {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                        }
                        wasOnGround.remove(id);
                        usedDoubleJump.remove(id);
                        continue;
                    }

                    boolean onGround = player.isOnGround();

                    if (onGround) {
                        usedDoubleJump.remove(id);
                        wasOnGround.add(id);

                        Long lastJump = doubleJumpCooldowns.get(id);
                        long now = System.currentTimeMillis();
                        boolean cooldownOver = lastJump == null || (now - lastJump) >= DOUBLE_JUMP_COOLDOWN_MS;

                        if (cooldownOver) {
                            player.setAllowFlight(true);
                        } else {
                            player.setAllowFlight(false);
                        }
                    } else {
                        if (wasOnGround.remove(id)) {
                            if (!usedDoubleJump.contains(id)) {
                                Long lastJump = doubleJumpCooldowns.get(id);
                                long now = System.currentTimeMillis();
                                boolean cooldownOver = lastJump == null || (now - lastJump) >= DOUBLE_JUMP_COOLDOWN_MS;

                                if (cooldownOver) {
                                    player.setAllowFlight(true);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // -------------------------------------------------------------------------
    // Wither Skeleton drop
    // -------------------------------------------------------------------------

    @EventHandler
    public void onWitherSkeletonDeath(EntityDeathEvent e) {
        if (e.getEntity().getType() == EntityType.WITHER_SKELETON) {
            if (random.nextDouble() < 0.08) {
                e.getDrops().add(itemManager.create("voidium"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Wither Leggings – fire immunity
    // -------------------------------------------------------------------------

    @EventHandler
    public void onLavaDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FIRE) return;
        if (e.getEntity().getType() != EntityType.PLAYER) return;

        Player player = (Player) e.getEntity();
        if (itemManager.isItem(player.getInventory().getLeggings(), "wither_leggings")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player player)) return;

        UUID id = player.getUniqueId();

        if (noFall.contains(id)) {
            e.setCancelled(true);
            noFall.remove(id);
        }
    }

    // -------------------------------------------------------------------------
    // Wither Mask – dodge
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player)) return;

        ItemStack helmet = victim.getInventory().getHelmet();
        if (!itemManager.isItem(helmet, "wither_mask")) return;

        Long lastDodge = dodgeCooldowns.get(victim.getUniqueId());
        long now = System.currentTimeMillis();

        if (lastDodge != null && (now - lastDodge) < DODGE_COOLDOWN_MS) return;
        if (random.nextDouble() > DODGE_CHANCE) return;

        dodgeCooldowns.put(victim.getUniqueId(), now);
        e.setCancelled(true);

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 1.0 + random.nextDouble() * 2.0;

        Location dodgeLoc = victim.getLocation().clone()
                .add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        dodgeLoc.setYaw(victim.getLocation().getYaw());
        dodgeLoc.setPitch(victim.getLocation().getPitch());

        Location safeLoc = findSafeLocation(dodgeLoc);
        victim.teleport(safeLoc != null ? safeLoc : dodgeLoc);

        victim.getWorld().spawnParticle(Particle.LARGE_SMOKE, victim.getLocation(), 12, 0.3, 0.5, 0.3, 0.02);
        victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation(), 8, 0.2, 0.3, 0.2, 0.01);
        victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.8f);
    }

    // -------------------------------------------------------------------------
    // Wither Leggings – mid-air dash
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDash(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;

        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.ADVENTURE) return;
        if (player.isOnGround()) return;

        ItemStack leggings = player.getInventory().getLeggings();
        if (!itemManager.isItem(leggings, "wither_leggings")) return;

        Long lastDash = dashCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastDash != null && (now - lastDash) < DASH_COOLDOWN_MS) return;

        dashCooldowns.put(player.getUniqueId(), now);

        activateNoFall(player);

        Vector direction = player.getLocation().getDirection().normalize();
        double horizontal = DASH_HORIZONTAL;
        double vertical = direction.getY() * horizontal;
        if (vertical > 0) vertical /= DASH_VERTICAL_DIVISOR;

        player.setVelocity(new Vector(
                direction.getX() * horizontal,
                vertical + 0.3,
                direction.getZ() * horizontal
        ));

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 8, 0.3, 0.1, 0.3, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.5f);
    }

    // -------------------------------------------------------------------------
    // Wither Boots – double jump
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.ADVENTURE) return;

        ItemStack boots = player.getInventory().getBoots();
        if (!itemManager.isItem(boots, "wither_boots")) return;

        e.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        UUID id = player.getUniqueId();

        if (usedDoubleJump.contains(id)) return;

        Long lastJump = doubleJumpCooldowns.get(id);
        long now = System.currentTimeMillis();

        if (lastJump != null && (now - lastJump) < DOUBLE_JUMP_COOLDOWN_MS) return;

        doubleJumpCooldowns.put(id, now);
        usedDoubleJump.add(id);

        activateNoFall(player);

        Vector velocity = player.getVelocity();
        velocity.setY(DOUBLE_JUMP_POWER);
        player.setVelocity(velocity);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 6, 0.2, 0.05, 0.2, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.4f, 1.2f);
    }

    private void activateNoFall(Player player) {
        noFall.add(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack boots = player.getInventory().getBoots();
        if (!itemManager.isItem(boots, "wither_boots")) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            usedDoubleJump.remove(player.getUniqueId());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Location findSafeLocation(Location loc) {
        Location check = loc.clone();

        if (check.getBlock().isPassable() && check.clone().add(0, 1, 0).getBlock().isPassable()) {
            int groundY = loc.getWorld().getHighestBlockYAt(check);
            if (check.getBlockY() - groundY > 5) {
                check.setY(groundY + 1);
            }
            return check;
        }

        for (int y = 1; y <= 3; y++) {
            Location up = check.clone().add(0, y, 0);
            if (up.getBlock().isPassable() && up.clone().add(0, 1, 0).getBlock().isPassable()) {
                return up;
            }
        }

        return null;
    }
}