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
    private LeaderboardManager leaderboardManager;
    private Inventory inventory;

    public LeaderboardGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        this.leaderboardManager = plugin.getLeaderboardManager();

        this.inventory = createInventory();
    }

    public void setCurrencyManager(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    public void setLeaderboardManager(LeaderboardManager leaderboardManager) {
        this.leaderboardManager = leaderboardManager;
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
        List<Map.Entry<UUID, Double>> sortedBalances = leaderboardManager.getSortedLeaderboard();

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, sortedBalances.size());

        inv.clear();

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Double> entry = sortedBalances.get(i);
            UUID playerUUID = entry.getKey();
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
            double balance = entry.getValue();
            double percentChange = leaderboardManager.getPercentageChange(playerUUID);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (skull.getItemMeta() instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(player);
                skullMeta.setDisplayName("§e" + player.getName());

                List<String> lore = new ArrayList<>();
                lore.add("§aBalance: §6$" + String.format("%.2f", balance));


                String changeColor = percentChange >= 0 ? "§a+" : "§c";
                lore.add("§f24h Change: " + changeColor + String.format("%.2f", percentChange) + "%");

                skullMeta.setLore(lore);
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

            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String displayName = clickedItem.getItemMeta().getDisplayName();
                int currentPage = getPage(event.getInventory());
                if (displayName.equals(ChatColor.YELLOW + "Previous Page")) {
                    openLeaderboard(player, currentPage - 1);
                } else if (displayName.equals(ChatColor.YELLOW + "Next Page")) {
                    openLeaderboard(player, currentPage + 1);
                }
            }
        }
    }

    private int getPage(Inventory inventory) {
        for (int i = 0; i < inventory.getViewers().size(); i++) {
            HumanEntity viewer = inventory.getViewers().get(i);
            if (viewer instanceof Player) {
                if (((Player) viewer).hasMetadata("leaderboard_page")) {
                    return ((Player) viewer).getMetadata("leaderboard_page").get(0).asInt();
                }
            }
        }
        return 1; //to pg 1
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

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}