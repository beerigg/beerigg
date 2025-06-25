package me.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TradePlugin extends JavaPlugin implements Listener {
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    public void onEnable() {
        getCommand("trade").setExecutor(this::tradeCommand);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private boolean tradeCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length != 1) return false;

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target == p) {
            p.sendMessage("§cPlayer not found.");
            return true;
        }

        if (pendingRequests.containsKey(target.getUniqueId()) && pendingRequests.get(target.getUniqueId()).equals(p.getUniqueId())) {
            startTrade(p, target);
            pendingRequests.remove(target.getUniqueId());
            return true;
        }

        pendingRequests.put(p.getUniqueId(), target.getUniqueId());
        p.sendMessage("§aTrade request sent to " + target.getName());
        target.sendMessage("§e" + p.getName() + " wants to trade. Type /trade " + p.getName() + " to accept.");
        return true;
    }

    private void startTrade(Player p1, Player p2) {
        TradeSession session = new TradeSession(p1, p2);
        activeTrades.put(p1.getUniqueId(), session);
        activeTrades.put(p2.getUniqueId(), session);
        session.openInventories();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null) session.handleClick(e);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        TradeSession session = activeTrades.remove(p.getUniqueId());
        if (session != null) session.cancel();
    }

    class TradeSession {
        private final Player p1, p2;
        private final Inventory inv;
        private boolean p1Confirmed = false;
        private boolean p2Confirmed = false;

        TradeSession(Player p1, Player p2) {
            this.p1 = p1;
            this.p2 = p2;
            this.inv = Bukkit.createInventory(null, 54, "§b§lTrade: " + p1.getName() + " ⇄ " + p2.getName());
        }

        void openInventories() {
            updateConfirmButtons();
            p1.openInventory(inv);
            p2.openInventory(inv);
        }

        void handleClick(InventoryClickEvent e) {
            if (!e.getInventory().equals(inv)) return;
            int slot = e.getRawSlot();
            if (slot >= inv.getSize()) return;

            e.setCancelled(true);
            Player clicker = (Player) e.getWhoClicked();

            if (slot == 45 && clicker.equals(p1)) {
                p1Confirmed = true;
                updateConfirmButtons();
                checkComplete();
            } else if (slot == 53 && clicker.equals(p2)) {
                p2Confirmed = true;
                updateConfirmButtons();
                checkComplete();
            } else if (slot == 44 || slot == 52) {
                cancel();
            } else {
                boolean isValidArea = (clicker.equals(p1) && slot >= 0 && slot <= 21)
                                   || (clicker.equals(p2) && slot >= 27 && slot <= 48);
                if (isValidArea) {
                    e.setCancelled(false);
                    p1Confirmed = false;
                    p2Confirmed = false;
                    updateConfirmButtons();
                }
            }
        }

        void updateConfirmButtons() {
            inv.setItem(45, createButton(p1Confirmed ? "§a✔ Ready" : "§7✔ Confirm", Material.LIME_DYE));
            inv.setItem(53, createButton(p2Confirmed ? "§a✔ Ready" : "§7✔ Confirm", Material.LIME_DYE));
            inv.setItem(44, createButton("§c✖ Cancel", Material.BARRIER));
            inv.setItem(52, createButton("§c✖ Cancel", Material.BARRIER));
        }

        void checkComplete() {
            if (p1Confirmed && p2Confirmed) {
                completeTrade();
            }
        }

        void completeTrade() {
            List<ItemStack> itemsP1 = new ArrayList<>();
            List<ItemStack> itemsP2 = new ArrayList<>();

            for (int i = 0; i <= 21; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null) itemsP1.add(item.clone());
            }

            for (int i = 27; i <= 48; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null) itemsP2.add(item.clone());
            }

            giveItems(p1, itemsP2);
            giveItems(p2, itemsP1);
            p1.sendMessage("§aTrade completed!");
            p2.sendMessage("§aTrade completed!");
            p1.closeInventory();
            p2.closeInventory();
            activeTrades.remove(p1.getUniqueId());
            activeTrades.remove(p2.getUniqueId());
        }

        void giveItems(Player p, List<ItemStack> items) {
            for (ItemStack item : items) {
                HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(item);
                for (ItemStack leftover : overflow.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), leftover);
                }
            }
        }

        void cancel() {
            returnItems(p1, 0, 21);
            returnItems(p2, 27, 48);
            p1.sendMessage("§cTrade cancelled.");
            p2.sendMessage("§cTrade cancelled.");
            p1.closeInventory();
            p2.closeInventory();
            activeTrades.remove(p1.getUniqueId());
            activeTrades.remove(p2.getUniqueId());
        }

        void returnItems(Player p, int from, int to) {
            for (int i = from; i <= to; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null) {
                    HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(item);
                    for (ItemStack leftover : overflow.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), leftover);
                    }
                }
            }
        }

        ItemStack createButton(String name, Material mat) {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            item.setItemMeta(meta);
            return item;
        }
    }
}
