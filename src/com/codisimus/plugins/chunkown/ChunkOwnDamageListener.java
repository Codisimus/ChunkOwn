package com.codisimus.plugins.chunkown;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Block PvP and PvE within Owned Chunks which have protection
 * 
 * @author Codisimus
 */
public class ChunkOwnDamageListener implements Listener {
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;
        
        //Return if the Enitity damaged is not a Player
        Entity wounded = event.getEntity();
        if (!(wounded instanceof Player))
            return;
        
        //Return if the Event was not within an Owned Chunk
        OwnedChunk chunk = ChunkOwn.findOwnedChunk(wounded.getLocation().getBlock());
        if (chunk == null)
            return;

        Entity attacker = event.getDamager();
        if (attacker instanceof Player) { //PvP
            //Return if the Player is suicidal
            if (attacker.equals(wounded))
                return;
            
            //Cancel the Event if PvP protection is enabled
            if (Econ.blockPvP != -2 && chunk.owner.blockPvP)
                event.setCancelled(true);
        }
        else //PvE
            //Cancel the Event if PvE protection is enabled
            if (Econ.blockPvE != -2 && chunk.owner.blockPvE)
                event.setCancelled(true);
    }
}