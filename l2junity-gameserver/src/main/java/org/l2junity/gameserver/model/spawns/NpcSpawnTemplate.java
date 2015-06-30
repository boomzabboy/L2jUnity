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
package org.l2junity.gameserver.model.spawns;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.data.xml.impl.NpcData;
import org.l2junity.gameserver.datatables.SpawnTable;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.ChanceLocation;
import org.l2junity.gameserver.model.L2Spawn;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.holders.MinionHolder;
import org.l2junity.gameserver.model.interfaces.IParameterized;
import org.l2junity.gameserver.model.zone.type.NpcSpawnTerritory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author UnAfraid
 */
public class NpcSpawnTemplate implements IParameterized<StatsSet>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SpawnTemplate.class);
	
	private final int _id;
	private final int _count;
	private final Duration _respawnTime;
	private final Duration _respawnTimeRandom;
	private List<ChanceLocation> _locations;
	private NpcSpawnTerritory _zone;
	private StatsSet _parameters;
	private List<MinionHolder> _minions;
	private final SpawnTemplate _spawnTemplate;
	private final SpawnGroup _group;
	private final Set<Npc> _spawnedNpcs = ConcurrentHashMap.newKeySet();
	
	public NpcSpawnTemplate(SpawnTemplate spawnTemplate, SpawnGroup group, StatsSet set) throws Exception
	{
		_spawnTemplate = spawnTemplate;
		_group = group;
		_id = set.getInt("id");
		_count = set.getInt("count", 1);
		_respawnTime = set.getDuration("respawnTime", null);
		_respawnTimeRandom = set.getDuration("respawnRandom", null);
		
		final int x = set.getInt("x", Integer.MAX_VALUE);
		final int y = set.getInt("y", Integer.MAX_VALUE);
		final int z = set.getInt("z", Integer.MAX_VALUE);
		final boolean xDefined = x != Integer.MAX_VALUE;
		final boolean yDefined = y != Integer.MAX_VALUE;
		final boolean zDefined = z != Integer.MAX_VALUE;
		if (xDefined && yDefined && zDefined)
		{
			_locations = new ArrayList<>();
			_locations.add(new ChanceLocation(x, y, z, set.getInt("heading", 0), 100));
		}
		else
		{
			if (xDefined || yDefined || zDefined)
			{
				throw new IllegalStateException(String.format("Spawn with partially declared and x: %s y: %s z: %s!", processParam(x), processParam(y), processParam(z)));
			}
			
			final String zoneName = set.getString("zone", null);
			if (zoneName == null)
			{
				return;
			}
			
			final NpcSpawnTerritory zone = ZoneManager.getInstance().getSpawnTerritory(zoneName);
			if (zone == null)
			{
				throw new NullPointerException("Spawn with non existing zone requested " + zoneName);
			}
			_zone = zone;
		}
	}
	
	public void addSpawnLocation(ChanceLocation loc)
	{
		if (_locations == null)
		{
			_locations = new ArrayList<>();
		}
		_locations.add(loc);
	}
	
	public SpawnTemplate getSpawnTemplate()
	{
		return _spawnTemplate;
	}
	
	public SpawnGroup getGroup()
	{
		return _group;
	}
	
	private String processParam(int value)
	{
		return value != Integer.MAX_VALUE ? Integer.toString(value) : "undefined";
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public Duration getRespawnTime()
	{
		return _respawnTime;
	}
	
	public Duration getRespawnTimeRandom()
	{
		return _respawnTimeRandom;
	}
	
	public List<ChanceLocation> getLocation()
	{
		return _locations;
	}
	
	public NpcSpawnTerritory getZone()
	{
		return _zone;
	}
	
	@Override
	public StatsSet getParameters()
	{
		return _parameters;
	}
	
	@Override
	public void setParameters(StatsSet parameters)
	{
		_parameters = parameters;
	}
	
	public List<MinionHolder> getMinions()
	{
		return _minions != null ? _minions : Collections.emptyList();
	}
	
	public void addMinion(MinionHolder minion)
	{
		if (_minions == null)
		{
			_minions = new ArrayList<>();
		}
		_minions.add(minion);
	}
	
	public Set<Npc> getSpawnedNpcs()
	{
		return _spawnedNpcs;
	}
	
	public final Location getSpawnLocation()
	{
		if (_locations != null)
		{
			final double locRandom = (100 * Rnd.nextDouble());
			float cumulativeChance = 0;
			for (ChanceLocation loc : _locations)
			{
				if (locRandom <= (cumulativeChance += loc.getChance()))
				{
					return loc;
				}
			}
			LOGGER.warn("Couldn't match location by chance turning first..");
			return null;
		}
		else if (_zone != null)
		{
			final Location loc = _zone.getRandomPoint();
			loc.setHeading(Rnd.get(65535));
			return loc;
		}
		return null;
	}
	
	public void spawn()
	{
		try
		{
			final L2NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_id);
			if (npcTemplate != null)
			{
				final L2Spawn spawn = new L2Spawn(npcTemplate);
				final Location loc = getSpawnLocation();
				if (loc == null)
				{
					LOGGER.warn("Couldn't initialize new spawn, no location found!");
					return;
				}
				
				spawn.setXYZ(loc);
				spawn.setHeading(loc.getHeading());
				spawn.setAmount(_count);
				int respawn = 0, respawnRandom = 0;
				if (_respawnTime != null)
				{
					respawn = (int) _respawnTime.getSeconds();
				}
				if (_respawnTimeRandom != null)
				{
					respawnRandom = (int) _respawnTimeRandom.getSeconds();
				}
				
				if ((respawn > 0) || (respawnRandom > 0))
				{
					spawn.setRespawnDelay(respawn, respawnRandom);
					spawn.startRespawn();
				}
				else
				{
					spawn.stopRespawn();
				}
				
				spawn.setSpawnTemplate(this);
				for (int i = 0; i < spawn.getAmount(); i++)
				{
					final Npc npc = spawn.doSpawn();
					if (npc.isMonster() && (_minions != null))
					{
						((L2MonsterInstance) npc).getMinionList().spawnMinions(_minions);
					}
					_spawnedNpcs.add(npc);
				}
				SpawnTable.getInstance().addNewSpawn(spawn, false);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't spawn npc {}", _id, e);
		}
	}
	
	public void despawn()
	{
		_spawnedNpcs.forEach(npc ->
		{
			SpawnTable.getInstance().deleteSpawn(npc.getSpawn(), false);
			npc.deleteMe();
		});
		_spawnedNpcs.clear();
	}
	
	public void notifySpawnNpc(Npc npc)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnNpc(_spawnTemplate, _group, npc));
	}
	
	public void notifyDespawnNpc(Npc npc)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnDespawnNpc(_spawnTemplate, _group, npc));
	}
	
	public void notifyNpcDeath(Npc npc, Creature killer)
	{
		_spawnTemplate.notifyEvent(event -> event.onSpawnNpcDeath(_spawnTemplate, _group, npc, killer));
	}
}