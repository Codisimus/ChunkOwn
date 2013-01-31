package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.chunkown.ChunkOwner.AddOn;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class ChunkOwn extends JavaPlugin {
    static Server server;
    static Logger logger;
    static Permission permission;
    static PluginManager pm;
    static int lowerLimit;
    static HashMap<String, Properties> savedData = new HashMap<String, Properties>();
    static int groupSize;
    static Properties lastDaySeen;
    static Plugin plugin;
    static BukkitScheduler scheduler;
    static int defaultAutoOwnBlock;
    static int disownTime;
    static boolean revertChunks;
    static LinkedList<World> worlds = new LinkedList<World>();
    static String dataFolder;
    private static Properties p;
    private static HashMap<String, OwnedChunk> ownedChunks = new HashMap<String, OwnedChunk>();
    private static HashMap<String, ChunkOwner> chunkOwners = new HashMap<String, ChunkOwner>();

    /**
     * Calls methods to load this Plugin when it is enabled
     *
     */
    @Override
    public void onEnable () {
        server = getServer();
        logger = getLogger();
        pm = server.getPluginManager();
        plugin = this;
        scheduler = server.getScheduler();

        /* Disable this plugin if Vault is not present */
        if (!pm.isPluginEnabled("Vault")) {
            logger.severe("Please install Vault in order to use this plugin!");
            pm.disablePlugin(this);
            return;
        }

        /* Create data folders */
        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dataFolder = dir.getPath();

        dir = new File(dataFolder+"/OwnedChunks");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/Chunk Snapshots");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/ChunkOwners");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        /* Link Permissions/Economy */
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            Econ.economy = economyProvider.getProvider();
        }

        loadAll();

        /* Register Events */
        pm.registerEvents(new ChunkOwnListener(), this);
        if (defaultAutoOwnBlock > 0) {
            pm.registerEvents(new ChunkOwnAutoOwnListener(), this);
        }
        if (Econ.blockPvP != -2 || Econ.blockPvE != -2) {
            pm.registerEvents(new ChunkOwnDamageListener(), this);
        }
        if (Econ.blockExplosions != -2) {
            pm.registerEvents(new ChunkOwnExplosionListener(), this);
        }
        if (Econ.lockChests != -2 || Econ.lockDoors != -2) {
            pm.registerEvents(new ChunkOwnInteractionListener(), this);
        }
        if (Econ.notify != -2 || Econ.alarm != -2 || Econ.heal != -2 || Econ.feed != -2) {
            pm.registerEvents(new ChunkOwnMovementListener(), this);
        }
        if (Econ.disablePistons != -2) {
            pm.registerEvents(new ChunkOwnPistonListener(), this);
        }

        /* Register the command found in the plugin.yml */
        String commands = this.getDescription().getCommands().toString();
        ChunkOwnCommand.command = commands.substring(1, commands.indexOf("="));
        getCommand(ChunkOwnCommand.command).setExecutor(new ChunkOwnCommand());

        /* Schedule repeating tasks */
        scheduleDisowner();
        ChunkOwnCommand.animateSelections();
        ChunkOwnMovementListener.scheduleHealer();
        ChunkOwnMovementListener.scheduleFeeder();

        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
            logger.severe("version.properties file not found within jar");
        }
        logger.info("ChunkOwn "+this.getDescription().getVersion()+" (Build "+version.getProperty("Build")+") is enabled!");
    }

    /**
     * Returns true if the given Player has the specific permission
     *
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return True if the given Player has the specific permission
     */
    static boolean hasPermission(String player, String type) {
        return permission.has(server.getWorlds().get(0), player, "chunkown."+type);
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
     * Returns true if the given Player has permission to buy the given AddOn
     *
     * @param player The Player who is being checked for permission
     * @param addOn The given AddOn
     * @return True if the given Player has the specific permission
     */
    public static boolean hasPermission(Player player, AddOn addOn) {
        if (Econ.getBuyPrice(addOn) == -2) {
            return false;
        } else {
            return hasPermission(player, "addon."+addOn.name().toLowerCase());
        }
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
        if (hasPermission(player, "limit.-1")) {
            return -1;
        }

        //Start at 100 and work down until a limit node is found
        for (int i = 100; i >= 0; i--) {
            if (hasPermission(player, "limit."+i)) {
                return i;
            }
        }

        //No limit if a limit node is not found
        return -1;
    }

    /**
     * Returns true if Player has permission to build*
     * *build also refers to many griefing events**
     * **all events can be found in Listener Classes
     *
     * @param player The Player who is trying to build
     * @param block The Block the ChunkOwnListener is modifying
     * @return True if Player has permission to 'build'
     */
    public static boolean canBuild(Player player, Block block) {
        //Return true if the Block is in a disabled World
        if (!enabledInWorld(block.getWorld())) {
            return true;
        }

        //Allow building if Player has admin node
        if (player != null && hasPermission(player, "admin")) {
            return true;
        }

        //Allow building if below the lower limit
        if (block.getY() < ChunkOwn.lowerLimit) {
            return true;
        }

        //See if the area is owned by a Player
        OwnedChunk ownedChunk = findOwnedChunk(block.getChunk());

        //If unowned, deny building if the Player cannot build on unclaimed land
        if (ownedChunk == null) {
            if (player != null && hasPermission(player, "mustowntobuild")) {
                player.sendMessage(ChunkOwnMessages.doNotOwn);
                return false;
            } else {
                return true;
            }
        }

        //Deny building if no Player can be determined (Mob)
        if (player == null) {
            return false;
        }

        //Allow building if the Player is an Owner or Co-owner of the land
        if (ownedChunk.owner.name.equals(player.getName())) {
            return true;
        } else if (ownedChunk.isCoOwner(player)) {
            return true;
        } else {
            player.sendMessage(ChunkOwnMessages.doNotOwn);
            return false;
        }
    }

    /**
     * Reads save file to load ChunkOwn data for all Worlds
     *
     */
    private static void loadAll() {
        ChunkOwnConfig.load();
        loadChunkOwners();

        for (World world: worlds.isEmpty() ? server.getWorlds() : worlds) {
            loadData(world.getName());
        }

        loadLastSeen();
    }

    /**
     * Loads ChunkOwners from file
     *
     */
    public static void loadChunkOwners() {
        for (File file: new File(dataFolder+"/ChunkOwners/").listFiles()) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                FileInputStream fis = null;
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    fis = new FileInputStream(file);
                    p.load(fis);

                    //Construct a new ChunkOwner using the file name
                    ChunkOwner owner = new ChunkOwner(name.substring(0, name.length() - 11));

                    owner.autoOwnBlock = Integer.parseInt(p.getProperty("AutoOwnBlock"));

                    owner.blockPvP = Boolean.parseBoolean(p.getProperty("BlockPvP"));
                    owner.blockPvE = Boolean.parseBoolean(p.getProperty("BlockPvE"));
                    owner.blockExplosions = Boolean.parseBoolean(p.getProperty("BlockExplosions"));
                    owner.lockChests = Boolean.parseBoolean(p.getProperty("LockChests"));
                    owner.lockDoors = Boolean.parseBoolean(p.getProperty("LockDoors"));
                    owner.disableButtons = Boolean.parseBoolean(p.getProperty("DisableButtons"));
                    owner.disablePistons = Boolean.parseBoolean(p.getProperty("DisablePistons"));
                    owner.alarm = Boolean.parseBoolean(p.getProperty("AlarmSystem"));
                    owner.heal = Boolean.parseBoolean(p.getProperty("RegenerateHealth"));
                    owner.feed = Boolean.parseBoolean(p.getProperty("RegenerateHunger"));
                    owner.notify = Boolean.parseBoolean(p.getProperty("NotifyWhenInOwnedChunk"));

                    //Convert the coOwners data into a LinkedList for the ChunkOwner
                    String data = p.getProperty("CoOwners");
                    if (!data.equals("none")) {
                        owner.coOwners = new LinkedList<String>(Arrays.asList(data.split(",")));
                    }

                    //Convert the groups data into a LinkedList for the ChunkOwner
                    data = p.getProperty("Groups");
                    if (!data.equals("none")) {
                        owner.groups = new LinkedList<String>(Arrays.asList(data.split(",")));
                    }

                    chunkOwners.put(owner.name, owner);
                } catch (Exception loadFailed) {
                    logger.severe("Failed to load "+name);
                    loadFailed.printStackTrace();
                } finally {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * Reads save file to load ChunkOwn data for given World
     *
     */
    private static void loadData(String world) {
        //Cancel if the World is disabled
        World w = server.getWorld(world);
        if (!enabledInWorld(w)) {
            return;
        }

        FileInputStream fis = null;
        try {
            File file = new File(dataFolder + "/OwnedChunks/" + world + ".properties");
            if (!file.exists()) {
                convertOldData(world);
                return;
            }

            fis = new FileInputStream(file);
            Properties data = new Properties();
            data.load(fis);
            savedData.put(world, data);

            for (String key: data.stringPropertyNames()) {
                String[] keyData = key.split("'");
                String[] valueData = data.getProperty(key).split(",");

                //Construct a new OwnedChunk using the World name and x/z coordinates
                OwnedChunk ownedChunk = new OwnedChunk(world, Integer.parseInt(keyData[0]), Integer.parseInt(keyData[1]), valueData[0]);

                //Convert the coOwners data into a LinkedList for the OwnedChunk
                if (!valueData[1].equals("none")) {
                    ownedChunk.coOwners = new LinkedList<String>(Arrays.asList(valueData[1].split("'")));
                }

                //Convert the groups data into a LinkedList for the OwnedChunk
                if (!valueData[2].equals("none")) {
                    ownedChunk.groups = new LinkedList<String>(Arrays.asList(valueData[2].split("'")));
                }

                ownedChunks.put(world + "'" + ownedChunk.x + "'" + ownedChunk.z, ownedChunk);
                ownedChunk.save();
            }
        } catch (Exception loadFailed) {
            logger.severe("Load Failed!");
            loadFailed.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Reads old save file to load ChunkOwn data for given World
     *
     */
    private static void convertOldData(String world) {
        BufferedReader bReader = null;
        try {
            //Open save file in BufferedReader
            File file = new File(dataFolder+"/OwnedChunks/"+world+".coc");
            if (!file.exists()) {
                return;
            }

            logger.severe("Converting outdated Owned Chunks data");
            bReader = new BufferedReader(new FileReader(file));

            //Convert each line into data until all lines are read
            String line;
            while ((line = bReader.readLine()) != null) {
                try {
                    String[] data = line.split(";");

                    //Construct a new OwnedChunk using the World name and x/z coordinates
                    OwnedChunk ownedChunk = new OwnedChunk(world, Integer.parseInt(data[0]), Integer.parseInt(data[1]), data[2]);

                    //Convert the coOwners data into a LinkedList for the OwnedChunk
                    if (!data[3].equals("none"))
                        ownedChunk.coOwners = new LinkedList<String>(Arrays.asList(data[3].split(",")));

                    //Convert the groups data into a LinkedList for the OwnedChunk
                    if (!data[4].equals("none"))
                        ownedChunk.groups = new LinkedList<String>(Arrays.asList(data[4].split(",")));

                    ownedChunks.put(world+"'"+ownedChunk.x+"'"+ownedChunk.z, ownedChunk);
                } catch (Exception corruptedData) {
                    logger.severe("Corrupted data: "+line);
                    corruptedData.printStackTrace();
                    /* Do not load this line */
                }
            }
        } catch (Exception loadFailed) {
            logger.severe("Load Failed!");
            loadFailed.printStackTrace();
        } finally {
            try {
                bReader.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads last seen data from file
     *
     */
    public static void loadLastSeen() {
        lastDaySeen = new Properties();
        FileInputStream fis = null;
        try {
            //Create the file if it does not exist
            File file = new File(dataFolder + "/lastseen.properties");
            File oldFile = new File(dataFolder + "/lastseen.map");
            if (oldFile.exists()) {
                oldFile.renameTo(file);
            }
            if (!file.exists()) {
                file.createNewFile();
            }

            fis = new FileInputStream(file);
            lastDaySeen.load(fis);
        } catch (Exception ex) {
            logger.severe("Failed to load lastseen.properties");
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Reloads ChunkOwn data
     *
     */
    public static void rl() {
        rl(null);
    }

    /**
     * Reloads ChunkOwn data
     *
     * @param player The Player reloading the data
     */
    public static void rl(Player player) {
        savedData.clear();
        ownedChunks.clear();
        chunkOwners.clear();
        loadAll();

        logger.info("reloaded");
        if (player != null) {
            player.sendMessage("§5ChunkOwn reloaded");
        }
    }

    /**
     * Writes data for specified World to save file
     * Old file is overwritten
     */
    public static void save(String world) {
        FileOutputStream fos = null;
        try {
            File file = new File(dataFolder+"/OwnedChunks/"+world+".properties");
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new Exception("File creation failed!");
                }
            }

            fos = new FileOutputStream(file);
            savedData.get(world).store(fos, null);
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Writes the given ChunkOwner to its save file
     * If the file already exists, it is overwritten
     *
     * @param owner The given ChunkOwner
     */
    static void saveChunkOwner(ChunkOwner owner) {
        FileOutputStream fos = null;
        try {
            Properties p = new Properties();

            p.setProperty("AutoOwnBlock", String.valueOf(owner.autoOwnBlock));
            p.setProperty("BlockPvP", String.valueOf(owner.blockPvP));
            p.setProperty("BlockPvE", String.valueOf(owner.blockExplosions));
            p.setProperty("BlockExplosions", String.valueOf(owner.blockExplosions));
            p.setProperty("LockChests", String.valueOf(owner.lockChests));
            p.setProperty("LockDoors", String.valueOf(owner.lockDoors));
            p.setProperty("DisableButtons", String.valueOf(owner.disableButtons));
            p.setProperty("DisablePistons", String.valueOf(owner.disablePistons));
            p.setProperty("AlarmSystem", String.valueOf(owner.alarm));
            p.setProperty("RegenerateHealth", String.valueOf(owner.heal));
            p.setProperty("RegenerateHunger", String.valueOf(owner.feed));
            p.setProperty("NotifyWhenInOwnedChunk", String.valueOf(owner.notify));

            String coOwnersString = "";
            for (String string : owner.coOwners) {
                coOwnersString += "'" + string;
            }
            coOwnersString = coOwnersString.isEmpty() ? "none" : coOwnersString.substring(1);
            p.setProperty("CoOwners", coOwnersString);

            String groupsString = "";
            for (String string : owner.groups) {
                groupsString += "'" + string;
            }
            groupsString = groupsString.isEmpty() ? "none" : groupsString.substring(1);
            p.setProperty("Groups", groupsString);

            //Write the ChunkOwner Properties to file
            fos = new FileOutputStream(dataFolder+"/ChunkOwners/"+owner.name+".properties");
            p.store(fos, null);
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Writes the Map of last seen data to the save file
     * Old file is over written
     */
    public static void saveLastSeen() {
        try {
            lastDaySeen.store(new FileOutputStream(dataFolder.concat("/lastseen.properties")), null);
        } catch (Exception ex) {
            logger.severe("Failed to save lastseen.properties");
        }
    }

    /**
     * Returns the amount of Chunks that the given Player owns
     *
     * @return The amount of Chunks that the given Player owns
     */
    public static int getChunkCounter(String player) {
        ChunkOwner owner = findOwner(player);
        return owner == null ? 0 : owner.chunkCounter;
    }

    /**
     * Returns the ChunkOwner object for the Chunk which contains the given Block
     * returns null if the Chunk is not claimed or if the Block is below the Own Lower Limit
     *
     * @param block The block that may be in an OwnedChunk
     */
    public static ChunkOwner findOwner(Block block) {
        if (block.getY() < lowerLimit) {
            return null;
        }

        return findOwner(block.getChunk());
    }

    /**
     * Returns the ChunkOwner object for the given Chunk
     * returns null if the Chunk is not claimed
     *
     * @param chunk The Chunk that the OwnedChunk would represent
     */
    public static ChunkOwner findOwner(Chunk chunk) {
        OwnedChunk ownedChunk = ownedChunks.get(chunkToString(chunk));
        if (ownedChunk == null) {
            return null;
        }

        return ownedChunk.owner;
    }

    /**
     * Returns the ChunkOwner object for the given Player Name
     * returns null if the ChunkOwner does not exist
     *
     * @param player The ChunkOwner for the given Player Name
     */
    public static ChunkOwner findOwner(String player) {
        return chunkOwners.get(player);
    }

    /**
     * Returns the ChunkOwner object for the given Player Name
     * A new ChunkOwner is created if one does not exist
     *
     * @param player The ChunkOwner for the given Player Name
     */
    public static ChunkOwner getOwner(String player) {
        ChunkOwner owner = findOwner(player);

        if (owner == null) {
            owner = new ChunkOwner(player);
            chunkOwners.put(player, owner);
        }

        return owner;
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
        return findOwnedChunk(server.getWorld(world).getChunkAt(x, z));
    }

    /**
     * Returns the OwnedChunk object for the Chunk which contains the given Block
     * returns null if the Chunk is not claimed or if the Block is below the Own Lower Limit
     *
     * @param block The block that may be in an OwnedChunk
     */
    public static OwnedChunk findOwnedChunk(Block block) {
        return (block.getY() < lowerLimit) ? null : findOwnedChunk(block.getChunk());
    }

    /**
     * Returns the OwnedChunk object for the given Chunk
     * returns null if the Chunk is not claimed
     *
     * @param chunk The Chunk that the OwnedChunk would represent
     */
    public static OwnedChunk findOwnedChunk(Chunk chunk) {
        return ownedChunks.get(chunkToString(chunk));
    }

    /**
     * Adds the OwnedChunk from the saved data
     *
     * @param ownedChunk The OwnedChunk to add
     */
    public static void addOwnedChunk(OwnedChunk ownedChunk) {
        //Cancel if the Chunk is in a disabled World
        World world = server.getWorld(ownedChunk.world);
        if (!enabledInWorld(world)) {
            return;
        }

        ownedChunk.saveSnapshot();
        ownedChunks.put(ownedChunk.world+"'"+ownedChunk.x+"'"+ownedChunk.z, ownedChunk);
        ownedChunk.save();
        ownedChunk.owner.save();
    }

    /**
     * Removes the OwnedChunk from the saved data
     *
     * @param ownedChunk The OwnedChunk to be removed
     */
    public static void removeOwnedChunk(OwnedChunk ownedChunk) {
        Chunk chunk = server.getWorld(ownedChunk.world).getChunkAt(ownedChunk.x, ownedChunk.z);
        removeOwnedChunk(chunk, ownedChunk);
    }

    /**
     * Removes the OwnedChunk from the saved data
     *
     * @param world The name of the World the OwnedChunk is in
     * @param x The x-coordinate of the OwnedChunk
     * @param z The z-coordinate of the OwnedChunk
     */
    public static void removeOwnedChunk(String world, int x, int z) {
        Chunk chunk = server.getWorld(world).getChunkAt(x, z);
        OwnedChunk ownedChunk = ownedChunks.get(chunkToString(chunk));
        removeOwnedChunk(chunk, ownedChunk);
    }

    /**
     * Removes the OwnedChunk from the saved data
     *
     * @param chunk The Chunk that the OwnedChunk would represent
     */
    public static void removeOwnedChunk(Chunk chunk) {
        OwnedChunk ownedChunk = ownedChunks.get(chunkToString(chunk));
        removeOwnedChunk(chunk, ownedChunk);
    }

    /**
     * Removes the OwnedChunk from the saved data
     *
     * @param chunk The Chunk that the OwnedChunk would represent
     */
    private static void removeOwnedChunk(Chunk chunk, OwnedChunk ownedChunk) {
        ownedChunk.revert();
        ownedChunk.owner.chunkCounter--;
        ownedChunks.remove(chunkToString(chunk));
        savedData.get(ownedChunk.world).remove(ownedChunk.x + "'" + ownedChunk.z);
        save(ownedChunk.world);
    }

    /**
     * Retrieves a list of Chunks that the given Player owns
     *
     * @param player The name of the given Player
     * @return The list of Chunks
     */
    public static LinkedList<Chunk> getOwnedChunks(String player) {
        LinkedList<Chunk> chunks = new LinkedList<Chunk>();

        int owned = getChunkCounter(player);
        if (owned == 0) {
            return chunks;
        }

        for (OwnedChunk chunk: ownedChunks.values()) {
            if (chunk.owner.name.equals(player)) {
                chunks.add(server.getWorld(chunk.world).getChunkAt(chunk.x, chunk.z));
                owned--;

                if (owned == 0) {
                    return chunks;
                }
            }
        }

        return chunks;
    }

    /**
     * Returns a Collection of all OwnedChunks
     *
     * @return A Collection of all OwnedChunks
     */
    public static Collection<OwnedChunk> getOwnedChunks() {
        return ownedChunks.values();
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
        if (groupSize < 2) {
            return true;
        }

        while (!chunkList.isEmpty()) {
            //Return false if the group size is not large enough
            if (getSize(chunkList, chunkList.removeFirst()) < groupSize) {
                return false;
            }
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
        if (owned.remove(chunk)) {
            size = size + getSize(owned, chunk);
        }

        //Check for connected Chunks to the East
        chunk = world.getChunkAt(x, z + 1);
        if (owned.remove(chunk)) {
            size = size + getSize(owned, chunk);
        }

        //Check for connected Chunks to the South
        chunk = world.getChunkAt(x - 1, z);
        if (owned.remove(chunk)) {
            size = size + getSize(owned, chunk);
        }

        //Check for connected Chunks to the West
        chunk = world.getChunkAt(x, z - 1);
        if (owned.remove(chunk)) {
            size = size + getSize(owned, chunk);
        }

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
        return (int) ((yearAD - 1) * 365.4) + dayOfYear;
    }

    /**
     * Returns true if this plugin is enabled in the given World
     *
     * @param world The given World
     * @return True if this plugin is enabled in the given World
     */
    public static boolean enabledInWorld(World world) {
        return worlds.isEmpty() || worlds.contains(world);
    }

    /**
     * Checks for Players who have not logged on within the given amount of time
     * These Players will have their OwnedChunks automatically disowned
     * Players that do not have any Owned Chunks are ignored
     */
    public void scheduleDisowner() {
        if (disownTime <= 0) {
            return;
        }

        //Repeat every day
    	server.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
    	    public void run() {
                int cutoffDay = getDayAD() - disownTime;

                for (String key: lastDaySeen.stringPropertyNames()) {
                    if (Integer.parseInt(lastDaySeen.getProperty(key)) < cutoffDay) {
                        if (findOwner(key).hasAddOn(AddOn.NOAUTODISOWN)) {
                            continue;
                        }
                        logger.info("Clearing Chunks that are owned by " + key);
                        ChunkOwnCommand.clear(key);
                        lastDaySeen.remove(key);
                        saveLastSeen();
                    }
                }
    	    }
    	}, 0L, 1728000L);
    }

    /**
     * Saves a snapshot of the given Chunk
     *
     */
    public static void saveSnapshot(Chunk chunk) {
        saveSnapshot(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /**
     * Saves a snapshot of the given chunk
     *
     */
    public static void saveSnapshot(String chunkWorld, int chunkX, int chunkZ) {
        //Return if Reverting Chunks is disabled
        if (!revertChunks) {
            return;
        }

        FileOutputStream fos = null;
        try {
            //Create the Directory if it does not exist
            File file = new File(dataFolder+"/Chunk Snapshots/"+chunkWorld);
            if (!file.isDirectory()) {
                file.mkdirs();
            }

            //Return if there is already a snapshot saved for this Chunk
            file = new File(dataFolder+"/Chunk Snapshots/"+chunkWorld+"/"+chunkX+"-"+chunkZ+".cota");
            if (file.exists()) {
                return;
            } else {
                file.createNewFile();
            }

            //Compute the size of the snapshot area
            World world = server.getWorld(chunkWorld);
            int maxHeight = world.getMaxHeight();
            int size = 256 * (maxHeight - lowerLimit);

            byte[] typeArray = new byte[size];
            byte[] dataArray = new byte[size];
            int index = 0;

            int x = 16 * chunkX;
            int z = 16 * chunkZ;
            /* (x, z) is the corner of the Chunk */

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    int y = world.getHighestBlockYAt(x, z); /* Array stores AIR as default */
                    if (y >= lowerLimit) {
                        index = index + (maxHeight-1) - y; /* Find the correct index in the Array */
                    }

                    while (y >= lowerLimit) {
                        //Store the Block's type ID
                        byte type = (byte)world.getBlockAt(x+i, y, z+j).getTypeId();
                        typeArray[index] = type;

                        //Store the data of the Block
                        byte data = world.getBlockAt(x+i, y, z+j).getData();
                        dataArray[index] = data;

                        index++;
                        y--;
                    }
                }
            }

            //Save the Type Array to file
            fos = new FileOutputStream(file);
            fos.write(typeArray);
            fos.close();

            //Save the Data Array to file
            file = new File(dataFolder+"/Chunk Snapshots/"+chunkWorld+"/"+chunkX+"-"+chunkZ+".coda");
            if (!file.exists()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(file);
            fos.write(dataArray);
        } catch (Exception ex) {
            logger.severe("Error when saving Chunk Snapshot...");
            ex.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Reverts given Chunk back to it's saved snapshot
     *
     */
    public static void revertChunk(Chunk chunk) {
        revertChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /**
     * Reverts given Chunk back to it's saved snapshot
     *
     */
    public static void revertChunk(String chunkWorld, int chunkX, int chunkZ) {
        //Return if Reverting Chunks is disabled
        if (!revertChunks) {
            return;
        }

        InputStream is = null;
        try {
            File file = new File(dataFolder+"/Chunk Snapshots/"+chunkWorld+"/"+chunkX+"-"+chunkZ+".cota");
            if (!file.exists()) {
                logger.info("Unable to revert Chunk '"+chunkWorld+","+chunkX+","+chunkZ+" because no snapshot was found.");
                return;
            }

            int length = (int)file.length();

            byte[] typeArray = new byte[length];
            byte[] dataArray = new byte[length];

            is = new FileInputStream(file);

            int offset = 0;
            int numRead = 0;
            while (offset < length && (numRead = is.read(typeArray, offset, length-offset)) >= 0) {
                offset += numRead;
            }

            //Ensure all the bytes have been read in
            if (offset < length) {
                throw new IOException("Could not completely read file "+file.getName());
            }

            is.close();

            file = new File(dataFolder+"/Chunk Snapshots/"+chunkWorld+"/"+chunkX+"-"+chunkZ+".coda");
            if (!file.exists()) {
                logger.info("Unable to revert Chunk '"+chunkWorld+","+chunkX+","+chunkZ+" because no snapshot was found.");
                return;
            }

            is = new FileInputStream(file);

            offset = 0;
            numRead = 0;
            while (offset < length && (numRead = is.read(dataArray, offset, length-offset)) >= 0) {
                offset += numRead;
            }

            //Ensure all the bytes have been read in
            if (offset < length) {
                throw new IOException("Could not completely read file "+file.getName());
            }

            World world = server.getWorld(chunkWorld);
            int maxHeight = world.getMaxHeight(); /* Hopefully the MaxHeight was not modified */

            int lowerLimit = maxHeight - (length / 256);

            int index = 0;

            int x = 16 * chunkX;
            int z = 16 * chunkZ;

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    for (int y = maxHeight - 1; y >= lowerLimit; y--) {
                        Block block = world.getBlockAt(x+i, y, z+j);
                        block.setTypeIdAndData((int)typeArray[index], dataArray[index], true);
                        index++;
                    }
                }
            }

            //Delete both snapshot files
            file.delete();
            new File(dataFolder+"/Chunk Snapshots/"+chunkWorld+"/"+chunkX+"-"+chunkZ+".cota").delete();
        } catch (Exception ex) {
            logger.severe("Error when reverting Chunk from Snapshot...");
            ex.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    static String chunkToString(Chunk chunk) {
        return chunk.getWorld().getName()+"'"+chunk.getX()+"'"+chunk.getZ();
    }

    public static LinkedList<Block> getBlocks(Chunk chunk) {
        LinkedList<Block> blockList = new LinkedList<Block>();
        World w = chunk.getWorld();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = lowerLimit; y < w.getMaxHeight(); y++) {
                    blockList.add(w.getBlockAt(x, y, z));
                }
            }
        }
        return blockList;
    }
}
