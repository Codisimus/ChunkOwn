package com.codisimus.plugins.chunkown;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

/**
 * Listens for griefing events
 * 
 * @author Codisimus
 */
public class ChunkOwnPistonListener implements Listener {
    
    /**
     * Blocks within an OwnedChunk cannot be effected by pistons
     * 
     * @param event The BlockPistonExtendEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockPistonExtend (BlockPistonExtendEvent event) {
        for (Block block: event.getBlocks())
            if (!ChunkOwn.canBuild(null, block))
                event.setCancelled(true);
    }
}
