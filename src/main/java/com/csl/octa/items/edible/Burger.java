package com.csl.octa.items.edible;

import com.csl.octa.managers.ItemManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Burger {

    private final ItemManager itemManager;

    public Burger(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("burger", Material.PAPER, "tesseract:burger", Component.text("Burger"),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(1.6f)
                                .animation(ItemUseAnimation.EAT)
                                .sound(Key.key("minecraft", "entity.generic.eat"))
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .nutrition(10)
                                .saturation(12.0f)
                                .build()
                )
        );
    }

}