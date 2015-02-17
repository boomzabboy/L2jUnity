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
package org.l2junity.gameserver.network.clientpackets.appearance;

import org.l2junity.gameserver.data.xml.impl.AppearanceItemData;
import org.l2junity.gameserver.enums.ItemLocation;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.request.ShapeShiftingItemRequest;
import org.l2junity.gameserver.model.itemcontainer.PcInventory;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.appearance.AppearanceStone;
import org.l2junity.gameserver.model.items.appearance.AppearanceType;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.items.type.ArmorType;
import org.l2junity.gameserver.model.items.type.WeaponType;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.clientpackets.L2GameClientPacket;
import org.l2junity.gameserver.network.serverpackets.appearance.ExPutShapeShiftingExtractionItemResult;
import org.l2junity.gameserver.network.serverpackets.appearance.ExShapeShiftingResult;

/**
 * @author UnAfraid
 */
public class RequestExTryToPutShapeShiftingEnchantSupportItem extends L2GameClientPacket
{
	private int _targetItemObjId;
	private int _extracItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_extracItemObjId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final ShapeShiftingItemRequest request = player.getRequest(ShapeShiftingItemRequest.class);
		
		if (player.isInStoreMode() || player.isInCraftMode() || player.isProcessingRequest() || player.isProcessingTransaction() || (request == null))
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			return;
		}
		
		final PcInventory inventory = player.getInventory();
		final ItemInstance targetItem = inventory.getItemByObjectId(_targetItemObjId);
		final ItemInstance extracItem = inventory.getItemByObjectId(_extracItemObjId);
		ItemInstance stone = request.getAppearanceStone();
		if ((targetItem == null) || (extracItem == null) || (stone == null))
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if (!extracItem.isAppearanceable())
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((extracItem.getItemLocation() != ItemLocation.INVENTORY) && (extracItem.getItemLocation() != ItemLocation.PAPERDOLL))
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((stone = inventory.getItemByObjectId(stone.getObjectId())) == null)
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		final AppearanceStone appearanceStone = AppearanceItemData.getInstance().getStone(stone.getId());
		if (appearanceStone == null)
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((appearanceStone.getType() == AppearanceType.RESTORE) || (appearanceStone.getType() == AppearanceType.FIXED))
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if (extracItem.getVisualId() > 0)
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.sendPacket(SystemMessageId.YOU_CANNOT_EXTRACT_FROM_A_MODIFIED_ITEM);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if (appearanceStone.getWeaponType() != WeaponType.NONE)
		{
			if (!targetItem.isWeapon() || (targetItem.getItemType() != appearanceStone.getWeaponType()))
			{
				player.sendPacket(ExShapeShiftingResult.FAILED);
				player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
				player.removeRequest(ShapeShiftingItemRequest.class);
				return;
			}
		}
		
		if (appearanceStone.getWeaponType() != WeaponType.NONE)
		{
			if (!targetItem.isWeapon() || (targetItem.getItemType() != appearanceStone.getWeaponType()))
			{
				player.sendPacket(ExShapeShiftingResult.FAILED);
				player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
				player.removeRequest(ShapeShiftingItemRequest.class);
				return;
			}
			
			switch (appearanceStone.getHandType())
			{
				case ONE_HANDED:
				{
					if ((targetItem.getItem().getBodyPart() & L2Item.SLOT_R_HAND) != L2Item.SLOT_R_HAND)
					{
						player.sendPacket(ExShapeShiftingResult.FAILED);
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						player.removeRequest(ShapeShiftingItemRequest.class);
						return;
					}
					break;
				}
				case TWO_HANDED:
				{
					if ((targetItem.getItem().getBodyPart() & L2Item.SLOT_LR_HAND) != L2Item.SLOT_LR_HAND)
					{
						player.sendPacket(ExShapeShiftingResult.FAILED);
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						player.removeRequest(ShapeShiftingItemRequest.class);
						return;
					}
					break;
				}
			}
			
			switch (appearanceStone.getMagicType())
			{
				case MAGICAL:
				{
					if (!targetItem.getItem().isMagicWeapon())
					{
						player.sendPacket(ExShapeShiftingResult.FAILED);
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						player.removeRequest(ShapeShiftingItemRequest.class);
						return;
					}
					break;
				}
				case PHYISICAL:
				{
					if (targetItem.getItem().isMagicWeapon())
					{
						player.sendPacket(ExShapeShiftingResult.FAILED);
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						player.removeRequest(ShapeShiftingItemRequest.class);
						return;
					}
				}
			}
		}
		
		if (appearanceStone.getArmorType() != ArmorType.NONE)
		{
			switch (appearanceStone.getArmorType())
			{
				case SHIELD:
				{
					if (!targetItem.isArmor() || (targetItem.getItemType() != ArmorType.SHIELD))
					{
						player.sendPacket(ExShapeShiftingResult.FAILED);
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						player.removeRequest(ShapeShiftingItemRequest.class);
						return;
					}
					break;
				}
				case SIGIL:
				{
					if (!targetItem.isArmor() || (targetItem.getItemType() != ArmorType.SIGIL))
					{
						player.sendPacket(ExShapeShiftingResult.FAILED);
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						player.removeRequest(ShapeShiftingItemRequest.class);
						return;
					}
				}
			}
		}
		
		if (extracItem.getOwnerId() != player.getObjectId())
		{
			player.sendPacket(ExShapeShiftingResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		request.setAppearanceExtractItem(extracItem);
		player.sendPacket(ExPutShapeShiftingExtractionItemResult.SUCCESS);
	}
	
	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}
