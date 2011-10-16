
package com.codisimus.plugins.chunkown;

import java.util.ArrayList;
import java.util.LinkedList;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 *
 * @author Cody
 */
public class ChunkCommand {
    public static int cornerID;
    public static String permissionMsg;
    public static String claimedMsg;
    public static String limitMsg;
    public static String unclaimedMsg;
    public static String buyFreeMsg;
    
    public static void buy(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "own")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Chunk chunk = player.getLocation().getBlock().getChunk();
        OwnedChunk ownedChunk = SaveSystem.getOwnedChunk(player.getWorld().getName(), chunk.getX(), chunk.getZ());

        if (ownedChunk.owner != null) {
            player.sendMessage(claimedMsg);
            return;
        }
        
        ownedChunk.owner = player.getName();

        int limit = ChunkOwn.getOwnLimit(player);
        int owned = 0;

        if (limit != -1) {
            Object object = SaveSystem.chunkCounter.get(player.getName());
            if (object != null)
                owned = (Integer)object;
            
            if (owned >= limit) {
                player.sendMessage(limitMsg);
                return;
            }
        }

        if (ChunkOwn.hasPermission(player, "free"))
            player.sendMessage(buyFreeMsg);
        else if(!Register.buy(player))
            return;
        
        SaveSystem.chunkCounter.put(ownedChunk.owner, owned + 1);

