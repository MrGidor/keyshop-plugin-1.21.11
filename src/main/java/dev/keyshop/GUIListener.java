package dev.keyshop;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class GUIListener implements Listener {

    private final KeyShopPlugin plugin;
    private final KeyManager keys;
    private final EconomyHandler economy;

    public GUIListener(KeyShopPlugin plugin, KeyManager keys, EconomyHandler economy) {
        this.plugin = plugin;
        this.keys = keys;
        this.economy = economy;
    }

    private boolean isShop(org.bukkit.inventory.InventoryView view) {
        return view.getTopInventory().getHolder() instanceof KeyShopHolder;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!isShop(e.getView())) return;

        // Cancel EVERYTHING while the shop is open: no taking, placing, shift-clicking,
        // hotbar swaps, double-click collecting, etc.
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null
                || e.getClickedInventory() != e.getView().getTopInventory()) return;

        ClickType type = e.getClick();
        if (type != ClickType.LEFT && type != ClickType.RIGHT
                && type != ClickType.SHIFT_LEFT && type != ClickType.SHIFT_RIGHT) return;

        KeyManager.ShopKey key = keys.getBySlot(e.getSlot());
        if (key == null) return;

        purchase(player, key);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (isShop(e.getView())) e.setCancelled(true);
    }

    private void purchase(Player player, KeyManager.ShopKey key) {
        if (!player.hasPermission("keyshop.use")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (!economy.isAvailable()) {
            player.sendMessage(plugin.msg("economy-unavailable"));
            playSound(player, "sounds.error", Sound.ENTITY_VILLAGER_NO);
            return;
        }

        double price = key.price();
        String priceStr = economy.format(price);

        if (!economy.has(player, price)) {
            player.sendMessage(plugin.msg("insufficient-funds",
                    "%price%", priceStr,
                    "%balance%", economy.format(economy.balance(player)),
                    "%key%", key.id()));
            playSound(player, "sounds.error", Sound.ENTITY_VILLAGER_NO);
            return;
        }

        boolean itemMode = !"command".equalsIgnoreCase(plugin.getConfig().getString("delivery", "item"));
        if (itemMode && !hasSpace(player, key.item())) {
            player.sendMessage(plugin.msg("inventory-full"));
            playSound(player, "sounds.error", Sound.ENTITY_VILLAGER_NO);
            return;
        }

        if (!economy.withdraw(player, price)) {
            player.sendMessage(plugin.msg("insufficient-funds",
                    "%price%", priceStr,
                    "%balance%", economy.format(economy.balance(player)),
                    "%key%", key.id()));
            playSound(player, "sounds.error", Sound.ENTITY_VILLAGER_NO);
            return;
        }

        boolean ok;
        if (itemMode) {
            ItemStack give = key.item().clone();
            give.setAmount(1);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(give);
            ok = leftover.isEmpty();
        } else {
            String command = plugin.getConfig()
                    .getString("purchase-command", "")
                    .replace("%key%", key.id())
                    .replace("%player%", player.getName());
            if (command.startsWith("/")) command = command.substring(1);
            try {
                ok = !command.isBlank() && Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception ex) {
                plugin.getLogger().severe("Error running '" + command + "': " + ex.getMessage());
                ok = false;
            }
        }

        if (!ok) {
            economy.deposit(player, price); // refund
            plugin.getLogger().warning("Delivery failed, refunded " + player.getName() + " for key " + key.id());
            player.sendMessage(plugin.msg("purchase-failed"));
            playSound(player, "sounds.error", Sound.ENTITY_VILLAGER_NO);
            return;
        }

        player.sendMessage(plugin.msg("purchase-success", "%key%", key.id(), "%price%", priceStr));
        playSound(player, "sounds.success", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    /** True if one copy of the item fits in the player's main inventory. */
    private boolean hasSpace(Player player, ItemStack item) {
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) return true;
            if (slot.isSimilar(item) && slot.getAmount() < slot.getMaxStackSize()) return true;
        }
        return false;
    }

    private void playSound(Player player, String path, Sound fallback) {
        Sound sound = fallback;
        String name = plugin.getConfig().getString(path);
        if (name != null) {
            try {
                sound = Sound.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid sound in config at " + path + ": " + name);
            }
        }
        float vol = (float) plugin.getConfig().getDouble("sounds.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds.pitch", 1.0);
        player.playSound(player.getLocation(), sound, vol, pitch);
    }
}
