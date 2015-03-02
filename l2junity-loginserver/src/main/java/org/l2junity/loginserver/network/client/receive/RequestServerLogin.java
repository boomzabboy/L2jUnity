/*
 * Copyright (C) 2004-2014 L2J Server
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
package org.l2junity.loginserver.network.client.receive;

import org.l2junity.loginserver.Config;
import org.l2junity.loginserver.manager.LoginManager;
import org.l2junity.loginserver.network.client.ClientHandler;
import org.l2junity.loginserver.network.client.send.LoginFail2;
import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.PacketReader;

/**
 * @author UnAfraid
 */
public class RequestServerLogin implements IIncomingPacket<ClientHandler>
{
	private long _loginSessionId;
	private short _serverId;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_loginSessionId = packet.readQ();
		_serverId = packet.readC();
		return true;
	}
	
	@Override
	public void run(ClientHandler client)
	{
		if (Config.SHOW_LICENCE && (client.getLoginSessionId() != _loginSessionId))
		{
			client.close(LoginFail2.ACCESS_FAILED_PLEASE_TRY_AGAIN_LATER);
			return;
		}
		
		LoginManager.getInstance().tryServerLogin(client, _serverId);
	}
}
