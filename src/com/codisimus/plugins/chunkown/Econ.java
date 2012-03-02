package com.codisimus.plugins.chunkown;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Manages payment of owning/locking Chests
 * 
 * @author Codisimus
 */
public class Econ {
    public static Economy economy;
    static double buyPrice;
    static double sellPrice;
    static double buyMultiplier;
    static double sellMultiplier;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param source The Player/Bank that will receive the money
     * @param amount The amount that will be charged
     * @return True if the transaction was successful
     */
    public static boolean buy(Player player) {
        String name = player.getName();
        double price = getBuyPrice(name);
        
        if (economy != null) {
            //Cancel if the Player cannot afford the transaction
            if (!economy.has(name, price)) {
                player.sendMessage(ChunkOwnMessages.insufficientFunds.replace("<price>", economy.format(price)));
                return false;
            }

            economy.withdrawPlayer(name, price);
        }
        
        player.sendMessage(ChunkOwnMessages.buy.replace("<price>", economy.format(price)));
        return true;
    }
    
    /**
     * Adds the sellPrice to the Player's total balance
     * 
     * @param player The Player who is selling
     */
    public static void sell(Player player) {
        String name = player.getName();
        String price = economy.format(getSellPrice(name));
        
        sell(name);
        
        player.sendMessage(ChunkOwnMessages.sell.replace("<price>", price));
    }
    
    /**
     * Adds the sellPrice to the Player's total balance
     * 
     * @param name The name of the Player who is selling
     */
    public static void sell(String name) {
        if (economy != null)
            economy.depositPlayer(name, getSellPrice(name));
    }
    
    /**
     * Forces the Player to sell their land
     * 
     * @param admin The Player who is forcing the sale
     * @param seller The Player who is being forced to sell
     */
    public static void sell(Player admin, String owner) {
        sell(owner);
        String price = economy.format(getSellPrice(owner));

        //Notify the Seller
        Player seller = ChunkOwn.server.getPlayer(owner);
        if (seller != null)
            seller.sendMessage(ChunkOwnMessages.adminSold.replace("<price>", price));
        
        admin.sendMessage(ChunkOwnMessages.adminSell.replace("<price>", price));
    }
    
    /**
     * Formats the money amount by adding the unit
     * 
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        if (economy == null)
            return "nothing";
        
        return economy.format(amount).replace(".00", "");
    }
    
    /**
     * Returns the BuyPrice for the given Player
     * 
     * @param player The given Player
     * @return The calculated BuyPrice
     */
    public static double getBuyPrice(String player) {
        if (ChunkOwn.hasPermission(player, "free"))
            return 0;
        
        int owned = 0;
        
        //Retrieve the ChunkCounter value of the Player
        Object object = ChunkOwn.chunkCounter.get(player);
        if (object != null)
            owned = (Integer)object;
        
        return buyPrice * Math.pow(buyMultiplier, owned);
    }
    
    /**
     * Returns the SellPrice for the given Player
     * 
     * @param player The given Player
     * @return The calculated SellPrice
     */
    public static double getSellPrice(String player) {
        if (ChunkOwn.hasPermission(player, "free"))
            return 0;
        
        int owned = 0;
        
        //Retrieve the ChunkCounter value of the Player
        Object object = ChunkOwn.chunkCounter.get(player);
        if (object != null)
            owned = (Integer)object;
        
        return sellPrice * Math.pow(sellMultiplier, owned);
    }
}