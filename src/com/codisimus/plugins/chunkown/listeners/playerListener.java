package com.codisimus.plugins.chunkown.listeners;

import com.codisimus.plugins.chunkown.ChunkOwn;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * Listens for commands and griefing events
 * 
 * @author Codisimus
 */
public class playerListener extends PlayerListener {

    /**
     * Buckets can only be emptied within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The PlayerBucketEmptyEvent that occurred
     */
    @Override
    public void onPlayerBucketEmpty (PlayerBucketEmptyEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace())))
            event.setCancelled(true);
    }

    /**
     * Buckets can only be filled within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The PlayerBucketFillEvent that occurred
     */
    @Override
    public void onPlayerBucketFill (PlayerBucketFillEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlockClicked()))
            event.setCancelled(true);
    }
}
