package com.csl.octa.items.drinkable;

import com.csl.octa.managers.ItemManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Mojito {

    private final ItemManager itemManager;

    public Mojito(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("mojito_drink", Material.PAPER, "tesseract:mojito", Component.text("Mojito"), ItemManager.DataComponent.of(
                DataComponentTypes.CONSUMABLE,
                Consumable.consumable()
                        .consumeSeconds(1.0f)
                        .animation(ItemUseAnimation.DRINK)
                        .sound(Key.key("minecraft","entity.generic.drink"))
                        .build()
        ));
    }

}
