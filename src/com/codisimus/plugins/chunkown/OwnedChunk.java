package com.codisimus.plugins.chunkown;

import java.util.LinkedList;
import java.util.Properties;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * An OwnedChunk is a Chunk that a Player has bought
 * An OwnedChunk has an Owner and any number of Co-owners/groups
 * The x-coordinate and z-coordinate together create a unique identifier
 *
 * @author Codisimus
 */
public class OwnedChunk {
    public String world;
    public int x;
    public int z;
    public ChunkOwner owner;
    public LinkedList<String> coOwners = new LinkedList<String>();
    public LinkedList<String> groups = new LinkedList<String>();

    /**
     * Constructs a new OwnedChunk
     * 
     * @param chunk The chunk that the OwnedChunks represents
     */
    public OwnedChunk(Chunk chunk, String owner) {
        this.world = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
        setOwner(owner);
    }
    
    /**
     * Constructs a new OwnedChunk
     * 
     * @param world The name of the world of the Chunk
     * @param x The x-coordinate of the Chunk
     * @param z The z-coordinate of the Chunk
     * @param owner The name of the owner of the Chunk
     */
    public OwnedChunk(String world, int x, int z, String owner) {
        this.world = world;
        this.x = x;
        this.z = z;
        setOwner(owner);
    }
    
    /**
     * Sets the ChunkOwner of the OwnedChunk and increments their chunkCounter by 1
     * 
     * @param player The name of the owner of the Chunk
     */
    private void setOwner(String player) {
        owner = ChunkOwn.findOwner(player);
        if (owner == null) {
            owner = new ChunkOwner(player);
        }
        
        owner.chunkCounter++;
    }

    /**
     * Returns whether the given player is a Co-owner
     * Co-owner includes being in a group that has Co-ownership
     * 
     * @param player The Player to be check for Co-ownership
     * @return true if the given player is a Co-owner
     */
    public boolean isCoOwner(Player player) {
        //Check to see if the Player is a Co-owner
        for (String coOwner: owner.coOwners) {
            if (coOwner.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }

        //Return true if the Player is in a group that has Co-ownership
        for (String group: owner.groups) {
            if (ChunkOwn.permission.playerInGroup(player, group)) {
                return true;
            }
        }
        
        //Check to see if the Player is a Co-owner
        for (String coOwner: coOwners) {
            if (coOwner.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }

        //Return true if the Player is in a group that has Co-ownership
        for (String group: groups) {
            if (ChunkOwn.permission.playerInGroup(player, group)) {
                return true;
            }
        }
        
        //Return false because the Player is not a coowner
        return false;
    }
    
    public void save() {
        String coOwnersString = "";
        for (String string: coOwners) {
            coOwnersString = coOwnersString+"'"+string;
        }
        coOwnersString = coOwnersString.isEmpty() ? "none" : coOwnersString.substring(1);
        
        String groupsString = "";
        for (String string: groups) {
            groupsString = groupsString+"'"+string;
        }
        groupsString = groupsString.isEmpty() ? "none" : groupsString.substring(1);
        
        if (!ChunkOwn.savedData.containsKey(world)) {
            ChunkOwn.savedData.put(world, new Properties());
        }
        Properties p = ChunkOwn.savedData.get(world);
        p.setProperty(x+"'"+z, owner.name+","+coOwnersString+","+groupsString);
        
        ChunkOwn.save(world);
    }
    
    /**
     * Saves a snapshot of this Chunk
     * 
     */
    public void saveSnapshot() {
        ChunkOwn.saveSnapshot(world, x, z);
    }
    
    /**
     * Reverts Chunk back to it's saved snapshot
     * 
     */
    public void revert() {
        ChunkOwn.revertChunk(world, x, z);
    }
    
    @Override
    public String toString() {
        return "Chunk @ world="+world+" x="+(x * 16 + 8)+" z="+(z * 16 + 8);
    }
}