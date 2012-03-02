package com.codisimus.plugins.chunkown;

import java.util.Iterator;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.painting.PaintingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

/**
 * Listens for griefing events
 * 
 * @author Codisimus
 */
public class ChunkOwnListener implements Listener {

    /**
     * Buckets can only be emptied within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The PlayerBucketEmptyEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerBucketEmpty (PlayerBucketEmptyEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace())))
            event.setCancelled(true);
    }

    /**
     * Buckets can only be filled within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The PlayerBucketFillEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerBucketFill (PlayerBucketFillEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlockClicked()))
            event.setCancelled(true);
    }
    
    /**
     * Chests can only be opened within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerInteract (PlayerInteractEvent event) {
        //Return if the event was not opening a Chest
        if (event.getMaterial() != Material.CHEST || event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getClickedBlock()))
            event.setCancelled(true);
    }
    
    /**
     * Updates the last time that Players that own Chunks were seen
     * 
     * @param event The PlayerJoinEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerJoin (PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        
        Object object = ChunkOwn.chunkCounter.get(name);
        if (object == null)
            return;
        
        if ((Integer)object > 0)
            ChunkOwn.lastDaySeen.setProperty(name, String.valueOf(ChunkOwn.getDayAD()));
        
        ChunkOwn.saveLastSeen();
    }
    
    /**
     * Updates the last time that Players that own Chunks were seen
     * 
     * @param event The PlayerQuitEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerQuit (PlayerQuitEvent event) {
        String name = event.getPlayer().getName();
        
        Object object = ChunkOwn.chunkCounter.get(name);
        if (object == null)
            return;
        
        if ((Integer)object > 0)
            ChunkOwn.lastDaySeen.setProperty(name, String.valueOf(ChunkOwn.getDayAD()));
        
        ChunkOwn.saveLastSeen();
    }
    
    /**
     * Paintings can only be placed within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The PaintingPlaceEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPaintingPlace (PaintingPlaceEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within an OwnedChunk cannot be destroyed from explosions
     * 
     * @param event The EntityExplodeEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onEntityExplode (EntityExplodeEvent event) {
        Iterator itr = event.blockList().iterator();
        while (itr.hasNext())
            if (!ChunkOwn.canBuild(null, (Block)itr.next()))
                itr.remove();
    }
    
    /**
     * Blocks within an OwnedChunk can only be broken by the owner, a coowner, or an admin
     * 
     * @param event The BlockBreakEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockBreak (BlockBreakEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within an OwnedChunk can only be ignited by the owner, a coowner, or an admin
     * 
     * @param event The BlockIgniteEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks can only be placed within an OwnedChunk by the owner, a coowner, or an admin
     * 
     * @param event The BlockPlaceEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Blocks within an OwnedChunk cannot be spread
     * 
     * @param event The BlockSpreadEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockSpread (BlockSpreadEvent event) {
        if (!ChunkOwn.canBuild(null, event.getBlock()))
            event.setCancelled(true);
    }

    /**
     * Signs within an OwnedChunk can only be changed by the owner, a coowner, or an admin
     * 
     * @param event The SignChangeEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onSignChange (SignChangeEvent event) {
        if (!ChunkOwn.canBuild(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }
    
    /**
     * Vehicles within an OwnedChunk can only be damaged by the owner, a coowner, or an admin
     * 
     * @param event The VehicleDamageEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onVehicleDamage (VehicleDamageEvent event) {
        Player player = null;
        Entity entity = event.getAttacker();
        if (entity instanceof Player)
            player = (Player)entity;
        
        if (!ChunkOwn.canBuild(player, event.getVehicle().getLocation().getBlock()))
            event.setCancelled(true);
    }

    /**
     * Vehicles within an OwnedChunk can only be destroyed by the owner, a coowner, or an admin
     * 
     * @param event The VehicleDestroyEvent that occurred
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onVehicleDestroy (VehicleDestroyEvent event) {
        Player player = null;
        Entity entity = event.getAttacker();
        if (entity instanceof Player)
            player = (Player)entity;
        
        if (!ChunkOwn.canBuild(player, event.getVehicle().getLocation().getBlock()))
            event.setCancelled(true);
    }
}
