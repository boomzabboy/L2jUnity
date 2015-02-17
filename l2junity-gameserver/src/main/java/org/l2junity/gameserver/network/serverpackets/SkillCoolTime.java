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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.l2junity.gameserver.model.TimeStamp;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

/**
 * Skill Cool Time server packet implementation.
 * @author KenM, Zoey76
 */
public class SkillCoolTime extends L2GameServerPacket
{
	private final List<TimeStamp> _skillReuseTimeStamps = new ArrayList<>();
	
	public SkillCoolTime(PlayerInstance player)
	{
		final Map<Integer, TimeStamp> skillReuseTimeStamps = player.getSkillReuseTimeStamps();
		if (skillReuseTimeStamps != null)
		{
			for (TimeStamp ts : skillReuseTimeStamps.values())
			{
				if (ts.hasNotPassed())
				{
					_skillReuseTimeStamps.add(ts);
				}
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xC7);
		writeD(_skillReuseTimeStamps.size());
		for (TimeStamp ts : _skillReuseTimeStamps)
		{
			writeD(ts.getSkillId());
			writeD(0x00); // ?
			writeD((int) ts.getReuse() / 1000);
			writeD((int) ts.getRemaining() / 1000);
		}
	}
}
