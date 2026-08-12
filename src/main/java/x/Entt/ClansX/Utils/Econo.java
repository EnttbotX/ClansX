package x.Entt.ClansX.Utils;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import x.Entt.ClansX.CX;

import java.io.File;
import java.util.UUID;

public class Econo {

    private final CX plugin;
    private final FileHandler files;

    private Economy vault;
    private boolean useVault;

    public Econo(CX plugin, FileHandler files) {
        this.plugin = plugin;
        this.files = files;
    }

    public void setupEconomy() {
        String configured = plugin.getConfig()
                .getString("economy.system", "internal")
                .toLowerCase();

        if (!configured.equals("vault")) {
            useVault = false;
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null) {
            useVault = false;
            vault = null;

            Bukkit.getConsoleSender().sendMessage(
                    Text.color(CX.prefix + "&eVault no disponible. Usando Internal.")
            );
            return;
        }

        vault = rsp.getProvider();
        useVault = true;
    }

    public void reload() {
        setupEconomy();
    }

    private boolean vaultActive() {
        return useVault && vault != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (vaultActive()) {
            try {
                return vault.getBalance(player);
            } catch (Exception ignored) {
                fallback();
            }
        }

        return getInternalBalance(player.getUniqueId());
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (amount < 0) return false;

        if (vaultActive()) {
            try {
                return vault.has(player, amount);
            } catch (Exception ignored) {
                fallback();
            }
        }

        return getInternalBalance(player.getUniqueId()) >= amount;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return;

        if (vaultActive()) {
            try {
                vault.depositPlayer(player, amount);
                return;
            } catch (Exception ignored) {
                fallback();
            }
        }

        UUID id = player.getUniqueId();
        setInternalBalance(id, getInternalBalance(id) + amount);
    }

    public void withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0 || !has(player, amount)) return;

        if (vaultActive()) {
            try {
                vault.withdrawPlayer(player, amount);
                return;
            } catch (Exception ignored) {
                fallback();
            }
        }

        UUID id = player.getUniqueId();
        setInternalBalance(id, getInternalBalance(id) - amount);
    }

    private void fallback() {
        useVault = false;
        vault = null;

        Bukkit.getConsoleSender().sendMessage(
                Text.color(CX.prefix + "&cVault falló en runtime. Cambiando a Internal.")
        );
    }

    private double getInternalBalance(UUID uuid) {
        var cfg = files.getPlayerData(uuid);
        if (cfg == null) return 0D;
        return cfg.getDouble("balance", 0D);
    }

    private void setInternalBalance(UUID uuid, double value) {
        var cfg = files.getPlayerData(uuid);

        if (cfg == null)
            cfg = YamlConfiguration.loadConfiguration(
                    new File(files.getPlayersDir(), uuid + ".yml")
            );

        cfg.set("balance", value);
        files.savePlayerData(uuid, cfg);
    }
}