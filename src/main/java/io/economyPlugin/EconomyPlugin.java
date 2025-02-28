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
    private LeaderboardManager leaderboardManager;
    private TradeSystem tradeSystem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Enabling EconomyPlugin...");

        // Initialize managers in correct order
        balanceFile = new File(getDataFolder(), "balances.yml");
        balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);

        currencyManager = new CurrencyManager(this);

        FileConfiguration balances = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "balances.yml"));
        leaderboardManager = new LeaderboardManager(this, balances);

        leaderboardGUI = new LeaderboardGUI(this);
        leaderboardGUI.setCurrencyManager(currencyManager);
        leaderboardGUI.setLeaderboardManager(leaderboardManager);

        transactionManager = new TransactionManager(this, currencyManager);

        getCommand("leaderboard").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                leaderboardGUI.openLeaderboard(player, 1);
            }
            return true;
        });

        tradeSystem = new TradeSystem(this);
        tradeSystem.onEnable();

        getServer().getPluginManager().registerEvents(leaderboardGUI, this);

        getServer().getScheduler().runTaskTimer(this, leaderboardManager::updateSnapshot, 20L * 60, 20L * 60 * 60);

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

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
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