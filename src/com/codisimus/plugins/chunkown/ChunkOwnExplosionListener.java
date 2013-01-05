package com.codisimus.plugins.chunkown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/**
 * Block Explosions within Owned Chunks which have protection
 *
 * @author Codisimus
 */
public class ChunkOwnExplosionListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(ExplosionPrimeEvent event) {
        OwnedChunk chunk = ChunkOwn.findOwnedChunk(event.getEntity().getLocation().getChunk());
    	if(chunk != null && chunk.owner.blockExplosions) {
    		event.setCancelled(true);
    	}
    }
}
