package dev.keyshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class KeyShopCommand implements CommandExecutor, TabCompleter {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_\\-]+");

    private final KeyShopPlugin plugin;
    private final KeyManager keys;
    private final ShopGUI gui;

    public KeyShopCommand(KeyShopPlugin plugin, KeyManager keys, ShopGUI gui) {
        this.plugin = plugin;
        this.keys = keys;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /keyshop → open GUI
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.msg("players-only"));
                return true;
            }
            if (!player.hasPermission("keyshop.use")) {
                player.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            gui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "addkey" -> addKey(sender, args);
            case "removekey" -> removeKey(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("keyshop.admin")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                keys.load();
                sender.sendMessage(plugin.msg("reloaded", "%count%", String.valueOf(keys.all().size())));
            }
            default -> sender.sendMessage(plugin.msg("usage"));
        }
        return true;
    }

    private void addKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("keyshop.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("players-only"));
            return;
        }
        if (args.length != 4) {
            sender.sendMessage(plugin.msg("usage"));
            return;
        }

        String name = args[1];
        if (!NAME.matcher(name).matches()) {
            player.sendMessage(plugin.msg("invalid-name"));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(plugin.msg("invalid-price"));
            return;
        }
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
            player.sendMessage(plugin.msg("invalid-price"));
            return;
        }

        int maxSlot = ShopGUI.rows(plugin) * 9 - 1;
        int slot;
        try {
            slot = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            player.sendMessage(plugin.msg("invalid-slot", "%max%", String.valueOf(maxSlot)));
            return;
        }
        if (slot < 0 || slot > maxSlot) {
            player.sendMessage(plugin.msg("invalid-slot", "%max%", String.valueOf(maxSlot)));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(plugin.msg("hold-item"));
            return;
        }

        KeyManager.ShopKey occupant = keys.getBySlot(slot);
        if (occupant != null && !occupant.id().equals(name)) {
            player.sendMessage(plugin.msg("slot-taken", "%slot%", String.valueOf(slot), "%key%", occupant.id()));
            return;
        }

        keys.addKey(name, price, slot, hand);
        player.sendMessage(plugin.msg("key-added",
                "%key%", name, "%price%", String.valueOf(price), "%slot%", String.valueOf(slot)));
    }

    private void removeKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("keyshop.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(plugin.msg("usage"));
            return;
        }
        if (keys.removeKey(args[1])) {
            sender.sendMessage(plugin.msg("key-removed", "%key%", args[1]));
        } else {
            sender.sendMessage(plugin.msg("key-not-found", "%key%", args[1]));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("keyshop.admin")) return out;

        if (args.length == 1) {
            for (String s : List.of("addkey", "removekey", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("removekey")) {
            for (KeyManager.ShopKey k : keys.all()) {
                if (k.id().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(k.id());
            }
        }
        return out;
    }
}