        markCorners(chunk);
        SaveSystem.save();
    }
    
    public static void sell(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "own")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        String world = player.getWorld().getName();
        Chunk chunk = player.getLocation().getBlock().getChunk();
        int x = chunk.getX();
        int z = chunk.getZ();
        OwnedChunk ownedChunk = SaveSystem.findOwnedChunk(world, x, z);

        if (ownedChunk == null || !ownedChunk.owner.equals(player.getName())) {
            player.sendMessage(ChunkOwn.doNotOwnMsg);
            return;
        }

        Register.sell(player);
        SaveSystem.removeOwnedChunk(world, x, z);
    }
    
    public static void list(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "own")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        String name = player.getName();
        int owned = 0;
        
        Object object = SaveSystem.chunkCounter.get(name);
        if (object != null)
            owned = (Integer)object;
        
        player.sendMessage("Number of Chunks owned: "+owned);

        int ownLimit = ChunkOwn.getOwnLimit(player);
        if (ownLimit > -1)
            player.sendMessage("Total amount you may own: "+ownLimit);

        for (int i=0; i<100; i++)
            for (int j=0; j<100; j++) {
                LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)SaveSystem.matrix[i][j];
                if (chunkList != null)
                    for (OwnedChunk ownedChunk: chunkList)
                        if (ownedChunk.owner.equals(name))
                            player.sendMessage("Chunk in world="+ownedChunk.world+", centered @: x="+(ownedChunk.x*16+8)+" z="+(ownedChunk.z*16+8));
            }
    }
    
    public static void info(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "info")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        String world = player.getWorld().getName();
        Chunk chunk = player.getLocation().getBlock().getChunk();
        int x = chunk.getX();
        int z = chunk.getZ();
        OwnedChunk ownedChunk = SaveSystem.findOwnedChunk(world, x, z);

        if (ownedChunk == null) {
            player.sendMessage(unclaimedMsg);
            return;
        }

        player.sendMessage("Chunk @ world="+world+" x="+(x*16+8)+" z="+(z*16+8));

        String coOwners = "CoOwners:  ";
        for (String coOwner: ownedChunk.coOwners)
            coOwners.concat(coOwner.concat(", "));
        player.sendMessage(coOwners.substring(0, coOwners.length() - 2));

        String groups = "CoOwner Groups:  ";
        for (String group: ownedChunk.groups)
            groups.concat(group.concat(", "));
        player.sendMessage(groups.substring(0, groups.length() - 2));
    }
    
    public static void coowner(Player player, String type, String action, String coOwner) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "coowner")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Chunk chunk = player.getLocation().getBlock().getChunk();
        OwnedChunk ownedChunk = SaveSystem.findOwnedChunk(player.getWorld().getName(), chunk.getX(), chunk.getZ());

        if (ownedChunk == null) {
            player.sendMessage(unclaimedMsg);
            return;
        }

        if (!ownedChunk.owner.equals(player.getName())) {
            player.sendMessage(ChunkOwn.doNotOwnMsg);
            return;
        }

        if (type.equals("player"))
            if (action.equals("add")) {
                if (ownedChunk.coOwners.contains(coOwner)) {
                    player.sendMessage(coOwner+" is already a CoOwner");
                    return;
                }
                ownedChunk.coOwners.add(coOwner);
                player.sendMessage(coOwner+" added as a CoOwner");
            }
            else if (action.equals("remove"))
                ownedChunk.coOwners.remove(coOwner);
            else {
                sendHelp(player);
                return;
            }
        else if(type.equals("group"))
            if (action.equals("add")) {
                if (ownedChunk.groups.contains(coOwner)) {
                    player.sendMessage(coOwner+" is already a CoOwner");
                    return;
                }
                ownedChunk.groups.add(coOwner);
                player.sendMessage(coOwner+" added as a CoOwner");
            }
            else if (action.equals("remove"))
                ownedChunk.groups.remove(coOwner);
            else {
                sendHelp(player);
                return;
            }
        else {
            sendHelp(player);
            return;
        }
        SaveSystem.save();
    }
    
    /**
     * Removes all the Chunks that are owned by the given Player
     * A Chunk is owned buy a Player if the owner field is the Player's name
     * 
     * @param player The name of the Player
     */
    public static void clear(Player player) {
        String name = player.getName();
        for (int i=0; i<100; i++)
            for (int j=0; j<100; j++) {
                LinkedList<OwnedChunk> chunkList = (LinkedList<OwnedChunk>)SaveSystem.matrix[i][j];
                if (chunkList != null) {
                    for (OwnedChunk ownedChunk: chunkList)
                        if (ownedChunk.owner.equals(name)) {
                            chunkList.remove(ownedChunk);
                            Register.sell(player);
                        }
                    if (chunkList.isEmpty())
                        SaveSystem.matrix[i][j] = null;
                }
            }
        SaveSystem.chunkCounter.put(player, 0);
        SaveSystem.save();
    }
    
    /**
     * Places Blocks of given type just above the highest Block at each corner of the Chunk
     *
     * @param world The World that the Chunk is in
     * @param x The x-coordinate of the Chunk
     * @param z The z-coordinate of the Chunk
     */
    public static void markCorners(Chunk chunk) {
        Block block = chunk.getBlock(0, 127, 0);
        while (block.getTypeId() == 0)
            block = block.getRelative(BlockFace.DOWN);
        block.getRelative(BlockFace.UP).setTypeId(cornerID);
        
        block = chunk.getBlock(0, 127, 15);
        while (block.getTypeId() == 0)
            block = block.getRelative(BlockFace.DOWN);
        block.getRelative(BlockFace.UP).setTypeId(cornerID);
        
        block = chunk.getBlock(15, 127, 0);
        while (block.getTypeId() == 0)
            block = block.getRelative(BlockFace.DOWN);
        block.getRelative(BlockFace.UP).setTypeId(cornerID);
        
        block = chunk.getBlock(15, 127, 15);
        while (block.getTypeId() == 0)
            block = block.getRelative(BlockFace.DOWN);
        block.getRelative(BlockFace.UP).setTypeId(cornerID);
    }
    
    /**
     * Displays the ChunkOwn Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§e     ChunkOwn Help Page:");
        player.sendMessage("§2/chunk buy§b Purchase the current chunk for "+Register.format(Register.buyPrice));
        player.sendMessage("§2/chunk sell§b Sell the current chunk for "+Register.format(Register.sellPrice));
        player.sendMessage("§2/chunk list§b List locations of owned Chunks");
        player.sendMessage("§2/chunk info§b List Owner and CoOwners of current Chunk");
        player.sendMessage("§2/chunk clear§b Sell all owned Chunks");
        player.sendMessage("§2/chunk coowner [Action] [Type] [Name]");
        player.sendMessage("§bAction = 'add' or 'remove'");
        player.sendMessage("§bType = 'player' or 'group'");
        player.sendMessage("§bName = The group name or the Player's name");
    }
}
