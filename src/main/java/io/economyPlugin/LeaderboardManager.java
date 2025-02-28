package io.economyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardManager {
    private final EconomyPlugin plugin;
    private FileConfiguration balances;
    private final Map<UUID, Double> lastBalances;
    private long lastUpdateTime;

    public LeaderboardManager(EconomyPlugin plugin, FileConfiguration balances) {
        this.plugin = plugin;
        this.balances = balances;
        this.lastBalances = new ConcurrentHashMap<>();
        this.lastUpdateTime = System.currentTimeMillis();

        scheduleDailyUpdates();

        loadInitialSnapshot();
    }

    private void scheduleDailyUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateSnapshot();
                lastUpdateTime = System.currentTimeMillis();

                saveSnapshotToConfig();

                // log update
                Bukkit.getLogger().info("[EconomyPlugin] Updated 24-hour balance snapshot");
            }
        }.runTaskTimer(plugin, 0L, 20L * 60 * 60 * 24);
    }

    private void loadInitialSnapshot() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("last_balances")) {
            for (String uuid : config.getConfigurationSection("last_balances").getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuid);
                    double amount = config.getDouble("last_balances." + uuid);
                    lastBalances.put(playerUUID, amount);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[EconomyPlugin] Invalid UUID in snapshot: " + uuid);
                }
            }
            lastUpdateTime = config.getLong("last_update_time", System.currentTimeMillis());
        } else {
            updateSnapshot();
            saveSnapshotToConfig();
        }
    }

    private void saveSnapshotToConfig() {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<UUID, Double> entry : lastBalances.entrySet()) {
            config.set("last_balances." + entry.getKey().toString(), entry.getValue());
        }
        config.set("last_update_time", lastUpdateTime);
        plugin.saveConfig();
    }

    public List<Map.Entry<UUID, Double>> getSortedLeaderboard() {
        Map<UUID, Double> currencyData = new HashMap<>();
        for (String uuid : balances.getKeys(false)) {
            try {
                currencyData.put(UUID.fromString(uuid), balances.getDouble(uuid));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[EconomyPlugin] Invalid UUID in balances: " + uuid);
            }
        }
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(currencyData.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted;
    }

    public double getPercentageChange(UUID uuid) {
        double current = balances.getDouble(uuid.toString(), 0.0);
        double last = lastBalances.getOrDefault(uuid, current);

        if (last == 0) {
            return current > 0 ? 100.0 : 0.0;
        }

        return ((current - last) / last) * 100;
    }

    public double getAbsoluteChange(UUID uuid) {
        double current = balances.getDouble(uuid.toString(), 0.0);
        double last = lastBalances.getOrDefault(uuid, current);
        return current - last;
    }

    public void updateSnapshot() {
        for (String uuid : balances.getKeys(false)) {
            try {
                lastBalances.put(UUID.fromString(uuid), balances.getDouble(uuid));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[EconomyPlugin] Invalid UUID while updating snapshot: " + uuid);
            }
        }
    }

    public long getTimeSinceLastUpdate() {
        return System.currentTimeMillis() - lastUpdateTime;
    }

    public String getFormattedTimeSinceUpdate() {
        long timeDiff = getTimeSinceLastUpdate();
        long hours = timeDiff / (1000 * 60 * 60);
        long minutes = (timeDiff % (1000 * 60 * 60)) / (1000 * 60);

        return hours + "h " + minutes + "m";
    }

    public static String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }

    public void setBalances(FileConfiguration balances) {
        this.balances = balances;
    }
}