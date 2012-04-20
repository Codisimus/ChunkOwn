package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.User;
import java.util.LinkedList;
import org.bukkit.entity.Player;

/**
 * An ChunkOwner is a Player that can own Chunks
 *
 * @author Codisimus
 */
public class ChunkOwner {
    public String name;
    public int chunkCounter = 0;
    
    public LinkedList<String> coOwners = new LinkedList<String>();
    public LinkedList<String> groups = new LinkedList<String>();
    
    public int autoOwnBlock = ChunkOwn.defaultAutoOwnBlock;
    
    public static enum AddOn { BLOCKPVP, BLOCKPVE, BLOCKEXPLOSIONS, LOCKCHESTS,
    LOCKDOORS, DISABLEBUTTONS, DISABLEPISTONS, ALARM, HEAL, FEED, NOTIFY }
    
    public boolean blockPvP = Econ.blockPvP == -1;
    public boolean blockPvE = Econ.blockPvE == -1;
    public boolean blockExplosions = Econ.blockExplosions == -1;
    public boolean lockChests = Econ.lockChests == -1;
    public boolean lockDoors = Econ.lockDoors == -1;
    public boolean disableButtons = Econ.disableButtons == -1;
    public boolean disablePistons = Econ.disablePistons == -1;
    public boolean notify = Econ.notify == -1;
    public boolean alarm = Econ.alarm == -1;
    public boolean heal = Econ.heal == -1;
    public boolean feed = Econ.feed == -1;
    
    /**
     * Constructs a new ChunkOwner to represent the given Player
     *
     * @param player The given Player
     */
    public ChunkOwner(String player) {
        name = player;
    }
    
    /**
     * Send the given message to the ChunkOwner
     * If they are offline it will attempt to Text them through TextPlayer
     * 
     * @param msg The message to be sent
     */
    public void sendMessage(String msg) {
        Player player = ChunkOwn.server.getPlayer(name);
        if (player != null)
            player.sendMessage(msg);
        else if (ChunkOwn.pm.isPluginEnabled("TextPlayer")) {
            User user = TextPlayer.findUser(name);
            user.sendText(msg);
        }
    }
    
    /**
     * Returns true if the ChunkOwner has the given AddOn enabled
     * 
     * @param addOn The given AddOn
     * @return True if the ChunkOwner has the given AddOn enabled
     */
    public boolean hasAddOn(AddOn addOn) {
        switch (addOn) {
            case BLOCKPVP: return blockPvP;
            case BLOCKPVE: return blockPvE;
            case BLOCKEXPLOSIONS: return blockExplosions;
            case LOCKCHESTS: return lockChests;
            case LOCKDOORS: return lockDoors;
            case DISABLEBUTTONS: return disableButtons;
            case DISABLEPISTONS: return disablePistons;
            case NOTIFY: return notify;
            case ALARM: return alarm;
            case HEAL: return heal;
            case FEED: return feed;
            default: return false;
        }
    }
    
    /**
     * Sets the status of the given AddOn
     * 
     * @param addOn The given AddOn
     * @param on The new status of the AddOn
     */
    public void setAddOn(AddOn addOn, boolean on) {
        switch (addOn) {
            case BLOCKPVP: blockPvP = on; break;
            case BLOCKPVE: blockPvE = on; break;
            case BLOCKEXPLOSIONS: blockExplosions = on; break;
            case LOCKCHESTS: lockChests = on; break;
            case LOCKDOORS: lockDoors = on; break;
            case DISABLEBUTTONS: disableButtons = on; break;
            case DISABLEPISTONS: disablePistons = on; break;
            case NOTIFY: notify = on; break;
            case ALARM: alarm = on; break;
            case HEAL: heal = on; break;
            case FEED: feed = on; break;
        }
    }
    
    /**
     * Write this ChunkOwner to file
     * 
     */
    public void save() {
        ChunkOwn.saveChunkOwner(this);
    }
}