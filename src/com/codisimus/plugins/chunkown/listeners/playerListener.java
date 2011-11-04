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

    @Override
    public void onPlayerBucketEmpty (PlayerBucketEmptyEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace())))
            event.setCancelled(true);
    }

    @Override
    public void onPlayerBucketFill (PlayerBucketFillEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlockClicked()))
            event.setCancelled(true);
    }
}
