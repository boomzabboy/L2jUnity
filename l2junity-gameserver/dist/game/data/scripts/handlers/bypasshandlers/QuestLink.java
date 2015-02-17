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
package handlers.bypasshandlers;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.l2junity.gameserver.handler.IBypassHandler;
import org.l2junity.gameserver.instancemanager.QuestManager;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.listeners.AbstractEventListener;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.ActionFailed;
import org.l2junity.gameserver.network.serverpackets.NpcHtmlMessage;

public class QuestLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Quest"
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance activeChar, Creature target)
	{
		String quest = "";
		try
		{
			quest = command.substring(5).trim();
		}
		catch (IndexOutOfBoundsException ioobe)
		{
		}
		if (quest.length() == 0)
		{
			showQuestWindow(activeChar, (Npc) target);
		}
		else
		{
			int questNameEnd = quest.indexOf(" ");
			if (questNameEnd == -1)
			{
				showQuestWindow(activeChar, (Npc) target, quest);
			}
			else
			{
				activeChar.processQuestEvent(quest.substring(0, questNameEnd), quest.substring(questNameEnd).trim());
			}
		}
		return true;
	}
	
	/**
	 * Open a choose quest window on client with all quests available of the L2NpcInstance.<br>
	 * <b><u>Actions</u>:</b><br>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param npc The table containing quests of the L2NpcInstance
	 * @param quests
	 */
	public static void showQuestChooseWindow(PlayerInstance player, Npc npc, Collection<Quest> quests)
	{
		final StringBuilder sb = new StringBuilder(128);
		sb.append("<html><body>");
		String state = "";
		String color = "";
		
		//@formatter:off
		final Set<Quest> startingQuests = npc.getListeners(EventType.ON_NPC_QUEST_START).stream()
			.map(AbstractEventListener::getOwner)
			.filter(Quest.class::isInstance)
			.map(Quest.class::cast)
			.distinct()
			.collect(Collectors.toSet());
		//@formatter:on
		
		for (Quest quest : quests)
		{
			final QuestState qs = player.getQuestState(quest.getScriptName());
			if ((qs == null) || qs.isCreated())
			{
				state = quest.isCustomQuest() ? "" : "01";
				if (startingQuests.contains(quest) && quest.canStartQuest(player))
				{
					color = "bbaa88";
				}
				else
				{
					color = "a62f31";
				}
			}
			else if (qs.isStarted())
			{
				state = quest.isCustomQuest() ? " (In Progress)" : "02";
				color = "ffdd66";
			}
			else if (qs.isCompleted())
			{
				state = quest.isCustomQuest() ? " (Done)" : "03";
				color = "787878";
			}
			sb.append("<font color=\"" + color + "\">");
			sb.append("<button icon=\"quest\" align=\"left\" action=\"bypass -h npc_" + npc.getObjectId() + "_Quest " + quest.getName() + "\">");
			
			if (quest.isCustomQuest())
			{
				sb.append(quest.getDescr() + state);
			}
			else
			{
				sb.append("<fstring>" + quest.getNpcStringId() + state + "</fstring>");
			}
			sb.append("</button></font>");
		}
		sb.append("</body></html>");
		
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		npc.insertObjectIdAndShowChatWindow(player, sb.toString());
	}
	
	/**
	 * Open a quest window on client with the text of the L2NpcInstance.<br>
	 * <b><u>Actions</u>:</b><br>
	 * <ul>
	 * <li>Get the text of the quest state in the folder data/scripts/quests/questId/stateId.htm</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li>
	 * </ul>
	 * @param player the L2PcInstance that talk with the {@code npc}
	 * @param npc the L2NpcInstance that chats with the {@code player}
	 * @param questId the Id of the quest to display the message
	 */
	public static void showQuestWindow(PlayerInstance player, Npc npc, String questId)
	{
		String content = null;
		
		final Quest q = QuestManager.getInstance().getQuest(questId);
		
		// Get the state of the selected quest
		final QuestState qs = player.getQuestState(questId);
		
		if (q != null)
		{
			if (((q.getId() >= 1) && (q.getId() < 20000)) && ((player.getWeightPenalty() >= 3) || !player.isInventoryUnder90(true)))
			{
				player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
				return;
			}
			
			if (qs == null)
			{
				if ((q.getId() >= 1) && (q.getId() < 20000))
				{
					// Too many ongoing quests.
					if (player.getAllActiveQuests().length > 40)
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setFile(player.getHtmlPrefix(), "data/html/fullquest.html");
						player.sendPacket(html);
						return;
					}
				}
			}
			
			q.notifyTalk(npc, player);
		}
		else
		{
			content = Quest.getNoQuestMsg(player); // no quests found
		}
		
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		if (content != null)
		{
			npc.insertObjectIdAndShowChatWindow(player, content);
		}
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.
	 * @param player the L2PcInstance that talk with the {@code npc}.
	 * @param npc the L2NpcInstance that chats with the {@code player}.
	 */
	private static void showQuestWindow(final PlayerInstance player, Npc npc)
	{
		//@formatter:off
		final Set<Quest> quests = npc.getListeners(EventType.ON_NPC_TALK).stream()
			.map(AbstractEventListener::getOwner)
			.filter(Quest.class::isInstance)
			.map(Quest.class::cast)
			.filter(quest -> (quest.getId() > 0) && (quest.getId() < 20000))
			.distinct()
			.collect(Collectors.toSet());
		//@formatter:on
		
		if (quests.size() > 1)
		{
			showQuestChooseWindow(player, npc, quests);
		}
		else if (quests.size() == 1)
		{
			showQuestWindow(player, npc, quests.stream().findFirst().get().getName());
		}
		else
		{
			showQuestWindow(player, npc, "");
		}
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
