package com.csl.octa.items.misc;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Barley {

    private final ItemManager itemManager;

    public Barley(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("barley", Material.PAPER, "tesseract:barley", Component.text("Barley"));
    }

}
