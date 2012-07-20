package com.codisimus.plugins.chunkown;

import java.util.Iterator;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Block Explosions within Owned Chunks which have protection
 * 
 * @author Codisimus
 */
public class ChunkOwnExplosionListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> itr = event.blockList().iterator();
        while (itr.hasNext()) {
            
            OwnedChunk chunk = ChunkOwn.findOwnedChunk(itr.next());
            if (chunk != null && chunk.owner.blockExplosions)
                itr.remove();
        }
    }
}