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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.gameserver.ai.CharacterAI;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.Creature;
import com.l2jserver.gameserver.model.actor.knownlist.StaticObjectKnownList;
import com.l2jserver.gameserver.model.actor.stat.StaticObjStat;
import com.l2jserver.gameserver.model.actor.status.StaticObjStatus;
import com.l2jserver.gameserver.model.actor.templates.L2CharTemplate;
import com.l2jserver.gameserver.model.items.Weapon;
import com.l2jserver.gameserver.model.items.instance.ItemInstance;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.ShowTownMap;
import com.l2jserver.gameserver.network.serverpackets.StaticObject;

/**
 * Static Object instance.
 * @author godson
 */
public final class L2StaticObjectInstance extends Creature
{
	/** The interaction distance of the L2StaticObjectInstance */
	public static final int INTERACTION_DISTANCE = 150;
	
	private final int _staticObjectId;
	private int _meshIndex = 0; // 0 - static objects, alternate static objects
	private int _type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
	private ShowTownMap _map;
	
	/** This class may be created only by L2Character and only for AI */
	public class AIAccessor extends Creature.AIAccessor
	{
		@Override
		public L2StaticObjectInstance getActor()
		{
			return L2StaticObjectInstance.this;
		}
		
		@Override
		public void moveTo(int x, int y, int z, int offset)
		{
		}
		
		@Override
		public void moveTo(int x, int y, int z)
		{
		}
		
		@Override
		public void stopMove(Location loc)
		{
		}
		
		@Override
		public void doAttack(Creature target)
		{
		}
		
		@Override
		public void doCast(Skill skill)
		{
		}
	}
	
	@Override
	protected CharacterAI initAI()
	{
		return null;
	}
	
	/**
	 * Gets the static object ID.
	 * @return the static object ID
	 */
	@Override
	public int getId()
	{
		return _staticObjectId;
	}
	
	/**
	 * @param objectId
	 * @param template
	 * @param staticId
	 */
	public L2StaticObjectInstance(int objectId, L2CharTemplate template, int staticId)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2StaticObjectInstance);
		_staticObjectId = staticId;
	}
	
	@Override
	public final StaticObjectKnownList getKnownList()
	{
		return (StaticObjectKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new StaticObjectKnownList(this));
	}
	
	@Override
	public final StaticObjStat getStat()
	{
		return (StaticObjStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new StaticObjStat(this));
	}
	
	@Override
	public final StaticObjStatus getStatus()
	{
		return (StaticObjStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new StaticObjStatus(this));
	}
	
	public int getType()
	{
		return _type;
	}
	
	public void setType(int type)
	{
		_type = type;
	}
	
	public void setMap(String texture, int x, int y)
	{
		_map = new ShowTownMap("town_map." + texture, x, y);
	}
	
	public ShowTownMap getMap()
	{
		return _map;
	}
	
	@Override
	public final int getLevel()
	{
		return 1;
	}
	
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Set the meshIndex of the object.<br>
	 * <B><U> Values </U> :</B>
	 * <ul>
	 * <li>default textures : 0</li>
	 * <li>alternate textures : 1</li>
	 * </ul>
	 * @param meshIndex
	 */
	public void setMeshIndex(int meshIndex)
	{
		_meshIndex = meshIndex;
		this.broadcastPacket(new StaticObject(this));
	}
	
	/**
	 * <B><U> Values </U> :</B>
	 * <ul>
	 * <li>default textures : 0</li>
	 * <li>alternate textures : 1</li>
	 * </ul>
	 * @return the meshIndex of the object
	 */
	public int getMeshIndex()
	{
		return _meshIndex;
	}
	
	@Override
	public void updateAbnormalVisualEffects()
	{
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new StaticObject(this));
	}
}
