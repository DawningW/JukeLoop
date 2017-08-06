package qingchenw.repack.com.norcode.bukkit.jukeloop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import qingchenw.repack.com.norcode.bukkit.jukeloop.LoopingJukebox;

public class JukeLoopPlugin extends JavaPlugin implements Listener
{
	public static JukeLoopPlugin instance;
	public static HashMap<Material, Integer> durations = new HashMap<Material, Integer>();
	public static HashSet<LoopingJukebox> jukeboxes = new HashSet<LoopingJukebox>();
	private BukkitTask checkTask = null;
	private BukkitTask saveTask = null;

	static
	{
		durations.put(Material.GOLD_RECORD, 178);
		durations.put(Material.GREEN_RECORD, 185);
		durations.put(Material.RECORD_3, 345);
		durations.put(Material.RECORD_4, 185);
		durations.put(Material.RECORD_5, 174);
		durations.put(Material.RECORD_6, 197);
		durations.put(Material.RECORD_7, 96);
		durations.put(Material.RECORD_8, 150);
		durations.put(Material.RECORD_9, 188);
		durations.put(Material.RECORD_10, 251);
		durations.put(Material.RECORD_11, 71);
		durations.put(Material.RECORD_12, 235);
	}

	public void onEnable()
	{
		instance = this;

		this.saveDefaultConfig();
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		this.reloadConfig();

		this.getServer().getPluginManager().registerEvents(this, this);
		this.checkTask = this.getServer().getScheduler().runTaskTimer(this, new Runnable()
		{
			public void run()
			{
				for(LoopingJukebox loopjukebox : jukeboxes)
				{
					if(loopjukebox != null && !loopjukebox.isDead())
					{
						loopjukebox.update();
					}
					else
					{
						jukeboxes.remove(loopjukebox);
					}
				}
			}
		}, 80L, 80L);
		this.saveTask = this.getServer().getScheduler().runTaskTimer(this, new Runnable()
		{
			public void run()
			{
				JukeLoopPlugin.this.saveConfig();
			}
		}, 6000L, 6000L);
	}

	public void onDisable()
	{
		this.saveTask.cancel();
		this.checkTask.cancel();
		this.saveConfig();
	}

	public void reloadConfig()
	{
		super.reloadConfig();
		for(Material material : durations.keySet())
		{
			durations.put(material, this.getConfig().getInt("durations." + material.name(), durations.get(material)));
		}
		for(String string : this.getConfig().getStringList("jukeboxes"))
		{
			String[] array = string.trim().split(",");
			World world = this.getServer().getWorld(array[0]);
			if(world == null) world = this.getServer().createWorld(new WorldCreator(array[0]));
			Location loc = new Location(world, Double.valueOf(array[1]), Double.valueOf(array[2]), Double.valueOf(array[3]));
			this.getLoopingJukeBox(loc);
		}
	}

	public void saveConfig()
	{
		ArrayList<String> jukeboxlist = new ArrayList<String>(jukeboxes.size());
		for(LoopingJukebox loopjukebox : jukeboxes)
		{
			Location loc = loopjukebox.getLocation();
			jukeboxlist.add(loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
		}
		this.getConfig().set("jukeboxes", jukeboxlist);
		super.saveConfig();
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInteractJukebox(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if(player.hasPermission("jukeloop.use"))
		{
			Block block = event.getClickedBlock();
			if(block != null && block.getType() == Material.JUKEBOX)
			{
				Jukebox jukebox = (Jukebox) block.getState();
				if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
				{
					if(!jukebox.isPlaying() && durations.containsKey(player.getItemInHand().getType()))
					{
						LoopingJukebox loopjukebox = this.getLoopingJukeBox(jukebox.getLocation());
						if(loopjukebox != null) loopjukebox.onInsert();
					}
				}
				/*
				else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
				{
					LoopingJukebox loopjukebox = this.getLoopingJukeBox(jukebox.getLocation());
					if(loopjukebox != null)
					{
						// TODO 切换模式:单曲循环/按序播放/随机播放
					}
				}
				*/
			}
		}
	}

	/**
	 * 公共API,通过位置查询循环唱片机
	 * 
	 * @param loc 要查询的唱片机位置
	 * @return 该位置的循环唱片机,没有则返回null
	 */
	public LoopingJukebox getLoopingJukeBox(Location loc)
	{
		for(LoopingJukebox loopjukebox : jukeboxes)
		{
			if(loopjukebox.getLocation().equals(loc) && !loopjukebox.isDead())
			{
				return loopjukebox;
			}
		}
		
		LoopingJukebox loopjukebox = new LoopingJukebox(loc);
		if(!loopjukebox.isDead())
		{
			jukeboxes.add(loopjukebox);
			return loopjukebox;
		}
		
		return null;
	}
}
