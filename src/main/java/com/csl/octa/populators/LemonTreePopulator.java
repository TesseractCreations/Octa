package com.csl.octa.populators;

import com.csl.octa.managers.BlockManager;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class LemonTreePopulator extends BlockPopulator {

    private final BlockManager blockManager;
    private final double treeChance;
    private final double leafChance;

    public LemonTreePopulator(BlockManager blockManager, double treeChance, double leafChance) {
        this.blockManager = blockManager;
        this.treeChance = treeChance;
        this.leafChance = leafChance;
    }

    public LemonTreePopulator(BlockManager blockManager) {
        this(blockManager, 0.15, 0.4);
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;

        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();

        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (!region.isInRegion(x, y, z)) continue;
                    if (region.getType(x, y, z) != Material.OAK_LOG) continue;

                    if (random.nextDouble() >= treeChance) continue;

                    convertNearbyLeaves(region, random, x, y, z);
                }
            }
        }
    }

    private void convertNearbyLeaves(LimitedRegion region, Random random, int logX, int logY, int logZ) {
        int radius = 5;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int bx = logX + dx;
                    int by = logY + dy;
                    int bz = logZ + dz;

                    if (!region.isInRegion(bx, by, bz)) continue;
                    if (region.getType(bx, by, bz) != Material.OAK_LEAVES) continue;

                    if (random.nextDouble() < leafChance) {
                        blockManager.placeCustomBlock(region, bx, by, bz, "lemon_leaves");
                    }
                }
            }
        }
    }
}