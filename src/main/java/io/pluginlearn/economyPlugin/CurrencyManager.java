package io.pluginlearn.economyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CurrencyManager implements CommandExecutor {
    private final EconomyPlugin plugin;
    private File balanceFile;
    private FileConfiguration balanceConfig;
    private Map<UUID, BalanceEntry> balanceHistory = new HashMap<>();

    public CurrencyManager(EconomyPlugin plugin) {
        this.plugin = plugin;
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
        return balanceConfig.getDouble(playerUUID.toString(), 0.0);
    }

    public void addBalance(UUID playerUUID, double amount) {
        double currentBalance = getBalance(playerUUID);
        balanceConfig.set(playerUUID.toString(), currentBalance + amount);
        saveBalances();
    }

    public void saveBalances() {
        try {
            balanceConfig.save(balanceFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save balances.yml!");
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
        // top players sorted by current balance
        List<Map.Entry<String, Double>> topPlayers = balanceConfig.getKeys(false).stream()
                .filter(key -> !key.equals("version")) // Exclude non-player entries
                .map(key -> {
                    String playerName = Bukkit.getOfflinePlayer(UUID.fromString(key)).getName();
                    double balance = balanceConfig.getDouble(key, 0.0);
                    return new AbstractMap.SimpleEntry<>(playerName, balance);
                })
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());


        sender.sendMessage("§6--- §eCurrency Leaderboard §6---");

        //player's stats
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<String, Double> entry = topPlayers.get(i);
            UUID uuid = Bukkit.getOfflinePlayer(entry.getKey()).getUniqueId();

            //percentage change
            BalanceEntry balanceEntry = balanceHistory.getOrDefault(uuid,
                    new BalanceEntry(entry.getValue()));

            double percentageChange = balanceEntry.previousBalance > 0 ?
                    ((entry.getValue() - balanceEntry.previousBalance) / balanceEntry.previousBalance * 100) :
                    0;

            // formatting
            String changeIndicator = percentageChange > 0 ? "§a▲" :
                    percentageChange < 0 ? "§c▼" : "§7-";

            sender.sendMessage(String.format("§e%d. §f%s §7- §6$%.2f §8%s %.2f%%",
                    i + 1,
                    entry.getKey(),
                    entry.getValue(),
                    changeIndicator,
                    Math.abs(percentageChange)
            ));
        }
    }

    public List<Map.Entry<UUID, Double>> getAllBalances() {
        return balanceConfig.getKeys(false).stream()
                .filter(key -> !key.equals("version"))
                .map(key -> new AbstractMap.SimpleEntry<>(
                        UUID.fromString(key),
                        balanceConfig.getDouble(key, 0.0)
                ))
                .collect(Collectors.toList());
    }

    public double calculateBalanceChange(UUID playerUUID) {
        BalanceEntry entry = balanceHistory.get(playerUUID);
        if (entry == null || entry.previousBalance == 0) return 0;

        return ((entry.currentBalance - entry.previousBalance) / entry.previousBalance * 100);
    }

}