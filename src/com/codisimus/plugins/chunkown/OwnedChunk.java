package com.codisimus.plugins.chunkown;

import java.util.LinkedList;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * An OwnedChunk is a Chunk that a Player has bought
 * An OwnedChunk has an Owner and any number of CoOwners/groups
 * The x-coordinate and z-coordinate together create a unique identifier
 *
 * @author Codisimus
 */
public class OwnedChunk {
    public String world;
    public int x;
    public int z;
    public String owner;
    public LinkedList<String> coOwners = new LinkedList<String>();
    public LinkedList<String> groups = new LinkedList<String>();

    /**
     * Constructs a new OwnedChunk
     * 
     * @param x The x-coordinate of the Chunk
     * @param z The z-coordinate of the Chunk
     * @param owner The name of the owner of the Chunk
     */
    public OwnedChunk(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    /**
     * Returns whether the given player is a CoOwner
     * CoOwner includes being in a group that has CoOwnership
     * 
     * @param player The Player to be check for CoOwnership
     * @return true if the given player is a CoOwner
     */
    public boolean isCoOwner(Player player) {
        //Check to see if the Player is a CoOwner
        for (String coOwner: coOwners)
            if (coOwner.equalsIgnoreCase(player.getName()))
                return true;

        //Return true if the Player is in a group that has CoOwnerShip
        for (String group: groups)
            if (ChunkOwn.permission.playerInGroup(player, group))
                return true;
        
        //Return false because the Player is not a coowner
        return false;
    }
    
    /**
     * Returns true if there are no neighboring Chunks with the same Owner
     * 
     * @param ownedChunks The list of Chunks to check for neighbors
     * @return True if there are no neighboring Chunks with the same Owner
     */
    public boolean isLoner(LinkedList<Chunk> ownedChunks) {
        for (Chunk chunk: ownedChunks) {
            int a = chunk.getX();
            int c = chunk.getZ();
            
            if (a == x) {
                if (c == z + 1 || c == z - 1)
                    if (chunk.getWorld().getName().equals(world))
                        return false;
            }
            else if (c == z)
                if (a == x + 1 || a == x - 1)
                    if (chunk.getWorld().getName().equals(world))
                        return false;
        }
        
        return true;
    }
    
    public void saveSnapshot() {
        ChunkOwn.saveSnapshot(ChunkOwn.server.getWorld(world), x, z);
    }
    
    public void revert() {
        ChunkOwn.revertChunk(ChunkOwn.server.getWorld(world), x, z);
    }
}