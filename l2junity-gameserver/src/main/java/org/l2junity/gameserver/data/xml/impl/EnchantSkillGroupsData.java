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
package org.l2junity.gameserver.data.xml.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.l2junity.Config;
import org.l2junity.gameserver.data.xml.IXmlReader;
import org.l2junity.gameserver.model.EnchantSkillGroup;
import org.l2junity.gameserver.model.EnchantSkillLearn;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.EnchantSkillGroup.EnchantSkillHolder;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class holds the Enchant Groups information.
 * @author Micr0
 */
public class EnchantSkillGroupsData implements IXmlReader
{
	public static final int NORMAL_ENCHANT_COST_MULTIPLIER = Config.NORMAL_ENCHANT_COST_MULTIPLIER;
	public static final int SAFE_ENCHANT_COST_MULTIPLIER = Config.SAFE_ENCHANT_COST_MULTIPLIER;
	
	public static final int NORMAL_ENCHANT_BOOK = 6622;
	public static final int SAFE_ENCHANT_BOOK = 9627;
	public static final int CHANGE_ENCHANT_BOOK = 9626;
	public static final int UNTRAIN_ENCHANT_BOOK = 9625;
	
	private final Map<Integer, EnchantSkillGroup> _enchantSkillGroups = new HashMap<>();
	private final Map<Integer, EnchantSkillLearn> _enchantSkillTrees = new HashMap<>();
	
	/**
	 * Instantiates a new enchant groups table.
	 */
	protected EnchantSkillGroupsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_enchantSkillGroups.clear();
		_enchantSkillTrees.clear();
		parseDatapackFile("data/enchantSkillGroups.xml");
		int routes = 0;
		for (EnchantSkillGroup group : _enchantSkillGroups.values())
		{
			routes += group.getEnchantGroupDetails().size();
		}
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _enchantSkillGroups.size() + " groups and " + routes + " routes.");
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		NamedNodeMap attrs;
		StatsSet set;
		Node att;
		int id = 0;
		EnchantSkillGroup group;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("group".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						id = parseInteger(attrs, "id");
						
						group = _enchantSkillGroups.get(id);
						if (group == null)
						{
							group = new EnchantSkillGroup(id);
							_enchantSkillGroups.put(id, group);
						}
						
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("enchant".equalsIgnoreCase(b.getNodeName()))
							{
								attrs = b.getAttributes();
								set = new StatsSet();
								
								for (int i = 0; i < attrs.getLength(); i++)
								{
									att = attrs.item(i);
									set.set(att.getNodeName(), att.getNodeValue());
								}
								group.addEnchantDetail(new EnchantSkillHolder(set));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Adds the new route for skill.
	 * @param skillId the skill id
	 * @param maxLvL the max lvl
	 * @param route the route
	 * @param group the group
	 * @return the int
	 */
	public int addNewRouteForSkill(int skillId, int maxLvL, int route, int group)
	{
		EnchantSkillLearn enchantableSkill = _enchantSkillTrees.get(skillId);
		if (enchantableSkill == null)
		{
			enchantableSkill = new EnchantSkillLearn(skillId, maxLvL);
			_enchantSkillTrees.put(skillId, enchantableSkill);
		}
		if (_enchantSkillGroups.containsKey(group))
		{
			enchantableSkill.addNewEnchantRoute(route, group);
			
			return _enchantSkillGroups.get(group).getEnchantGroupDetails().size();
		}
		LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error while loading generating enchant skill id: " + skillId + "; route: " + route + "; missing group: " + group);
		return 0;
	}
	
	/**
	 * Gets the skill enchantment for skill.
	 * @param skill the skill
	 * @return the skill enchantment for skill
	 */
	public EnchantSkillLearn getSkillEnchantmentForSkill(Skill skill)
	{
		// there is enchantment for this skill and we have the required level of it
		final EnchantSkillLearn esl = getSkillEnchantmentBySkillId(skill.getId());
		if ((esl != null) && (skill.getLevel() >= esl.getBaseLevel()))
		{
			return esl;
		}
		return null;
	}
	
	/**
	 * Gets the skill enchantment by skill id.
	 * @param skillId the skill id
	 * @return the skill enchantment by skill id
	 */
	public EnchantSkillLearn getSkillEnchantmentBySkillId(int skillId)
	{
		return _enchantSkillTrees.get(skillId);
	}
	
	/**
	 * Gets the enchant skill group by id.
	 * @param id the id
	 * @return the enchant skill group by id
	 */
	public EnchantSkillGroup getEnchantSkillGroupById(int id)
	{
		return _enchantSkillGroups.get(id);
	}
	
	/**
	 * Gets the enchant skill sp cost.
	 * @param skill the skill
	 * @return the enchant skill sp cost
	 */
	public int getEnchantSkillSpCost(Skill skill)
	{
		final EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
		if (enchantSkillLearn != null)
		{
			final EnchantSkillHolder esh = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
			if (esh != null)
			{
				return esh.getSpCost();
			}
		}
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Gets the enchant skill Adena cost.
	 * @param skill the skill
	 * @return the enchant skill Adena cost
	 */
	public int getEnchantSkillAdenaCost(Skill skill)
	{
		final EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
		if (enchantSkillLearn != null)
		{
			final EnchantSkillHolder esh = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
			if (esh != null)
			{
				return esh.getAdenaCost();
			}
		}
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Gets the enchant skill rate.
	 * @param player the player
	 * @param skill the skill
	 * @return the enchant skill rate
	 */
	public byte getEnchantSkillRate(PlayerInstance player, Skill skill)
	{
		final EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
		if (enchantSkillLearn != null)
		{
			final EnchantSkillHolder esh = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
			if (esh != null)
			{
				return esh.getRate(player);
			}
		}
		return 0;
	}
	
	/**
	 * Gets the single instance of EnchantGroupsData.
	 * @return single instance of EnchantGroupsData
	 */
	public static EnchantSkillGroupsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantSkillGroupsData _instance = new EnchantSkillGroupsData();
	}
}