package com.codisimus.plugins.chunkown;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;
import org.bukkit.World;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class ChunkOwnConfig {
    private static Properties p;

    public static void load() {
        //Load Config settings
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(ChunkOwn.dataFolder + "/config.properties");
            if (!file.exists()) {
                ChunkOwn.plugin.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);

            /* Prices */
            Econ.buyPrice = loadDouble("BuyPrice", 0);
            Econ.sellPrice = loadDouble("SellPrice", 0);
            Econ.buyMultiplier = loadDouble("BuyMultiplier", 1);
            Econ.sellMultiplier = loadDouble("SellMultiplier", 1);
            Econ.blockPvP = loadDouble("BlockPvP", -1);
            Econ.blockPvE = loadDouble("BlockPvE", -2);
            Econ.blockExplosions = loadDouble("BlockExplosions", -2);
            Econ.lockChests = loadDouble("LockChests", -1);
            Econ.lockDoors = loadDouble("LockDoors", 0);
            Econ.disableButtons = loadDouble("DisableButtons", 0);
            Econ.disablePistons = loadDouble("DisablePistons", -1);
            Econ.alarm = loadDouble("AlarmSystem", -2);
            Econ.heal = loadDouble("RegenerateHealth", -2);
            Econ.feed = loadDouble("RegenerateHunger", -2);
            Econ.notify = loadDouble("NotifyWhenInOwnedChunk", 0);
            Econ.noAutoDisown = loadDouble("NoAutoDisown", 0);

            /* Messages */
            String string = "PLUGIN CONFIG MUST BE REGENERATED!";
            ChunkOwnMessages.permission = loadString("PermissionMessage", string);
            ChunkOwnMessages.doNotOwn = loadString("DoNotOwnMessage", string);
            ChunkOwnMessages.claimed = loadString("AlreadyClaimedMessage", string);
            ChunkOwnMessages.limit = loadString("LimitReachedMessage", string);
            ChunkOwnMessages.unclaimed = loadString("UnclaimedMessage", string);
            ChunkOwnMessages.buyFree = loadString("BuyFreeMessage", string);
            ChunkOwnMessages.insufficientFunds = loadString("InsufficientFundsMessage", string);
            ChunkOwnMessages.buy = loadString("BuyMessage", string);
            ChunkOwnMessages.sell = loadString("SellMessage", string);
            ChunkOwnMessages.adminSell = loadString("AdminSellMessage", string);
            ChunkOwnMessages.adminSold = loadString("SoldByAdminMessage", string);
            ChunkOwnMessages.groupLand = loadString("MustGroupLandMessage", string);
            ChunkOwnMessages.worldGuard = loadString("WorldGuardMessage", string);
            ChunkOwnMessages.formatAll();

            /* Other */
            ChunkOwnCommand.edgeID = loadInt("EdgeMarkerID", -1);
            ChunkOwn.defaultAutoOwnBlock = loadInt("AutoOwnBlock", -1);
            ChunkOwn.groupSize = loadInt("MinimumGroupSize", 0);
            ChunkOwn.lowerLimit = loadInt("OwnLowerLimit", 50);
            ChunkOwn.disownTime = loadInt("AutoDisownTimer", 0);
            ChunkOwn.revertChunks = loadBool("RevertChunks", false);
            ChunkOwnMovementListener.rate = loadInt("RegenerateRate", 1);
            ChunkOwnMovementListener.rate = loadInt("RegenerateAmount", 2);
            ChunkOwnCommand.wgSupport = loadBool("WorldGuardSupport", false);

            String data = loadString("EnabledOnlyInWorlds", "");
            if (!data.isEmpty()) {
                for (String s : data.split(", ")) {
                    World world = ChunkOwn.server.getWorld(s);
                    if (world != null) {
                        ChunkOwn.worlds.add(world);
                    }
                }
            }
        } catch (Exception missingProp) {
            ChunkOwn.logger.severe("Failed to load PhatLoots Config");
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private static String loadString(String key, String defaultString) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            ChunkOwn.logger.severe("Missing value for " + key);
            ChunkOwn.logger.severe("Please regenerate the config.properties file (delete the old file to allow a new one to be created)");
            ChunkOwn.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultString;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not an Integer
     *
     * @param key The key to be loaded
     * @return The Integer value of the loaded key
     */
    private static int loadInt(String key, int defaultValue) {
        String string = loadString(key, null);
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            ChunkOwn.logger.severe("The setting for " + key + " must be a valid integer");
            ChunkOwn.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not a Double
     *
     * @param key The key to be loaded
     * @return The Double value of the loaded key
     */
    private static double loadDouble(String key, double defaultValue) {
        String string = loadString(key, null);
        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            ChunkOwn.logger.severe("The setting for " + key + " must be a valid number");
            ChunkOwn.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not a boolean
     *
     * @param key The key to be loaded
     * @return The boolean value of the loaded key
     */
    private static boolean loadBool(String key, boolean defaultValue) {
        String string = loadString(key, null);
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception e) {
            ChunkOwn.logger.severe("The setting for " + key + " must be 'true' or 'false' ");
            ChunkOwn.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }
}
