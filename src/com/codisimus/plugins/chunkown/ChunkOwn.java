package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.chunkown.listeners.CommandListener;
import com.codisimus.plugins.chunkown.listeners.VehicleEventListener;
import com.codisimus.plugins.chunkown.listeners.PlayerEventListener;
import com.codisimus.plugins.chunkown.listeners.EntityEventListener;
import com.codisimus.plugins.chunkown.listeners.BlockEventListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Permissions
 * 
 * @author Codisimus
 */
public class ChunkOwn extends JavaPlugin {
    public static Server server;
    public static Permission permission;
    public static PluginManager pm;
    public Properties p;
    public static int lowerLimit;
    public static String doNotOwnMsg;

    @Override
    public void onDisable () {
    }

    /**
     * Calls methods to load this Plugin when it is enabled
     *
     */
    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        
        loadConfig();
        
        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null)
            permission = permissionProvider.getProvider();
        
        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            Econ.economy = economyProvider.getProvider();
        
        SaveSystem.load();
        
        registerEvents();
        getCommand("chunk").setExecutor(new CommandListener());
        
        System.out.println("ChunkOwn "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Moves file from ChunkOwn.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    public void moveFile(String fileName) {
        try {
            //Retrieve file from this plugin's .jar
            JarFile jar = new JarFile("plugins/ChunkOwn.jar");
            ZipEntry entry = jar.getEntry(fileName);
            
            //Create the destination folder if it does not exist
            String destination = "plugins/ChunkOwn/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            
            //Copy the file
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            while (true) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0)
                    break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
        }
        catch (Exception moveFailed) {
            System.err.println("[ChunkOwn] File Move Failed!");
            moveFailed.printStackTrace();
        }
    }
    
    /**
     * Loads settings from the config.properties file
     * 
     */
    public void loadConfig() {
        p = new Properties();
        try {
            //Copy the file from the jar if it is missing
            if (!new File("plugins/ChunkOwn/config.properties").exists())
                moveFile("config.properties");
            
            p.load(new FileInputStream("plugins/ChunkOwn/config.properties"));
            
            Econ.buyPrice = Double.parseDouble(loadValue("BuyPrice"));
            Econ.sellPrice = Double.parseDouble(loadValue("SellPrice"));
            
            lowerLimit = Integer.parseInt(loadValue("OwnLowerLimit"));
            
            CommandListener.cornerID = Integer.parseInt(loadValue("CornerBlockID"));
            
            doNotOwnMsg = format(loadValue("DoNotOwnMessage"));
            CommandListener.permissionMsg = format(loadValue("PermissionMessage"));
            CommandListener.claimedMsg = format(loadValue("AlreadyClaimedMessage"));
            CommandListener.limitMsg = format(loadValue("LimitReachedMessage"));
            CommandListener.unclaimedMsg = format(loadValue("UnclaimedMessage"));
            CommandListener.buyFreeMsg = format(loadValue("BuyFreeMessage"));
            Econ.insufficientFundsMsg = format(loadValue("InsufficientFundsMessage"));
            Econ.buyMsg = format(loadValue("BuyMessage"));
            Econ.sellMsg = format(loadValue("SellMessage"));
            Econ.adminSellMsg = format(loadValue("AdminSellMessage"));
            Econ.adminSoldMsg = format(loadValue("SoldByAdminMessage"));
        }
        catch (Exception e) {
        }
    }

    /**
     * Loads the given key and prints an error message if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    public String loadValue(String key) {
        //Print an error if the key is not found
        if (!p.containsKey(key)) {
            System.err.println("[ChunkOwn] Missing value for "+key+" in config file");
            System.err.println("[ChunkOwn] Please regenerate config file");
        }
        
        return p.getProperty(key);
    }
    
    /**
     * Registers events for the ChunkOwn Plugin
     *
     */
    public void registerEvents() {
        PlayerEventListener playerListener = new PlayerEventListener();
        BlockEventListener blockListener = new BlockEventListener();
        EntityEventListener entityListener = new EntityEventListener();
        VehicleEventListener vehicleListener = new VehicleEventListener();
        pm.registerEvent(Type.PLAYER_BUCKET_EMPTY, playerListener, Priority.Highest, this);
        pm.registerEvent(Type.PLAYER_BUCKET_FILL, playerListener, Priority.Highest, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_DAMAGE, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_IGNITE, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_SPREAD, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.SIGN_CHANGE, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.PAINTING_PLACE, entityListener, Priority.Highest, this);
        pm.registerEvent(Type.VEHICLE_DAMAGE, vehicleListener, Priority.Highest, this);
        pm.registerEvent(Type.VEHICLE_DESTROY, vehicleListener, Priority.Highest, this);
    }

    /**
     * Returns true if the given Player has the specific permission
     * 
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return True if the given Player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        return permission.has(player, "chunkown."+type);
    }

    /**
     * Returns the Integer value that is the limit of Chunks the given Player can own
     * Returns -1 if there is no limit
     *
     * @param player The Player who is being checked for a limit
     * @return The Integer value that is the limit of Chunks the given Player can own
     */
    public static int getOwnLimit(Player player) {
        //Check for the unlimited node first
        if (hasPermission(player, "limit.-1"))
            return -1;

        //Start at 100 and work down until a limit node is found
        for (int i = 100; i >= 0; i--)
            if (hasPermission(player, "limit."+i))
                return i;

        //No limit if a limit node is not found
        return -1;
    }
    
    /**
     * Adds various Unicode characters and colors to a string
     * 
     * @param string The string being formated
     * @return The formatted String
     */
    public static String format(String string) {
        return string.replaceAll("&", "§").replaceAll("<ae>", "æ").replaceAll("<AE>", "Æ")
                .replaceAll("<o/>", "ø").replaceAll("<O/>", "Ø")
                .replaceAll("<a>", "å").replaceAll("<A>", "Å");
    }
    
    /**
     * Returns true if Player has permission to build*
     * *build also refers to many griefing events**
     * **all events can be found in the ChunkOwn.registerEvents() method
     * 
     * @param player The Player who is trying to build
     * @param block The Block the PlayerEventListener is modifying
     * @return True if Player has permission to 'build'
     */
    public static boolean canBuild(Player player, Block block) {
        //Allow building if Player has admin node
        if (player != null && hasPermission(player, "admin"))
            return true;

        //Allow building if below the lower limit
        if (block.getY() < ChunkOwn.lowerLimit)
            return true;

        String world = block.getWorld().getName();
        Chunk chunk = block.getChunk();
        int x = chunk.getX();
        int z = chunk.getZ();
        
        //See if the area is owned by a Player
        OwnedChunk ownedChunk = SaveSystem.findOwnedChunk(world, x, z);

        //If unowned, deny building if the Player cannot build on unclaimed land
        if (ownedChunk == null)
            if (player != null && hasPermission(player, "mustowntobuild")) {
                player.sendMessage(doNotOwnMsg);
                return false;
            }
            else
                return true;

        //Deny building if no Player can be determined
        if (player == null)
            return false;

        //Allow building if the Player is an Owner or CoOwner of the land
        if (ownedChunk.owner.equals(player.getName()))
            return true;
        else if (ownedChunk.isCoOwner(player))
            return true;
        else {
            player.sendMessage(doNotOwnMsg);
            return false;
        }
    }
}