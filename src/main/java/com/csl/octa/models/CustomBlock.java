package com.csl.octa.models;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public class CustomBlock {
    private final String id;
    private final int index;
    private final String texture;
    private final Component displayName;
    private final double hardness;
    private ItemStack drops;

    public CustomBlock(String id, int index, String texture, Component displayName, double hardness, ItemStack drops) {
        this.id = id;
        this.index = index;
        this.texture = texture;
        this.displayName = displayName;
        this.hardness = hardness;
        this.drops = drops;
    }

    public String getId() { return id; }
    public int getIndex() { return index; }
    public String getTexture() { return texture; }
    public double getHardness() { return hardness; }
    public ItemStack getDrops() { return drops; }
    public void setDrops(ItemStack drops) { this.drops = drops; }
}