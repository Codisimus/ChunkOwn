package com.codisimus.plugins.chunkown;

import com.codisimus.plugins.chunkown.ChunkOwner.AddOn;
import com.palmergames.bukkit.towny.Towny;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Executes Player Commands
 *
 * @author Codisimus
 */
public class ChunkOwnCommand implements CommandExecutor {
    public static String command;
    public static boolean wgSupport;
    public static int edgeID;
    private static enum Action { HELP, BUY, SELL, LIST, INFO, COOWNER, CLEAR, PREVIEW, RL }
    private static HashMap<Player, LinkedList<Location>> chunkOutlines = new HashMap<Player, LinkedList<Location>>();

    /**
     * Listens for ChunkOwn commands to execute them
     *
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player)) {
            return true;
        } else if (args.length == 1 && args[0].equals("rl")) {
            ChunkOwn.rl();
        }

        Player player = (Player) sender;

        //Cancel if the Player is in a disabled World
        if (!ChunkOwn.enabledInWorld(player.getWorld())) {
            player.sendMessage("§4ChunkOwn is disabled in your current World");
            return true;
        }

        //Display help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        Action action;

        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException notEnum) {
            sendHelp(player);
            return true;
        }

        //Execute the correct command
        switch (action) {
        case BUY:
            switch (args.length) {
            case 1:
                buy(player);
                return true;

            case 2:
                try {
                    buyAddOn(player, AddOn.valueOf(args[1].toUpperCase()));
                } catch (IllegalArgumentException notAddOn) {
                    player.sendMessage("§6" + args[1] + "§4 is not a valid Add-on");
                }
                return true;

            default: break;
            }
            break;

        case SELL:
            switch (args.length) {
            case 1:
                sell(player);
                return true;

            case 2:
                try {
                    sellAddOn(player, AddOn.valueOf(args[1].toUpperCase()));
                } catch (IllegalArgumentException notAddOn) {
                    player.sendMessage("§6" + args[1] + "§4 is not a valid Add-on");
                }
                return true;

            default: break;
            }
            break;

        case LIST:
            switch (args.length) {
            case 1:
                list(player);
                return true;

            case 2:
                if (args[1].equals("addons")) {
                    listAddOns(player);
                } else if (args[1].equals("coowners")) {
                    listCoOwners(player);
                } else {
                    sendAddOnHelp(player);
                }
                return true;

            default: break;
            }
            break;

        case INFO:
            info(player);
            return true;

        case COOWNER:
            String coOwner = args[args.length - 1];
            boolean all = false;
            boolean add = true;
            boolean isPlayer = true;

            for (int i = 1; i < args.length - 1; i++) {
                if (args[i].equals("all")) {
                    all = true;
                } else if (args[i].equals("remove")) {
                    add = false;
                } else if (args[i].equals("group")) {
                    isPlayer = false;
                }
            }

            coowner(player, all, add, isPlayer, coOwner);
            return true;

        case CLEAR:
            switch (args.length) {
            case 1:
                clear(player);
                return true;

            case 2:
                if (!ChunkOwn.hasPermission(player, "admin")) {
                    player.sendMessage(ChunkOwnMessages.permission);
                } else {
                    clear(args[1]);
                    player.sendMessage("You have sold all chunks owned by " + args[1]);
                }
                return true;

            default: break;
            }
            break;

        case PREVIEW:
            preview(player);
            return true;

        case HELP:
            if (args.length == 2 && args[1].equals("addons")) {
                sendAddOnHelp(player);
            } else {
                sendHelp(player);
            }
            return true;

        case RL:
            //Cancel if the Player does not have permission to use the command
            if (!ChunkOwn.hasPermission(player, "admin")) {
                player.sendMessage(ChunkOwnMessages.permission);
            } else {
                ChunkOwn.rl(player);
            }
            return true;

        default: break;
        }

        sendHelp(player);
        return true;
    }

    /**
     * Gives ownership of the current Chunk to the Player
     *
     * @param player The Player buying the Chunk
     */
    public static void buy(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "buy")) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        //Retrieve the OwnedChunk that the Player is in
        Chunk chunk = player.getLocation().getBlock().getChunk();
        OwnedChunk ownedChunk = ChunkOwn.findOwnedChunk(chunk);

        //If the owner of the OwnedChunk is not blank then the Chunk is already claimed
        if (ownedChunk != null) {
            player.sendMessage(ChunkOwnMessages.claimed);
            return;
        }

        if (wgSupport) {
            Plugin plugin = ChunkOwn.pm.getPlugin("WorldGuard");
            if (plugin != null) {
                WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
                for (Block block: ChunkOwn.getBlocks(chunk)) {
                    if (!wg.canBuild(player, block)) {
                        player.sendMessage(ChunkOwnMessages.worldGuard);
                        return;
                    }
                }
            }
        }

