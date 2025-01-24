package io.pluginlearn.economyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager implements CommandExecutor {
    private final EconomyPlugin plugin;
    private final CurrencyManager currencyManager;

    public LeaderboardManager(EconomyPlugin plugin, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendTextLeaderboard(sender);
            return true;
        }

        Player player = (Player) sender;
        openLeaderboardGUI(player);
        return true;
    }

    private void sendTextLeaderboard(CommandSender sender) {
        List<LeaderboardEntry> topPlayers = getTopPlayers(10);

        sender.sendMessage("§6--- §eCurrency Leaderboard §6---");
        for (int i = 0; i < topPlayers.size(); i++) {
            LeaderboardEntry entry = topPlayers.get(i);
            sender.sendMessage(String.format("§e%d. §f%s §7- §6$%.2f §8(24h: %s%.2f%%§8)",
                    i + 1,
                    entry.playerName,
                    entry.currentBalance,
                    entry.percentageChange > 0 ? "§a+" : "§c",
                    Math.abs(entry.percentageChange)
            ));
        }
    }

    private void openLeaderboardGUI(Player player) {
        List<LeaderboardEntry> topPlayers = getTopPlayers(27);
        Inventory gui = Bukkit.createInventory(null, 27, "§6Economy Leaderboard");

        for (int i = 0; i < topPlayers.size() && i < 27; i++) {
            LeaderboardEntry entry = topPlayers.get(i);
            ItemStack playerHead = createPlayerHead(entry);
            gui.setItem(i, playerHead);
        }

        // Add event listener to prevent inventory modifications and interactions
        Bukkit.getPluginManager().registerEvents(new InventoryListener(player), plugin);

        player.openInventory(gui);
    }




    private ItemStack createPlayerHead(LeaderboardEntry entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        // Set player name and texture
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.playerUUID));

        // Detailed lore
        List<String> lore = new ArrayList<>();
        lore.add("§7Balance: §6$" + String.format("%.2f", entry.currentBalance));
        lore.add("§724h Change: " +
                (entry.percentageChange > 0 ? "§a+" : "§c") +
                String.format("%.2f%%", Math.abs(entry.percentageChange))
        );
        lore.add("§7Rank: §e" + (entry.rank + 1));

        meta.setLore(lore);
        meta.setDisplayName("§e" + entry.playerName);

        head.setItemMeta(meta);
        return head;
    }

    private List<LeaderboardEntry> getTopPlayers(int limit) {
        return currencyManager.getAllBalances().stream()
                .map(entry -> new LeaderboardEntry(
                        entry.getKey(),
                        entry.getValue(),
                        currencyManager.calculateBalanceChange(entry.getKey())
                ))
                .sorted(Comparator
                        .comparing((LeaderboardEntry e) -> e.currentBalance)
                        .reversed()
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Inner class to hold leaderboard entry details
    private static class LeaderboardEntry {
        UUID playerUUID;
        String playerName;
        double currentBalance;
        double percentageChange;
        int rank;

        LeaderboardEntry(UUID uuid, double balance, double change) {
            this.playerUUID = uuid;
            this.playerName = Bukkit.getOfflinePlayer(uuid).getName();
            this.currentBalance = balance;
            this.percentageChange = change;
            this.rank = 0; // Will be set during sorting
        }
    }


    private class InventoryListener implements Listener {
        private final Player player;

        public InventoryListener(Player player) {
            this.player = player;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getView().getTitle().equals("§6Economy Leaderboard")) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (event.getView().getTitle().equals("§6Economy Leaderboard")) {
                HandlerList.unregisterAll(this);
            }
        }
    }
}
