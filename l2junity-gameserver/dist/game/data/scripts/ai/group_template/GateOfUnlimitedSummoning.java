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
package ai.group_template;

import java.util.HashMap;
import java.util.Map;

import org.l2junity.gameserver.datatables.SkillData;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;

import ai.npc.AbstractNpcAI;

/**
 * Death Gate AI.
 * @author Sdw
 */
public final class GateOfUnlimitedSummoning extends AbstractNpcAI
{
	// NPCs
	private static final Map<Integer, Integer> DEATH_GATE = new HashMap<>(); // ai_gate_of_unlimited_summoning
	
	static
	{
		DEATH_GATE.put(14927, 1); // Death Gate
		DEATH_GATE.put(15200, 2); // Death Gate
		DEATH_GATE.put(15201, 3); // Death Gate
		DEATH_GATE.put(15202, 4); // Death Gate
	}
	
	// Skills
	final static private int GATE_ROOT = 11289;
	final static private int GATE_VORTEX = 11291;
	
	private GateOfUnlimitedSummoning()
	{
		super(GateOfUnlimitedSummoning.class.getSimpleName(), "ai/group_template");
		addSpawnId(DEATH_GATE.keySet());
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		final Creature summoner = npc.getSummoner();
		if ((summoner != null) && summoner.isPlayer())
		{
			final PlayerInstance player = summoner.getActingPlayer();
			getTimers().addTimer("SKILL_CAST_SLOW", 1000, npc, player);
			getTimers().addTimer("SKILL_CAST_DAMAGE", 2000, npc, player);
			getTimers().addTimer("END_OF_LIFE", 30000, npc, player);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		if (event.equals("SKILL_CAST_SLOW"))
		{
			final int skillLevel = DEATH_GATE.get(npc.getId());
			if (skillLevel > 0)
			{
				final Skill skill = SkillData.getInstance().getSkill(GATE_ROOT, skillLevel);
				if (skill != null)
				{
					npc.doCast(skill);
				}
			}
			getTimers().addTimer("SKILL_CAST_SLOW", 3000, npc, player);
		}
		else if (event.equals("SKILL_CAST_DAMAGE"))
		{
			final Skill skill = SkillData.getInstance().getSkill(GATE_VORTEX, 1);
			if (skill != null)
			{
				npc.doCast(skill);
			}
			
			getTimers().addTimer("SKILL_CAST_DAMAGE", 2000, npc, player);
		}
		else if (event.equals("END_OF_LIFE"))
		{
			getTimers().cancelTimer("SKILL_CAST_SLOW", npc, player);
			getTimers().cancelTimer("SKILL_CAST_DAMAGE", npc, player);
			npc.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new GateOfUnlimitedSummoning();
	}
}
