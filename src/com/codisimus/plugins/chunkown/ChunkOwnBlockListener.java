
package com.codisimus.plugins.chunkown;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Listens for griefing events
 * 
 * @author Cody
 */
public class ChunkOwnBlockListener extends BlockListener {

    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    @Override
    public void onBlockDamage (BlockDamageEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    @Override
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    @Override
    public void onBlockPistonExtend (BlockPistonExtendEvent event) {
        if (!ChunkOwn.canBuild(null, event.getBlocks().get(event.getLength()).getRelative(event.getDirection())))
            event.setCancelled(true);
    }

    @Override
    public void onBlockPlace (BlockPlaceEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    @Override
    public void onBlockSpread (BlockSpreadEvent event) {
        if (!ChunkOwn.canBuild(null, event.getBlock()))
            event.setCancelled(true);
    }

    @Override
    public void onSignChange (SignChangeEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }
}
