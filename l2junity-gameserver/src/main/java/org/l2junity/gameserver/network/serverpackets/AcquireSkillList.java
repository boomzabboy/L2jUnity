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
package org.l2junity.gameserver.network.serverpackets;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.model.SkillLearn;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * @author Sdw
 */
public class AcquireSkillList extends L2GameServerPacket
{
	final PlayerInstance _activeChar;
	final List<SkillLearn> _learnable;
	
	public AcquireSkillList(PlayerInstance activeChar)
	{
		_activeChar = activeChar;
		_learnable = SkillTreesData.getInstance().getAvailableSkills(activeChar, activeChar.getClassId(), false, false);
		_learnable.addAll(SkillTreesData.getInstance().getNextAvailableSkills(activeChar, activeChar.getClassId(), false, false));
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0x90);
		writeH(_learnable.size());
		for (SkillLearn skill : _learnable)
		{
			writeD(skill.getSkillId());
			writeH(skill.getSkillLevel());
			writeQ(skill.getLevelUpSp());
			writeC(skill.getGetLevel());
			writeC(0x00); // Dual Class Level Required
			writeC(skill.getRequiredItems().size());
			for (ItemHolder item : skill.getRequiredItems())
			{
				writeD(item.getId());
				writeQ(item.getCount());
			}
			
			final List<Skill> skillRem = skill.getRemoveSkills().stream().map(_activeChar::getKnownSkill).filter(Objects::nonNull).collect(Collectors.toList());
			
			writeC(skillRem.size());
			for (Skill skillRemove : skillRem)
			{
				writeD(skillRemove.getId());
				writeH(skillRemove.getLevel());
			}
		}
	}
}
