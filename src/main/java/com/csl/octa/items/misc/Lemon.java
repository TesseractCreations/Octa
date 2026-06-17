package com.csl.octa.items.misc;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Lemon {

    private final ItemManager itemManager;

    public Lemon(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("lemon", Material.PAPER, "tesseract:lemon", Component.text("Lemon"));
    }

}
