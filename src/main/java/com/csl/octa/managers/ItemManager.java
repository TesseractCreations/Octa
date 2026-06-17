package com.csl.octa.managers;

import io.papermc.paper.datacomponent.DataComponentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ItemManager {

    private final JavaPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final Map<String, CustomItem> items = new HashMap<>();

    public ItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "custom_item_id");
    }

    public void register(String id, Material baseMaterial, String modelPath, Component displayName) {
        register(id, baseMaterial, modelPath, displayName, null, new DataComponent[0]);
    }

    public void register(String id, Material baseMaterial, String modelPath, Component displayName, Consumer<ItemMeta> metaModifier) {
        register(id, baseMaterial, modelPath, displayName, metaModifier, new DataComponent[0]);
    }

    public void register(String id, Material baseMaterial, String modelPath, Component displayName, DataComponent... components) {
        register(id, baseMaterial, modelPath, displayName, null, components);
    }

    public void register(String id, Material baseMaterial, String modelPath, Component displayName, Consumer<ItemMeta> metaModifier, DataComponent... components) {
        items.put(id, new CustomItem(id, baseMaterial, modelPath, displayName, metaModifier, components));
    }

    public ItemStack create(String id) {
        return create(id, 1);
    }

    public ItemStack create(String id, int amount) {
        CustomItem customItem = items.get(id);
        if (customItem == null) return null;

        ItemStack item = new ItemStack(customItem.baseMaterial, amount);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(customItem.displayName.decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);

        if (customItem.modelPath != null && !customItem.modelPath.isEmpty()) {
            meta.setItemModel(NamespacedKey.fromString(customItem.modelPath));
        }

        if (customItem.metaModifier != null) {
            customItem.metaModifier.accept(meta);
        }

        item.setItemMeta(meta);

        for (DataComponent component : customItem.components) {
            component.apply(item);
        }

        return item;
    }

    public void give(Player plr, String id) {
        give(plr, id, 1);
    }

    public void give(Player plr, String id, int amount) {
        ItemStack item = create(id, amount);
        if (item == null) {
            plr.sendMessage(Component.text("Item not found: " + id).color(NamedTextColor.RED));
            return;
        }
        plr.getInventory().addItem(item);
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    public boolean isItem(ItemStack item, String id) {
        String itemId = getItemId(item);
        return itemId != null && itemId.equals(id);
    }

    public boolean isCustomItem(ItemStack item) {
        return getItemId(item) != null;
    }

    public CustomItem getCustomItem(String id) {
        return items.get(id);
    }

    public Map<String, CustomItem> getAllItems() {
        return new HashMap<>(items);
    }

    public static class DataComponent<T> {
        private final DataComponentType.Valued<T> type;
        private final T value;

        private DataComponent(DataComponentType.Valued<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        public static <T> DataComponent<T> of(DataComponentType.Valued<T> type, T value) {
            return new DataComponent<>(type, value);
        }

        public void apply(ItemStack item) {
            item.setData(type, value);
        }
    }

    public static class CustomItem {
        public final String id;
        public final Material baseMaterial;
        public final String modelPath;
        public final Component displayName;
        public final Consumer<ItemMeta> metaModifier;
        public final DataComponent[] components;

        CustomItem(String id, Material baseMaterial, String modelPath, Component displayName, Consumer<ItemMeta> metaModifier, DataComponent... components) {
            this.id = id;
            this.baseMaterial = baseMaterial;
            this.modelPath = modelPath;
            this.displayName = displayName;
            this.metaModifier = metaModifier;
            this.components = components;
        }
    }
}