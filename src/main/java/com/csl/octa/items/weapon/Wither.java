package com.csl.octa.items.weapon;

import com.csl.octa.managers.ItemManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

public class Wither {

    private final ItemManager itemManager;

    public Wither(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void register() {
        itemManager.register("wither_mask", Material.PAPER, "tesseract:wither_helmet", Component.text("Wither Mask"),
                ItemManager.DataComponent.of(DataComponentTypes.EQUIPPABLE,
                        Equippable.equippable(EquipmentSlot.HEAD)
                                .assetId(Key.key("tesseract", "wither"))
                                .build()
                ));

        itemManager.register("wither_chestplate", Material.PAPER, "tesseract:wither_chestplate", Component.text("Wither Chestplate"),
                ItemManager.DataComponent.of(DataComponentTypes.EQUIPPABLE,
                        Equippable.equippable(EquipmentSlot.CHEST)
                                .assetId(Key.key("tesseract", "wither"))
                                .build()
                ));

        itemManager.register("wither_leggings", Material.PAPER, "tesseract:wither_leggings", Component.text("Wither Leggings"),
                ItemManager.DataComponent.of(DataComponentTypes.EQUIPPABLE,
                        Equippable.equippable(EquipmentSlot.LEGS)
                                .assetId(Key.key("tesseract", "wither"))
                                .build()
                ));

        itemManager.register("wither_boots", Material.PAPER, "tesseract:wither_boots", Component.text("Wither Boots"),
                ItemManager.DataComponent.of(DataComponentTypes.EQUIPPABLE,
                        Equippable.equippable(EquipmentSlot.FEET)
                                .assetId(Key.key("tesseract", "wither"))
                                .build()
                ));

        itemManager.register("wither_sword", Material.NETHERITE_SWORD, "tesseract:wither_sword", Component.text("Wither Sword"));
        itemManager.register("wither_pickaxe", Material.NETHERITE_PICKAXE, "tesseract:wither_pickaxe", Component.text("Wither Pickaxe"));
        itemManager.register("wither_axe", Material.NETHERITE_AXE, "tesseract:wither_axe", Component.text("Wither Axe"));
        itemManager.register("voidium", Material.PAPER, "tesseract:voidium", Component.text("Voidium"));
    }
}