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
package handlers.targethandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.ClanMember;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.TvTEvent;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class CorpseClan implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<WorldObject> targetList = new ArrayList<>();
		if (activeChar.isPlayable())
		{
			final PlayerInstance player = activeChar.getActingPlayer();
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			if (player.isInOlympiadMode())
			{
				return new WorldObject[]
				{
					player
				};
			}
			
			final L2Clan clan = player.getClan();
			if (clan != null)
			{
				final int radius = skill.getAffectRange();
				final int maxTargets = skill.getAffectLimit();
				for (ClanMember member : clan.getMembers())
				{
					final PlayerInstance obj = member.getPlayerInstance();
					if ((obj == null) || (obj == player))
					{
						continue;
					}
					
					if (player.isInDuel())
					{
						if (player.getDuelId() != obj.getDuelId())
						{
							continue;
						}
						if (player.isInParty() && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
						{
							continue;
						}
					}
					
					// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
					if (!player.checkPvpSkill(obj, skill))
					{
						continue;
					}
					
					if (!TvTEvent.checkForTvTSkill(player, obj, skill))
					{
						continue;
					}
					
					if (!Skill.addCharacter(activeChar, obj, radius, true))
					{
						continue;
					}
					
					// check target is not in a active siege zone
					if (obj.isInsideZone(ZoneId.SIEGE) && !obj.isInSiege())
					{
						continue;
					}
					
					if (onlyFirst)
					{
						return new WorldObject[]
						{
							obj
						};
					}
					
					if ((maxTargets > 0) && (targetList.size() >= maxTargets))
					{
						break;
					}
					
					targetList.add(obj);
				}
			}
		}
		else if (activeChar.isNpc())
		{
			// for buff purposes, returns friendly mobs nearby and mob itself
			final Npc npc = (Npc) activeChar;
			if ((npc.getTemplate().getClans() == null) || npc.getTemplate().getClans().isEmpty())
			{
				return new WorldObject[]
				{
					activeChar
				};
			}
			
			targetList.add(activeChar);
			
			final Collection<WorldObject> objs = activeChar.getKnownList().getKnownObjects().values();
			int maxTargets = skill.getAffectLimit();
			for (WorldObject newTarget : objs)
			{
				if (newTarget.isNpc() && npc.isInMyClan((Npc) newTarget))
				{
					if (!Util.checkIfInRange(skill.getCastRange(), activeChar, newTarget, true))
					{
						continue;
					}
					
					if (targetList.size() >= maxTargets)
					{
						break;
					}
					
					targetList.add(newTarget);
				}
			}
		}
		
		return targetList.toArray(new WorldObject[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.CORPSE_CLAN;
	}
}
