package com.csl.octa.items.weapon;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class GrapplingHook {

    private final ItemManager itemManager;

    public GrapplingHook(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("grappling_hook", Material.PAPER, "tesseract:grappling_hook", Component.text("Grappling Hook"));
    }

}
