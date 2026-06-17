package com.csl.octa.items.drinkable;

import com.csl.octa.managers.ItemManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class Beer {

    private final ItemManager itemManager;

    public Beer(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("beer_drink", Material.PAPER, "tesseract:beer",
                Component.text("Beer", NamedTextColor.GOLD),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("A cold one.", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(2)
                                .saturation(0.5f)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(3.0f)
                                .animation(ItemUseAnimation.DRINK)
                                .sound(Key.key("minecraft", "entity.generic.drink"))
                                .addEffect(ConsumeEffect.applyStatusEffects(List.of(
                                        new PotionEffect(PotionEffectType.STRENGTH, 20 * 30, 0),
                                        new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 0),
                                        new PotionEffect(PotionEffectType.NAUSEA, 20 * 5, 0)
                                ), 1.0f))
                                .build()
                ));

        itemManager.register("sweet_beer_drink", Material.PAPER, "tesseract:sweet_beer",
                Component.text("Sweet Beer", NamedTextColor.YELLOW),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("Tastes like honey.", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(3)
                                .saturation(1.0f)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(3.0f)
                                .animation(ItemUseAnimation.DRINK)
                                .sound(Key.key("minecraft", "entity.generic.drink"))
                                .addEffect(ConsumeEffect.applyStatusEffects(List.of(
                                        new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0),
                                        new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 15, 0),
                                        new PotionEffect(PotionEffectType.NAUSEA, 20 * 3, 0)
                                ), 1.0f))
                                .build()
                ));

        itemManager.register("malted_glass", Material.PAPER, "tesseract:malted_glass", Component.text("Malted Glass"));
        itemManager.register("sweet_malted_glass", Material.PAPER, "tesseract:sweet_malted_glass", Component.text("Sweet Malted Glass"));
        itemManager.register("fermented_glass", Material.PAPER, "tesseract:fermented_glass", Component.text("Fermented Glass"));
        itemManager.register("sweet_fermented_glass", Material.PAPER, "tesseract:sweet_fermented_glass", Component.text("Sweet Fermented Glass"));
        itemManager.register("empty_glass", Material.PAPER, "tesseract:empty_glass", Component.text("Empty Glass"));
    }
}