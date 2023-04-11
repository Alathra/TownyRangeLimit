package org.aubrithehuman.townyrangelimit;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class TownyRangeLimit extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info("TownyRangeLimit loaded with limit of: " + this.getConfig().getInt("townblockRangeMax") + " townblocks");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void doCheck(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");
        if(args.length >= 2) {
            if (args[0].equalsIgnoreCase("/n") || args[0].equalsIgnoreCase("/nat") || args[0].equalsIgnoreCase("/nation")) {
                //n merge
                if (args[1].equalsIgnoreCase("merge")) {
                    if (!args[2].equalsIgnoreCase("deny") && !args[2].equalsIgnoreCase("accept")) {
                        Resident r = TownyAPI.getInstance().getResident(event.getPlayer());
                        if(r == null) return;
                        Nation nation1 = r.getNationOrNull();
                        Nation nation2 = TownyAPI.getInstance().getNation(args[2]);

                        if(nation1 == null || nation2 == null) return;
                        int dist = shortestDistance(nation1, nation2, event.getPlayer());

                        if (dist > this.getConfig().getInt("townblockRangeMax")) {
                            event.getPlayer().sendMessage("§e[Towny] " + "§c" + nation2.getName() + " is too far away from any town in your nation. Must be within " + (this.getConfig().getInt("townblockRangeMax") * TownySettings.getTownBlockSize()) + " blocks of the nearest town in either nation!");
                            event.setCancelled(true);
                        }
                    }
                }
                //n invite
                else if (args[1].equalsIgnoreCase("invite")) {
                    if (!args[2].equalsIgnoreCase("deny") && !args[1].equalsIgnoreCase("accept")) {
                        TownyWorld tw = WorldCoord.parseWorldCoord(event.getPlayer()).getTownyWorld();
                        if(tw == null) return;
                        Town town = tw.getTowns().get(args[2]);
                        Resident r = TownyAPI.getInstance().getResident(event.getPlayer());
                        if(r == null) return;
                        Nation nation = r.getNationOrNull();

                        int dist = shortestDistance(town, nation, event.getPlayer());

                        if (dist > this.getConfig().getInt("townblockRangeMax")) {
                            event.getPlayer().sendMessage("§e[Towny] " + "§c" + town.getName() + " is too far away from any town in your nation. Must be within " + (this.getConfig().getInt("townblockRangeMax") * TownySettings.getTownBlockSize()) + " blocks of the nearest town in your nation!");
                            event.setCancelled(true);
                        }
                    }
                }
                //n join (public)
                else if (args[1].equalsIgnoreCase("join")) {
                    Nation nation = TownyAPI.getInstance().getNation(args[2]);
                    if(nation == null) return;
                    if(nation.isOpen()) {
                        Resident r = TownyAPI.getInstance().getResident(event.getPlayer());
                        if(r == null) return;
                        Town town = r.getTownOrNull();

                        int dist = shortestDistance(town, nation, event.getPlayer());

                        if (dist > this.getConfig().getInt("townblockRangeMax")) {
                            event.getPlayer().sendMessage("§e[Towny] " + "§c" + nation.getName() + " is too far away from any town in your town. Must be within " + (this.getConfig().getInt("townblockRangeMax") * TownySettings.getTownBlockSize()) + " blocks of the nearest town in that nation!");
                            event.setCancelled(true);
                        }
                    }
                }
            }
            // t merge (wierd case lol)
            else if (args[0].equalsIgnoreCase("/t") || args[0].equalsIgnoreCase("/town")) {
                if (args[1].equalsIgnoreCase("merge")) {
                    if (!args[2].equalsIgnoreCase("deny") && !args[2].equalsIgnoreCase("accept")) {
                        this.getLogger().info(args[1]);
                        Resident r = TownyAPI.getInstance().getResident(event.getPlayer());
                        if(r == null) return;
                        Town town1 = r.getTownOrNull();
                        Town town2 = TownyAPI.getInstance().getTown(args[2]);

                        if(town1 == null || town2 == null) return;

                        int dist = shortestDistance(town1, town2, event.getPlayer());

                        if (dist > this.getConfig().getInt("townblockRangeMax")) {
                            event.getPlayer().sendMessage("§e[Towny] " + "§c" + town2.getName() + " is too far away from your town. Must be within " + (this.getConfig().getInt("townblockRangeMax") * TownySettings.getTownBlockSize()) + " blocks of the nearest chunk in your town!");
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds the shortest distance (in townblocks) between two towns
     * @param town1 town1
     * @param town2 town1
     * @return shortest distance
     */
    private static int shortestDistance(Town town1, Town town2, Player p) {
        //max possible range (60M*sqrt2)
        // 16 is block size
        int dist = 84852814 / 16;
        if (town1 != null && town2 != null) {
            TownBlock townHomeBlock1 = town1.getHomeBlockOrNull();
            TownBlock townHomeBlock2 = town2.getHomeBlockOrNull();
            if(townHomeBlock1 == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[TownyRangeLimit] " + town1.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                p.sendMessage("[TownyRangeLimit] " + town1.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                return dist;
            }
            if(townHomeBlock2 == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[TownyRangeLimit] " + town2.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                p.sendMessage("[TownyRangeLimit] " + town2.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                return dist;
            }
            ArrayList<WorldCoord> clusterInvited = getCluster(townHomeBlock1.getWorldCoord());
            ArrayList<WorldCoord> clusterInviter = getCluster(townHomeBlock2.getWorldCoord());
            WorldCoord invitedHomeblock = townHomeBlock1.getWorldCoord();
            WorldCoord inviterHomeblock = townHomeBlock2.getWorldCoord();

            //find nearest block to inviter homeblock
            WorldCoord nearestInviter = null;
            int distInviter = 84852814 / 16;
            for (WorldCoord coord1 : clusterInvited) {
                int distX = inviterHomeblock.getX() - coord1.getX();
                int distZ = inviterHomeblock.getZ() - coord1.getZ();
                if (distX < 0) distX *= -1;
                if (distZ < 0) distZ *= -1;
                //pythagorus my boi
                int dist1 = (int) Math.sqrt((distX * distX) + (distZ * distZ));

                //if we find a new min, save it
                if (dist1 <= distInviter) {
                    distInviter = dist1;
                    nearestInviter = coord1;
                   }
            }

            //if we didn't find anything for some reason? IDK its worth putting
            if(nearestInviter == null) {
                return dist;
            }

            //find nearest block to the found block
            WorldCoord nearestInvited = nearestInviter;
            int distInvited = 84852814 / 16;
            for (WorldCoord coord2 : clusterInviter) {
                int distX = invitedHomeblock.getX() - coord2.getX();
                int distZ = invitedHomeblock.getZ() - coord2.getZ();
                if (distX < 0) distX *= -1;
                if (distZ < 0) distZ *= -1;
                //pythagorus my boi
                int dist2 = (int) Math.sqrt((distX * distX) + (distZ * distZ));

                //if we find a new min, save it
                if (dist2 <= distInvited) {
                    distInvited = dist2;
                    nearestInvited = coord2;
                }
            }

            dist = (int) Math.sqrt(((nearestInviter.getX() - nearestInvited.getX()) * (nearestInviter.getX() - nearestInvited.getX())) + ((nearestInviter.getZ() - nearestInvited.getZ()) * (nearestInviter.getZ() - nearestInvited.getZ())));

        }
        return dist;
    }

    /**
     * Finds the shortest distance (in townblocks) between a town and a nation
     * @param town town
     * @param nation nation
     * @return shortest distance
     */
    private static int shortestDistance(Town town, Nation nation, Player p) {
        //max possible range (60M*sqrt2)
        // 16 is block size
        int dist = 84852814 / 16;
        if (town != null && nation != null) {
            TownBlock townHomeBlockM = town.getHomeBlockOrNull();
            if(townHomeBlockM == null) {
                Bukkit.getLogger().log(Level.SEVERE, "[TownyRangeLimit] " + town.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                p.sendMessage("[TownyRangeLimit] " + town.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                return dist;
            }
            ArrayList<WorldCoord> clusterInvited = getCluster(townHomeBlockM.getWorldCoord());
            WorldCoord invitedHomeblock = townHomeBlockM.getWorldCoord();

            //Grab all the towns in the nation
            List<Town> nation1Towns = nation.getTowns();
            ArrayList<ArrayList<WorldCoord>> clustersInviter = new ArrayList<>();
            ArrayList<WorldCoord> inviterHomeblocks = new ArrayList<>();
            for (Town t : nation1Towns) {
                TownBlock townHomeBlock = t.getHomeBlockOrNull();
                if(townHomeBlock == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "[TownyRangeLimit] " + t.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                    p.sendMessage("[TownyRangeLimit] " + t.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                    return dist;
                }
                clustersInviter.add(getCluster(townHomeBlock.getWorldCoord()));
                //grab homeblocks
                inviterHomeblocks.add(townHomeBlock.getWorldCoord());
            }

            //find nearest block to invited homeblock
            WorldCoord nearestInvited = null;
            int distInvited = 84852814 / 16;
            for (ArrayList<WorldCoord> cluster : clustersInviter) {
                for (WorldCoord coord2 : cluster) {
                    int distX = invitedHomeblock.getX() - coord2.getX();
                    int distZ = invitedHomeblock.getZ() - coord2.getZ();
                    if (distX < 0) distX *= -1;
                    if (distZ < 0) distZ *= -1;
                    //pythagorus my boi
                    int dist2 = (int) Math.sqrt((distX * distX) + (distZ * distZ));

                    //if we find a new min, save it
                    if (dist2 <= distInvited) {
                        distInvited = dist2;
                        nearestInvited = coord2;

                    }
                }
            }

            //if we didn't find anything for some reason? IDK its worth putting
            if(nearestInvited == null) {
                return dist;
            }

            //find nearest block to found block
            WorldCoord nearestInviter = invitedHomeblock;
            int distInviter = 84852814 / 16;
            for (WorldCoord coord1 : clusterInvited) {
                int distX = nearestInvited.getX() - coord1.getX();
                int distZ = nearestInvited.getZ() - coord1.getZ();
                if (distX < 0) distX *= -1;
                if (distZ < 0) distZ *= -1;
                //pythagorus my boi
                int dist1 = (int) Math.sqrt((distX * distX) + (distZ * distZ));

                //if we find a new min, save it
                if (dist1 <= distInviter) {
                    distInviter = dist1;
                    nearestInviter = coord1;
                }
            }

            dist = (int) Math.sqrt(((nearestInviter.getX() - nearestInvited.getX()) * (nearestInviter.getX() - nearestInvited.getX())) + ((nearestInviter.getZ() - nearestInvited.getZ()) * (nearestInviter.getZ() - nearestInvited.getZ())));

        }
        return dist;
    }

    /**
     * Finds the shortest distance (in townblocks) between two nations
     * @param nation1 nation1
     * @param nation2 nation2
     * @return shortest distance
     */

    private static int shortestDistance(Nation nation1, Nation nation2, Player p) {
        //max possible range (60M*sqrt2)
        // 16 is block size
        int dist = 84852814 / 16;
        if (nation1 != null && nation2 != null) {
            //Grab all the towns in the nation
            List<Town> nation1Towns = nation1.getTowns();
            ArrayList<ArrayList<WorldCoord>> clustersInviter = new ArrayList<>();
            ArrayList<WorldCoord> inviterHomeblocks = new ArrayList<>();
            for (Town t : nation1Towns) {
                TownBlock townHomeBlock = t.getHomeBlockOrNull();
                if(townHomeBlock == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "[TownyRangeLimit] " + t.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                    p.sendMessage("[TownyRangeLimit] " + t.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                    return dist;
                }
                clustersInviter.add(getCluster(townHomeBlock.getWorldCoord()));
                inviterHomeblocks.add(townHomeBlock.getWorldCoord());

            }

            //Grab all the towns in the nation
            List<Town> nation2Towns = nation2.getTowns();
            ArrayList<ArrayList<WorldCoord>> clustersInvited = new ArrayList<>();
            ArrayList<WorldCoord> invitedHomeblocks = new ArrayList<>();
            for (Town t : nation2Towns) {
                TownBlock townHomeBlock = t.getHomeBlockOrNull();
                if(townHomeBlock == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "[TownyRangeLimit] " + t.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                    p.sendMessage("[TownyRangeLimit] " + t.getName() + " has not set a home block! Town must have a homeblock! Failing automatically.");
                    return dist;
                }
                clustersInvited.add(getCluster(townHomeBlock.getWorldCoord()));
                invitedHomeblocks.add(townHomeBlock.getWorldCoord());
            }

            //This may take forever :skull: at like multiple 900 size towns it takes like 1mil iterations?????
            //IDK how tf else to check this
            //find nearest block to invited homeblock

            WorldCoord nearestInvited1 = null;
            int distInvited1 = 84852814 / 16;

            for (WorldCoord homeblock1 : invitedHomeblocks) {
                WorldCoord nearestInvited2 = null;
                int distInvited2 = 84852814 / 16;
                for (ArrayList<WorldCoord> cluster2 : clustersInviter) {
                    for (WorldCoord coord2 : cluster2) {
                        int distX = homeblock1.getX() - coord2.getX();
                        int distZ = homeblock1.getZ() - coord2.getZ();
                        if (distX < 0) distX *= -1;
                        if (distZ < 0) distZ *= -1;
                        //pythagorus my boi
                        int dist2 = (int) Math.sqrt((distX * distX) + (distZ * distZ));

                        //if we find a new min, save it
                        if (dist2 <= distInvited2) {
                            distInvited2 = dist2;
                            nearestInvited2 = coord2;

                        }
                    }
                }
                //compare to other homeblock searches
                if (distInvited2 <= distInvited1) {
                    distInvited1 = distInvited2;
                    nearestInvited1 = nearestInvited2;

                }
            }


            //repeat in the other direction
            WorldCoord nearestInviter1 = null;
            int distInviter1 = 84852814 / 16;

            for (WorldCoord homeblock1 : inviterHomeblocks) {
                WorldCoord nearestInviter2 = null;
                int distInviter2 = 84852814 / 16;
                for (ArrayList<WorldCoord> cluster2 : clustersInvited) {
                    for (WorldCoord coord2 : cluster2) {
                        int distX = homeblock1.getX() - coord2.getX();
                        int distZ = homeblock1.getZ() - coord2.getZ();
                        if (distX < 0) distX *= -1;
                        if (distZ < 0) distZ *= -1;
                        //pythagorus my boi
                        int dist2 = (int) Math.sqrt((distX * distX) + (distZ * distZ));

                        //if we find a new min, save it
                        if (dist2 <= distInviter2) {
                            distInviter2 = dist2;
                            nearestInviter2 = coord2;

                        }
                    }
                }
                //compare to other homeblock searches
                if (distInviter2 <= distInviter1) {
                    distInviter1 = distInviter2;
                    nearestInviter1 = nearestInviter2;

                }
            }

            if(nearestInviter1 == null || nearestInvited1 == null) throw new NullPointerException();

            dist = (int) Math.sqrt(((nearestInviter1.getX() - nearestInvited1.getX()) * (nearestInviter1.getX() - nearestInvited1.getX())) + ((nearestInviter1.getZ() - nearestInvited1.getZ()) * (nearestInviter1.getZ() - nearestInvited1.getZ())));

        }
        return dist;
    }

    /**
     *
     * @param worldCoord worldCoord
     * @return List of connected chunks
     * @author NinjaMandalorian
     */
    private static ArrayList<WorldCoord> getAdjCells(WorldCoord worldCoord) {
        ArrayList<WorldCoord> ReturnList = new ArrayList<>();

        int[][] XZarray = new int[][]{
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        }; // Array that contains relative orthogonal shifts from origin
        for ( int[] pair: XZarray) {
            // Constructs new WorldCoord for comparison
            WorldCoord tCoord = new WorldCoord(worldCoord.getWorldName(), worldCoord.getX() + pair[0], worldCoord.getZ() + pair[1]);
            if (tCoord.getTownOrNull() != null && tCoord.getTownOrNull() == worldCoord.getTownOrNull()) {
                // If in town, and in same town, adds to return list
                ReturnList.add(tCoord);
            }
        }
        return ReturnList;
    }

    /** Gets all adjacently connected townblocks
     * @param chunkCoord - WorldCoord to check at
     * @return List of WorldCoords
     * @author NinjaMandalorian
     */
    public static ArrayList<WorldCoord> getCluster(WorldCoord chunkCoord){
        // WCoordList is the returning array, SearchList is the to-search list.
        ArrayList<WorldCoord> WCoordList = new ArrayList<>();
        ArrayList<WorldCoord> SearchList = new ArrayList<>();
        SearchList.add(chunkCoord); // Adds 1st chunk to list

        // Iterates through SearchList, to create a full list of every adjacent cell.
        while (SearchList.size() > 0) {
            // Gets WorldCoord
            WorldCoord toSearch = SearchList.get(0);
            // Gets adjacent cells
            ArrayList<WorldCoord> adjCells = getAdjCells(toSearch);
            for (WorldCoord cell : adjCells) {
                // If in final list, ignore.
                if (WCoordList.contains(cell)) {
                    continue;
                    // If in to-search list, ignore
                } else if (SearchList.contains(cell)) {
                    continue;
                    // Otherwise, add to search-list.
                } else {
                    SearchList.add(cell);
                }
            }
            // Removes from search list and adds to finished list. After checking all adjacent chunks.
            SearchList.remove(toSearch);
            WCoordList.add(toSearch);
        }

        return WCoordList; // Returns list
    }

}