//        if (townySupport) {
//            Plugin plugin = ChunkOwn.pm.getPlugin("Towny");
//            if (plugin != null) {
//                Towny towny = (Towny) plugin;
//                for (Block block: ChunkOwn.getBlocks(chunk)) {
//                    if (!towny.getTownyUniverse().getWorldMap().canBuild(player, block)) {
//                        player.sendMessage(ChunkOwnMessages.towny);
//                        return;
//                    }
//                }
//            }
//        }

        String name = player.getName();

        //Check if the Player is limited
        int limit = ChunkOwn.getOwnLimit(player);
        if (limit != -1) {
            //Cancel if the Player owns their maximum limit
            if (ChunkOwn.getChunkCounter(name) >= limit) {
                player.sendMessage(ChunkOwnMessages.limit);
                return;
            }
        }

        //Check if a group size is required
        if (ChunkOwn.groupSize > 1) {
            //Check if the Chunk is a loner (not connected to other owned Chunks
            if (isLoner(chunk, name)) {
                if (!ChunkOwn.canBuyLoner(ChunkOwn.getOwnedChunks(name))) {
                    player.sendMessage(ChunkOwnMessages.groupLand.replace("<MinimumGroupSize>", String.valueOf(ChunkOwn.groupSize)));
                    return;
                }
            }
        }

        //Charge the Player only if they don't have the 'chunkown.free' node
        if (ChunkOwn.hasPermission(player, "free")) {
            player.sendMessage(ChunkOwnMessages.buyFree);
        } else if(!Econ.buy(player)) {
            return;
        }

        previewChunk(player, chunk);
        ChunkOwn.addOwnedChunk(new OwnedChunk(chunk, name));
    }

    /**
     * Gives ownership of the specified Chunk to the Player
     *
     * @param player The Player buying the Chunk
     * @param chunk The Chunk being purchased
     */
    public static void buy(Player player, Chunk chunk) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "autobuy")) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        //If the owner of the OwnedChunk is not blank then the Chunk is already claimed
        OwnedChunk ownedChunk = ChunkOwn.findOwnedChunk(chunk);
        if (ownedChunk != null) {
            player.sendMessage(ChunkOwnMessages.claimed);
            return;
        }

        String name = player.getName();

        //Check if the Player is limited
        int limit = ChunkOwn.getOwnLimit(player);
        if (limit != -1) {
            //Cancel if the Player owns their maximum limit
            if (ChunkOwn.getChunkCounter(name) >= limit) {
                player.sendMessage(ChunkOwnMessages.limit);
                return;
            }
        }


        //Check if a group size is required
        if (ChunkOwn.groupSize > 1) {
            //Check if the Chunk is a loner (not connected to other owned Chunks
            if (isLoner(chunk, name)) {
                if (!ChunkOwn.canBuyLoner(ChunkOwn.getOwnedChunks(name))) {
                    player.sendMessage(ChunkOwnMessages.groupLand.replace("<MinimumGroupSize>", String.valueOf(ChunkOwn.groupSize)));
                    return;
                }
            }
        }

        //Charge the Player only if they don't have the 'chunkown.free' node
        if (ChunkOwn.hasPermission(player, "free")) {
            player.sendMessage(ChunkOwnMessages.buyFree);
        } else if(!Econ.buy(player)) {
            return;
        }

        if (edgeID == -1) {
            previewChunk(player, chunk);
        } else {
            chunkOutlines.remove(player);
            markEdges(chunk);
        }
        ChunkOwn.addOwnedChunk(new OwnedChunk(chunk, name));
    }

    /**
     * Gives the Player the Given Add-on
     *
     * @param player The Player buying the Chunk
     */
    public static void buyAddOn(Player player, AddOn addOn) {
        //Cancel if the Player does not have permission to buy the Add-on
        if (!ChunkOwn.hasPermission(player, addOn)) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        //Retrieve the ChunkOwner for the Player
        ChunkOwner owner = ChunkOwn.getOwner(player.getName());

        //Cancel if the Player already has the Add-on
        if (owner.hasAddOn(addOn)) {
            player.sendMessage("§4You already have that Add-on enabled");
            return;
        }

        //Cancel if the Player could not afford the transaction
        if (!Econ.charge(player, Econ.getBuyPrice(addOn))) {
            return;
        }

        owner.setAddOn(addOn, true);
        owner.save();
    }

    /**
     * Removes the given Add-on from the Player's ChunkOwner
     *
     * @param player The Player buying the Chunk
     */
    public static void sellAddOn(Player player, AddOn addOn) {
        //Retrieve the ChunkOwner for the Player
        ChunkOwner owner = ChunkOwn.getOwner(player.getName());

        //Cancel if the Player already has the Add-on
        if (!owner.hasAddOn(addOn)) {
            player.sendMessage("§4You already have that Add-on disabled");
            return;
        }

        Econ.refund(player, Econ.getSellPrice(addOn));
        owner.setAddOn(addOn, false);
        owner.save();
    }

    /**
     * Previews the boundaries of the current Chunk
     *
     * @param player The Player previewing the Chunk
     */
    public void preview(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "preview")) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        //Retrieve the OwnedChunk that the Player is in
        Chunk chunk = player.getLocation().getBlock().getChunk();
        OwnedChunk ownedChunk = ChunkOwn.findOwnedChunk(chunk);
        String name = player.getName();

        //Check if another Player already owns the Chunk
        if (ownedChunk != null && !ownedChunk.isCoOwner(player) && ChunkOwn.hasPermission(player, "admin")) {
            player.sendMessage(ChunkOwnMessages.claimed);
            return;
        }

        //Check if the Player is limited
        int limit = ChunkOwn.getOwnLimit(player);
        if (limit != -1) {
            //Cancel if the Player owns their maximum limit
            if (ChunkOwn.getChunkCounter(name) >= limit) {
                player.sendMessage(ChunkOwnMessages.limit);
                return;
            }
        }

        //Check if a group size is required
        if (ChunkOwn.groupSize > 1) {
            //Check if the Chunk is a loner (not connected to other owned Chunks
            if (isLoner(chunk, name)) {
                if (!ChunkOwn.canBuyLoner(ChunkOwn.getOwnedChunks(name))) {
                    player.sendMessage(ChunkOwnMessages.groupLand.replace("<MinimumGroupSize>", String.valueOf(ChunkOwn.groupSize)));
                    return;
                }
            }
        }

        previewChunk(player, chunk);
    }

    /**
     * Removes ownership of the current Chunk from the Player
     *
     * @param player The Player selling the Chunk
     */
    public static void sell(Player player) {
        //Retrieve the OwnedChunk that the Player is in
        Chunk chunk = player.getLocation().getBlock().getChunk();
        OwnedChunk ownedChunk = ChunkOwn.findOwnedChunk(chunk);

        //Cancel if the Chunk is not owned
        if (ownedChunk == null) {
            player.sendMessage(ChunkOwnMessages.doNotOwn);
            return;
        }

        //Cancel if the OwnedChunk is owned by someone else
        if (!ownedChunk.owner.name.equals(player.getName())) {
            if (ChunkOwn.hasPermission(player, "admin")) {
                Econ.sell(player, ownedChunk.owner.name);
            }
            else {
                player.sendMessage(ChunkOwnMessages.doNotOwn);
                return;
            }
        }
        else {
            Econ.sell(player);
        }

        ChunkOwn.removeOwnedChunk(ownedChunk);
    }

    /**
     * Display to the Player all of the Chunks that they own
     *
     * @param player The Player requesting the list
     */
    public static void list(Player player) {
        String name = player.getName();
        player.sendMessage("§3Number of Chunks owned: §6" + ChunkOwn.getChunkCounter(name));

        //Retrieve the ownLimit to display to the Player
        int ownLimit = ChunkOwn.getOwnLimit(player);
        if (ownLimit > -1) {
            player.sendMessage("§3Total amount you may own: §6" + ownLimit);
        }

        for (OwnedChunk ownedChunk: ChunkOwn.getOwnedChunks()) {
            if (ownedChunk.owner.name.equals(name)) {
                player.sendMessage(ownedChunk.toString());
            }
        }
    }

    /**
     * Display to the Player all of the Add-ons that they own
     *
     * @param player The Player requesting the list
     */
    public static void listAddOns(Player player) {
        ChunkOwner owner = ChunkOwn.getOwner(player.getName());

        String list = "§3Enabled Add-ons: ";
        if (Econ.blockPvP != -2 && owner.blockPvP) {
            list = list.concat("§6BlockPvP§0, ");
        }
        if (Econ.blockPvE != -2 && owner.blockPvE) {
            list = list.concat("§6BlockPvE§0, ");
        }
        if (Econ.blockExplosions != -2 && owner.blockExplosions) {
            list = list.concat("§6BlockExplosions§0, ");
        }
        if (Econ.lockChests != -2 && owner.lockChests) {
            list = list.concat("§6LockChests§0, ");
        }
        if (Econ.lockDoors != -2 && owner.lockDoors) {
            list = list.concat("§6LockDoors§0, ");
        }
        if (Econ.disableButtons != -2 && owner.disableButtons) {
            list = list.concat("§6DisableButtons§0, ");
        }
        if (Econ.disablePistons != -2 && owner.disablePistons) {
            list = list.concat("§6DisablePistons§0, ");
        }
        if (Econ.alarm != -2 && owner.alarm) {
            list = list.concat("§6AlarmSystem§0, ");
        }
        if (Econ.heal != -2 && owner.heal) {
            list = list.concat("§6Heal§0, ");
        }
        if (Econ.feed != -2 && owner.feed) {
            list = list.concat("§6Feed§0, ");
        }
        if (Econ.notify != -2 && owner.notify) {
            list = list.concat("§6Notify§0, ");
        }
        if (Econ.notify != -2 && owner.notify) {
            list = list.concat("§6NoAutoDisown§0, ");
        }

        player.sendMessage(list.substring(0, list.length() - 2));

        if (ChunkOwn.defaultAutoOwnBlock != -1) {
            Material material = Material.getMaterial(owner.autoOwnBlock);
            player.sendMessage("§3You will automattically buy a Chunk if you place a §6" + material.toString() + "§3 in it");
        }
    }

    public void listCoOwners(Player player) {
        if (!ChunkOwn.hasPermission(player, "admin")) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        player.sendMessage("§3Co-owners of all Chunks:");

        Properties list = new Properties();
        for (OwnedChunk chunk : ChunkOwn.getOwnedChunks()) {
            String location = "§6" + chunk.world + "'" + (chunk.x * 16 + 8) + "'" + (chunk.z * 16 + 8);
            for (String coOwner : chunk.coOwners) {
                list.setProperty(coOwner, list.containsKey(coOwner)
                                          ? list.getProperty(coOwner) + "§0, " + location
                                          : location);
            }
        }

        for (String coOwner : list.stringPropertyNames()) {
            player.sendMessage("§3" + coOwner + "§0:" + list.getProperty(coOwner));
        }
    }

    /**
     * Display to the Player the info of the current Chunk
     * Info displayed is the Location of the Chunk and the current CoOwners
     *
     * @param player The Player requesting the info
     */
    public static void info(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "info")) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        //Retrieve the OwnedChunk that the Player is in
        Chunk chunk = player.getLocation().getBlock().getChunk();
        OwnedChunk ownedChunk = ChunkOwn.findOwnedChunk(chunk);

        //Cancel if the OwnedChunk does not exist
        if (ownedChunk == null) {
            player.sendMessage(ChunkOwnMessages.unclaimed);
            return;
        }

        //Display the world and x/y-coordinates of the center of the OwnedChunk to the Player
        player.sendMessage("§6" + ownedChunk.toString()+"§3 belongs to §6"+ownedChunk.owner.name);

        //Display CoOwners of OwnedChunk to Player
        String coOwners = "§3Co-owners:  ";
        for (String coOwner: ownedChunk.coOwners) {
            coOwners = coOwners.concat(coOwner.concat(", "));
        }
        player.sendMessage(coOwners.substring(0, coOwners.length() - 2));

        //Display CoOwner Groups of OwnedChunk to Player
        String groups = "§3Co-owner Groups:  ";
        for (String group: ownedChunk.groups) {
            groups += "§6" + group + "§0, ";

        }
        player.sendMessage(groups.substring(0, groups.length() - 4));
    }

    /**
     * Manages Co-Ownership of the ChunkOwner of the Player
     *
     * @param player The given Player who may be the Owner
     * @param type true if co-owner is a Player, false if it is a group
     * @param add true if adding a co-owner, false if removing
     * @param coOwner The given Co-Owner
     */
    public static void coowner(Player player, boolean all, boolean add, boolean isPlayer, String coOwner) {
        //Cancel if the Player does not have permission to use the command
        if (!ChunkOwn.hasPermission(player, "coowner")) {
            player.sendMessage(ChunkOwnMessages.permission);
            return;
        }

        ChunkOwner owner = null;
        OwnedChunk ownedChunk = null;
        LinkedList<String> coOwnerList;

        if (all) {
            owner = ChunkOwn.getOwner(player.getName());
            coOwnerList = isPlayer ? owner.coOwners : owner.groups;
        } else {
            //Retrieve the OwnedChunk that the Player is in
            Chunk chunk = player.getLocation().getBlock().getChunk();
            ownedChunk = ChunkOwn.findOwnedChunk(chunk);

            //Cancel if the OwnedChunk does not exist
            if (ownedChunk == null) {
                player.sendMessage(ChunkOwnMessages.unclaimed);
                return;
            }

            //Cancel if the OwnedChunk is owned by someone else
            if (!ownedChunk.owner.name.equals(player.getName())) {
                player.sendMessage(ChunkOwnMessages.doNotOwn);
                return;
            }

            coOwnerList = isPlayer ? ownedChunk.coOwners : ownedChunk.groups;
        }

        //Determine the command to execute
        if (add) {
            //Cancel if the Player is already a Co-owner
            if (coOwnerList.contains(coOwner)) {
                player.sendMessage("§6" + coOwner + "§4 is already a Co-owner");
                return;
            }

            coOwnerList.add(coOwner);
            player.sendMessage("§6" + coOwner + "§5 added as a Co-owner");
        } else {
            //Cancel if the Player is not a Co-owner
            if (!coOwnerList.contains(coOwner)) {
                player.sendMessage("§6" + coOwner + "§4 is not a Co-owner");
                return;
            }

            coOwnerList.remove(coOwner);
            player.sendMessage("§6" + coOwner + "§5 removed as a Co-owner");
        }

        if (all) {
            owner.save();
        } else {
            ownedChunk.save();
        }
    }

    /**
     * Removes all the Chunks that are owned by the given Player
     *
     * @param player The given Player
     */
    public static void clear(Player player) {
        clear(player, player.getName());
    }

    /**
     * Removes all the Chunks that are owned by the given Player
     *
     * @param player The name of the Player
     */
    public static void clear(String player) {
        clear(null, player);
    }

    /**
     * Removes all the Chunks that are owned by the given Player
     *
     * @param player The given Player
     */
    private static void clear(Player player, String name) {
        Iterator <OwnedChunk> itr = ChunkOwn.getOwnedChunks().iterator();
        OwnedChunk ownedChunk;

        while (itr.hasNext()) {
            ownedChunk = itr.next();

            //Sell the Chunk if it is owned by the given Player
            if (ownedChunk.owner.name.equals(name)) {
                if (player == null) {
                    Econ.sell(name);
                } else {
                    Econ.sell(player);
                }

                itr.remove();
                ChunkOwn.removeOwnedChunk(ownedChunk);
            }
        }
    }

    /**
     * Displays the ChunkOwn Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§e     ChunkOwn Help Page:");
        player.sendMessage("§2/"+command+" help addons§b Display the Add-on Help Page");
        if (ChunkOwn.hasPermission(player, "buy")) {
            player.sendMessage("§2/"+command+" buy§b Purchase the current chunk: "+Econ.format(Econ.getBuyPrice(player.getName())));
            player.sendMessage("§2/"+command+" sell§b Sell the current chunk: "+Econ.format(Econ.getSellPrice(player.getName())));
        }
        if (ChunkOwn.hasPermission(player, "preview")) {
            player.sendMessage("§2/"+command+" preview§b Preview the current chunk's boundaries");
        }
        player.sendMessage("§2/"+command+" list§b List locations of owned Chunks");
        if (ChunkOwn.hasPermission(player, "admin")) {
            player.sendMessage("§2/"+command+" list coowners§b List Co-owners for all Chunks");
        }
        if (ChunkOwn.hasPermission(player, "info")) {
            player.sendMessage("§2/"+command+" info§b List Owner and Co-Owners of current Chunk");
        }
        player.sendMessage("§2/"+command+" clear§b Sell all owned Chunks");
        if (ChunkOwn.hasPermission(player, "coowner")) {
            player.sendMessage("§2/"+command+" coowner [remove] [group] <Name>§b Co-Owner for current Chunk");
            player.sendMessage("§2/"+command+" coowner all [remove] [group] <Name>§b Co-Owner for all Chunks");
        }
        player.sendMessage("§bName = The group name or the Player's name");
    }

    /**
     * Displays the Add-on Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendAddOnHelp(Player player) {
        player.sendMessage("§e     Add-on Help Page:");
        player.sendMessage("§2Add-ons apply to all Chunks that you own");
        player.sendMessage("§2/"+command+" list addons§b List your current add-ons");

        //Display available Add-ons
        if (ChunkOwn.hasPermission(player, AddOn.BLOCKPVP)) {
            player.sendMessage("§2/"+command+" buy blockpvp§b No damage from Players: "+Econ.format(Econ.getBuyPrice(AddOn.BLOCKPVP)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.BLOCKPVE)) {
            player.sendMessage("§2/"+command+" buy blockpve§b No damage from Mobs: "+Econ.format(Econ.getBuyPrice(AddOn.BLOCKPVE)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.BLOCKEXPLOSIONS)) {
            player.sendMessage("§2/"+command+" buy blockexplosions§b No TNT/Creeper griefing: "+Econ.format(Econ.getBuyPrice(AddOn.BLOCKEXPLOSIONS)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.LOCKCHESTS)) {
            player.sendMessage("§2/"+command+" buy lockchests§b Players can't open Chests/Furnaces/Dispensers: "+Econ.format(Econ.getBuyPrice(AddOn.LOCKCHESTS)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.LOCKDOORS)) {
            player.sendMessage("§2/"+command+" buy lockdoors§b Players can't open Doors: "+Econ.format(Econ.getBuyPrice(AddOn.LOCKDOORS)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.DISABLEBUTTONS)) {
            player.sendMessage("§2/"+command+" buy disablebuttons§b Other Players can't use Buttons/Levers/Plates: "+Econ.format(Econ.getBuyPrice(AddOn.DISABLEBUTTONS)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.DISABLEPISTONS)) {
            player.sendMessage("§2/"+command+" buy disablepistons§b Pistons will no longer function: "+Econ.format(Econ.getBuyPrice(AddOn.DISABLEPISTONS)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.ALARM)) {
            player.sendMessage("§2/"+command+" buy alarm§b Be alerted when a Player enters your land: "+Econ.format(Econ.getBuyPrice(AddOn.ALARM)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.HEAL)) {
            player.sendMessage("§2/"+command+" buy heal§b Players gain half a heart every "+ChunkOwnMovementListener.rate+" seconds: "+Econ.format(Econ.getBuyPrice(AddOn.HEAL)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.FEED)) {
            player.sendMessage("§2/"+command+" buy feed§b Players gain half a food every "+ChunkOwnMovementListener.rate+" seconds: "+Econ.format(Econ.getBuyPrice(AddOn.FEED)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.NOTIFY)) {
            player.sendMessage("§2/"+command+" buy notify§b Be notified when you enter owned land: "+Econ.format(Econ.getBuyPrice(AddOn.NOTIFY)));
        }
        if (ChunkOwn.hasPermission(player, AddOn.NOAUTODISOWN)) {
            player.sendMessage("§2/"+command+" buy noautodisown§b Keep your land even if you become inactive: "+Econ.format(Econ.getBuyPrice(AddOn.NOAUTODISOWN)));
        }

        player.sendMessage("§2/"+command+" sell <addon>§b Sell an addon for "+Econ.moneyBack+"% of its buy price");
    }

    /**
     * Sets the edges of the given Chunk to be animated to the given Player
     *
     * @param player The Player to send the smoke animations to
     * @param chunk The Chunk with the edges to be displayed
     */
    public static void previewChunk(final Player player, Chunk chunk) {
        LinkedList<Location> outline = new LinkedList<Location>();
        int y = player.getLocation().getBlockY();
        for (int x = 0; x <= 15; x = x + 15) {
            for (int z = 0; z <= 15; z++) {
                outline.add(chunk.getBlock(x, y, z).getLocation());
            }
        }
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z = z + 15) {
                outline.add(chunk.getBlock(x, y, z).getLocation());
            }
        }
        chunkOutlines.put(player, outline);
        ChunkOwn.scheduler.runTaskLater(ChunkOwn.plugin, new Runnable() {
                @Override
                public void run() {
                    chunkOutlines.remove(player);
                }
            }, 1200L);
    }

    /**
     * Places Blocks of a predetermined type just above the highest Block at each edge of the given Chunk
     *
     * @param chunk The given Chunk
     */
    public static void markEdges(Chunk chunk) {
        World w = chunk.getWorld();
        for (int x = 0; x <= 15; x = x + 15) {
            for (int z = 0; z <= 15; z++) {
                w.getHighestBlockAt(x, z);
                int y = w.getHighestBlockYAt(x, z) + 1;
                boolean next = false;

                while (y >= 0) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getTypeId() == edgeID) {
                        break;
                    }

                    switch (block.getType()) {
                    case LEAVES: //Fall through
                    case AIR: //Keep going down
                        y--;
                        break;

                    case SAPLING: //Fall through
                    case LONG_GRASS: //Fall through
                    case DEAD_BUSH: //Fall through
                    case YELLOW_FLOWER: //Fall through
                    case RED_ROSE: //Fall through
                    case BROWN_MUSHROOM: //Fall through
                    case RED_MUSHROOM: //Fall through
                    case SNOW: //Replace existing Block
                        block.setTypeId(edgeID);
                        next = true;
                        break;

                    default:
                        if (block.getType().isSolid()) { //Place Block above this block
                            block.getRelative(0, 1, 0).setTypeId(edgeID);
                        }
                        next = true;
                        break;
                    }

                    if (next) {
                        break;
                    }
                }
            }
        }
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z = z + 15) {
                w.getHighestBlockAt(x, z);
                int y = w.getHighestBlockYAt(x, z) + 1;
                boolean next = false;

                while (y >= 0) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getTypeId() == edgeID) {
                        break;
                    }

                    switch (block.getType()) {
                    case LEAVES: //Fall through
                    case AIR: //Keep going down
                        y--;
                        break;

                    case SAPLING: //Fall through
                    case LONG_GRASS: //Fall through
                    case DEAD_BUSH: //Fall through
                    case YELLOW_FLOWER: //Fall through
                    case RED_ROSE: //Fall through
                    case BROWN_MUSHROOM: //Fall through
                    case RED_MUSHROOM: //Fall through
                    case SNOW: //Replace existing Block
                        block.setTypeId(edgeID);
                        next = true;
                        break;

                    default:
                        if (block.getType().isSolid()) { //Place Block above this block
                            block.getRelative(0, 1, 0).setTypeId(edgeID);
                        }
                        next = true;
                        break;
                    }

                    if (next) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns true if there are no neighboring Chunks with the given Owner
     *
     * @param chunk The Chunk that may be a Loner
     * @param player The Player who may own neighboring Chunks
     * @return True if there are no neighboring Chunks with the given Owner
     */
    private static boolean isLoner(Chunk chunk, String player) {
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();

        OwnedChunk ownedChunk = ChunkOwn.findOwnedChunk(world, x, z + 1);
        if (ownedChunk != null && ownedChunk.owner.name.equals(player)) {
            return false;
        }

        ownedChunk = ChunkOwn.findOwnedChunk(world, x, z - 1);
        if (ownedChunk != null && ownedChunk.owner.name.equals(player)) {
            return false;
        }

        ownedChunk = ChunkOwn.findOwnedChunk(world, x + 1, z);
        if (ownedChunk != null && ownedChunk.owner.name.equals(player)) {
            return false;
        }

        ownedChunk = ChunkOwn.findOwnedChunk(world, x - 1, z);
        if (ownedChunk != null && ownedChunk.owner.name.equals(player)) {
            return false;
        }

        return true;
    }

    /**
     * Creates smoke animations for all Locations
     */
    static void animateSelections() {
        //Repeat every tick
    	ChunkOwn.scheduler.scheduleSyncRepeatingTask(ChunkOwn.plugin, new Runnable() {
                @Override
                public void run() {
                    for (Player player : chunkOutlines.keySet()) {
                        World world = player.getWorld();
                        for (Location location : chunkOutlines.get(player)) {
                            //Play smoke effect
                            world.playEffect(location, Effect.SMOKE, 4);
                        }
                    }
                }
            }, 0L, 1L);
    }
}
