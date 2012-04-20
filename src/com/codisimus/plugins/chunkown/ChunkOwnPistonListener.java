package com.codisimus.plugins.chunkown;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

/**
 * Block Pistons within Owned Chunks which have protection
 * 
 * @author Codisimus
 */
public class ChunkOwnPistonListener implements Listener {
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onEntityExplode(BlockPistonExtendEvent event) {
        for (Block block: event.getBlocks()) {
            OwnedChunk chunk = ChunkOwn.findOwnedChunk(block);
            if (chunk != null && chunk.owner.disablePistons);
                event.setCancelled(true);
        }
    }
}