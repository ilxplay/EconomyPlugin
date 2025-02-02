package io.economyPlugin.tradeSystem;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeSystem implements Listener {
    private final JavaPlugin plugin;
    private final Map<String, List<ListedItem>> listedItems = new HashMap<>();
    private static final String GUI_TITLE = ChatColor.BLUE + "Listed Items";

    public TradeSystem(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {

        plugin.getCommand("list").setExecutor(new ListCommand());
        plugin.getCommand("shop").setExecutor(new ShopCommand());

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    public class ListedItem {
        private final String seller;
        private final ItemStack item;
        private final int amount;

        public ListedItem(String seller, ItemStack item, int amount) {
            this.seller = seller;
            this.item = item;
            this.amount = amount;
        }

        public String getSeller() {
            return seller;
        }

        public ItemStack getItem() {
            return item;
        }

        public int getAmount() {
            return amount;
        }
    }

    public class ListCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /list <item name> <amount>");
                return true;
            }

            Player player = (Player) sender;
            String itemName = args[0].toUpperCase();
            int amount;

            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Amount must be a number!");
                return true;
            }

            Material material = Material.matchMaterial(itemName);
            if (material == null) {
                player.sendMessage(ChatColor.RED + "Invalid item name!");
                return true;
            }

            ItemStack itemToList = new ItemStack(material, amount);

            if (!player.getInventory().containsAtLeast(itemToList, amount)) {
                player.sendMessage(ChatColor.RED + "You don't have enough items!");
                return true;
            }

            player.getInventory().removeItem(itemToList);

            ListedItem listedItem = new ListedItem(player.getName(), itemToList, amount);
            listedItems.computeIfAbsent(player.getName(), k -> new ArrayList<>()).add(listedItem);

            player.sendMessage(ChatColor.GREEN + "Successfully listed " + amount + " " + itemName + " for sale!");
            return true;
        }
    }

    public class ShopCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            openShopGUI(player);
            return true;
        }
    }

    private void openShopGUI(Player player) {
        int size = (listedItems.values().stream().mapToInt(List::size).sum() + 8) / 9 * 9;
        size = Math.min(54, Math.max(9, size));
        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        for (List<ListedItem> items : listedItems.values()) {
            for (ListedItem listedItem : items) {
                ItemStack displayItem = listedItem.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Seller: " + listedItem.getSeller());
                lore.add(ChatColor.YELLOW + "Amount: " + listedItem.getAmount());
                lore.add(ChatColor.GREEN + "Click to purchase!");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                gui.addItem(displayItem);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            String sellerName = lore.get(0).replace(ChatColor.YELLOW + "Seller: ", "");


            List<ListedItem> sellerItems = listedItems.get(sellerName);
            if (sellerItems != null) {
                for (ListedItem listedItem : sellerItems) {
                    if (listedItem.getAmount() == clickedItem.getAmount() &&
                            listedItem.getItem().getType() == clickedItem.getType()) {


                        player.getInventory().addItem(listedItem.getItem().clone());


                        sellerItems.remove(listedItem);
                        if (sellerItems.isEmpty()) {
                            listedItems.remove(sellerName);
                        }

                        player.sendMessage(ChatColor.GREEN + "Successfully purchased items!");
                        player.closeInventory();


                        Player seller = Bukkit.getPlayer(sellerName);
                        if (seller != null) {
                            seller.sendMessage(ChatColor.GREEN + player.getName() + " purchased your " +
                                    listedItem.getAmount() + " " + listedItem.getItem().getType().name());
                        }

                        break;
                    }
                }
            }
        }
    }
}