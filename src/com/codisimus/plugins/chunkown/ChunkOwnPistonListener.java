package com.codisimus.plugins.chunkown;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

/**
 * Block Pistons within Owned Chunks which have protection
 * 
 * @author Codisimus
 */
public class ChunkOwnPistonListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        for (Block block: blocks) {
            OwnedChunk chunk = ChunkOwn.findOwnedChunk(block);
            if (chunk != null && chunk.owner.disablePistons) {
                event.setCancelled(true);
                return;
            }
        }
        
        int size = blocks.size();
        if (size != 0) {
            OwnedChunk chunk = ChunkOwn.findOwnedChunk(blocks.get(size - 1).getRelative(event.getDirection()));
            if (chunk != null && chunk.owner.disablePistons)
                event.setCancelled(true);
        }
    }
    
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isSticky()) {
            OwnedChunk chunk = ChunkOwn.findOwnedChunk(event.getBlock().getRelative(event.getDirection(), 2));
            if (chunk != null && chunk.owner.disablePistons)
                event.setCancelled(true);
        }
    }
}