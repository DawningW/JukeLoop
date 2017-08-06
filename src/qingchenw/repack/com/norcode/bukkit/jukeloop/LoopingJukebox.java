package qingchenw.repack.com.norcode.bukkit.jukeloop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import qingchenw.repack.com.norcode.bukkit.jukeloop.JukeLoopPlugin;

public class LoopingJukebox
{
	public static final BlockFace[] directions = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	private LoopType type;
	private Location loc;
	private Chest chest;
	private int startedAt = -1;
	private int chestSlot = -1;

	public LoopingJukebox(Location location)
	{
		this(LoopType.LOOP, location);
	}
	
	public LoopingJukebox(LoopType looptype, Location location)
	{
		this.type = looptype;
		this.loc = location;
	}

	// 我真是写mod写多了,不过这个循环唱片机和tileentity真的很像呢
	public void update()
	{
		Jukebox jukebox = this.getJukebox();
		if(jukebox.isPlaying())
		{
			int now = (int)(System.currentTimeMillis() / 1000L);
			Material record = jukebox.getPlaying();
			Integer duration = (Integer) JukeLoopPlugin.durations.get(record);
			if(duration != null && now - this.startedAt >= duration)
			{
				if(this.playersNearby())
				{
					jukebox.setPlaying(record);
					this.onInsert();
//					TODO 切换模式待实现
//					if(!this.putInChest()){}
//					if(!this.takeFromChest()){}
				}
			}
		}
	}

	public boolean playersNearby()
	{
		for(Player player : JukeLoopPlugin.instance.getServer().getOnlinePlayers())
		{
			try
			{
				return this.getLocation().distance(player.getLocation()) <= 64.0D;
			}
			catch (IllegalArgumentException e) {}
		}
		return false;
	}

	public Chest getChest()
	{
		if(this.chest == null)
		{
			for(BlockFace side : directions)
			{
				BlockState blockstate = this.getJukebox().getBlock().getRelative(side).getState();
				if(blockstate instanceof Chest)
				{
					this.chest = (Chest) blockstate;
					if(this.containsRecords(this.chest.getInventory()))
					{
						this.chest = (Chest)blockstate;
						break;
					}
				}
			}
		}
		else if(!this.chest.getLocation().getWorld().getBlockAt(this.chest.getLocation()).getType().equals(Material.CHEST))
		{
			this.chest = this.getChest();
		}
		return this.chest;
	}

	public boolean containsRecords(Inventory inv)
	{
		for(ItemStack itemstack : inv.getContents())
		{
			if(itemstack != null && JukeLoopPlugin.durations.containsKey(itemstack.getType()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean putInChest()
	{
		Chest chest = this.getChest();
		if(chest != null)
		{
			Inventory inv = chest.getInventory();
			if(this.chestSlot == -1 || this.chestSlot > chest.getInventory().getSize() - 1 || inv.getItem(this.chestSlot) != null && !inv.getItem(this.chestSlot).getType().equals(Material.AIR))
			{
				this.chestSlot = inv.firstEmpty();
			}

			if(this.chestSlot >= 0)
			{
				inv.setItem(this.chestSlot, new ItemStack(this.getJukebox().getPlaying()));
				this.getJukebox().setPlaying((Material)null);
				return true;
			}
		}
		return false;
	}

	public boolean takeFromChest()
	{
		Chest chest = this.getChest();
		if(chest != null)
		{
			Inventory inv = chest.getInventory();

			for(int i = this.chestSlot + 1; i != this.chestSlot; ++i)
			{
				if(i > inv.getSize() - 1)
				{
					i = 0;
				}

				ItemStack s = inv.getItem(i);
				if(s != null && JukeLoopPlugin.durations.containsKey(s.getType()))
				{

					this.getJukebox().setPlaying(s.getType());
					this.onInsert();
					inv.setItem(i, (ItemStack)null);
					this.chestSlot = i;
					return true;
				}
			}
		}
		return false;
	}

	public boolean isDead()
	{
		return this.getJukebox() == null;
	}

	public Location getLocation()
	{
		return this.loc;
	}

	public Jukebox getJukebox()
	{
		BlockState blockstate = this.loc.getBlock().getState();
		if(blockstate instanceof Jukebox)
		{
			return (Jukebox) blockstate;
		}
		return null;
	}

	public void onInsert()
	{
		this.startedAt = (int)(System.currentTimeMillis() / 1000L);
	}

	public void onEject()
	{
		this.startedAt = -1;
	}
	
	public LoopType getType()
	{
		return type;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof LoopingJukebox)
		{
			LoopingJukebox loopjukebox = (LoopingJukebox) obj;
			return this.getLocation().equals(loopjukebox.getLocation());
		}
		return false;
	}

	public enum LoopType
	{
		LOOP(0),ORDER(1),RANDOM(2);

		int _id;
		
		LoopType(int ordinal)
		{
			this._id = ordinal;
		}
	}
}
