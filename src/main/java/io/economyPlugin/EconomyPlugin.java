package io.economyPlugin;

import io.economyPlugin.tradeSystem.TradeSystem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class EconomyPlugin extends JavaPlugin {
    private File balanceFile;
    private FileConfiguration balanceConfig;
    private CurrencyManager currencyManager;
    private TransactionManager transactionManager;
    private LeaderboardGUI leaderboardGUI;
    private TradeSystem tradeSystem;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Enabling EconomyPlugin...");

        // Initialize managers in correct order
        balanceFile = new File(getDataFolder(), "balances.yml");
        balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);

        // Initialize managers
        currencyManager = new CurrencyManager(this);
        leaderboardGUI = new LeaderboardGUI(this);
        // Pass 'this' directly
        leaderboardGUI.setCurrencyManager(currencyManager);
        transactionManager = new TransactionManager(this, currencyManager);

        FileConfiguration balances = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "balances.yml"));
        LeaderboardManager leaderboardManager = new LeaderboardManager(balances);

        // Register commands and events
        getCommand("leaderboard").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                leaderboardGUI.openLeaderboard(player, 1);
            }
            return true;
        });
        tradeSystem = new TradeSystem(this); // Initialize the trade system
        tradeSystem.onEnable();

        getServer().getPluginManager().registerEvents(leaderboardGUI, this);

        // Periodic snapshot update
        getServer().getScheduler().runTaskTimer(this, leaderboardManager::updateSnapshot, 20L * 60, 20L * 60);

        // Register commands
        try {
            Objects.requireNonNull(getCommand("balance")).setExecutor(currencyManager);
            Objects.requireNonNull(getCommand("addcurrency")).setExecutor(currencyManager);
            Objects.requireNonNull(getCommand("leaderboard")).setExecutor(currencyManager);
            getCommand("pay").setExecutor(transactionManager);
        } catch (Exception e) {
            getLogger().severe("Error registering commands: " + e.getMessage());
            e.printStackTrace();
        }

        // Periodic save task
        getServer().getScheduler().runTaskTimer(this, () -> {
            currencyManager.saveBalances();
            getLogger().info("Periodic save: Balances saved successfully.");
        }, 20L * 300, 20L * 300);
    }

    public LeaderboardGUI getLeaderboardGUI() {
        return leaderboardGUI;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("Saving balances before shutdown...");
            currencyManager.saveBalances();
            getLogger().info("Balances saved successfully.");
        } catch (Exception e) {
            getLogger().severe("An error occurred while saving balances: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


// Illia Reznikov (c) (aka. ilxplay) all rights reserved
