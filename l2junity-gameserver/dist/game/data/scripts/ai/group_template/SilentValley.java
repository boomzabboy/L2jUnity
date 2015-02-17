/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.group_template;

import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.network.NpcStringId;

import ai.npc.AbstractNpcAI;

/**
 * Silent Valley AI
 * @author malyelfik
 */
public final class SilentValley extends AbstractNpcAI
{
	// Skills
	private static final SkillHolder BETRAYAL = new SkillHolder(6033, 1); // Treasure Seeker's Betrayal
	private static final SkillHolder BLAZE = new SkillHolder(4157, 10); // NPC Blaze - Magic
	// Item
	private static final int SACK = 13799; // Treasure Sack of the Ancient Giants
	// Chance
	private static final int SPAWN_CHANCE = 2;
	private static final int CHEST_DIE_CHANCE = 5;
	// Monsters
	private static final int CHEST = 18693; // Treasure Chest of the Ancient Giants
	private static final int GUARD1 = 18694; // Treasure Chest Guard
	private static final int GUARD2 = 18695; // Treasure Chest Guard
	private static final int[] MOBS =
	{
		20965, // Chimera Piece
		20966, // Changed Creation
		20967, // Past Creature
		20968, // Nonexistent Man
		20969, // Giant's Shadow
		20970, // Soldier of Ancient Times
		20971, // Warrior of Ancient Times
		20972, // Shaman of Ancient Times
		20973, // Forgotten Ancient People
	};
	
	private SilentValley()
	{
		super(SilentValley.class.getSimpleName(), "ai/group_template");
		addAttackId(MOBS);
		addAttackId(CHEST, GUARD1, GUARD2);
		addEventReceivedId(GUARD1, GUARD2);
		addKillId(MOBS);
		addSeeCreatureId(MOBS);
		addSeeCreatureId(GUARD1, GUARD2);
		addSpawnId(CHEST, GUARD2);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if ((npc != null) && !npc.isDead())
		{
			switch (event)
			{
				case "CLEAR":
					npc.doDie(null);
					break;
				case "CLEAR_EVENT":
					npc.broadcastEvent("CLEAR_ALL_INSTANT", 2000, null);
					npc.doDie(null);
					break;
				case "SPAWN_CHEST":
					addSpawn(CHEST, npc.getX() - 100, npc.getY(), npc.getZ() - 100, 0, false, 0);
					break;
			}
		}
		return null;
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance player, int damage, boolean isSummon)
	{
		switch (npc.getId())
		{
			case CHEST:
			{
				if (!isSummon && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					broadcastNpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.YOU_WILL_BE_CURSED_FOR_SEEKING_THE_TREASURE);
					npc.setTarget(player);
					npc.doCast(BETRAYAL.getSkill());
				}
				else if (isSummon || (getRandom(100) < CHEST_DIE_CHANCE))
				{
					npc.dropItem(player, SACK, 1);
					npc.broadcastEvent("CLEAR_ALL", 2000, null);
					npc.doDie(null);
					cancelQuestTimer("CLEAR_EVENT", npc, null);
				}
				break;
			}
			case GUARD1:
			case GUARD2:
			{
				npc.setTarget(player);
				npc.doCast(BLAZE.getSkill());
				addAttackPlayerDesire(npc, player);
				break;
			}
			default:
			{
				if (isSummon)
				{
					addAttackPlayerDesire(npc, player);
				}
			}
		}
		return super.onAttack(npc, player, damage, isSummon);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		if (getRandom(1000) < SPAWN_CHANCE)
		{
			final int newZ = npc.getZ() + 100;
			addSpawn(GUARD2, npc.getX() + 100, npc.getY(), newZ, 0, false, 0);
			addSpawn(GUARD1, npc.getX() - 100, npc.getY(), newZ, 0, false, 0);
			addSpawn(GUARD1, npc.getX(), npc.getY() + 100, newZ, 0, false, 0);
			addSpawn(GUARD1, npc.getX(), npc.getY() - 100, newZ, 0, false, 0);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (creature.isPlayable())
		{
			final PlayerInstance player = (isSummon) ? ((Summon) creature).getOwner() : creature.getActingPlayer();
			if ((npc.getId() == GUARD1) || (npc.getId() == GUARD2))
			{
				npc.setTarget(player);
				npc.doCast(BLAZE.getSkill());
				addAttackPlayerDesire(npc, player);
			}
			else if (creature.isAffectedBySkill(BETRAYAL.getSkillId()))
			{
				addAttackPlayerDesire(npc, player);
			}
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
	
	@Override
	public String onSpawn(Npc npc)
	{
		if (npc.getId() == CHEST)
		{
			npc.setIsInvul(true);
			startQuestTimer("CLEAR_EVENT", 300000, npc, null);
		}
		else
		{
			startQuestTimer("SPAWN_CHEST", 10000, npc, null);
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if ((receiver != null) && !receiver.isDead())
		{
			switch (eventName)
			{
				case "CLEAR_ALL":
					startQuestTimer("CLEAR", 60000, receiver, null);
					break;
				case "CLEAR_ALL_INSTANT":
					receiver.doDie(null);
					break;
			}
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	public static void main(String[] args)
	{
		new SilentValley();
	}
}