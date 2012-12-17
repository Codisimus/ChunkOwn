package com.codisimus.plugins.chunkown;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * If a specific Block is placed, then the Chunk will be automatically owned
 *
 * @author Codisimus
 */
public class ChunkOwnAutoOwnListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        //Return if the Player is in a disabled World
        Player player = event.getPlayer();
        if (!ChunkOwn.enabledInWorld(player.getWorld())) {
            return;
        }

        Block block = event.getBlock();

        //Return if the Chunk is already Owned
        OwnedChunk chunk = ChunkOwn.findOwnedChunk(block);
        if (chunk != null) {
            return;
        }

        ChunkOwner owner = ChunkOwn.findOwner(player.getName());
        if (block.getTypeId() == (owner == null ? ChunkOwn.defaultAutoOwnBlock : owner.autoOwnBlock)) {
            ChunkOwnCommand.buy(player, block.getChunk());
        }
    }
}
