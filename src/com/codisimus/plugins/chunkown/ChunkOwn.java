package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.chunkown.listeners.*;
import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Data/Permissions
 * 
 * @author Codisimus
 */
public class ChunkOwn extends JavaPlugin {
    static Server server;
    static Permission permission;
    private static PluginManager pm;
    private Properties p;
    public static int lowerLimit;
    public static String doNotOwnMsg;
    public static Object[][] matrix = new Object[100][100];
    public static HashMap chunkCounter = new HashMap();
    public static int groupSize;
    private static int disownTime;
    private static boolean revertChunks;
    private boolean disablePistons;
    public static Properties lastDaySeen;

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
        
        loadSettings();
        
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
        
        loadData();
        loadLastSeen();
        
        registerEvents();
        getCommand("chunk").setExecutor(new CommandListener());
        
        //Start the tickListener if there is an AutoDisownTimer
        if (disownTime > 0)
            tickListener();
        
        System.out.println("ChunkOwn "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Moves file from ChunkOwn.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    private void moveFile(String fileName) {
        try {
            //Retrieve file from this plugin's .jar
            JarFile jar = new JarFile("plugins/ChunkOwn.jar");
            ZipEntry entry = jar.getEntry(fileName);
            
            //Create the destination folder if it does not exist
            String destination = "plugins/ChunkOwn/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            
            //Copy the file
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
    public void loadSettings() {
        p = new Properties();
        try {
            //Copy the file from the jar if it is missing
            if (!new File("plugins/ChunkOwn/config.properties").exists())
                moveFile("config.properties");
            
            FileInputStream fis = new FileInputStream("plugins/ChunkOwn/config.properties");
            p.load(fis);
            
            Econ.buyPrice = Double.parseDouble(loadValue("BuyPrice"));
            Econ.sellPrice = Double.parseDouble(loadValue("SellPrice"));
            Econ.multiplier = Double.parseDouble(loadValue("BuyMultiplier"));
            
            groupSize = Integer.parseInt(loadValue("MinimumGroupSize"));
            
            lowerLimit = Integer.parseInt(loadValue("OwnLowerLimit"));
            
            CommandListener.cornerID = Integer.parseInt(loadValue("CornerBlockID"));
            
            CommandListener.cooldown = Integer.parseInt(loadValue("PreviewCooldown"));
            
            disownTime = Integer.parseInt(loadValue("AutoDisownTimer"));
            
            revertChunks = Boolean.parseBoolean(loadValue("RevertChunks"));
            
            disablePistons = Boolean.parseBoolean(loadValue("DisableUseOfPistonsInOwnedChunks"));
            
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
            CommandListener.groupLandMsg = format(loadValue("MustGroupLandMessage"));
            
            fis.close();
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
    private String loadValue(String key) {
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
    private void registerEvents() {
        PlayerEventListener playerListener = new PlayerEventListener();
        BlockEventListener blockListener = new BlockEventListener();
        EntityEventListener entityListener = new EntityEventListener();
        VehicleEventListener vehicleListener = new VehicleEventListener();
        pm.registerEvent(Type.PLAYER_BUCKET_EMPTY, playerListener, Priority.Highest, this);
        pm.registerEvent(Type.PLAYER_BUCKET_FILL, playerListener, Priority.Highest, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Highest, this);
        pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_IGNITE, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Highest, this);
        if (disablePistons)
            pm.registerEvent(Type.BLOCK_PISTON_EXTEND, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.SIGN_CHANGE, blockListener, Priority.Highest, this);
        pm.registerEvent(Type.PAINTING_PLACE, entityListener, Priority.Highest, this);
        pm.registerEvent(Type.ENTITY_EXPLODE, entityListener, Priority.Highest, this);
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
    private static String format(String string) {
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
        OwnedChunk ownedChunk = findOwnedChunk(world, x, z);
        
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
    
    /**
     * Reads save file to load ChunkOwn data
     *
     */
    private static void loadData() {
        try {
            //Open save file in BufferedReader
            new File("plugins/ChunkOwn").mkdir();
            new File("plugins/ChunkOwn/chunkown.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ChunkOwn/chunkown.save"));

            //Convert each line into data until all lines are read
            String line;
            while ((line = bReader.readLine()) != null) {
                String[] data = line.split(";");

                //Construct a new OwnedChunk using the World name and x/z coordinates
                OwnedChunk ownedChunk = getOwnedChunk(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]));
                
                //Set the Owner and increment the count of how many Chunks the Player owns
                ownedChunk.owner = data[3];
                int owned = 0;
                Object object = chunkCounter.get(ownedChunk.owner);
                if (object != null)
                    owned = (Integer)object;
                chunkCounter.put(ownedChunk.owner, owned + 1);

                //Convert the coOwners data into a LinkedList for the OwnedChunk
                if (!data[4].equals("none")) 
                    ownedChunk.coOwners = new LinkedList<String>(Arrays.asList(data[4].split(",")));

                //Convert the groups data into a LinkedList for the OwnedChunk
                if (!data[5].equals("none")) 
                    ownedChunk.groups = new LinkedList<String>(Arrays.asList(data[5].split(",")));
            }
            
            bReader.close();
        }
        catch (Exception loadFailed) {
            System.err.println("[ChunkOwn] Load Failed!");
            loadFailed.printStackTrace();
        }
    }
    
    /**
     * Loads last seen data from file
     * 
     */
    public static void loadLastSeen() {
        lastDaySeen = new Properties();
        try {
            //Create the file if it does not exist
            new File("plugins/ChunkOwn").mkdir();
            new File("plugins/ChunkOwn/lastseen.map").createNewFile();
            
            FileInputStream fis = new FileInputStream("plugins/ChunkOwn/lastseen.map");
            lastDaySeen.load(fis);
            fis.close();
        }
        catch (Exception ex) {
        }
    }
    
    /**
     * Writes data to save file
     * Old file is overwritten
     */
    public static void save() {
        try {
            //Open save file for writing data
            BufferedWriter bWriter = new BufferedWriter(new FileWriter("plugins/ChunkOwn/chunkown.save"));
            
            //Iterate through all OwnedChunks to write each to the file
            for (int i = 0; i < 100; i++)
                for (int j = 0; j < 100; j++) {
                    LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)matrix[i][j];
                    if (chunkList != null)
                        for (OwnedChunk ownedChunk: chunkList) {
                            //Write data in format "world;x;z;owner;coOwner1,coOwner2,...;group1,group2,...;
                            bWriter.write(ownedChunk.world.concat(";"));
                            bWriter.write(ownedChunk.x+";");
                            bWriter.write(ownedChunk.z+";");
                            bWriter.write(ownedChunk.owner.concat(";"));

                            if (ownedChunk.coOwners.isEmpty())
                                bWriter.write("none");
                            else
                                for (String coOwner: ownedChunk.coOwners)
                                    bWriter.write(coOwner.concat(","));
                            bWriter.write(";");

                            if (ownedChunk.groups.isEmpty())
                                bWriter.write("none");
                            else
                                for (String group: ownedChunk.groups)
                                    bWriter.write(group.concat(","));
                            bWriter.write(";");

                            //Write each OwnedChunk on its own line
                            bWriter.newLine();
                        }
                }
            bWriter.close();
        }
        catch (Exception saveFailed) {
            System.err.println("[ChunkOwn] Save Failed!");
            saveFailed.printStackTrace();
        }
    }
    
    /**
     * Writes the Map of last seen data to the save file
     * Old file is over written
     */
    public static void saveLastSeen() {
        try {
            lastDaySeen.store(new FileOutputStream("plugins/ChunkOwn/lastseen.map"), null);
        }
        catch (Exception ex) {
        }
    }
    
    /**
     * Returns the OwnedChunk object for the given Chunk
     * It is created if it does not exist
     * 
     * @param world The name of the World the OwnedChunk is in
     * @param x The x-coordinate of the OwnedChunk
     * @param z The z-coordinate of the OwnedChunk
     * @return The OwnedChunk object for the given Chunk
     */
    public static OwnedChunk getOwnedChunk(String world, int x, int z) {
        int row = Math.abs(x % 100);
        int column = Math.abs(z % 100);
        
        //Fetch the ChunkList from the Matrix
        LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)matrix[row][column];
        
        //If the ChunkList doesn't exist, create it
        if (chunkList == null) {
            chunkList = new LinkedList<OwnedChunk>();
            matrix[row][column] = chunkList;
        }
        
        //Iterate through the ChunkList to find the OwnedChunk that matches the Location data
        for (OwnedChunk tempChunk: chunkList)
            if (tempChunk.x == x && tempChunk.z == z && tempChunk.world.equals(world))
                return tempChunk;
        
        //Create the OwnedChunk because it does not exist
        OwnedChunk ownedChunk = new OwnedChunk(world, x, z);
        chunkList.add(ownedChunk);
        return ownedChunk;
    }
    
    /**
     * Returns the OwnedChunk object for the given Chunk
     * returns null if the Chunk is not claimed
     * 
     * @param world The name of the World the OwnedChunk is in
     * @param x The x-coordinate of the OwnedChunk
     * @param z The z-coordinate of the OwnedChunk
     * @return The OwnedChunk object for the given Chunk
     */
    public static OwnedChunk findOwnedChunk(String world, int x, int z) {
        //Retrieve chunkList that the OwnedChunk would be in
        LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)matrix[Math.abs(x % 100)][Math.abs(z % 100)];
        
        //Return null if the chunkList does not exist
        if (chunkList == null)
            return null;
        
        //Iterate through the ChunkList to find the OwnedChunk that matches the Location data
        for (OwnedChunk ownedChunk: chunkList)
            if (ownedChunk.x == x && ownedChunk.z == z && ownedChunk.world.equals(world))
                return ownedChunk;
        
        //Return null because the OwnedChunk does not exist
        return null;
    }
    
    /**
     * Removes the OwnedChunk from the saved data
     * 
     * @param world The name of the World the OwnedChunk is in
     * @param x The x-coordinate of the OwnedChunk
     * @param z The z-coordinate of the OwnedChunk
     */
    public static void removeOwnedChunk(String world, int x, int z) {
        //Retrieve chunkList that the OwnedChunk would be in
        int row = Math.abs(x % 100);
        int column = Math.abs(z % 100);
        LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)matrix[row][column];
        
        //Cancel if the chunkList does not exist
        if (chunkList == null)
            return;
        
        //Iterate through the ChunkList to find the OwnedChunk that matches the Location data
        for (OwnedChunk ownedChunk: chunkList)
            if (ownedChunk.x == x && ownedChunk.z == z && ownedChunk.world.equals(world)) {
                //Decrement the chunkCounter of the owner if there is one
                if (ownedChunk.owner != null)
                    chunkCounter.put(ownedChunk.owner, (Integer)chunkCounter.get(ownedChunk.owner) - 1);
                
                //Remove the OwnedChunk when it is found
                chunkList.remove(ownedChunk);
                
                ownedChunk.revert();
                break;
            }
        
        //Delete the chunkList if it is now empty
        if (chunkList.isEmpty())
            matrix[row][column] = null;
        
        save();
    }
    
