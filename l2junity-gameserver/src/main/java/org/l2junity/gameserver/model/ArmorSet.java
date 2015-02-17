/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.ArmorsetSkillHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.itemcontainer.PcInventory;
import org.l2junity.gameserver.model.items.instance.ItemInstance;

/**
 * @author Luno
 */
public final class ArmorSet
{
	private boolean _isVisual;
	private int _minimumPieces;
	private final List<Integer> _chests;
	private final List<Integer> _legs;
	private final List<Integer> _head;
	private final List<Integer> _gloves;
	private final List<Integer> _feet;
	private final List<Integer> _shield;
	
	private final List<ArmorsetSkillHolder> _skills;
	private final List<SkillHolder> _shieldSkills;
	private final List<ArmorsetSkillHolder> _enchantSkills;
	
	private int _con;
	private int _dex;
	private int _str;
	private int _men;
	private int _wit;
	private int _int;
	
	private static final int[] ARMORSET_SLOTS = new int[]
	{
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET
	};
	
	public ArmorSet()
	{
		_chests = new ArrayList<>();
		_legs = new ArrayList<>();
		_head = new ArrayList<>();
		_gloves = new ArrayList<>();
		_feet = new ArrayList<>();
		_shield = new ArrayList<>();
		
		_skills = new ArrayList<>();
		_shieldSkills = new ArrayList<>();
		_enchantSkills = new ArrayList<>();
	}
	
	public boolean isVisual()
	{
		return _isVisual;
	}
	
	public void setIsVisual(boolean val)
	{
		_isVisual = val;
	}
	
	/**
	 * @return the minimum amount of pieces equipped to form a set.
	 */
	public int getMinimumPieces()
	{
		return _minimumPieces;
	}
	
	public void setMinimumPieces(int pieces)
	{
		_minimumPieces = pieces;
	}
	
	public void addChest(int id)
	{
		_chests.add(id);
	}
	
	public void addLegs(int id)
	{
		_legs.add(id);
	}
	
	public void addHead(int id)
	{
		_head.add(id);
	}
	
	public void addGloves(int id)
	{
		_gloves.add(id);
	}
	
	public void addFeet(int id)
	{
		_feet.add(id);
	}
	
	public void addShield(int id)
	{
		_shield.add(id);
	}
	
	public void addSkill(ArmorsetSkillHolder holder)
	{
		_skills.add(holder);
	}
	
	public void addShieldSkill(SkillHolder holder)
	{
		_shieldSkills.add(holder);
	}
	
	public void addEnchantSkill(ArmorsetSkillHolder holder)
	{
		_enchantSkills.add(holder);
	}
	
	public void addCon(int val)
	{
		_con = val;
	}
	
	public void addDex(int val)
	{
		_dex = val;
	}
	
	public void addStr(int val)
	{
		_str = val;
	}
	
	public void addMen(int val)
	{
		_men = val;
	}
	
	public void addWit(int val)
	{
		_wit = val;
	}
	
	public void addInt(int val)
	{
		_int = val;
	}
	
	public int getPiecesCount(PlayerInstance player)
	{
		final Inventory inv = player.getInventory();
		
		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
		{
			legs = legsItem.getId();
		}
		if (headItem != null)
		{
			head = headItem.getId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getId();
		}
		
		return getPiecesCount(legs, head, gloves, feet);
	}
	
	public int getPiecesCount(int legs, int head, int gloves, int feet)
	{
		int pieces = 1;
		if (_legs.contains(legs))
		{
			pieces++;
		}
		if (_head.contains(head))
		{
			pieces++;
		}
		if (_gloves.contains(gloves))
		{
			pieces++;
		}
		if (_feet.contains(feet))
		{
			pieces++;
		}
		
		return pieces;
	}
	
	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
			{
				return _chests.contains(itemId);
			}
			case Inventory.PAPERDOLL_LEGS:
			{
				return _legs.contains(itemId);
			}
			case Inventory.PAPERDOLL_HEAD:
			{
				return _head.contains(itemId);
			}
			case Inventory.PAPERDOLL_GLOVES:
			{
				return _gloves.contains(itemId);
			}
			case Inventory.PAPERDOLL_FEET:
			{
				return _feet.contains(itemId);
			}
			default:
			{
				return false;
			}
		}
	}
	
	public List<Integer> getChests()
	{
		return _chests;
	}
	
	public List<ArmorsetSkillHolder> getSkills()
	{
		return _skills;
	}
	
	public boolean containShield(PlayerInstance player)
	{
		Inventory inv = player.getInventory();
		
		ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return ((shieldItem != null) && _shield.contains(Integer.valueOf(shieldItem.getId())));
	}
	
	public boolean containShield(int shield_id)
	{
		if (_shield.isEmpty())
		{
			return false;
		}
		
		return _shield.contains(Integer.valueOf(shield_id));
	}
	
	public List<SkillHolder> getShieldSkills()
	{
		return _shieldSkills;
	}
	
	public List<ArmorsetSkillHolder> getEnchantSkills()
	{
		return _enchantSkills;
	}
	
	/**
	 * @param player
	 * @return true if all parts of set are enchanted to +6 or more
	 */
	public int getLowestSetEnchant(PlayerInstance player)
	{
		// Player don't have full set
		if (getPiecesCount(player) < getMinimumPieces())
		{
			return 0;
		}
		
		final PcInventory inv = player.getInventory();
		
		// No Chest - No Bonus
		if (inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST) == null)
		{
			return 0;
		}
		
		int enchantLevel = Byte.MAX_VALUE;
		for (int armorSlot : ARMORSET_SLOTS)
		{
			final ItemInstance itemPart = inv.getPaperdollItem(armorSlot);
			if (itemPart != null)
			{
				if (enchantLevel > itemPart.getEnchantLevel())
				{
					enchantLevel = itemPart.getEnchantLevel();
				}
			}
		}
		if (enchantLevel == Byte.MAX_VALUE)
		{
			enchantLevel = 0;
		}
		return enchantLevel;
		
	}
	
	public int getVisualPiecesCount(PlayerInstance player)
	{
		final Inventory inv = player.getInventory();
		
		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
		{
			legs = legsItem.getVisualId();
		}
		if (headItem != null)
		{
			head = headItem.getVisualId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getVisualId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getVisualId();
		}
		return getPiecesCount(legs, head, gloves, feet);
	}
	
	public int getCON()
	{
		return _con;
	}
	
	public int getDEX()
	{
		return _dex;
	}
	
	public int getSTR()
	{
		return _str;
	}
	
	public int getMEN()
	{
		return _men;
	}
	
	public int getWIT()
	{
		return _wit;
	}
	
	public int getINT()
	{
		return _int;
	}
}
