package com.codisimus.plugins.chunkown.listeners;

import com.codisimus.plugins.chunkown.ChunkOwn;
import org.bukkit.event.block.*;

/**
 * Listens for griefing events
 * 
 * @author Codisimus
 */
public class BlockEventListener extends BlockListener {
    
    /**
     * Blocks within an OwnedChunk can only be broken by the owner, a coowner, or an admin
     * 
     * @param event The BlockBreakEvent that occurred
     */
    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within an OwnedChunk can only be ignited by the owner, a coowner, or an admin
     * 
     * @param event The BlockIgniteEvent that occurred
     */
    @Override
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks can only be placed within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The BlockPlaceEvent that occurred
     */
    @Override
    public void onBlockPlace (BlockPlaceEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within an OwnedChunk cannot be spread
     * 
     * @param event The BlockSpreadEvent that occurred
     */
    @Override
    public void onBlockSpread (BlockSpreadEvent event) {
        if (!ChunkOwn.canBuild(null, event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Signs within an OwnedChunk can only be changed by the owner, a coowner, or an admin
     * 
     * @param event The SignChangeEvent that occurred
     */
    @Override
    public void onSignChange (SignChangeEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }
}
