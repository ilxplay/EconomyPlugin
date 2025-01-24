package io.pluginlearn.economyPlugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class EconomyPlugin extends JavaPlugin {
    private CurrencyManager currencyManager;

    @Override
    public void onEnable() {

        getLogger().info("Enabling EconomyPlugin...");

        currencyManager = new CurrencyManager(this);
        LeaderboardManager leaderboardManager = new LeaderboardManager(this, currencyManager);

        // Register commands
        Objects.requireNonNull(getCommand("balance")).setExecutor(currencyManager);
        Objects.requireNonNull(getCommand("addcurrency")).setExecutor(currencyManager);

        try {
            if (getCommand("balance") != null) {
                Objects.requireNonNull(getCommand("balance")).setExecutor(currencyManager);
            } else {
                getLogger().warning("Could not register /balance command. Check plugin.yml!");
            }

            if (getCommand("addcurrency") != null) {
                getCommand("addcurrency").setExecutor(currencyManager);
            } else {
                getLogger().warning("Could not register /addcurrency command. Check plugin.yml!");
            }
        } catch (Exception e) {
            getLogger().severe("Error registering commands: " + e.getMessage());
            e.printStackTrace();
        }

        if (getCommand("leaderboard") != null) {
            getCommand("leaderboard").setExecutor(currencyManager);
        }

        currencyManager = new CurrencyManager(this);
        TransactionManager transactionManager = new TransactionManager(this, currencyManager);

        // Register commands
        getCommand("pay").setExecutor(transactionManager);
        getCommand("leaderboard").setExecutor(leaderboardManager);

    }

    @Override
    public void onDisable() {
        // Ensure any final saving occurs
        if (currencyManager != null) {
            currencyManager.saveBalances();
        }
    }

    // Optional: Provide access to currency manager from other parts of the plugin
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
}