    /**
     * Retrieves a list of Chunks that the given Player owns
     * 
     * @param player The name of the given Player
     * @return The list of Chunks
     */
    public static LinkedList<Chunk> getOwnedChunks(String player) {
        //Retrieve the ChunkCounter value of the Player
        int owned = 0;
        Object object = ChunkOwn.chunkCounter.get(player);
        if (object != null)
            owned = (Integer)object;
            
        LinkedList<Chunk> ownedChunks = new LinkedList<Chunk>();
        
        if (owned == 0)
            return ownedChunks;
        
        for (int i = 0; i < 100; i++)
            for (int j = 0; j < 100; j++) {
                LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)matrix[i][j];
                if (chunkList != null)
                    for (OwnedChunk chunk: chunkList)
                        if (chunk.owner != null && chunk.owner.equals(player)) {
                            ownedChunks.add(server.getWorld(chunk.world).getChunkAt(chunk.x, chunk.z));
                            owned--;
                            
                            if (owned == 0)
                                return ownedChunks;
                        }
            }
        
        return ownedChunks;
    }
    
    /**
     * Returns true if all connected Owned Chunks are grouped in sizes that equal or exceed the chunkSize
     * True is returned if specific group sizes are not required
     * True is returned if the ChunkList is empty
     * 
     * @return true if all Chunk groups are large enough
     */
    public static boolean canBuyLoner(LinkedList<Chunk> chunkList) {
        //Return true if specific group sizes are not required
        if (groupSize < 2)
            return true;
        
        while (!chunkList.isEmpty()) {
            //Return false if the group size is not large enough
            if (getSize(chunkList, chunkList.removeFirst()) < groupSize)
                return false;
        }
        
        return true;
    }
    
    /**
     * Returns the total amount of connected Chunks that this Chunk is included in
     * A Chunk is connected if it is adjacent to one of the 4 sides and has the same owner
     * 
     * @return The Chunk Size
     */
    private static int getSize(LinkedList<Chunk> owned, Chunk chunk) {
        int size = 1;
        World world = chunk.getWorld();
        int x = chunk.getX();
        int z = chunk.getZ();
        
        //Compass directions may not be accurate but you get the idea
        //Check for connected Chunks to the North
        chunk = world.getChunkAt(x + 1, z);
        if (owned.remove(chunk))
            size = size + getSize(owned, chunk);
        
        //Check for connected Chunks to the East
        chunk = world.getChunkAt(x, z + 1);
        if (owned.remove(chunk))
            size = size + getSize(owned, chunk);
        
        //Check for connected Chunks to the South
        chunk = world.getChunkAt(x - 1, z);
        if (owned.remove(chunk))
            size = size + getSize(owned, chunk);
        
        //Check for connected Chunks to the West
        chunk = world.getChunkAt(x, z - 1);
        if (owned.remove(chunk))
            size = size + getSize(owned, chunk);
        
        return size;
    }
    
    /**
     * Returns the number of the current day in the AD time period
     * 
     * @return The number of the current day in the AD time period
     */
    public static int getDayAD() {
        Calendar calendar = Calendar.getInstance();
        int yearAD = calendar.get(Calendar.YEAR);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return (int)((yearAD - 1) * 365.4) + dayOfYear;
    }
    
    /**
     * Checks for Players who have not logged on within the given amount of time
     * These Players will have their OwnedChunks automatically disowned
     * Players that do not have any Owned Chunks are ignored
     */
    public void tickListener() {
        //Repeat every day
    	server.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
    	    public void run() {
                int cutoffDay = getDayAD() - disownTime;
                
                for (String key: lastDaySeen.stringPropertyNames())
                    if (Integer.parseInt(lastDaySeen.getProperty(key)) < cutoffDay) {
                        System.out.println("[ChunkOwn] Clearing Chunks that are owned by "+key);
                        CommandListener.clear(key);
                        lastDaySeen.remove(key);
                        saveLastSeen();
                    }
    	    }
    	}, 0L, 1728000L);
    }
    
    public static void saveSnapshot(Chunk chunk) {
        saveSnapshot(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }
    
    public static void saveSnapshot(World world, int chunkX, int chunkZ) {
        if (!revertChunks)
            return;
        
        try {
            File file = new File("plugins/ChunkOwn/Chunk Snapshots/"+world.getName());
            if (!file.exists())
                file.mkdirs();
            
            file = new File("plugins/ChunkOwn/Chunk Snapshots/"+world.getName()+"/"+chunkX+"-"+chunkZ+".cota");
            if (file.exists())
                return;
            else
                file.createNewFile();

            int size = 256 * (128 - lowerLimit);

            byte[] typeArray = new byte[size];
            byte[] dataArray = new byte[size];
            int index = 0;

            int x = 16 * chunkX;
            int z = 16 * chunkZ;

            for (int i = 0; i < 16; i++)
                for (int j = 0; j < 16; j++) {
                    int y = world.getHighestBlockYAt(x, z);
                    if (y >= lowerLimit)
                        index = index + 127 - y;

                    while (y >= lowerLimit) {
                        byte type = (byte)world.getBlockAt(x + i, y, z + j).getTypeId();
                        typeArray[index] = type;
                        
                        byte data = world.getBlockAt(x + i, y, z + j).getData();
                        dataArray[index] = data;
                        
                        index++;
                        y--;
                    }
                }
        
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(typeArray);
            fos.close();
            
            file = new File("plugins/ChunkOwn/Chunk Snapshots/"+world.getName()+"/"+chunkX+"-"+chunkZ+".coda");
            if (!file.exists())
                file.createNewFile();
            
            fos = new FileOutputStream(file);
            fos.write(dataArray);
            fos.close();
        }
        catch (Exception ex) {
            System.err.println("[ChunkOwn] Error when saving Chunk Snapshot...");
            ex.printStackTrace();
        }
    }
    
    public static void revertChunk(Chunk chunk) {
        revertChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }
    
    public static void revertChunk(World world, int chunkX, int chunkZ) {
        if (!revertChunks)
            return;
        
        try {
            File file = new File("plugins/ChunkOwn/Chunk Snapshots/"+world.getName()+"/"+chunkX+"-"+chunkZ+".cota");
            if (!file.exists()) {
                System.out.println("[ChunkOwn] Unable to revert Chunk '"+world.getName()+","+chunkX+","+chunkZ+" because no snapshot was found.");
                return;
            }
            
            int length = (int)file.length();
            
            byte[] typeArray = new byte[length];
            byte[] dataArray = new byte[length];
            
            InputStream is = new FileInputStream(file);
            
            int offset = 0;
            int numRead = 0;
            while (offset < length && (numRead = is.read(typeArray, offset, length - offset)) >= 0)
                offset += numRead;

            // Ensure all the bytes have been read in
            if (offset < length)
                throw new IOException("Could not completely read file "+file.getName());

            // Close the input stream and return bytes
            is.close();
            
            file = new File("plugins/ChunkOwn/Chunk Snapshots/"+world.getName()+"/"+chunkX+"-"+chunkZ+".coda");
            if (!file.exists()) {
                System.out.println("[ChunkOwn] Unable to revert Chunk '"+world.getName()+","+chunkX+","+chunkZ+" because no snapshot was found.");
                return;
            }
            
            is = new FileInputStream(file);
            
            offset = 0;
            numRead = 0;
            while (offset < length && (numRead = is.read(dataArray, offset, length - offset)) >= 0)
                offset += numRead;

            // Ensure all the bytes have been read in
            if (offset < length)
                throw new IOException("Could not completely read file "+file.getName());

            // Close the input stream and return bytes
            is.close();
            
            int lowerLimit = 128 - (length / 256);

            int index = 0;

            int x = 16 * chunkX;
            int z = 16 * chunkZ;

            for (int i = 0; i < 16; i++)
                for (int j = 0; j < 16; j++)
                    for (int y = 127; y >= lowerLimit; y--) {
                        Block block = world.getBlockAt(x + i, y, z + j);
                        block.setTypeIdAndData((int)typeArray[index], dataArray[index], true);
                        
                        index++;
                    }
            
            file.delete();
            new File("plugins/ChunkOwn/Chunk Snapshots/"+world.getName()+"/"+chunkX+"-"+chunkZ+".cota").delete();
        }
        catch (Exception ex) {
            System.err.println("[ChunkOwn] Error when reverting Chunk from Snapshot...");
            ex.printStackTrace();
        }
    }
}