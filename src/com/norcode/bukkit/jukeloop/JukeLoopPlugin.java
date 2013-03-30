package com.norcode.bukkit.jukeloop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class JukeLoopPlugin extends JavaPlugin implements Listener {
    
    private static Pattern locRegex = Pattern.compile("(\\w+)_(\\-?\\d+)_(\\-?\\d+)_(\\-?\\d+)");
    public static HashMap<Material, String> recordNames = new HashMap<Material, String>(
            13);

    public static BlockFace[] directions = new BlockFace[] { BlockFace.EAST,
            BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN,
            BlockFace.UP };

    public static ArrayList<Material> playlistOrder;
    public static HashMap<Material, Integer> recordDurations = new HashMap<Material, Integer>();
    private BukkitTask checkTask = null;
    private BukkitTask saveTask = null;
    static {
        // set record names
        recordNames.put(Material.GOLD_RECORD, "13");
        recordNames.put(Material.GREEN_RECORD, "cat");
        recordNames.put(Material.RECORD_3, "blocks");
        recordNames.put(Material.RECORD_4, "chirp");
        recordNames.put(Material.RECORD_5, "far");
        recordNames.put(Material.RECORD_6, "mall");
        recordNames.put(Material.RECORD_7, "mellohi");
        recordNames.put(Material.RECORD_8, "stal");
        recordNames.put(Material.RECORD_9, "strad");
        recordNames.put(Material.RECORD_10, "ward");
        recordNames.put(Material.RECORD_11, "11");
        recordNames.put(Material.RECORD_12, "wait");

        // set record durations
        recordDurations.put(Material.GOLD_RECORD, (2 * 60) + 58);
        recordDurations.put(Material.GREEN_RECORD, (3 * 60) + 5);
        recordDurations.put(Material.RECORD_3, (5 * 60) + 45);
        recordDurations.put(Material.RECORD_4, (3 * 60) + 5);
        recordDurations.put(Material.RECORD_5, (2 * 60) + 54);
        recordDurations.put(Material.RECORD_6, (3 * 60) + 17);
        recordDurations.put(Material.RECORD_7, (1 * 60) + 36);
        recordDurations.put(Material.RECORD_8, (2 * 60) + 30);
        recordDurations.put(Material.RECORD_9, (3 * 60) + 8);
        recordDurations.put(Material.RECORD_10, (4 * 60) + 11);
        recordDurations.put(Material.RECORD_11, (1 * 60) + 11);
        recordDurations.put(Material.RECORD_12, (3 * 60) + 55);
        playlistOrder = new ArrayList<Material>(recordDurations.size());
        for (Material m : recordDurations.keySet()) {
            playlistOrder.add(m);
        }
    };

    @Override
    public void onEnable() {
        // TODO Auto-generated method stub
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        checkTask = getServer().getScheduler().runTaskTimer(this,
            new Runnable() {
                @Override
                public void run() {
                    ArrayList<Location> toRemove = new ArrayList<Location>();
                    LoopingJukebox jb;
                    for (Entry<Location, LoopingJukebox> e : LoopingJukebox.jukeboxMap
                            .entrySet()) {
                        jb = e.getValue();
                        if (jb == null || jb.isDead) {
                            toRemove.add(e.getKey());
                        } else {
                            jb.doLoop();
                        }
                    }
                    for (Location l : toRemove) {
                        LoopingJukebox.jukeboxMap.remove(l);
                    }
            }
        }, 40, 40);
        saveTask = getServer().getScheduler().runTaskTimer(this, 
            new Runnable() {
                @Override
                public void run() {
                    saveData();
                }
            }, 20*60*5, 20*60*5);
    }

    
    private Location parseLocation(String s) {
        Matcher m = locRegex.matcher(s);
        if (m.matches()) {
            try {
                return new Location(getServer().getWorld(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(3)));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Invalid entry: " + s);
            }
        }
        return null;
    }
    
    private void loadData() {
        World w = null;
        Location l = null;
        for (String s : getConfig().getStringList("jukeboxes")) {
            getLogger().info("initializing jukebox@" + s);
            l = parseLocation(s);
            LoopingJukebox.jukeboxMap.put(l, LoopingJukebox.getAt(this, l));
        }
    }

    private void saveData() {
        List<String> boxlist = new ArrayList<String>(LoopingJukebox.jukeboxMap
                .keySet().size());
        for (Location l : LoopingJukebox.jukeboxMap.keySet()) {
            boxlist.add(l.getWorld().getName() + "_" + l.getBlockX() + "_"
                    + l.getBlockY() + "_" + l.getBlockZ());
        }
        getConfig().set("jukeboxes", boxlist);
        saveConfig();
    }

    @Override
    public void onDisable() {
        saveTask.cancel();
        checkTask.cancel();
        saveData();
        super.onDisable();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractJukebox(PlayerInteractEvent e) {
        if (!e.getPlayer().hasPermission("jukeloop.use"))
            return;
        if (e.getAction() == Action.LEFT_CLICK_BLOCK
                && e.getClickedBlock().getType() == Material.JUKEBOX) {
            if (((Jukebox) e.getClickedBlock().getState()).isPlaying()) {
                LoopingJukebox jb = LoopingJukebox.getAt(this, e
                        .getClickedBlock().getLocation());
                if (jb != null && jb.getJukebox().isPlaying()) {
                    jb.onEject();
                    jb.doLoop();
                    e.setCancelled(true);
                }
            }
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock().getType() == Material.JUKEBOX
                && recordDurations.containsKey(e.getPlayer().getItemInHand()
                        .getType())) {

            Jukebox box = (Jukebox) e.getClickedBlock().getState();
            LoopingJukebox jb = LoopingJukebox.getAt(this, box.getLocation());
            Material record = e.getPlayer().getItemInHand().getType();
            if (!box.isPlaying()) {
                e.setCancelled(true);
                box.setPlaying(record);
                e.getPlayer().setItemInHand(new ItemStack(Material.AIR));
                jb.onInsert(record);
            } else {
                jb.onEject();
            }
        }
    }
}
