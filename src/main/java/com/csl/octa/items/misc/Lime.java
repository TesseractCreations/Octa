package com.csl.octa.items.misc;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Lime {

    private final ItemManager itemManager;

    public Lime(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("lime", Material.PAPER, "tesseract:lime", Component.text("Lime"));
    }

}
