package dev.keyshop;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Thin wrapper around Vault's Economy service. Resolves the provider lazily. */
public final class EconomyHandler {

    private Economy economy;

    public boolean setup() {
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            economy = null;
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public boolean isAvailable() {
        return economy != null || setup();
    }

    public boolean has(OfflinePlayer player, double amount) {
        return isAvailable() && economy.has(player, amount);
    }

    public double balance(OfflinePlayer player) {
        return isAvailable() ? economy.getBalance(player) : 0D;
    }

    /** @return true only if the withdrawal actually succeeded. */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        EconomyResponse r = economy.withdrawPlayer(player, amount);
        return r != null && r.transactionSuccess();
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (isAvailable()) economy.depositPlayer(player, amount);
    }

    public String format(double amount) {
        return isAvailable() ? economy.format(amount) : String.format("%.2f", amount);
    }
}
