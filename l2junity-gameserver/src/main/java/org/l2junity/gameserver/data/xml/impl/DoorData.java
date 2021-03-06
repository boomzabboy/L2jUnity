/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.data.xml.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2junity.commons.util.IXmlReader;
import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.DoorInstance;
import org.l2junity.gameserver.model.actor.templates.DoorTemplate;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.pathfinding.AbstractNodeLoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class loads and hold info about doors.
 * @author JIV, GodKratos, UnAfraid
 */
public final class DoorData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DoorData.class);
	
	// Info holders
	private final Map<String, Set<Integer>> _groups = new HashMap<>();
	private final Map<Integer, DoorInstance> _doors = new HashMap<>();
	private final Map<Integer, StatsSet> _templates = new HashMap<>();
	private final Map<Integer, List<DoorInstance>> _regions = new HashMap<>();
	
	protected DoorData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_doors.clear();
		_groups.clear();
		_regions.clear();
		parseDatapackFile("data/DoorData.xml");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "door", doorNode -> spawnDoor(parseDoor(doorNode))));
		LOGGER.info("Loaded {} Door Templates for {} regions.", _doors.size(), _regions.size());
	}
	
	public StatsSet parseDoor(Node doorNode)
	{
		final StatsSet params = new StatsSet(parseAttributes(doorNode));
		params.set("baseHpMax", 1); // Avoid doors without HP value created dead due to default value 0 in L2CharTemplate
		
		forEach(doorNode, IXmlReader::isNode, innerDoorNode ->
		{
			final NamedNodeMap attrs = innerDoorNode.getAttributes();
			if (innerDoorNode.getNodeName().equals("nodes"))
			{
				params.set("nodeZ", parseInteger(attrs, "nodeZ"));
				
				final AtomicInteger count = new AtomicInteger();
				forEach(innerDoorNode, IXmlReader::isNode, nodes ->
				{
					final NamedNodeMap nodeAttrs = nodes.getAttributes();
					if ("node".equals(nodes.getNodeName()))
					{
						params.set("nodeX_" + count.get(), parseInteger(nodeAttrs, "x"));
						params.set("nodeY_" + count.getAndIncrement(), parseInteger(nodeAttrs, "y"));
					}
				});
			}
			else if (attrs != null)
			{
				for (int i = 0; i < attrs.getLength(); i++)
				{
					final Node att = attrs.item(i);
					params.set(att.getNodeName(), att.getNodeValue());
				}
			}
		});
		
		applyCollisions(params);
		return params;
	}
	
	/**
	 * @param set
	 */
	private void applyCollisions(StatsSet set)
	{
		// Insert Collision data
		if (set.contains("nodeX_0") && set.contains("nodeY_0") && set.contains("nodeX_1") && set.contains("nodeX_1"))
		{
			int posX, posY, nodeX, nodeY, height;
			height = set.getInt("height", 150);
			nodeX = set.getInt("nodeX_0");
			nodeY = set.getInt("nodeY_0");
			posX = set.getInt("nodeX_1");
			posY = set.getInt("nodeX_1");
			int collisionRadius; // (max) radius for movement checks
			collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY));
			if (collisionRadius < 20)
			{
				collisionRadius = 20;
			}
			
			set.set("collision_radius", collisionRadius);
			set.set("collision_height", height);
		}
	}
	
	/**
	 * Spawns the door, adds the group name and registers it to templates, regions and doors also inserts collisions data
	 * @param set
	 * @return
	 */
	public DoorInstance spawnDoor(StatsSet set)
	{
		// Create door template + door instance
		final DoorTemplate template = new DoorTemplate(set);
		final DoorInstance door = spawnDoor(template, null);
		
		// Register the door
		_templates.put(door.getId(), set);
		_doors.put(door.getId(), door);
		_regions.computeIfAbsent(MapRegionManager.getInstance().getMapRegionLocId(door), key -> new ArrayList<>()).add(door);
		return door;
	}
	
	/**
	 * Spawns the door, adds the group name and registers it to templates
	 * @param template
	 * @param instance
	 * @return a new door instance based on provided template
	 */
	public DoorInstance spawnDoor(DoorTemplate template, Instance instance)
	{
		final DoorInstance door = new DoorInstance(template);
		door.setCurrentHp(door.getMaxHp());
		
		// Set instance world if provided
		if (instance != null)
		{
			door.setInstance(instance);
		}
		
		// Spawn the door on the world
		door.spawnMe(template.getX(), template.getY(), template.getZ());
		
		// Register door's group
		if (template.getGroupName() != null)
		{
			_groups.computeIfAbsent(door.getGroupName(), key -> new HashSet<>()).add(door.getId());
		}
		return door;
	}
	
	public StatsSet getDoorTemplate(int doorId)
	{
		return _templates.get(doorId);
	}
	
	public DoorInstance getDoor(int doorId)
	{
		return _doors.get(doorId);
	}
	
	public Set<Integer> getDoorsByGroup(String groupName)
	{
		return _groups.getOrDefault(groupName, Collections.emptySet());
	}
	
	public Collection<DoorInstance> getDoors()
	{
		return _doors.values();
	}
	
	public boolean checkIfDoorsBetween(AbstractNodeLoc start, AbstractNodeLoc end, Instance instance)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instance);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		return checkIfDoorsBetween(x, y, z, tx, ty, tz, instance, false);
	}
	
	/**
	 * GodKratos: TODO: remove GeoData checks from door table and convert door nodes to Geo zones
	 * @param x
	 * @param y
	 * @param z
	 * @param tx
	 * @param ty
	 * @param tz
	 * @param instance
	 * @param doubleFaceCheck
	 * @return {@code boolean}
	 */
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance, boolean doubleFaceCheck)
	{
		final Collection<DoorInstance> allDoors = (instance != null) ? instance.getDoors() : _regions.get(MapRegionManager.getInstance().getMapRegionLocId(x, y));
		if (allDoors == null)
		{
			return false;
		}
		
		for (DoorInstance doorInst : allDoors)
		{
			// check dead and open
			if (doorInst.isDead() || doorInst.isOpen() || !doorInst.checkCollision() || (doorInst.getX(0) == 0))
			{
				continue;
			}
			
			boolean intersectFace = false;
			for (int i = 0; i < 4; i++)
			{
				int j = (i + 1) < 4 ? i + 1 : 0;
				// lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel
				int denominator = ((ty - y) * (doorInst.getX(i) - doorInst.getX(j))) - ((tx - x) * (doorInst.getY(i) - doorInst.getY(j)));
				if (denominator == 0)
				{
					continue;
				}
				
				// multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect
				float multiplier1 = (float) (((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i))) - ((doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i)))) / denominator;
				float multiplier2 = (float) (((tx - x) * (y - doorInst.getY(i))) - ((ty - y) * (x - doorInst.getX(i)))) / denominator;
				if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
				{
					int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
					// now checking if the resulting point is between door's min and max z
					if ((intersectZ > doorInst.getZMin()) && (intersectZ < doorInst.getZMax()))
					{
						if (!doubleFaceCheck || intersectFace)
						{
							return true;
						}
						intersectFace = true;
					}
				}
			}
		}
		return false;
	}
	
	public static DoorData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorData _instance = new DoorData();
	}
}
