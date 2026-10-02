package dev.keyshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ShopGUI {

    private final KeyShopPlugin plugin;
    private final KeyManager keys;

    public ShopGUI(KeyShopPlugin plugin, KeyManager keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public static int rows(KeyShopPlugin plugin) {
        return Math.max(1, Math.min(6, plugin.getConfig().getInt("gui.rows", 3)));
    }

    public void open(Player player) {
        int rows = rows(plugin);
        int size = rows * 9;

        KeyShopHolder holder = new KeyShopHolder();
        Inventory inv = Bukkit.createInventory(holder, size,
                KeyShopPlugin.color(plugin.getConfig().getString("gui.title", "&8Key Shop")));
        holder.setInventory(inv);

        EconomyHandler eco = new EconomyHandler();
        eco.setup();

        for (KeyManager.ShopKey key : keys.all()) {
            if (key.slot() < 0 || key.slot() >= size) {
                plugin.getLogger().warning("Key '" + key.id() + "' slot " + key.slot()
                        + " is outside the GUI (" + size + " slots); not displayed.");
                continue;
            }
            inv.setItem(key.slot(), buildDisplay(key, eco));
        }
        player.openInventory(inv);
    }

    /** Clones the saved item (keeping every tag) and appends the price lore. */
    private ItemStack buildDisplay(KeyManager.ShopKey key, EconomyHandler eco) {
        ItemStack display = key.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        String price = eco.format(key.price());
        for (String line : plugin.getConfig().getStringList("price-lore")) {
            lore.add(KeyShopPlugin.color(line
                    .replace("%price%", price)
                    .replace("%raw_price%", String.valueOf(key.price()))));
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }
}
