package dev.keyshop;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class KeyShopPlugin extends JavaPlugin {

    private EconomyHandler economy;
    private KeyManager keyManager;
    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.economy = new EconomyHandler();
        if (!economy.setup()) {
            getLogger().warning("No Vault economy provider found yet; will retry when a purchase is attempted.");
        }

        this.keyManager = new KeyManager(this);
        keyManager.load();

        this.shopGUI = new ShopGUI(this, keyManager);

        getServer().getPluginManager().registerEvents(new GUIListener(this, keyManager, economy), this);

        KeyShopCommand executor = new KeyShopCommand(this, keyManager, shopGUI);
        PluginCommand cmd = getCommand("keyshop");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("Loaded " + keyManager.all().size() + " shop key(s).");
    }

    /** Reads a message from config, applies prefix, placeholders (pairs of key,value) and colors. */
    public String msg(String path, String... replacements) {
        String raw = getConfig().getString("messages." + path, "&cMissing message: " + path);
        String prefix = getConfig().getString("messages.prefix", "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return color(prefix + raw);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
