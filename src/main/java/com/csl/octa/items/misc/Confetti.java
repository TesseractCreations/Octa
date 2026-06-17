package com.csl.octa.items.misc;

import com.csl.octa.managers.ItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Confetti {

    private final ItemManager itemManager;

    public Confetti(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("confetti", Material.PAPER, "tesseract:confetti_popper", Component.text("Confetti Popper"));
    }

}
