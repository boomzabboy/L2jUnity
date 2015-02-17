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
package org.l2junity.gameserver.network.clientpackets;

import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.serverpackets.PledgeShowMemberListAll;

/**
 * This class ...
 * @version $Revision: 1.5.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPledgeMemberList extends L2GameClientPacket
{
	private static final String _C__4D_REQUESTPLEDGEMEMBERLIST = "[C] 4D RequestPledgeMemberList";
	
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		PlayerInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			PledgeShowMemberListAll pm = new PledgeShowMemberListAll(clan);
			activeChar.sendPacket(pm);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__4D_REQUESTPLEDGEMEMBERLIST;
	}
}
