package com.csl.octa.managers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MapManager {

    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, MapData> maps = new ConcurrentHashMap<>();
    private final Map<UUID, MapCreatorSession> creatorSessions = new ConcurrentHashMap<>();
    private final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    private int nextId = 1;

    public MapManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "MapData.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadMaps();
    }

    public MapData createMap(Player creator, String url, int size) {
        String id = String.valueOf(nextId++);
        MapData data = new MapData(id, creator.getUniqueId(), creator.getName(), url, size, null, new ArrayList<>());
        maps.put(id, data);
        save();
        return data;
    }

    public void removeMap(String id) {
        MapData data = maps.remove(id);
        if (data == null) return;
        imageCache.remove(data.url);
        if (data.placedLocation != null) {
            for (int mapId : data.mapViewIds) {
                MapView view = Bukkit.getMap(mapId);
                if (view != null) view.getRenderers().clear();
            }
        }
        save();
    }

    public List<MapData> removeAllByPlayer(UUID playerUUID) {
        List<MapData> removed = new ArrayList<>();
        Iterator<Map.Entry<String, MapData>> it = maps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, MapData> entry = it.next();
            if (entry.getValue().creatorUUID.equals(playerUUID)) {
                imageCache.remove(entry.getValue().url);
                removed.add(entry.getValue());
                it.remove();
            }
        }
        if (!removed.isEmpty()) save();
        return removed;
    }

    public MapData getMap(String id) {
        return maps.get(id);
    }

    public Collection<MapData> getAllMaps() {
        return maps.values();
    }

    public void setCreatorSession(UUID uuid, MapCreatorSession session) {
        creatorSessions.put(uuid, session);
    }

    public MapCreatorSession getCreatorSession(UUID uuid) {
        return creatorSessions.get(uuid);
    }

    public void removeCreatorSession(UUID uuid) {
        creatorSessions.remove(uuid);
    }

    public CompletableFuture<BufferedImage> downloadImage(String url) {
        BufferedImage cached = imageCache.get(url);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Minecraft-MapPlugin/1.0")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(response.body()));
                        if (img != null) {
                            imageCache.put(url, img);
                        }
                        return img;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to decode image", e);
                    }
                });
    }

    public CompletableFuture<ItemStack> createMapItemAsync(MapData data) {
        return downloadImage(data.url).thenCompose(original -> {
            if (original == null) {
                return CompletableFuture.completedFuture(createFallbackMapItem(data));
            }

            BufferedImage scaled = scaleImage(original, 128, 128);
            int[] pixels = scaled.getRGB(0, 0, 128, 128, null, 0, 128);

            CompletableFuture<ItemStack> future = new CompletableFuture<>();

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    MapView preview = createMapView(pixels);
                    ItemStack item = new ItemStack(Material.FILLED_MAP);
                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                    mapMeta.setMapView(preview);
                    mapMeta.displayName(mm.deserialize("<gold>Image Map <gray>(#" + data.id + ")"));
                    mapMeta.lore(List.of(
                            mm.deserialize("<gray>Size: <white>" + data.size + "x" + data.size),
                            mm.deserialize(""),
                            mm.deserialize("<yellow>Right-click a wall to place"),
                            mm.deserialize("<yellow>Shift+Right-click to delete")
                    ));
                    mapMeta.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "map_id"),
                            org.bukkit.persistence.PersistentDataType.STRING,
                            data.id
                    );
                    item.setItemMeta(mapMeta);
                    data.previewMapId = preview.getId();
                    save();
                    future.complete(item);
                } catch (Exception e) {
                    future.complete(createFallbackMapItem(data));
                }
            });

            return future;
        }).exceptionally(ex -> createFallbackMapItem(data));
    }

    public CompletableFuture<List<int[]>> renderImageAsync(String url, int size) {
        return downloadImage(url).thenApply(original -> {
            if (original == null) throw new RuntimeException("Image is null");

            int totalPixels = size * 128;
            BufferedImage scaled = scaleImage(original, totalPixels, totalPixels);

            List<int[]> tiles = new ArrayList<>();
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    int[] pixels = new int[128 * 128];
                    scaled.getRGB(col * 128, row * 128, 128, 128, pixels, 0, 128);
                    tiles.add(pixels);
                }
            }
            return tiles;
        });
    }

    public int autoDetectSize(BufferedImage img) {
        int maxDim = Math.max(img.getWidth(), img.getHeight());
        return Math.min(5, Math.max(1, (int) Math.ceil(maxDim / 128.0)));
    }

    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private ItemStack createFallbackMapItem(MapData data) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize("<gold>Image Map <gray>(#" + data.id + ")"));
        meta.lore(List.of(
                mm.deserialize("<gray>Size: <white>" + data.size + "x" + data.size),
                mm.deserialize("<red>Preview failed to load"),
                mm.deserialize(""),
                mm.deserialize("<yellow>Right-click a wall to place"),
                mm.deserialize("<yellow>Shift+Right-click to delete")
        ));
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "map_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                data.id
        );
        item.setItemMeta(meta);
        return item;
    }

    public String getMapIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "map_id"),
                org.bukkit.persistence.PersistentDataType.STRING
        );
    }

    public MapView createMapView(int[] pixels) {
        MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
        view.getRenderers().clear();
        view.addRenderer(new ImageMapRenderer(pixels));
        view.setLocked(true);
        view.setTrackingPosition(false);
        return view;
    }

    public Inventory createListGui(Player viewer) {
        Collection<MapData> allMaps = getAllMaps();
        int slots = Math.max(9, (int) Math.ceil(allMaps.size() / 9.0) * 9);
        slots = Math.min(slots, 54);

        Inventory gui = Bukkit.createInventory(null, slots, mm.deserialize("<dark_gray>Map List"));

        int slot = 0;
        for (MapData data : allMaps) {
            if (slot >= slots) break;

            ItemStack item = new ItemStack(Material.FILLED_MAP);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(mm.deserialize("<gold>Map #" + data.id));
            meta.lore(List.of(
                    mm.deserialize("<gray>Creator: <white>" + data.creatorName),
                    mm.deserialize("<gray>Size: <white>" + data.size + "x" + data.size),
                    mm.deserialize(""),
                    mm.deserialize(data.placedLocation != null
                            ? "<green>Click to teleport"
                            : "<red>Not placed yet")
            ));
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "map_list_id"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    data.id
            );
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }

        return gui;
    }

    public Inventory createSizeGui(UUID playerUUID) {
        Inventory gui = Bukkit.createInventory(null, 9, mm.deserialize("<dark_gray>Select Map Size"));

        for (int i = 1; i <= 5; i++) {
            ItemStack item = new ItemStack(Material.MAP, i);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(mm.deserialize("<gold>" + i + "x" + i));
            meta.lore(List.of(
                    mm.deserialize("<gray>Total frames: <white>" + (i * i)),
                    mm.deserialize("<yellow>Click to select")
            ));
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "map_size"),
                    org.bukkit.persistence.PersistentDataType.INTEGER,
                    i
            );
            item.setItemMeta(meta);
            gui.setItem(i + 1, item);
        }

        ItemStack auto = new ItemStack(Material.COMPASS);
        ItemMeta autoMeta = auto.getItemMeta();
        autoMeta.displayName(mm.deserialize("<aqua>Auto Size"));
        autoMeta.lore(List.of(mm.deserialize("<gray>Automatically determines size")));
        autoMeta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "map_size"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                0
        );
        auto.setItemMeta(autoMeta);
        gui.setItem(0, auto);

        return gui;
    }

    private void save() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (dataConfig) {
                dataConfig.set("next_id", nextId);
                dataConfig.set("maps", null);

                for (MapData data : maps.values()) {
                    String path = "maps." + data.id;
                    dataConfig.set(path + ".creator_uuid", data.creatorUUID.toString());
                    dataConfig.set(path + ".creator_name", data.creatorName);
                    dataConfig.set(path + ".url", data.url);
                    dataConfig.set(path + ".size", data.size);
                    dataConfig.set(path + ".map_view_ids", data.mapViewIds);
                    dataConfig.set(path + ".preview_map_id", data.previewMapId);

                    if (data.placedLocation != null) {
                        dataConfig.set(path + ".placed.world", data.placedLocation.getWorld().getName());
                        dataConfig.set(path + ".placed.x", data.placedLocation.getBlockX());
                        dataConfig.set(path + ".placed.y", data.placedLocation.getBlockY());
                        dataConfig.set(path + ".placed.z", data.placedLocation.getBlockZ());
                    }
                }

                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadMaps() {
        nextId = dataConfig.getInt("next_id", 1);

        if (!dataConfig.contains("maps") || dataConfig.getConfigurationSection("maps") == null) return;

        for (String id : dataConfig.getConfigurationSection("maps").getKeys(false)) {
            String path = "maps." + id;

            UUID creatorUUID = UUID.fromString(dataConfig.getString(path + ".creator_uuid"));
            String creatorName = dataConfig.getString(path + ".creator_name");
            String url = dataConfig.getString(path + ".url");
            int size = dataConfig.getInt(path + ".size");
            List<Integer> mapViewIds = dataConfig.getIntegerList(path + ".map_view_ids");
            int previewMapId = dataConfig.getInt(path + ".preview_map_id", -1);

            org.bukkit.Location placedLocation = null;
            if (dataConfig.contains(path + ".placed")) {
                placedLocation = new org.bukkit.Location(
                        Bukkit.getWorld(dataConfig.getString(path + ".placed.world")),
                        dataConfig.getInt(path + ".placed.x"),
                        dataConfig.getInt(path + ".placed.y"),
                        dataConfig.getInt(path + ".placed.z")
                );
            }

            MapData data = new MapData(id, creatorUUID, creatorName, url, size, placedLocation, mapViewIds);
            data.previewMapId = previewMapId;
            maps.put(id, data);

            int idNum = Integer.parseInt(id);
            if (idNum >= nextId) nextId = idNum + 1;
        }
    }

    public static class MapData {
        public final String id;
        public final UUID creatorUUID;
        public final String creatorName;
        public final String url;
        public int size;
        public org.bukkit.Location placedLocation;
        public List<Integer> mapViewIds;
        public List<org.bukkit.entity.ItemFrame> itemFrames = new ArrayList<>();
        public int previewMapId = -1;

        public MapData(String id, UUID creatorUUID, String creatorName, String url, int size, org.bukkit.Location placedLocation, List<Integer> mapViewIds) {
            this.id = id;
            this.creatorUUID = creatorUUID;
            this.creatorName = creatorName;
            this.url = url;
            this.size = size;
            this.placedLocation = placedLocation;
            this.mapViewIds = mapViewIds != null ? new ArrayList<>(mapViewIds) : new ArrayList<>();
        }
    }

    public static class MapCreatorSession {
        public UUID playerUUID;
        public int size = 0;
        public String url = null;
        public boolean awaitingUrl = false;
        public String mapId = null;

        public MapCreatorSession(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }
    }

    private static class ImageMapRenderer extends MapRenderer {
        private final int[] pixels;
        private boolean rendered = false;

        public ImageMapRenderer(int[] pixels) {
            this.pixels = pixels;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (rendered) return;
            rendered = true;

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    int argb = pixels[y * 128 + x];
                    canvas.setPixelColor(x, y, new java.awt.Color(argb, true));
                }
            }
        }
    }
}