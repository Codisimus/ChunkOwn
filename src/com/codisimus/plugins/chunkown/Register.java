package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.chunkown.register.payment.Method;
import com.codisimus.plugins.chunkown.register.payment.Method.MethodAccount;
import org.bukkit.entity.Player;

/**
 * Manages payment for buying and selling Chunks
 * Using Nijikokun's Register API
 * 
 * @author Codisimus
 */
public class Register {
    public static String economy;
    public static Method econ;
    public static double buyPrice;
    public static double sellPrice;
    public static String insufficientFundsMsg;
    public static String buyMsg;
    public static String sellMsg;

    /**
     * Subtracts the buyPrice from the Player's total balance
     * Returns false if the Player does not have enough money to pay
     * 
     * @param player The Player who is buying
     * @return true if the transaction was successful
     */
    public static boolean buy(Player player) {
        MethodAccount account = econ.getAccount(player.getName());

        //Return false if the Player does not have enough money to pay
        if (!account.hasEnough(buyPrice)) {
            player.sendMessage(insufficientFundsMsg.replaceAll("<price>", Register.format(buyPrice)));
            return false;
        }

        account.subtract(buyPrice);
        player.sendMessage(buyMsg.replaceAll("<price>", Register.format(buyPrice)));
        return true;
    }
    
    /**
     * Adds the sellPrice to the Player's total balance
     * 
     * @param player The Player who is selling
     */
    public static void sell(Player player) {
        econ.getAccount(player.getName()).add(sellPrice);
        player.sendMessage(sellMsg.replaceAll("<price>", Register.format(buyPrice)));
    }

    /**
     * Formats the money amount by adding the unit
     *
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return econ.format(amount);
    }
}
