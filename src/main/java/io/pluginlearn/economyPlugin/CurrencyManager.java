package io.pluginlearn.economyPlugin;

import io.pluginlearn.economyPlugin.LeaderboardGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CurrencyManager implements CommandExecutor {
    private final EconomyPlugin plugin;
    private File balanceFile;
    private FileConfiguration balanceConfig;
    private Map<UUID, BalanceEntry> balanceHistory = new HashMap<>();

    public CurrencyManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.balanceFile = new File(plugin.getDataFolder(), "balances.yml");
        this.balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);
        setupBalanceFile();
        startBalanceTracker();
    }




    private void setupBalanceFile() {
        balanceFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!balanceFile.exists()) {
            try {
                balanceFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create balances.yml file!");
                e.printStackTrace();
            }
        }
        balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("leaderboard") || cmd.getName().equalsIgnoreCase("lb")) {
            sendLeaderboard(sender);
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "balance":
                return handleBalanceCommand(sender);
            case "addcurrency":
                return handleAddCurrencyCommand(sender, args);
            default:
                return false;
        }


    }

    private boolean handleBalanceCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        double balance = getBalance(player.getUniqueId());
        player.sendMessage("§aYour current balance: §6" + balance);
        return true;
    }

    private boolean handleAddCurrencyCommand(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("currency.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        // Validate command usage
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /addcurrency <player> <amount>");
            return true;
        }

        // Find target player
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        // Parse amount
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount. Please use a number.");
            return true;
        }

        // Add currency
        addBalance(target.getUniqueId(), amount);
        sender.sendMessage("§aAdded §6" + amount + " §acurrency to §e" + target.getName());

        // Notify target player if online
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            onlineTarget.sendMessage("§aYou received §6" + amount + " §acurrency.");
        }

        return true;
    }

    public double getBalance(UUID playerUUID) {
        ConfigurationSection playerSection = balanceConfig.getConfigurationSection(playerUUID.toString());
        return playerSection != null ? playerSection.getDouble("balance", 0.0) : 0.0;
    }

    public void saveBalances() {
        try {
            balanceConfig.save(balanceFile);
            plugin.getLogger().info("Balances saved successfully to " + balanceFile.getPath());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save balances.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void startBalanceTracker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : balanceHistory.keySet()) {
                    BalanceEntry entry = balanceHistory.get(uuid);
                    double currentBalance = getBalance(uuid);

                    // Update if 24h have passed or balance changed
                    if (System.currentTimeMillis() - entry.lastUpdated >= 86400000 ||
                            currentBalance != entry.currentBalance) {
                        entry.previousBalance = entry.currentBalance;
                        entry.currentBalance = currentBalance;
                        entry.lastUpdated = System.currentTimeMillis();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60 * 60); // Run every hour
    }



    private static class BalanceEntry {
        double currentBalance;
        double previousBalance;
        long lastUpdated;

        BalanceEntry(double balance) {
            this.currentBalance = balance;
            this.previousBalance = balance;
            this.lastUpdated = System.currentTimeMillis();
        }
    }



    private void sendLeaderboard(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // Instead of trying to create a new GUI instance, use an existing one
            plugin.getLeaderboardGUI().openLeaderboard(player, 1);  // Use the openLeaderboard method instead of createInventory
        } else {
            sender.sendMessage("§cThis command can only be used by players.");
        }
    }

    public void updateLeaderboard(Inventory openInventory) {
        plugin.getLeaderboardGUI().updateLeaderboard(openInventory, 1);  // Use the existing updateLeaderboard method
    }


    public void addBalance(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        String playerName = player.getName();

        ConfigurationSection playerSection = balanceConfig.getConfigurationSection(playerUUID.toString());
        if (playerSection == null) {
            playerSection = balanceConfig.createSection(playerUUID.toString());
        }

        double currentBalance = playerSection.getDouble("balance", 0.0);
        playerSection.set("balance", currentBalance + amount);
        playerSection.set("name", playerName);

        saveBalances(); // Save changes to file

        // Refresh leaderboard
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                Inventory openInventory = online.getOpenInventory().getTopInventory();

                /*
                if (event.getView().getTitle().equals("Leaderboard")) {
                    this.updateLeaderboard(openInventory);
                    online.updateInventory();
                }

                 */
            }
        });
    }


    public Map<UUID, Double> getAllBalances() {
        Map<UUID, Double> balances = new HashMap<>();
        for (String key : balanceConfig.getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(key);
                double balance = balanceConfig.getDouble(key + ".balance", 0.0);
                balances.put(playerUUID, balance);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in balances.yml: " + key);
            }
        }
        return balances;
    }

    public double calculateBalanceChange(UUID playerUUID) {
        BalanceEntry entry = balanceHistory.get(playerUUID);
        if (entry == null || entry.previousBalance == 0) return 0;

        return ((entry.currentBalance - entry.previousBalance) / entry.previousBalance * 100);
    }

}