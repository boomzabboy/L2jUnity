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

import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

public final class PartySmallWindowAll extends L2GameServerPacket
{
	private final Party _party;
	private final PlayerInstance _exclude;
	
	public PartySmallWindowAll(PlayerInstance exclude, Party party)
	{
		_exclude = exclude;
		_party = party;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x4E);
		writeD(_party.getLeaderObjectId());
		writeC(_party.getDistributionType().getId());
		writeC(_party.getMemberCount() - 1);
		
		for (PlayerInstance member : _party.getMembers())
		{
			if ((member != null) && (member != _exclude))
			{
				writeD(member.getObjectId());
				writeS(member.getName());
				
				writeD((int) member.getCurrentCp()); // c4
				writeD(member.getMaxCp()); // c4
				
				writeD((int) member.getCurrentHp());
				writeD(member.getMaxHp());
				writeD((int) member.getCurrentMp());
				writeD(member.getMaxMp());
				writeD(member.getVitalityPoints());
				writeC(member.getLevel());
				writeH(member.getClassId().getId());
				writeC(0x01); // Unk
				writeH(member.getRace().ordinal());
				final Summon pet = member.getPet();
				writeD(member.getServitors().size() + (pet != null ? 1 : 0)); // Summon size, one only atm
				if (pet != null)
				{
					writeD(pet.getObjectId());
					writeD(pet.getId() + 1000000);
					writeC(pet.getSummonType());
					writeS(pet.getName());
					writeD((int) pet.getCurrentHp());
					writeD(pet.getMaxHp());
					writeD((int) pet.getCurrentMp());
					writeD(pet.getMaxMp());
					writeC(pet.getLevel());
				}
				member.getServitors().values().forEach(s ->
				{
					writeD(s.getObjectId());
					writeD(s.getId() + 1000000);
					writeC(s.getSummonType());
					writeS(s.getName());
					writeD((int) s.getCurrentHp());
					writeD(s.getMaxHp());
					writeD((int) s.getCurrentMp());
					writeD(s.getMaxMp());
					writeC(s.getLevel());
				});
			}
		}
	}
}
