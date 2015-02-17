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
package org.l2junity.gameserver.network.serverpackets.shuttle;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author UnAfraid
 */
public class ExStopMoveInShuttle extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _boatId;
	private final Location _pos;
	private final int _heading;
	
	public ExStopMoveInShuttle(PlayerInstance player, int boatId)
	{
		_charObjId = player.getObjectId();
		_boatId = boatId;
		_pos = player.getInVehiclePosition();
		_heading = player.getHeading();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xD0);
		
		writeD(_charObjId);
		writeD(_boatId);
		writeLoc(_pos);
		writeD(_heading);
	}
}
