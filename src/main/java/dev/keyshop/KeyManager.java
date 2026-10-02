package dev.keyshop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores shop keys in config.yml.
 *
 * Items are saved with Paper's {@link ItemStack#serializeAsBytes()} (Base64), which keeps the
 * complete item: ItemMeta, custom model data, PDC, and all data components/NBT. If that method is
 * unavailable (plain Spigot), Bukkit's ConfigurationSerializable form is used as a fallback.
 */
public final class KeyManager {

    public record ShopKey(String id, double price, int slot, ItemStack item) {}

    private final KeyShopPlugin plugin;
    private final Map<String, ShopKey> keys = new LinkedHashMap<>();

    public KeyManager(KeyShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        keys.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("keys");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                ItemStack item = readItem(s);
                if (item == null || item.getType().isAir()) {
                    plugin.getLogger().warning("Key '" + id + "' has no valid item; skipped.");
                    continue;
                }
                keys.put(id, new ShopKey(id, s.getDouble("price"), s.getInt("slot"), item));
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not load key '" + id + "': " + ex.getMessage());
            }
        }
    }

    public void addKey(String id, double price, int slot, ItemStack heldItem) {
        ItemStack copy = heldItem.clone();
        copy.setAmount(1);

        String path = "keys." + id;
        plugin.getConfig().set(path, null);
        plugin.getConfig().set(path + ".price", price);
        plugin.getConfig().set(path + ".slot", slot);
        writeItem(path, copy);
        plugin.saveConfig();

        keys.put(id, new ShopKey(id, price, slot, copy));
    }

    public boolean removeKey(String id) {
        if (keys.remove(id) == null) return false;
        plugin.getConfig().set("keys." + id, null);
        plugin.saveConfig();
        return true;
    }

    public ShopKey get(String id) {
        return keys.get(id);
    }

    public ShopKey getBySlot(int slot) {
        for (ShopKey k : keys.values()) {
            if (k.slot() == slot) return k;
        }
        return null;
    }

    public Collection<ShopKey> all() {
        return Collections.unmodifiableCollection(keys.values());
    }

    // ── Serialization ────────────────────────────────────────────────

    private void writeItem(String path, ItemStack item) {
        try {
            byte[] bytes = item.serializeAsBytes();
            plugin.getConfig().set(path + ".item-data", Base64.getEncoder().encodeToString(bytes));
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            plugin.getConfig().set(path + ".item", item); // Spigot fallback
        }
    }

    private ItemStack readItem(ConfigurationSection s) {
        String data = s.getString("item-data");
        if (data != null && !data.isBlank()) {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        }
        return s.getItemStack("item");
    }
}
