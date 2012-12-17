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
    public static enum AddOn {
        BLOCKPVP, BLOCKPVE, BLOCKEXPLOSIONS, LOCKCHESTS, LOCKDOORS,
        DISABLEBUTTONS, DISABLEPISTONS, ALARM, HEAL, FEED, NOTIFY,
        NOAUTODISOWN }

    public String name;
    public int chunkCounter = 0;

    public LinkedList<String> coOwners = new LinkedList<String>();
    public LinkedList<String> groups = new LinkedList<String>();

    public int autoOwnBlock = ChunkOwn.defaultAutoOwnBlock;

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
    public boolean noAutoDisown = Econ.noAutoDisown == -1;

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
        if (player != null) {
            player.sendMessage(msg);
        } else if (ChunkOwn.pm.isPluginEnabled("TextPlayer")) {
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
        case NOAUTODISOWN: return noAutoDisown;
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
        case BLOCKPVP:
            blockPvP = on;
            sendMessage("Players "+(on ? "can" : "cannot")+" be hurt by other Players while on your property");
            break;

        case BLOCKPVE:
            blockPvE = on;
            sendMessage("Players "+(on ? "can" : "cannot")+" be hurt by Mobs while on your property");
            break;

        case BLOCKEXPLOSIONS:
            blockExplosions = on;
            sendMessage("Explosions on your property will "+(on ? "be" : "not be")+" neutralized");
            break;

        case LOCKCHESTS:
            lockChests = on;
            sendMessage("Chests/Furnaces/Dispensers on your property will be "+(on ? "locked" : "accessible")+" to non-owners");
            break;

        case LOCKDOORS:
            lockDoors = on;
            sendMessage("Doors on your property will be "+(on ? "locked" : "accessible")+" to non-owners");
            break;

        case DISABLEBUTTONS:
            disableButtons = on;
            sendMessage("Buttons/Levers/Plates on your property will be "+(on ? "disabled" : "enabled")+" to non-owners");
            break;

        case DISABLEPISTONS:
            disablePistons = on;
            sendMessage("Pistons on your property will be "+(on ? "non-functional" : "functional"));
            break;

        case NOTIFY:
            notify = on;
            sendMessage("You will "+(on ? "be" : "not be")+" notified when you enter an Owned Chunk");
            break;

        case ALARM:
            alarm = on;
            sendMessage("You will "+(on ? "be" : "not be")+" notified when a Player enters your property");
            break;

        case HEAL:
            heal = on;
            sendMessage("Players "+(on ? "will" : "will not")+" gain health while on your property");
            break;

        case FEED:
            feed = on;
            sendMessage("Players "+(on ? "will" : "will not")+" gain food while on your property");
            break;

        case NOAUTODISOWN:
            noAutoDisown = on;
            sendMessage("You "+(on ? "will not" : "will")+" lose your claimed land if you become inactive");
            break;
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
