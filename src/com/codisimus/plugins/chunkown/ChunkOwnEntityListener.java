
package com.codisimus.plugins.chunkown;

import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.painting.PaintingPlaceEvent;

/**
 * Listens for griefing events
 * 
 * @author Cody
 */
public class ChunkOwnEntityListener extends EntityListener {

    @Override
    public void onPaintingPlace (PaintingPlaceEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }
}