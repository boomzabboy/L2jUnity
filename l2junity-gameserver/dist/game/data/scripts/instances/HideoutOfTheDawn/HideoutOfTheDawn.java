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
package instances.HideoutOfTheDawn;

import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

import instances.AbstractInstance;

/**
 * Hideout of the Dawn instance zone.
 * @author Adry_85
 */
public final class HideoutOfTheDawn extends AbstractInstance
{
	// NPCs
	private static final int WOOD = 32593;
	private static final int JAINA = 32617;
	// Misc
	private static final int TEMPLATE_ID = 113;
	
	public HideoutOfTheDawn()
	{
		addStartNpc(WOOD);
		addTalkId(WOOD, JAINA);
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance talker)
	{
		if (npc.getId() == WOOD)
		{
			enterInstance(talker, npc, TEMPLATE_ID);
			return "32593-01.htm";
		}
		finishInstance(talker, 0);
		return "32617-01.htm";
	}
	
	public static void main(String[] args)
	{
		new HideoutOfTheDawn();
	}
}