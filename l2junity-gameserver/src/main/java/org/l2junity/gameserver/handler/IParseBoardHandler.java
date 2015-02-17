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
package org.l2junity.gameserver.handler;

import java.util.logging.Logger;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

/**
 * Community Board interface.
 * @author Zoey76
 */
public interface IParseBoardHandler
{
	public static final Logger LOG = Logger.getLogger(IParseBoardHandler.class.getName());
	
	/**
	 * Parses a community board command.
	 * @param command the command
	 * @param player the player
	 * @return
	 */
	public boolean parseCommunityBoardCommand(String command, PlayerInstance player);
	
	/**
	 * Gets the community board commands.
	 * @return the community board commands
	 */
	public String[] getCommunityBoardCommands();
}
