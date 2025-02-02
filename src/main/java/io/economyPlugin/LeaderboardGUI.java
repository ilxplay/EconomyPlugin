package io.economyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class LeaderboardGUI implements Listener {
    private EconomyPlugin plugin;
    private CurrencyManager currencyManager;
    private Inventory inventory;

    public LeaderboardGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        this.inventory = createInventory();
    }

    public void setCurrencyManager(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "Leaderboard");
    }

    public void openLeaderboard(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.BLUE + "Currency Leaderboard");
        updateLeaderboard(inv, page);
        player.openInventory(inv);
    }

    public void updateLeaderboard(Inventory inv, int page) {
        Map<UUID, Double> balances = currencyManager.getAllBalances();
        List<Map.Entry<UUID, Double>> sortedBalances = balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, sortedBalances.size());

        inv.clear();

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Double> entry = sortedBalances.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            double balance = entry.getValue();

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (skull.getItemMeta() instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(player);
                skullMeta.setDisplayName("§e" + player.getName());
                skullMeta.setLore(List.of("§aBalance: §6$" + balance));
                skull.setItemMeta(skullMeta);
            }

            inv.setItem(slot++, skull);
        }

        if (page > 1) inv.setItem(45, createButton(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        if (endIndex < sortedBalances.size()) inv.setItem(53, createButton(Material.ARROW, ChatColor.YELLOW + "Next Page"));
    }

    private ItemStack createButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.BLUE + "Currency Leaderboard")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int currentPage = getPage(event.getInventory());
            if (displayName.equals(ChatColor.YELLOW + "Previous Page")) {
                openLeaderboard(player, currentPage - 1);
            } else if (displayName.equals(ChatColor.YELLOW + "Next Page")) {
                openLeaderboard(player, currentPage + 1);
            }
        }
    }

    private int getPage(Inventory inventory) {
        return 1; // Needs proper implementation for tracking pages
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void refresh() {
        inventory = createInventory();
        updateLeaderboard(inventory, 1);
        for (HumanEntity viewer : inventory.getViewers()) {
            if (viewer instanceof Player) {
                ((Player) viewer).updateInventory();
            }
        }
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
}
