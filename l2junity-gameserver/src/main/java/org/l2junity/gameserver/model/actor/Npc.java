/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.actor;

import java.util.List;

import org.l2junity.Config;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ItemsAutoDestroy;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.cache.HtmCache;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.AISkillScope;
import org.l2junity.gameserver.enums.AIType;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.enums.InstanceType;
import org.l2junity.gameserver.enums.PrivateStoreType;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.enums.Team;
import org.l2junity.gameserver.handler.BypassHandler;
import org.l2junity.gameserver.handler.IBypassHandler;
import org.l2junity.gameserver.instancemanager.CHSiegeManager;
import org.l2junity.gameserver.instancemanager.CastleManager;
import org.l2junity.gameserver.instancemanager.FortManager;
import org.l2junity.gameserver.instancemanager.TownManager;
import org.l2junity.gameserver.instancemanager.WalkingManager;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.L2Spawn;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import org.l2junity.gameserver.model.actor.instance.L2DoormenInstance;
import org.l2junity.gameserver.model.actor.instance.L2FishermanInstance;
import org.l2junity.gameserver.model.actor.instance.L2MerchantInstance;
import org.l2junity.gameserver.model.actor.instance.L2TeleporterInstance;
import org.l2junity.gameserver.model.actor.instance.L2WarehouseInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.stat.NpcStat;
import org.l2junity.gameserver.model.actor.status.NpcStatus;
import org.l2junity.gameserver.model.actor.tasks.npc.RandomAnimationTask;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.entity.Castle;
import org.l2junity.gameserver.model.entity.Fort;
import org.l2junity.gameserver.model.entity.clanhall.SiegableHall;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcCanBeSeen;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcEventReceived;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcSkillFinished;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcSpawn;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcTeleport;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.olympiad.Olympiad;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2junity.gameserver.model.variables.NpcVariables;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.ExChangeNpcState;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.NpcInfo;
import org.l2junity.gameserver.network.client.send.NpcInfoAbnormalVisualEffect;
import org.l2junity.gameserver.network.client.send.NpcSay;
import org.l2junity.gameserver.network.client.send.ServerObjectInfo;
import org.l2junity.gameserver.network.client.send.SocialAction;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.taskmanager.DecayTaskManager;
import org.l2junity.gameserver.util.Broadcast;

/**
 * This class represents a Non-Player-Character in the world.<br>
 * It can be a monster or a friendly character.<br>
 * It uses a template to fetch some static values.
 */
public class Npc extends Creature
{
	/** The interaction distance of the L2NpcInstance(is used as offset in MovetoLocation method) */
	public static final int INTERACTION_DISTANCE = 150;
	/** Maximum distance where the drop may appear given this NPC position. */
	public static final int RANDOM_ITEM_DROP_LIMIT = 70;
	/** The L2Spawn object that manage this L2NpcInstance */
	private L2Spawn _spawn;
	/** The flag to specify if this L2NpcInstance is busy */
	private boolean _isBusy = false;
	/** True if endDecayTask has already been called */
	private volatile boolean _isDecayed = false;
	/** True if this L2Npc is autoattackable **/
	private boolean _isAutoAttackable = false;
	/** Time of last social packet broadcast */
	private long _lastSocialBroadcast = 0;
	/** Minimum interval between social packets */
	private static final int MINIMUM_SOCIAL_INTERVAL = 6000;
	/** Support for random animation switching */
	private boolean _isRandomAnimationEnabled = true;
	private boolean _isRandomWalkingEnabled = true;
	private boolean _isTalkable = getTemplate().isTalkable();
	
	protected RandomAnimationTask _rAniTask;
	private int _currentLHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentRHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentEnchant; // normally this shouldn't change from the template, but there exist exceptions
	private double _currentCollisionHeight; // used for npc grow effect skills
	private double _currentCollisionRadius; // used for npc grow effect skills
	
	private int _soulshotamount = 0;
	private int _spiritshotamount = 0;
	private int _state = 0;
	
	private int _shotsMask = 0;
	private int _killingBlowWeaponId;
	
