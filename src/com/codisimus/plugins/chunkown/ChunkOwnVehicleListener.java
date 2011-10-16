
package com.codisimus.plugins.chunkown;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleListener;

/**
 * Listens for griefing events
 * 
 * @author Cody
 */
public class ChunkOwnVehicleListener extends VehicleListener {

    @Override
    public void onVehicleDamage (VehicleDamageEvent event) {
        Player player = null;
        Entity entity = event.getAttacker();
        if (entity instanceof Player)
            player = (Player)entity;
        if (!ChunkOwn.canBuild(player, event.getVehicle().getLocation().getBlock()))
            event.setCancelled(true);
    }

    @Override
    public void onVehicleDestroy (VehicleDestroyEvent event) {
        Player player = null;
        Entity entity = event.getAttacker();
        if (entity instanceof Player)
            player = (Player)entity;
        if (!ChunkOwn.canBuild(player, event.getVehicle().getLocation().getBlock()))
            event.setCancelled(true);
    }
}