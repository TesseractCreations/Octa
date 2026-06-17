package com.csl.octa.listeners;

import com.csl.octa.managers.MapManager;
import com.csl.octa.managers.MapManager.MapCreatorSession;
import com.csl.octa.managers.MapManager.MapData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;

public class MapListener implements Listener {

    private final JavaPlugin plugin;
    private final MapManager mapManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final NamespacedKey mapIdKey;
    private final NamespacedKey mapListIdKey;
    private final NamespacedKey mapSizeKey;

    public MapListener(JavaPlugin plugin, MapManager mapManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
        this.mapIdKey = new NamespacedKey(plugin, "map_id");
        this.mapListIdKey = new NamespacedKey(plugin, "map_list_id");
        this.mapSizeKey = new NamespacedKey(plugin, "map_size");
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getType() != Material.MAP && held.getType() != Material.FILLED_MAP) return;

        String mapId = getMapIdFromHeld(held);

        if (mapId != null) {
            e.setCancelled(true);
            MapData data = mapManager.getMap(mapId);
            if (data == null) return;

            if (player.isSneaking()) {
                deleteMapItem(player, data);
                return;
            }

            if (data.placedLocation != null) {
                player.sendMessage(mm.deserialize("<red>This map is already placed. Interact with it on the wall to remove."));
                return;
            }

            if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
                placeMap(data, player, e.getClickedBlock(), e.getBlockFace());
            }
            return;
        }

        if (held.getType() == Material.MAP && player.isSneaking()) {
            e.setCancelled(true);
            MapCreatorSession session = new MapCreatorSession(player.getUniqueId());
            mapManager.setCreatorSession(player.getUniqueId(), session);
            player.openInventory(mapManager.createSizeGui(player.getUniqueId()));
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof ItemFrame frame)) return;

        Player player = e.getPlayer();

        for (MapData data : mapManager.getAllMaps()) {
            if (data.placedLocation == null) continue;

            for (ItemFrame mapFrame : data.itemFrames) {
                if (mapFrame != null && mapFrame.getUniqueId().equals(frame.getUniqueId())) {
                    e.setCancelled(true);
                    pickUpMap(player, data);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(e.getView().title());

        if (title.equals("Select Map Size")) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            Integer size = clicked.getItemMeta().getPersistentDataContainer().get(mapSizeKey, PersistentDataType.INTEGER);
            if (size == null) return;

            MapCreatorSession session = mapManager.getCreatorSession(player.getUniqueId());
            if (session == null) {
                session = new MapCreatorSession(player.getUniqueId());
                mapManager.setCreatorSession(player.getUniqueId(), session);
            }

            session.size = size;
            player.closeInventory();

            session.awaitingUrl = true;
            player.sendMessage(mm.deserialize("<yellow>Type the image URL in chat. Type <red>cancel</red> to cancel."));
            return;
        }

        if (title.equals("Map List")) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String listMapId = clicked.getItemMeta().getPersistentDataContainer().get(mapListIdKey, PersistentDataType.STRING);
            if (listMapId == null) return;

            MapData data = mapManager.getMap(listMapId);
            if (data == null) return;

            if (data.placedLocation != null) {
                player.closeInventory();
                player.teleport(data.placedLocation.clone().add(0.5, 0, 0.5));
                player.sendMessage(mm.deserialize("<green>Teleported to map <gold>#" + listMapId + "<green>."));
            } else {
                player.sendMessage(mm.deserialize("<red>This map has not been placed yet."));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        MapCreatorSession session = mapManager.getCreatorSession(player.getUniqueId());

        if (session == null || !session.awaitingUrl) return;

        e.setCancelled(true);
        String message = e.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            mapManager.removeCreatorSession(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(mm.deserialize("<red>Map creation cancelled."))
            );
            return;
        }

        if (!message.startsWith("http://") && !message.startsWith("https://")) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(mm.deserialize("<red>Invalid URL. Must start with http:// or https://"))
            );
            return;
        }

        session.url = message;
        session.awaitingUrl = false;

        mapManager.downloadImage(message).thenAccept(img -> {
            if (img == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(mm.deserialize("<red>Failed to download image."));
                    mapManager.removeCreatorSession(player.getUniqueId());
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                int size = session.size;
                if (size == 0) {
                    size = mapManager.autoDetectSize(img);
                    session.size = size;
                }

                MapData data = mapManager.createMap(player, session.url, session.size);
                player.sendMessage(mm.deserialize("<yellow>Creating map preview..."));

                mapManager.createMapItemAsync(data).thenAccept(mapItem -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ItemStack heldMap = player.getInventory().getItemInMainHand();
                        if (heldMap.getType() == Material.MAP) {
                            if (heldMap.getAmount() > 1) {
                                heldMap.setAmount(heldMap.getAmount() - 1);
                            } else {
                                player.getInventory().setItemInMainHand(null);
                            }
                        }

                        giveItem(player, mapItem);
                        player.sendMessage(mm.deserialize("<green>Map <gold>#" + data.id + "</gold> created! Right-click a wall to place it."));
                        mapManager.removeCreatorSession(player.getUniqueId());
                    });
                });
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(mm.deserialize("<red>Failed to load image: " + ex.getMessage()));
                mapManager.removeCreatorSession(player.getUniqueId());
            });
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        mapManager.removeCreatorSession(e.getPlayer().getUniqueId());
    }

    private void placeMap(MapData data, Player player, Block clickedBlock, BlockFace face) {
        if (face != BlockFace.NORTH && face != BlockFace.SOUTH &&
                face != BlockFace.EAST && face != BlockFace.WEST) {
            player.sendMessage(mm.deserialize("<red>You can only place maps on walls."));
            return;
        }

        player.sendMessage(mm.deserialize("<yellow>Placing map..."));

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        Block surfaceBlock = clickedBlock;
        BlockFace surfaceFace = face;

        mapManager.renderImageAsync(data.url, data.size).thenAccept(tiles -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Location center = surfaceBlock.getRelative(surfaceFace).getLocation();
                int half = data.size / 2;

                BlockFace right;

                switch (surfaceFace) {
                    case NORTH -> right = BlockFace.WEST;
                    case SOUTH -> right = BlockFace.EAST;
                    case EAST -> right = BlockFace.NORTH;
                    case WEST -> right = BlockFace.SOUTH;
                    default -> {
                        player.sendMessage(mm.deserialize("<red>Invalid surface."));
                        return;
                    }
                }

                data.mapViewIds.clear();
                data.itemFrames.clear();

                int tileIndex = 0;
                for (int row = 0; row < data.size; row++) {
                    for (int col = 0; col < data.size; col++) {
                        int offsetRight = col - half;
                        int offsetDown = row - half;

                        Location frameLoc = center.clone()
                                .add(right.getModX() * offsetRight, -offsetDown, right.getModZ() * offsetRight);

                        MapView view = mapManager.createMapView(tiles.get(tileIndex++));
                        data.mapViewIds.add(view.getId());

                        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                        mapMeta.setMapView(view);
                        mapItem.setItemMeta(mapMeta);

                        try {
                            ItemFrame frame = frameLoc.getWorld().spawn(frameLoc, ItemFrame.class, f -> {
                                f.setVisible(false);
                                f.setFixed(true);
                                f.setFacingDirection(surfaceFace);
                                f.setItem(mapItem);
                                f.setRotation(Rotation.NONE);
                            });
                            data.itemFrames.add(frame);
                        } catch (Exception ex) {
                        }
                    }
                }

                data.placedLocation = center;
                player.sendMessage(mm.deserialize("<green>Map <gold>#" + data.id + "</gold> placed!"));
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(mm.deserialize("<red>Failed to render: " + ex.getMessage()));
                mapManager.createMapItemAsync(data).thenAccept(returnItem ->
                        Bukkit.getScheduler().runTask(plugin, () -> giveItem(player, returnItem))
                );
            });
            return null;
        });
    }

    private void pickUpMap(Player player, MapData data) {
        for (ItemFrame frame : data.itemFrames) {
            if (frame != null && !frame.isDead()) {
                frame.remove();
            }
        }
        data.itemFrames.clear();

        for (int mapId : data.mapViewIds) {
            MapView view = Bukkit.getMap(mapId);
            if (view != null) view.getRenderers().clear();
        }
        data.mapViewIds.clear();
        data.placedLocation = null;

        player.sendMessage(mm.deserialize("<yellow>Picking up map..."));

        mapManager.createMapItemAsync(data).thenAccept(mapItem ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    giveItem(player, mapItem);
                    player.sendMessage(mm.deserialize("<yellow>Map <gold>#" + data.id + "</gold> picked up."));
                })
        );
    }

    private void deleteMapItem(Player player, MapData data) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        if (data.placedLocation != null) {
            for (ItemFrame frame : data.itemFrames) {
                if (frame != null && !frame.isDead()) frame.remove();
            }
            data.itemFrames.clear();

            for (int mapId : data.mapViewIds) {
                MapView view = Bukkit.getMap(mapId);
                if (view != null) view.getRenderers().clear();
            }
            data.mapViewIds.clear();
            data.placedLocation = null;
        }

        mapManager.removeMap(data.id);

        giveItem(player, new ItemStack(Material.MAP));
        player.sendMessage(mm.deserialize("<red>Map <gold>#" + data.id + "</gold> deleted."));
    }

    private void giveItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private String getMapIdFromHeld(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(mapIdKey, PersistentDataType.STRING);
    }
}