	private int _cloneObjId; // Used in NpcInfo packet to clone the specified player.
	private int _clanId; // Used in NpcInfo packet to show the specified clan.
	
	/**
	 * Constructor of L2NpcInstance (use L2Character constructor).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Call the L2Character constructor to set the _template of the L2Character (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2Character</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * </ul>
	 * @param template The L2NpcTemplate to apply to the NPC
	 */
	public Npc(L2NpcTemplate template)
	{
		// Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(template);
		setInstanceType(InstanceType.L2Npc);
		initCharStatusUpdateValues();
		setTargetable(getTemplate().isTargetable());
		
		// initialize the "current" equipment
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentEnchant = Config.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4, 21) : getTemplate().getWeaponEnchant();
		
		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().getfCollisionHeight();
		_currentCollisionRadius = getTemplate().getfCollisionRadius();
		
		setIsFlying(template.isFlying());
	}
	
	public void startRandomAnimationTask()
	{
		if (!hasRandomAnimation())
		{
			return;
		}
		
		if (_rAniTask == null)
		{
			synchronized (this)
			{
				if (_rAniTask == null)
				{
					_rAniTask = new RandomAnimationTask(this);
				}
			}
		}
		
		_rAniTask.startRandomAnimationTimer();
	}
	
	public void stopRandomAnimationTask()
	{
		RandomAnimationTask rAniTask = _rAniTask;
		if (rAniTask != null)
		{
			rAniTask.stopRandomAnimationTimer();
			_rAniTask = null;
		}
	}
	
	/**
	 * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance and create a new RandomAnimation Task.
	 * @param animationId
	 */
	public void onRandomAnimation(int animationId)
	{
		// Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		long now = System.currentTimeMillis();
		if ((now - _lastSocialBroadcast) > MINIMUM_SOCIAL_INTERVAL)
		{
			_lastSocialBroadcast = now;
			broadcastPacket(new SocialAction(getObjectId(), animationId));
		}
	}
	
	/**
	 * @return true if the server allows Random Animation.
	 */
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_NPC_ANIMATION > 0) && _isRandomAnimationEnabled && !getAiType().equals(AIType.CORPSE));
	}
	
	/**
	 * Switches random Animation state into val.
	 * @param val needed state of random animation
	 */
	public void setRandomAnimation(boolean val)
	{
		_isRandomAnimationEnabled = val;
	}
	
	/**
	 * @return {@code true}, if random animation is enabled, {@code false} otherwise.
	 */
	public boolean isRandomAnimationEnabled()
	{
		return _isRandomAnimationEnabled;
	}
	
	public void setRandomWalking(boolean enabled)
	{
		_isRandomWalkingEnabled = enabled;
	}
	
	public boolean isRandomWalkingEnabled()
	{
		return _isRandomWalkingEnabled;
	}
	
	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}
	
	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}
	
	/** Return the L2NpcTemplate of the L2NpcInstance. */
	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Gets the NPC ID.
	 * @return the NPC ID
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return Config.ALT_ATTACKABLE_NPCS;
	}
	
	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.
	 */
	@Override
	public final int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * @return false.
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * @return the Aggro Range of this L2NpcInstance either contained in the L2NpcTemplate, or overriden by spawnlist AI value.
	 */
	public int getAggroRange()
	{
		return getTemplate().getAggroRange();
	}
	
	/**
	 * @param npc
	 * @return if both npcs have the same clan by template.
	 */
	public boolean isInMyClan(Npc npc)
	{
		return getTemplate().isClan(npc.getTemplate().getClans());
	}
	
	/**
	 * Return True if this L2NpcInstance is undead in function of the L2NpcTemplate.
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().getRace() == Race.UNDEAD;
	}
	
	/**
	 * Send a packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		World.getInstance().forEachVisibleObject(this, PlayerInstance.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else
			{
				player.sendPacket(new NpcInfoAbnormalVisualEffect(this));
			}
		});
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return _isAutoAttackable;
	}
	
	public void setAutoAttackable(boolean flag)
	{
		_isAutoAttackable = flag;
	}
	
	/**
	 * @return the Identifier of the item in the left hand of this L2NpcInstance contained in the L2NpcTemplate.
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}
	
	/**
	 * @return the Identifier of the item in the right hand of this L2NpcInstance contained in the L2NpcTemplate.
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	public int getEnchantEffect()
	{
		return _currentEnchant;
	}
	
	/**
	 * @return the busy status of this L2NpcInstance.
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}
	
	/**
	 * @param isBusy the busy status of this L2Npc
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}
	
	/**
	 * @return true if this L2Npc instance can be warehouse manager.
	 */
	public boolean isWarehouse()
	{
		return false;
	}
	
	public boolean canTarget(PlayerInstance player)
	{
		if (player.isControlBlocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (player.isLockedTarget() && (player.getLockedTarget() != this))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ENMITY);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		// TODO: More checks...
		
		return true;
	}
	
	public boolean canInteract(PlayerInstance player)
	{
		if (player.isCastingNow(s -> true))
		{
			return false;
		}
		else if (player.isDead() || player.isFakeDeath())
		{
			return false;
		}
		else if (player.isSitting())
		{
			return false;
		}
		else if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			return false;
		}
		else if (!isInsideRadius(player, INTERACTION_DISTANCE, true, false))
		{
			return false;
		}
		else if ((player.getInstanceId() != getInstanceId()) && (player.getInstanceId() != -1))
		{
			return false;
		}
		else if (isBusy())
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return the nearest L2Castle this L2NpcInstance belongs to. Otherwise null.
	 */
	public final Castle getCastle()
	{
		return CastleManager.getInstance().findNearestCastle(this);
	}
	
	public final SiegableHall getConquerableHall()
	{
		return CHSiegeManager.getInstance().getNearbyClanHall(getX(), getY(), 10000);
	}
	
	/**
	 * Return closest castle in defined distance
	 * @param maxDistance long
	 * @return Castle
	 */
	public final Castle getCastle(long maxDistance)
	{
		return CastleManager.getInstance().findNearestCastle(this, maxDistance);
	}
	
	/**
	 * @return the nearest L2Fort this L2NpcInstance belongs to. Otherwise null.
	 */
	public final Fort getFort()
	{
		return FortManager.getInstance().findNearestFort(this);
	}
	
	/**
	 * Return closest Fort in defined distance
	 * @param maxDistance long
	 * @return Fort
	 */
	public final Fort getFort(long maxDistance)
	{
		return FortManager.getInstance().findNearestFort(this, maxDistance);
	}
	
	public final boolean isInTown()
	{
		return TownManager.getTown(getX(), getY(), getZ()) != null;
	}
	
	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<br>
	 * <B><U> Example of use </U> :</B>
	 * <ul>
	 * <li>Client packet : RequestBypassToServer</li>
	 * </ul>
	 * @param player
	 * @param command The command string received from client
	 */
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		// if (canInteract(player))
		{
			IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
			if (handler != null)
			{
				handler.useBypass(command, player, this);
			}
			else
			{
				_log.info(getClass().getSimpleName() + ": Unknown NPC bypass: \"" + command + "\" NpcId: " + getId());
			}
		}
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instances).
	 */
	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Return the weapon item equipped in the right hand of the L2NpcInstance or null.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instances).
	 */
	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Return the weapon item equipped in the left hand of the L2NpcInstance or null.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * <B><U Format of the pathfile</U>:</B>
	 * <ul>
	 * <li>if the file exists on the server (page number = 0) : <B>data/html/default/12006.htm</B> (npcId-page number)</li>
	 * <li>if the file exists on the server (page number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page number)</li>
	 * <li>if the file doesn't exist on the server : <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to you")</li>
	 * </ul>
	 * @param npcId The Identifier of the L2NpcInstance whose text must be display
	 * @param val The number of the page to display
	 * @return the pathfile of the selected HTML file in function of the npcId and of the page number.
	 */
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		String temp = "data/html/default/" + pom + ".htm";
		
		if (!Config.LAZY_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it doesnt exist
			if (HtmCache.getInstance().contains(temp))
			{
				return temp;
			}
		}
		else
		{
			if (HtmCache.getInstance().isLoadable(temp))
			{
				return temp;
			}
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	public void showChatWindow(PlayerInstance player)
	{
		showChatWindow(player, 0);
	}
	
	/**
	 * Returns true if html exists
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(PlayerInstance player, String type)
	{
		String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/" + type + "/" + getId() + "-pk.htm");
		if (html != null)
		{
			html = html.replaceAll("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(new NpcHtmlMessage(getObjectId(), html));
			if (player.isGM() && player.isDebug())
			{
				player.sendMessage("HTML : data/html/" + type + "/" + getId() + "-pk.htm");
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		return false;
	}
	
	/**
	 * Open a chat window on client with the text of the L2NpcInstance.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li>
	 * </ul>
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 */
	public void showChatWindow(PlayerInstance player, int val)
	{
		if (!isTalkable())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isCursedWeaponEquipped() && (!(player.getTarget() instanceof L2ClanHallManagerInstance) || !(player.getTarget() instanceof L2DoormenInstance)))
		{
			player.setTarget(player);
			return;
		}
		if (player.getReputation() < 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof L2MerchantInstance))
			{
				if (showPkDenyChatWindow(player, "merchant"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (this instanceof L2TeleporterInstance))
			{
				if (showPkDenyChatWindow(player, "teleporter"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (this instanceof L2WarehouseInstance))
			{
				if (showPkDenyChatWindow(player, "warehouse"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof L2FishermanInstance))
			{
				if (showPkDenyChatWindow(player, "fisherman"))
				{
					return;
				}
			}
		}
		
		if (getTemplate().isType("L2Auctioneer") && (val == 0))
		{
			return;
		}
		
		int npcId = getTemplate().getId();
		
		String filename;
		switch (npcId)
		{
			case 31688:
				if (player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, val));
				}
				break;
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isHero() || player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, val));
				}
				break;
			case 36402:
				if (player.getOlympiadBuffCount() > 0)
				{
					filename = (player.getOlympiadBuffCount() == Config.ALT_OLY_MAX_BUFFS ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
				}
				else
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm";
				}
				break;
			case 30298: // Blacksmith Pinter
				if (player.isAcademyMember())
				{
					filename = (getHtmlPath(npcId, 1));
				}
				else
				{
					filename = (getHtmlPath(npcId, val));
				}
				break;
			default:
				if (((npcId >= 31093) && (npcId <= 31094)) || ((npcId >= 31172) && (npcId <= 31201)) || ((npcId >= 31239) && (npcId <= 31254)))
				{
					return;
				}
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = (getHtmlPath(npcId, val));
				break;
		}
		
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		if (player.isGM() && player.isDebug())
		{
			player.sendMessage("HTML: " + filename);
		}
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Open a chat window on client with the text specified by the given file name and path, relative to the datapack root.
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param filename The filename that contains the text to send
	 */
	public void showChatWindow(PlayerInstance player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		if (player.isGM() && player.isDebug())
		{
			player.sendMessage("HTML: " + filename);
		}
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * @return the Exp Reward of this L2Npc (modified by RATE_XP).
	 */
	public long getExpReward()
	{
		return (long) (getLevel() * getLevel() * getTemplate().getExpRate() * Config.RATE_XP);
	}
	
	/**
	 * @return the SP Reward of this L2Npc (modified by RATE_SP).
	 */
	public int getSpReward()
	{
		return (int) (getTemplate().getSP() * Config.RATE_SP);
	}
	
	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Create a DecayTask to remove the corpse of the L2NpcInstance after 7 seconds</li>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the L2Character</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li>
	 * <li>Notify L2Character AI</li>
	 * </ul>
	 * @param killer The L2Character who killed it
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial template weapon.
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentCollisionHeight = getTemplate().getfCollisionHeight();
		_currentCollisionRadius = getTemplate().getfCollisionRadius();
		
		final Weapon weapon = (killer != null) ? killer.getActiveWeaponItem() : null;
		_killingBlowWeaponId = (weapon != null) ? weapon.getId() : 0;
		
		DecayTaskManager.getInstance().add(this);
		
		final L2Spawn spawn = getSpawn();
		if (spawn != null)
		{
			final NpcSpawnTemplate npcTemplate = spawn.getNpcSpawnTemplate();
			if (npcTemplate != null)
			{
				npcTemplate.notifyNpcDeath(this, killer);
			}
		}
		return true;
	}
	
	/**
	 * Set the spawn of the L2NpcInstance.
	 * @param spawn The L2Spawn that manage the L2NpcInstance
	 */
	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Recharge shots
		_soulshotamount = getTemplate().getSoulShot();
		_spiritshotamount = getTemplate().getSpiritShot();
		_killingBlowWeaponId = 0;
		_isRandomAnimationEnabled = getTemplate().isRandomAnimationEnabled();
		_isRandomWalkingEnabled = getTemplate().isRandomWalkEnabled();
		
		if (isTeleporting())
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcTeleport(this), this);
		}
		else
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSpawn(this), this);
		}
		
		if (!isTeleporting())
		{
			WalkingManager.getInstance().onSpawn(this);
		}
		
		// Display clan flag
		if (isInsideZone(ZoneId.TOWN) && (getCastle() != null) && (Config.SHOW_CREST_WITHOUT_QUEST || getCastle().getShowNpcCrest()) && (getCastle().getOwnerId() != 0))
		{
			int townId = TownManager.getTown(getX(), getY(), getZ()).getTownId();
			if ((townId != 33) && (townId != 22))
			{
				setClanId(getCastle().getOwnerId());
			}
		}
	}
	
	/**
	 * Remove the L2NpcInstance from the world and update its spawn object (for a complete removal use the deleteMe method).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Remove the L2NpcInstance from the world when the decay task is launched</li>
	 * <li>Decrease its spawn counter</li>
	 * <li>Manage Siege task (killFlag, killCT)</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR> <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT>
	 */
	@Override
	public void onDecay()
	{
		if (isDecayed())
		{
			return;
		}
		setDecayed(true);
		
		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();
		
		// Decrease its spawn counter
		if (_spawn != null)
		{
			_spawn.decreaseCount(this);
		}
		
		// Notify Walking Manager
		WalkingManager.getInstance().onDeath(this);
	}
	
	/**
	 * Remove PROPERLY the L2NpcInstance from the world.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Remove the L2NpcInstance from the world and update its spawn object</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2NpcInstance then cancel Attack or Cast and notify AI</li>
	 * <li>Remove L2Object object from _allObjects of L2World</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B><U>Caution</U>: This method DOESN'T SEND Server->Client packets to players</B></FONT><br> UnAfraid: TODO: Add Listener here
	 */
	@Override
	public boolean deleteMe()
	{
		try
		{
			onDecay();
		}
		catch (Exception e)
		{
			_log.error("Failed decayMe().", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		
		// Remove L2Object object from _allObjects of L2World
		World.getInstance().removeObject(this);
		
		return super.deleteMe();
	}
	
	/**
	 * @return the L2Spawn object that manage this L2NpcInstance.
	 */
	public L2Spawn getSpawn()
	{
		return _spawn;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ":" + getName() + "(" + getId() + ")" + "[" + getObjectId() + "]";
	}
	
	public boolean isDecayed()
	{
		return _isDecayed;
	}
	
	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}
	
	public void endDecayTask()
	{
		if (!isDecayed())
		{
			DecayTaskManager.getInstance().cancel(this);
			onDecay();
		}
	}
	
	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		broadcastInfo();
	}
	
	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		broadcastInfo();
	}
	
	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		broadcastInfo();
	}
	
	public void setEnchant(int newEnchantValue)
	{
		_currentEnchant = newEnchantValue;
		broadcastInfo();
	}
	
	public boolean isShowName()
	{
		return getTemplate().isShowName();
	}
	
	public void setCollisionHeight(double height)
	{
		_currentCollisionHeight = height;
	}
	
	public void setCollisionRadius(double radius)
	{
		_currentCollisionRadius = radius;
	}
	
	public double getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	public double getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	@Override
	public void sendInfo(PlayerInstance activeChar)
	{
		if (isVisibleFor(activeChar))
		{
			if (Config.CHECK_KNOWN && activeChar.isGM())
			{
				activeChar.sendMessage("Added NPC: " + getName());
			}
			
			if (getRunSpeed() == 0)
			{
				activeChar.sendPacket(new ServerObjectInfo(this, activeChar));
			}
			else
			{
				activeChar.sendPacket(new NpcInfo(this));
			}
		}
	}
	
	public Npc scheduleDespawn(long delay)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(() ->
		{
			if (!isDecayed())
			{
				deleteMe();
			}
		} , delay);
		return this;
	}
	
	@Override
	public final void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
		if ((target != null) && target.isPlayable())
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillFinished(this, target.getActingPlayer(), skill), this);
		}
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || !getTemplate().canMove() || getAiType().equals(AIType.CORPSE);
	}
	
	public AIType getAiType()
	{
		return getTemplate().getAIType();
	}
	
	public void setState(int state)
	{
		if (state != _state)
		{
			_state = state;
			broadcastPacket(new ExChangeNpcState(getObjectId(), state));
		}
	}
	
	public boolean isState(int state)
	{
		return _state == state;
	}
	
	public int getState()
	{
		return _state;
	}
	
	public int getColorEffect()
	{
		return 0;
	}
	
	@Override
	public boolean isNpc()
	{
		return true;
	}
	
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastInfo();
	}
	
	/**
	 * @return {@code true} if this L2Npc is registered in WalkingManager
	 */
	@Override
	public boolean isWalker()
	{
		return WalkingManager.getInstance().isRegistered(this);
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
		{
			_shotsMask |= type.getMask();
		}
		else
		{
			_shotsMask &= ~type.getMask();
		}
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		if (physical && (_soulshotamount > 0))
		{
			if (Rnd.get(100) > getTemplate().getSoulShotChance())
			{
				return;
			}
			_soulshotamount--;
			Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 2154, 1, 0, 0), 600);
			setChargedShot(ShotType.SOULSHOTS, true);
		}
		
		if (magic && (_spiritshotamount > 0))
		{
			if (Rnd.get(100) > getTemplate().getSpiritShotChance())
			{
				return;
			}
			_spiritshotamount--;
			Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 2061, 1, 0, 0), 600);
			setChargedShot(ShotType.SPIRITSHOTS, true);
		}
	}
	
	/**
	 * Short wrapper for backward compatibility
	 * @return stored script value
	 */
	public int getScriptValue()
	{
		return getVariables().getInt("SCRIPT_VAL");
	}
	
	/**
	 * Short wrapper for backward compatibility. Stores script value
	 * @param val value to store
	 */
	public void setScriptValue(int val)
	{
		getVariables().set("SCRIPT_VAL", val);
	}
	
	/**
	 * Short wrapper for backward compatibility.
	 * @param val value to store
	 * @return {@code true} if stored script value equals given value, {@code false} otherwise
	 */
	public boolean isScriptValue(int val)
	{
		return getVariables().getInt("SCRIPT_VAL") == val;
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if both given NPC and this NPC is in the same spawn group, {@code false} otherwise
	 */
	public boolean isInMySpawnGroup(Npc npc)
	{
		return ((getSpawn() != null) && (npc.getSpawn() != null) && (getSpawn().getName() != null) && (getSpawn().getName().equals(npc.getSpawn().getName())));
	}
	
	/**
	 * @return {@code true} if NPC currently located in own spawn point, {@code false} otherwise
	 */
	public boolean staysInSpawnLoc()
	{
		return ((getSpawn() != null) && (getSpawn().getX() == getX()) && (getSpawn().getY() == getY()));
	}
	
	/**
	 * @return {@code true} if {@link NpcVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(NpcVariables.class) != null;
	}
	
	/**
	 * @return {@link NpcVariables} instance containing parameters regarding NPC.
	 */
	public NpcVariables getVariables()
	{
		final NpcVariables vars = getScript(NpcVariables.class);
		return vars != null ? vars : addScript(new NpcVariables());
	}
	
	/**
	 * Send an "event" to all NPC's within given radius
	 * @param eventName - name of event
	 * @param radius - radius to send event
	 * @param reference - L2Object to pass, if needed
	 */
	public void broadcastEvent(String eventName, int radius, WorldObject reference)
	{
		World.getInstance().forEachVisibleObjectInRange(this, Npc.class, radius, obj ->
		{
			if (obj.hasListener(EventType.ON_NPC_EVENT_RECEIVED))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, obj, reference), obj);
			}
		});
	}
	
	/**
	 * Sends an event to a given object.
	 * @param eventName the event name
	 * @param receiver the receiver
	 * @param reference the reference
	 */
	public void sendScriptEvent(String eventName, WorldObject receiver, WorldObject reference)
	{
		EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, (Npc) receiver, reference), receiver);
	}
	
	/**
	 * Gets point in range between radiusMin and radiusMax from this NPC
	 * @param radiusMin miminal range from NPC (not closer than)
	 * @param radiusMax maximal range from NPC (not further than)
	 * @return Location in given range from this NPC
	 */
	public Location getPointInRange(int radiusMin, int radiusMax)
	{
		if ((radiusMax == 0) || (radiusMax < radiusMin))
		{
			return new Location(getX(), getY(), getZ());
		}
		
		final int radius = Rnd.get(radiusMin, radiusMax);
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		
		return new Location((int) (getX() + (radius * Math.cos(angle))), (int) (getY() + (radius * Math.sin(angle))), getZ());
	}
	
	/**
	 * Drops an item.
	 * @param player the last attacker or main damage dealer
	 * @param itemId the item ID
	 * @param itemCount the item count
	 * @return the dropped item
	 */
	public ItemInstance dropItem(PlayerInstance player, int itemId, long itemCount)
	{
		ItemInstance item = null;
		for (int i = 0; i < itemCount; i++)
		{
			// Randomize drop position.
			final int newX = (getX() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newY = (getY() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newZ = getZ() + 20;
			
			if (ItemTable.getInstance().getTemplate(itemId) == null)
			{
				_log.error("Item doesn't exist so cannot be dropped. Item ID: " + itemId + " Quest: " + getName());
				return null;
			}
			
			item = ItemTable.getInstance().createItem("Loot", itemId, itemCount, player, this);
			if (item == null)
			{
				return null;
			}
			
			if (player != null)
			{
				item.getDropProtection().protect(player);
			}
			
			item.dropMe(this, newX, newY, newZ);
			
			// Add drop to auto destroy item task.
			if (!Config.LIST_PROTECTED_ITEMS.contains(itemId))
			{
				if (((Config.AUTODESTROY_ITEM_AFTER > 0) && !item.getItem().hasExImmediateEffect()) || ((Config.HERB_AUTO_DESTROY_TIME > 0) && item.getItem().hasExImmediateEffect()))
				{
					ItemsAutoDestroy.getInstance().addItem(item);
				}
			}
			item.setProtected(false);
			
			// If stackable, end loop as entire count is included in 1 instance of item.
			if (item.isStackable() || !Config.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}
		return item;
	}
	
	/**
	 * Method overload for {@link Attackable#dropItem(PlayerInstance, int, long)}
	 * @param player the last attacker or main damage dealer
	 * @param item the item holder
	 * @return the dropped item
	 */
	public ItemInstance dropItem(PlayerInstance player, ItemHolder item)
	{
		return dropItem(player, item.getId(), item.getCount());
	}
	
	@Override
	public final String getName()
	{
		return getTemplate().getName();
	}
	
	@Override
	public boolean isVisibleFor(PlayerInstance player)
	{
		if (hasListener(EventType.ON_NPC_CAN_BE_SEEN))
		{
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnNpcCanBeSeen(this, player), this, TerminateReturn.class);
			if (term != null)
			{
				return term.terminate();
			}
		}
		return super.isVisibleFor(player);
	}
	
	/**
	 * Sets if the players can talk with this npc or not
	 * @param val {@code true} if the players can talk, {@code false} otherwise
	 */
	public void setIsTalkable(boolean val)
	{
		_isTalkable = val;
	}
	
	/**
	 * Checks if the players can talk to this npc.
	 * @return {@code true} if the players can talk, {@code false} otherwise.
	 */
	public boolean isTalkable()
	{
		return _isTalkable;
	}
	
	/**
	 * Sets the weapon id with which this npc was killed.
	 * @param weaponId
	 */
	public void setKillingBlowWeapon(int weaponId)
	{
		_killingBlowWeaponId = weaponId;
	}
	
	/**
	 * @return the id of the weapon with which player killed this npc.
	 */
	public int getKillingBlowWeapon()
	{
		return _killingBlowWeaponId;
	}
	
	/**
	 * @return The player's object Id this NPC is cloning.
	 */
	public int getCloneObjId()
	{
		return _cloneObjId;
	}
	
	/**
	 * @param cloneObjId object id of player or 0 to disable it.
	 */
	public void setCloneObjId(int cloneObjId)
	{
		_cloneObjId = cloneObjId;
	}
	
	/**
	 * @return The clan's object Id this NPC is displaying.
	 */
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @param clanObjId object id of clan or 0 to disable it.
	 */
	public void setClanId(int clanObjId)
	{
		_clanId = clanObjId;
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players.
	 * @param chatType the chat type
	 * @param text the text
	 */
	public void broadcastSay(ChatType chatType, String text)
	{
		Broadcast.toKnownPlayers(this, new NpcSay(this, chatType, text));
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with NPC string id.
	 * @param chatType the chat type
	 * @param npcStringId the NPC string id
	 * @param parameters the NPC string id parameters
	 */
	public void broadcastSay(ChatType chatType, NpcStringId npcStringId, String... parameters)
	{
		final NpcSay npcSay = new NpcSay(this, chatType, npcStringId);
		if (parameters != null)
		{
			for (String parameter : parameters)
			{
				if (parameter != null)
				{
					npcSay.addStringParameter(parameter);
				}
			}
		}
		Broadcast.toKnownPlayers(this, npcSay);
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with custom string in specific radius.
	 * @param chatType the chat type
	 * @param text the text
	 * @param radius the radius
	 */
	public void broadcastSay(ChatType chatType, String text, int radius)
	{
		Broadcast.toKnownPlayersInRadius(this, new NpcSay(this, chatType, text), radius);
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with NPC string id in specific radius.
	 * @param chatType the chat type
	 * @param npcStringId the NPC string id
	 * @param radius the radius
	 */
	public void broadcastSay(ChatType chatType, NpcStringId npcStringId, int radius)
	{
		Broadcast.toKnownPlayersInRadius(this, new NpcSay(this, chatType, npcStringId), radius);
	}
	
	/**
	 * @return the parameters of the npc merged with the spawn parameters (if there are any)
	 */
	public StatsSet getParameters()
	{
		final L2Spawn spawn = getSpawn();
		if (spawn != null) // Minions doesn't have L2Spawn object bound
		{
			final NpcSpawnTemplate npcSpawnTemplate = spawn.getNpcSpawnTemplate();
			if ((npcSpawnTemplate != null) && (npcSpawnTemplate.getParameters() != null) && !npcSpawnTemplate.getParameters().isEmpty())
			{
				return npcSpawnTemplate.getParameters();
			}
		}
		return getTemplate().getParameters();
	}
	
	public List<Skill> getLongRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.LONG_RANGE);
	}
	
	public List<Skill> getShortRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.SHORT_RANGE);
	}
	
	/**
	 * Verifies if the NPC can cast a skill given the minimum and maximum skill chances.
	 * @return {@code true} if the NPC has chances of casting a skill
	 */
	public boolean hasSkillChance()
	{
		return Rnd.get(100) < Rnd.get(getTemplate().getMinSkillChance(), getTemplate().getMaxSkillChance());
	}
}
