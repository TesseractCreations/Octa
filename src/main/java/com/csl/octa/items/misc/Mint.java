package com.csl.octa.items.misc;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Mint {

    private final ItemManager itemManager;

    public Mint(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("mint_leaves", Material.PAPER, "tesseract:mint", Component.text("Mint Leaf"));
    }

}
