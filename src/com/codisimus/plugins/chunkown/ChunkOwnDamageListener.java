package com.codisimus.plugins.chunkown;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Block PvP and PvE within Owned Chunks which have protection
 *
 * @author Codisimus
 */
public class ChunkOwnDamageListener implements Listener {
    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        //Return if the Enitity damaged is not a Player
        Entity wounded = event.getEntity();
        if (!(wounded instanceof Player)) {
            return;
        }

        //Return if the Event was not within an Owned Chunk
        OwnedChunk chunk = ChunkOwn.findOwnedChunk(wounded.getLocation().getBlock());
        if (chunk == null) {
            return;
        }

        Entity attacker = event.getDamager();
        if (attacker instanceof Projectile) {
            attacker = ((Projectile) attacker).getShooter();
        }

        if (attacker instanceof Player) { //PvP
            //Return if the Player is suicidal
            if (attacker.equals(wounded)) {
                return;
            }

            //Cancel the Event if PvP protection is enabled
            if (Econ.blockPvP != -2 && chunk.owner.blockPvP) {
                event.setCancelled(true);
            }
        } else //PvE
            //Cancel the Event if PvE protection is enabled
            if (Econ.blockPvE != -2 && chunk.owner.blockPvE) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        switch (event.getEntityType()) {
        case ARROW: //Fall through
        case FIREBALL: //Fall through
        case SMALL_FIREBALL: break;
        default: return;
        }

        //Return if the Event was not within an Owned Chunk
        OwnedChunk chunk = ChunkOwn.findOwnedChunk(event.getEntity().getLocation().getChunk());
        if (chunk == null) {
            return;
        }

        //Cancel the Event if PvP protection is enabled
        if (Econ.blockPvP != -2 && chunk.owner.blockPvP) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        //Return if the Event was not within an Owned Chunk
        OwnedChunk chunk = ChunkOwn.findOwnedChunk(event.getTarget().getLocation().getChunk());
        if (chunk == null) {
            return;
        }

        //Cancel the Event if PvP protection is enabled
        if (Econ.blockPvE != -2 && chunk.owner.blockPvE) {
            event.setCancelled(true);
        }
    }
}
