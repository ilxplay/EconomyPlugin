package io.pluginlearn.economyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LeaderboardManager {
    private static FileConfiguration balances;
    private static final Map<UUID, Double> lastBalances = new HashMap<>();

    public LeaderboardManager(FileConfiguration balances) {
        this.balances = balances;
    }

    public static List<Map.Entry<UUID, Double>> getSortedLeaderboard() {
        Map<UUID, Double> currencyData = new HashMap<>();
        for (String uuid : balances.getKeys(false)) {
            currencyData.put(UUID.fromString(uuid), balances.getDouble(uuid));
        }
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(currencyData.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // Sort descending
        return sorted;
    }

    public static double getPercentageChange(UUID uuid) {
        double current = balances.getDouble(uuid.toString());
        double last = lastBalances.getOrDefault(uuid, current);
        return ((current - last) / last) * 100;
    }

    public void updateSnapshot() {
        for (String uuid : balances.getKeys(false)) {
            lastBalances.put(UUID.fromString(uuid), balances.getDouble(uuid));
        }
    }

    // Get player name from UUID
    public static String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }



}
