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
package org.l2junity.gameserver.ai;

import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.concurrent.Future;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.GameTimeController;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.AutoAttackStart;
import org.l2junity.gameserver.network.client.send.AutoAttackStop;
import org.l2junity.gameserver.network.client.send.Die;
import org.l2junity.gameserver.network.client.send.MoveToLocation;
import org.l2junity.gameserver.network.client.send.MoveToPawn;
import org.l2junity.gameserver.network.client.send.StopMove;
import org.l2junity.gameserver.network.client.send.StopRotation;
import org.l2junity.gameserver.taskmanager.AttackStanceTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mother class of all objects AI in the world.<br>
 * AbastractAI :<br>
 * <li>L2CharacterAI</li>
 */
public abstract class AbstractAI implements Ctrl
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAI.class);
	
	private NextAction _nextAction;
	
	/**
	 * @return the _nextAction
	 */
	public NextAction getNextAction()
	{
		return _nextAction;
	}
	
	/**
	 * @param nextAction the next action to set.
	 */
	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	/** The character that this AI manages */
	protected final Creature _actor;
	
	/** Current long-term intention */
	protected CtrlIntention _intention = AI_INTENTION_IDLE;
	/** Current long-term intention parameter */
	protected Object _intentionArg0 = null;
	/** Current long-term intention parameter */
	protected Object _intentionArg1 = null;
	
	/** Flags about client's state, in order to know which messages to send */
	protected volatile boolean _clientMoving;
	/** Flags about client's state, in order to know which messages to send */
	protected volatile boolean _clientAutoAttacking;
	/** Flags about client's state, in order to know which messages to send */
	protected int _clientMovingToPawnOffset;
	
	/** Different targets this AI maintains */
	private WorldObject _target;
	private Creature _castTarget;
	protected Creature _attackTarget;
	protected Creature _followTarget;
	
	/** The skill we are currently casting by INTENTION_CAST */
	Skill _skill;
	
	/** Different internal state flags */
	protected int _moveToPawnTimeout;
	
	protected Future<?> _followTask = null;
	private static final int FOLLOW_INTERVAL = 1000;
	private static final int ATTACK_FOLLOW_INTERVAL = 500;
	
	protected AbstractAI(Creature creature)
	{
		_actor = creature;
	}
	
	/**
	 * @return the L2Character managed by this Accessor AI.
	 */
	@Override
	public Creature getActor()
	{
		return _actor;
	}
	
	/**
	 * @return the current Intention.
	 */
	@Override
	public CtrlIntention getIntention()
	{
		return _intention;
	}
	
	protected void setCastTarget(Creature target)
	{
		_castTarget = target;
	}
	
	/**
	 * @return the current cast target.
	 */
	public Creature getCastTarget()
	{
		return _castTarget;
	}
	
	protected void setAttackTarget(Creature target)
	{
		_attackTarget = target;
	}
	
	/**
	 * @return current attack target.
	 */
	@Override
	public Creature getAttackTarget()
	{
		return _attackTarget;
	}
	
	/**
	 * Set the Intention of this AbstractAI.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is USED by AI classes</B></FONT><B><U><br> Overridden in </U> : </B><BR> <B>L2AttackableAI</B> : Create an AI Task executed every 1s (if necessary)<BR> <B>L2PlayerAI</B> : Stores the current AI intention parameters to later restore it if
	 * necessary.
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_intention = intention;
		_intentionArg0 = arg0;
		_intentionArg1 = arg1;
	}
	
	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT>
	 * @param intention The new Intention to set to the AI
	 */
	@Override
	public final void setIntention(CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}
	
	/**
	 * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 */
	@Override
	public final void setIntention(CtrlIntention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}
	
	@Override
	public final void setIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		// Stop the follow mode if necessary
		if ((intention != AI_INTENTION_FOLLOW) && (intention != AI_INTENTION_ATTACK))
		{
			stopFollow();
		}
		
		// Launch the onIntention method of the L2CharacterAI corresponding to the new Intention
		switch (intention)
		{
			case AI_INTENTION_IDLE:
				onIntentionIdle();
				break;
			case AI_INTENTION_ACTIVE:
				onIntentionActive();
				break;
			case AI_INTENTION_REST:
				onIntentionRest();
				break;
			case AI_INTENTION_ATTACK:
				onIntentionAttack((Creature) arg0);
				break;
			case AI_INTENTION_CAST:
				onIntentionCast((Skill) arg0, (WorldObject) arg1);
				break;
			case AI_INTENTION_MOVE_TO:
				onIntentionMoveTo((Location) arg0);
				break;
			case AI_INTENTION_FOLLOW:
				onIntentionFollow((Creature) arg0);
				break;
			case AI_INTENTION_PICK_UP:
				onIntentionPickUp((WorldObject) arg0);
				break;
			case AI_INTENTION_INTERACT:
				onIntentionInteract((WorldObject) arg0);
				break;
		}
		
		// If do move or follow intention drop next action.
		if ((_nextAction != null) && _nextAction.getIntentions().contains(intention))
		{
			_nextAction = null;
		}
	}
	
	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned period)</B></FONT>
	 * @param evt The event whose the AI must be notified
	 */
	@Override
	public final void notifyEvent(CtrlEvent evt)
	{
		notifyEvent(evt, null, null);
	}
	
	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event. <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned period)</B></FONT>
	 * @param evt The event whose the AI must be notified
	 * @param arg0 The first parameter of the Event (optional target)
	 */
	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0)
	{
		notifyEvent(evt, arg0, null);
	}
	
	/**
	 * Launch the L2CharacterAI onEvt method corresponding to the Event. <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned period)</B></FONT>
	 * @param evt The event whose the AI must be notified
	 * @param arg0 The first parameter of the Event (optional target)
	 * @param arg1 The second parameter of the Event (optional target)
	 */
	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		switch (evt)
		{
			case EVT_THINK:
				onEvtThink();
				break;
			case EVT_ATTACKED:
				onEvtAttacked((Creature) arg0);
				break;
			case EVT_AGGRESSION:
				onEvtAggression((Creature) arg0, ((Number) arg1).intValue());
				break;
			case EVT_ACTION_BLOCKED:
				onEvtActionBlocked((Creature) arg0);
				break;
			case EVT_ROOTED:
				onEvtRooted((Creature) arg0);
				break;
			case EVT_CONFUSED:
				onEvtConfused((Creature) arg0);
				break;
			case EVT_MUTED:
				onEvtMuted((Creature) arg0);
				break;
			case EVT_EVADED:
				onEvtEvaded((Creature) arg0);
				break;
			case EVT_READY_TO_ACT:
				if (!_actor.isCastingNow(s -> true))
				{
					onEvtReadyToAct();
				}
				break;
			case EVT_ARRIVED:
				// happens e.g. from stopmove but we don't process it if we're casting
				if (!_actor.isCastingNow(s -> true))
				{
					onEvtArrived();
				}
				break;
			case EVT_ARRIVED_REVALIDATE:
				// this is disregarded if the char is not moving any more
				if (_actor.isMoving())
				{
					onEvtArrivedRevalidate();
				}
				break;
			case EVT_ARRIVED_BLOCKED:
				onEvtArrivedBlocked((Location) arg0);
				break;
			case EVT_FORGET_OBJECT:
				onEvtForgetObject((WorldObject) arg0);
				break;
			case EVT_CANCEL:
				onEvtCancel();
				break;
			case EVT_DEAD:
				onEvtDead();
				break;
			case EVT_FAKE_DEATH:
				onEvtFakeDeath();
				break;
			case EVT_FINISH_CASTING:
				onEvtFinishCasting();
				break;
		}
		
		// Do next action.
		if ((_nextAction != null) && _nextAction.getEvents().contains(evt))
		{
			_nextAction.doAction();
		}
	}
	
	protected abstract void onIntentionIdle();
	
	protected abstract void onIntentionActive();
	
	protected abstract void onIntentionRest();
	
	protected abstract void onIntentionAttack(Creature target);
	
	protected abstract void onIntentionCast(Skill skill, WorldObject target);
	
	protected abstract void onIntentionMoveTo(Location destination);
	
	protected abstract void onIntentionFollow(Creature target);
	
	protected abstract void onIntentionPickUp(WorldObject item);
	
	protected abstract void onIntentionInteract(WorldObject object);
	
	protected abstract void onEvtThink();
	
	protected abstract void onEvtAttacked(Creature attacker);
	
	protected abstract void onEvtAggression(Creature target, int aggro);
	
	protected abstract void onEvtActionBlocked(Creature attacker);
	
	protected abstract void onEvtRooted(Creature attacker);
	
	protected abstract void onEvtConfused(Creature attacker);
	
	protected abstract void onEvtMuted(Creature attacker);
	
	protected abstract void onEvtEvaded(Creature attacker);
	
	protected abstract void onEvtReadyToAct();
	
	protected abstract void onEvtArrived();
	
	protected abstract void onEvtArrivedRevalidate();
	
	protected abstract void onEvtArrivedBlocked(Location blocked_at_pos);
	
	protected abstract void onEvtForgetObject(WorldObject object);
	
	protected abstract void onEvtCancel();
	
	protected abstract void onEvtDead();
	
	protected abstract void onEvtFakeDeath();
	
	protected abstract void onEvtFinishCasting();
	
	/**
	 * Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor. <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 */
	protected void clientActionFailed()
	{
		if (_actor instanceof PlayerInstance)
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <I>(broadcast)</I>.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 * @param pawn
	 * @param offset
	 */
	protected void moveToPawn(WorldObject pawn, int offset)
	{
		// Check if actor can move
		if (!_actor.isMovementDisabled())
		{
			if (offset < 10)
			{
				offset = 10;
			}
			
			// prevent possible extra calls to this function (there is none?),
			// also don't send movetopawn packets too often
			boolean sendPacket = true;
			if (_clientMoving && (_target == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (GameTimeController.getInstance().getGameTicks() < _moveToPawnTimeout)
					{
						return;
					}
					sendPacket = false;
				}
				else if (_actor.isOnGeodataPath())
				{
					// minimum time to calculate new route is 2 seconds
					if (GameTimeController.getInstance().getGameTicks() < (_moveToPawnTimeout + 10))
					{
						return;
					}
				}
			}
			
			// Set AI movement data
			_clientMoving = true;
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = GameTimeController.getInstance().getGameTicks();
			_moveToPawnTimeout += 1000 / GameTimeController.MILLIS_IN_TICK;
			
			if (pawn == null)
			{
				return;
			}
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
			_actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			if (!_actor.isMoving())
			{
				clientActionFailed();
				return;
			}
			
			// Send a Server->Client packet MoveToPawn/CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
			if (pawn.isCreature())
			{
				if (_actor.isOnGeodataPath())
				{
					_actor.broadcastPacket(new MoveToLocation(_actor));
					_clientMovingToPawnOffset = 0;
				}
				else if (sendPacket)
				{
					_actor.broadcastPacket(new MoveToPawn(_actor, (Creature) pawn, offset));
				}
			}
			else
			{
				_actor.broadcastPacket(new MoveToLocation(_actor));
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	/**
	 * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation <I>(broadcast)</I>.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 * @param x
	 * @param y
	 * @param z
	 */
	protected void moveTo(int x, int y, int z)
	{
		// Chek if actor can move
		if (!_actor.isMovementDisabled())
		{
			// Set AI movement data
			_clientMoving = true;
			_clientMovingToPawnOffset = 0;
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
			_actor.moveToLocation(x, y, z, 0);
			
			// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
			_actor.broadcastPacket(new MoveToLocation(_actor));
			
		}
		else
		{
			clientActionFailed();
		}
	}
	
	/**
	 * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation <I>(broadcast)</I>.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 * @param loc
	 */
	protected void clientStopMoving(Location loc)
	{
		// Stop movement of the L2Character
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
		}
		
		_clientMovingToPawnOffset = 0;
		
		if (_clientMoving || (loc != null))
		{
			_clientMoving = false;
			
			// Send a Server->Client packet StopMove to the actor and all L2PcInstance in its _knownPlayers
			_actor.broadcastPacket(new StopMove(_actor));
			
			if (loc != null)
			{
				// Send a Server->Client packet StopRotation to the actor and all L2PcInstance in its _knownPlayers
				_actor.broadcastPacket(new StopRotation(_actor.getObjectId(), loc.getHeading(), 0));
			}
		}
	}
	
	/**
	 * Client has already arrived to target, no need to force StopMove packet.
	 */
	protected void clientStoppedMoving()
	{
		if (_clientMovingToPawnOffset > 0) // movetoPawn needs to be stopped
		{
			_clientMovingToPawnOffset = 0;
			_actor.broadcastPacket(new StopMove(_actor));
		}
		_clientMoving = false;
	}
	
	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}
	
	public void setAutoAttacking(boolean isAutoAttacking)
	{
		if (_actor.isSummon())
		{
			Summon summon = (Summon) _actor;
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
			}
			return;
		}
		_clientAutoAttacking = isAutoAttacking;
	}
	
	/**
	 * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <I>(broadcast)</I>.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 */
	public void clientStartAutoAttack()
	{
		if (_actor.isSummon())
		{
			Summon summon = (Summon) _actor;
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStartAutoAttack();
			}
			return;
		}
		if (!isAutoAttacking())
		{
			if (_actor.isPlayer() && _actor.hasSummon())
			{
				final Summon pet = _actor.getPet();
				if (pet != null)
				{
					pet.broadcastPacket(new AutoAttackStart(pet.getObjectId()));
				}
				_actor.getServitors().values().forEach(s -> s.broadcastPacket(new AutoAttackStart(s.getObjectId())));
			}
			// Send a Server->Client packet AutoAttackStart to the actor and all L2PcInstance in its _knownPlayers
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}
	
	/**
	 * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <I>(broadcast)</I>.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 */
	public void clientStopAutoAttack()
	{
		if (_actor.isSummon())
		{
			Summon summon = (Summon) _actor;
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStopAutoAttack();
			}
			return;
		}
		if (_actor instanceof PlayerInstance)
		{
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor) && isAutoAttacking())
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
			}
		}
		else if (isAutoAttacking())
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}
	
	/**
	 * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die <I>(broadcast)</I>.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 */
	protected void clientNotifyDead()
	{
		// Send a Server->Client packet Die to the actor and all L2PcInstance in its _knownPlayers
		Die msg = new Die(_actor);
		_actor.broadcastPacket(msg);
		
		// Init AI
		_intention = AI_INTENTION_IDLE;
		_target = null;
		_castTarget = null;
		_attackTarget = null;
		
		// Cancel the follow task if necessary
		stopFollow();
	}
	
	/**
	 * Update the state of this actor client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance player.<br>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT>
	 * @param player The L2PcIstance to notify with state of this L2Character
	 */
	public void describeStateToPlayer(PlayerInstance player)
	{
		if (getActor().isVisibleFor(player))
		{
			if (_clientMoving)
			{
				if ((_clientMovingToPawnOffset != 0) && (_followTarget != null))
				{
					// Send a Server->Client packet MoveToPawn to the actor and all L2PcInstance in its _knownPlayers
					player.sendPacket(new MoveToPawn(_actor, _followTarget, _clientMovingToPawnOffset));
				}
				else
				{
					// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
					player.sendPacket(new MoveToLocation(_actor));
				}
			}
		}
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 1s.
	 * @param target The L2Character to follow
	 */
	public synchronized void startFollow(Creature target)
	{
		startFollow(target, -1);
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 0.5s, following at specified range.
	 * @param target The L2Character to follow
	 * @param range
	 */
	public synchronized void startFollow(Creature target, int range)
	{
		if (_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		
		_followTarget = target;
		
		final int followRange = range == -1 ? Rnd.get(50, 100) : range;
		_followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(() ->
		{
			try
			{
				if (_followTask == null)
				{
					return;
				}
				
				final Creature followTarget = _followTarget; // copy to prevent NPE
				if (followTarget == null)
				{
					if (_actor.isSummon())
					{
						((Summon) _actor).setFollowStatus(false);
					}
					setIntention(AI_INTENTION_IDLE);
					return;
				}
				
				if (!_actor.isInsideRadius(followTarget, followRange, true, false))
				{
					if (!_actor.isInsideRadius(followTarget, 3000, true, false))
					{
						// if the target is too far (maybe also teleported)
						if (_actor.isSummon())
						{
							((Summon) _actor).setFollowStatus(false);
						}
						
						setIntention(AI_INTENTION_IDLE);
						return;
					}
					
					moveToPawn(followTarget, followRange);
				}
			}
			catch (Exception e)
			{
				LOGGER.warn("Error: " + e.getMessage());
			}
		} , 5, range == -1 ? FOLLOW_INTERVAL : ATTACK_FOLLOW_INTERVAL);
	}
	
	/**
	 * Stop an AI Follow Task.
	 */
	public synchronized void stopFollow()
	{
		if (_followTask != null)
		{
			// Stop the Follow Task
			_followTask.cancel(false);
			_followTask = null;
		}
		_followTarget = null;
	}
	
	protected Creature getFollowTarget()
	{
		return _followTarget;
	}
	
	protected WorldObject getTarget()
	{
		return _target;
	}
	
	protected void setTarget(WorldObject target)
	{
		_target = target;
	}
	
	/**
	 * Stop all Ai tasks and futures.
	 */
	public void stopAITask()
	{
		stopFollow();
	}
	
	@Override
	public String toString()
	{
		return "Actor: " + _actor;
	}
}
