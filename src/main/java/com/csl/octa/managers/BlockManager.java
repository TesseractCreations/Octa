package com.csl.octa.managers;

import com.csl.octa.models.CustomBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BlockManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final Map<String, CustomBlock> registry = new HashMap<>();
    private final Map<Integer, String> indexToId = new HashMap<>();
    private final Map<Location, String> placedBlocks = new HashMap<>();
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    private static final String VANILLA_MUSHROOM_STEM_MODEL = "minecraft:block/mushroom_stem";

    public BlockManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.dataFile = new File(plugin.getDataFolder(), "BlockData.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadPlacedBlocks();
    }

    public void register(String id, Material itemMaterial, String blockTexture, Component displayName, double hardness, ItemStack customDrops) {
        int index = registry.size();
        if (index >= 64) throw new IllegalStateException("Max custom blocks reached (64)!");

        String modelPath = "minecraft:block/custom/" + id;
        String resolvedTexture = resolveTexturePath(blockTexture);

        CustomBlock cb = new CustomBlock(id, index, resolvedTexture, displayName, hardness, null);
        registry.put(id, cb);
        indexToId.put(index, id);

        itemManager.register(id, itemMaterial, modelPath, displayName);
        cb.setDrops(customDrops != null ? customDrops : itemManager.create(id));

        generateBlockModel(id, resolvedTexture);
    }

    private String resolveTexturePath(String input) {
        if (input == null || input.isEmpty()) return "minecraft:block/mushroom_stem";

        if (!input.contains(":")) {
            return "minecraft:" + input;
        }

        return input;
    }

    private void generateBlockModel(String id, String texture) {
        File outputDir = new File(plugin.getDataFolder(), "generated/assets/minecraft/models/block/custom");
        outputDir.mkdirs();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("parent", "minecraft:block/cube_all");
        Map<String, String> textures = new LinkedHashMap<>();
        textures.put("all", texture);
        model.put("textures", textures);

        File modelFile = new File(outputDir, id + ".json");
        writeJson(modelFile, model);
    }

    public void placeCustomBlock(LimitedRegion region, int x, int y, int z, String id) {
        CustomBlock cb = registry.get(id);
        if (cb == null) return;

        region.setType(x, y, z, Material.MUSHROOM_STEM);
        MultipleFacing data = (MultipleFacing) region.getBlockData(x, y, z);
        applyIndexToData(data, cb.getIndex());
        region.setBlockData(x, y, z, data);
    }

    public void generateBlockStateJson() {
        File outputDir = new File(plugin.getDataFolder(), "generated/assets/minecraft/blockstates");
        outputDir.mkdirs();

        Map<String, Object> blockstate = new LinkedHashMap<>();
        Map<String, Object> variants = new LinkedHashMap<>();

        for (int i = 0; i < 64; i++) {
            String variantKey = buildVariantKey(i);
            Map<String, String> modelEntry = new LinkedHashMap<>();

            if (indexToId.containsKey(i)) {
                modelEntry.put("model", "minecraft:block/custom/" + indexToId.get(i));
            } else {
                modelEntry.put("model", VANILLA_MUSHROOM_STEM_MODEL);
            }

            variants.put(variantKey, modelEntry);
        }

        blockstate.put("variants", variants);

        File blockstateFile = new File(outputDir, "mushroom_stem.json");
        writeJson(blockstateFile, blockstate);

        plugin.getLogger().info("Generated mushroom_stem.json blockstate with " + registry.size() + " custom block(s) and " + (64 - registry.size()) + " vanilla fallback(s).");
    }

    private String buildVariantKey(int index) {
        boolean up    = (index & (1))  != 0;
        boolean down  = (index & (1 << 1)) != 0;
        boolean north = (index & (1 << 2)) != 0;
        boolean south = (index & (1 << 3)) != 0;
        boolean west  = (index & (1 << 4)) != 0;
        boolean east  = (index & (1 << 5)) != 0;

        return "down=" + down
                + ",east=" + east
                + ",north=" + north
                + ",south=" + south
                + ",up=" + up
                + ",west=" + west;
    }

    private void writeJson(File file, Object data) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write JSON file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public void placeBlock(Location loc, String id) {
        CustomBlock cb = registry.get(id);
        if (cb == null) return;
        Block block = loc.getBlock();
        block.setType(Material.MUSHROOM_STEM, false);
        MultipleFacing data = (MultipleFacing) block.getBlockData();
        applyIndexToData(data, cb.getIndex());
        block.setBlockData(data, false);
        placedBlocks.put(loc, id);
        savePlacedBlocks();
    }

    public CustomBlock getCustomBlock(String id) {
        return registry.get(id);
    }

    public CustomBlock getCustomBlockAt(Block block) {
        if (block.getType() != Material.MUSHROOM_STEM) return null;
        int index = getIndexFromData((MultipleFacing) block.getBlockData());
        return registry.get(indexToId.get(index));
    }

    private void applyIndexToData(MultipleFacing data, int index) {
        for (int i = 0; i < FACES.length; i++) {
            data.setFace(FACES[i], (index & (1 << i)) != 0);
        }
    }

    private int getIndexFromData(MultipleFacing data) {
        int index = 0;
        for (int i = 0; i < FACES.length; i++) {
            if (data.hasFace(FACES[i])) index |= (1 << i);
        }
        return index;
    }

    public void savePlacedBlocks() {
        dataConfig.set("blocks", null);
        placedBlocks.forEach((loc, id) -> {
            String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            dataConfig.set("blocks." + key, id);
        });
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlacedBlocks() {
        if (!dataConfig.contains("blocks") || dataConfig.getConfigurationSection("blocks") == null) return;
        for (String key : dataConfig.getConfigurationSection("blocks").getKeys(false)) {
            String id = dataConfig.getString("blocks." + key);
            String[] split = key.split(",");
            placedBlocks.put(new Location(
                    Bukkit.getWorld(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]),
                    Integer.parseInt(split[3])
            ), id);
        }
    }

    public Collection<CustomBlock> getRegisteredBlocks() {
        return registry.values();
    }

    public void removePlacedBlock(Location loc) {
        placedBlocks.remove(loc);
        savePlacedBlocks();
    }
}