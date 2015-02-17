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
package handlers.admincommandhandlers;

import hellbound.HellboundEngine;

import java.util.StringTokenizer;

import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Hellbound admin command.
 * @author DS, Gladicek
 */
public class AdminHellbound implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_hellbound_setlevel",
		"admin_hellbound"
	};
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.startsWith(ADMIN_COMMANDS[0]))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int level = Integer.parseInt(st.nextToken());
				if ((level < 0) || (level > 11))
				{
					throw new NumberFormatException();
				}
				
				HellboundEngine.getInstance().setLevel(level);
				activeChar.sendMessage("Hellbound level set to " + level);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //hellbound_setlevel 0-11");
				return false;
			}
		}
		else if (command.startsWith(ADMIN_COMMANDS[1]))
		{
			showMenu(activeChar);
			return true;
		}
		return false;
	}
	
	private void showMenu(PlayerInstance activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar.getHtmlPrefix(), "data/html/admin/hellbound.htm");
		html.replace("%hbstage%", String.valueOf(HellboundEngine.getInstance().getLevel()));
		html.replace("%trust%", String.valueOf(HellboundEngine.getInstance().getTrust()));
		html.replace("%maxtrust%", String.valueOf(HellboundEngine.getInstance().getMaxTrust()));
		html.replace("%mintrust%", String.valueOf(HellboundEngine.getInstance().getMinTrust()));
		activeChar.sendPacket(html);
	}
}
