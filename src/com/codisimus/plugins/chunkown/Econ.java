package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.chunkown.ChunkOwner.AddOn;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Manages payment of owning/locking Chests
 *
 * @author Codisimus
 */
public class Econ {
    static Economy economy;
    static double buyPrice;
    static double sellPrice;
    static double buyMultiplier;
    static double sellMultiplier;
    static double moneyBack;

    /* Add-on Prices */
    static double blockPvP;
    static double blockPvE;
    static double blockExplosions;
    static double lockChests;
    static double lockDoors;
    static double disableButtons;
    static double disablePistons;
    static double alarm;
    static double heal;
    static double feed;
    static double notify;
    static double noAutoDisown;

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
                player.sendMessage(ChunkOwnMessages.insufficientFunds.replace("<price>", format(price)));
                return false;
            }

            economy.withdrawPlayer(name, price);
        }

        player.sendMessage(ChunkOwnMessages.buy.replace("<price>", format(price)));
        return true;
    }

    /**
     * Charges the Player the given amount of money
     *
     * @param player The Player being charged
     * @param amount The amount being charged
     * @return true if the transaction was successful;
     */
    public static boolean charge(Player player, double amount) {
        String name = player.getName();

        if (economy != null) {
            //Cancel if the Player cannot afford the transaction
            if (!economy.has(name, amount)) {
                player.sendMessage("You must have "+format(amount)+" to complete the transaction");
                return false;
            }

            economy.withdrawPlayer(name, amount);
        }

        if (amount > 0) {
            player.sendMessage(format(amount)+" has been withdrawn from your account");
        }
        return true;
    }

    /**
     * Refunds the Player the given amount of money
     *
     * @param player The Player being refunded
     * @param amount The amount being refunded
     */
    public static void refund(Player player, double amount) {
        String name = player.getName();

        if (economy != null) {
            economy.depositPlayer(name, amount);
        }

        player.sendMessage(format(amount)+"has been refunded to your account");
    }

    /**
     * Adds the sellPrice to the Player's total balance
     *
     * @param player The Player who is selling
     */
    public static void sell(Player player) {
        String name = player.getName();
        String price = format(getSellPrice(name));

        sell(name);
        player.sendMessage(ChunkOwnMessages.sell.replace("<price>", price));
    }

    /**
     * Adds the sellPrice to the Player's total balance
     *
     * @param name The name of the Player who is selling
     */
    public static void sell(String name) {
        if (economy != null) {
            economy.depositPlayer(name, getSellPrice(name));
        }
    }

    /**
     * Forces the Player to sell their land
     *
     * @param admin The Player who is forcing the sale
     * @param seller The Player who is being forced to sell
     */
    public static void sell(Player admin, String owner) {
        sell(owner);
        String price = format(getSellPrice(owner));

        //Notify the Seller
        Player seller = ChunkOwn.server.getPlayer(owner);
        if (seller != null) {
            seller.sendMessage(ChunkOwnMessages.adminSold.replace("<price>", price));
        }

        admin.sendMessage(ChunkOwnMessages.adminSell.replace("<price>", price));
    }

    /**
     * Formats the money amount by adding the unit
     *
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return economy == null ? "free" : economy.format(amount).replace(".00", "");
    }

    /**
     * Returns the BuyPrice for the given Player
     *
     * @param player The given Player
     * @return The calculated BuyPrice
     */
    public static double getBuyPrice(String player) {
        return ChunkOwn.hasPermission(player, "free")
                ? 0
                : buyPrice * Math.pow(buyMultiplier, ChunkOwn.getChunkCounter(player));
    }

    /**
     * Returns the SellPrice for the given Player
     *
     * @param player The given Player
     * @return The calculated SellPrice
     */
    public static double getSellPrice(String player) {
        return ChunkOwn.hasPermission(player, "free")
                ? 0
                : sellPrice * Math.pow(sellMultiplier, ChunkOwn.getChunkCounter(player));
    }

    /**
     * Returns the BuyPrice for the given Add-on
     *
     * @param addon The given Add-on
     * @return The BuyPrice of the add-on or 0 if the price is -1
     */
    public static double getBuyPrice(AddOn addon) {
        double price;

        switch (addon) {
        case BLOCKPVP: price = blockPvP; break;
        case BLOCKPVE: price = blockPvE; break;
        case BLOCKEXPLOSIONS: price = blockExplosions; break;
        case LOCKCHESTS: price = lockChests; break;
        case LOCKDOORS: price = lockDoors; break;
        case DISABLEBUTTONS: price = disableButtons;  break;
        case DISABLEPISTONS: price = disablePistons; break;
        case ALARM: price = alarm; break;
        case HEAL: price = heal; break;
        case FEED: price = feed; break;
        case NOTIFY: price = notify; break;
        case NOAUTODISOWN: price = noAutoDisown; break;
        default: price = -2; break;
        }

        if (price == -1) {
            price = 0;
        }

        return price;
    }

    /**
     * Returns the SellPrice for the given Add-on
     *
     * @param addon The given Add-on
     * @return The SellPrice of the add-on
     */
    public static double getSellPrice(AddOn addon) {
        return getBuyPrice(addon) * (moneyBack / 100);
    }
}
