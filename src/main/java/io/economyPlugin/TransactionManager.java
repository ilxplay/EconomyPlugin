package io.economyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TransactionManager implements CommandExecutor {
    private final EconomyPlugin plugin;
    private File transactionLogFile;
    private FileConfiguration transactionLogConfig;
    private CurrencyManager currencyManager;

    public TransactionManager(EconomyPlugin plugin, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        setupTransactionLogFile();
    }

    private void setupTransactionLogFile() {
        transactionLogFile = new File(plugin.getDataFolder(), "transactions.yml");
        if (!transactionLogFile.exists()) {
            try {
                transactionLogFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create transactions.yml file!");
                e.printStackTrace();
            }
        }
        transactionLogConfig = YamlConfiguration.loadConfiguration(transactionLogFile);
    }


    public boolean processTransaction(Player sender, Player recipient, double amount) {
        // Check sender's balance
        double senderBalance = currencyManager.getBalance(sender.getUniqueId());
        if (senderBalance < amount) {
            sender.sendMessage("§cInsufficient balance.");
            return false;
        }

        // Deduct from sender
        currencyManager.addBalance(sender.getUniqueId(), -amount);

        // Add to recipient
        currencyManager.addBalance(recipient.getUniqueId(), amount);

        // Log transaction
        logTransaction(sender.getUniqueId(), recipient.getUniqueId(), amount);

        // Send confirmation messages
        sender.sendMessage("§aSent §6" + amount + " §ato §e" + recipient.getName());
        recipient.sendMessage("§aReceived §6" + amount + " §afrom §e" + sender.getName());

        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("pay")) {
            // Validate command usage
            if (args.length != 2) {
                player.sendMessage("§cUsage: /pay <player> <amount>");
                return true;
            }

            // Find target player
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found or offline.");
                return true;
            }

            // Parse amount
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
                if (amount <= 0) {
                    player.sendMessage("§cAmount must be positive.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount. Please use a number.");
                return true;
            }

            // Process transaction
            processTransaction(player, target, amount);
            return true;
        }

        return false;
    }



    private void logTransaction(UUID sender, UUID recipient, double amount) {
        String senderName = Bukkit.getOfflinePlayer(sender).getName();
        String recipientName = Bukkit.getOfflinePlayer(recipient).getName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String transactionKey = timestamp + "_" + UUID.randomUUID();

        transactionLogConfig.set(transactionKey + ".sender_uuid", sender.toString());
        transactionLogConfig.set(transactionKey + ".sender_name", senderName);
        transactionLogConfig.set(transactionKey + ".recipient_uuid", recipient.toString());
        transactionLogConfig.set(transactionKey + ".recipient_name", recipientName);
        transactionLogConfig.set(transactionKey + ".amount", amount);
        transactionLogConfig.set(transactionKey + ".timestamp", timestamp);

        try {
            transactionLogConfig.save(transactionLogFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save transaction log!");
            e.printStackTrace();
        }
    }
}