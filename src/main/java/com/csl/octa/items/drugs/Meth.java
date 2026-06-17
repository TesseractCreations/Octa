package com.csl.octa.items.drugs;

import com.csl.octa.managers.ItemManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

import java.util.List;

public class Meth {

    private final ItemManager itemManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private static final Key EMPTY_SOUND = Key.key("tesseract", "meth.sniff");

    public Meth(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {

        itemManager.register("street_meth", Material.DRIED_KELP, "tesseract:meth5",
                Component.text("Un-pure Crystals", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("Purity: 60%", NamedTextColor.BLUE),
                        Component.text("Cook more to get better", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(0)
                                .saturation(0)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(1.0f)
                                .animation(ItemUseAnimation.BOW)
                                .sound(EMPTY_SOUND)
                                .hasConsumeParticles(false)
                                .build()
                ));

        itemManager.register("double_wash_meth", Material.DRIED_KELP, "tesseract:meth4",
                Component.text("Un-pure Crystals", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("Purity: 80%", NamedTextColor.BLUE),
                        Component.text("Cook more to get better", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(0)
                                .saturation(0)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(1.0f)
                                .animation(ItemUseAnimation.BOW)
                                .sound(EMPTY_SOUND)
                                .hasConsumeParticles(false)
                                .build()
                ));

        itemManager.register("glass_meth", Material.DRIED_KELP, "tesseract:meth3",
                Component.text("Un-pure Crystals", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("Purity: 90%", NamedTextColor.BLUE),
                        Component.text("Cook more to get better", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(0)
                                .saturation(0)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(1.0f)
                                .animation(ItemUseAnimation.BOW)
                                .sound(EMPTY_SOUND)
                                .hasConsumeParticles(false)
                                .build()
                ));

        itemManager.register("blue_stuff_meth", Material.DRIED_KELP, "tesseract:meth2",
                Component.text("Un-pure Crystals", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("Purity: 95%", NamedTextColor.BLUE),
                        Component.text("Cook more to get better", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(0)
                                .saturation(0)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(1.0f)
                                .animation(ItemUseAnimation.BOW)
                                .sound(EMPTY_SOUND)
                                .hasConsumeParticles(false)
                                .build()
                ));

        itemManager.register("heisenberg_meth", Material.DRIED_KELP, "tesseract:meth1",
                mm.deserialize("<gradient:#3A7C85:#67E2F3><bold><italic:false>Sweet Crystals</italic:false></bold></gradient>"),
                meta -> meta.lore(List.of(
                        Component.empty(),
                        Component.text("Purity: 99.3%", NamedTextColor.BLUE),
                        Component.text("Walt likes this", NamedTextColor.DARK_GRAY)
                )),
                ItemManager.DataComponent.of(
                        DataComponentTypes.FOOD,
                        FoodProperties.food()
                                .canAlwaysEat(true)
                                .nutrition(0)
                                .saturation(0)
                                .build()
                ),
                ItemManager.DataComponent.of(
                        DataComponentTypes.CONSUMABLE,
                        Consumable.consumable()
                                .consumeSeconds(1.0f)
                                .animation(ItemUseAnimation.BOW)
                                .sound(EMPTY_SOUND)
                                .hasConsumeParticles(false)
                                .build()
                ));
    }
}