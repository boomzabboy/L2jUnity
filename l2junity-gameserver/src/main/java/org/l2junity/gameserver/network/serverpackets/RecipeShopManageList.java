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
package org.l2junity.gameserver.network.serverpackets;

import java.util.Iterator;

import org.l2junity.gameserver.model.ManufactureItem;
import org.l2junity.gameserver.model.RecipeList;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

public class RecipeShopManageList extends L2GameServerPacket
{
	private final PlayerInstance _seller;
	private final boolean _isDwarven;
	private RecipeList[] _recipes;
	
	public RecipeShopManageList(PlayerInstance seller, boolean isDwarven)
	{
		_seller = seller;
		_isDwarven = isDwarven;
		
		if (_isDwarven && _seller.hasDwarvenCraft())
		{
			_recipes = _seller.getDwarvenRecipeBook();
		}
		else
		{
			_recipes = _seller.getCommonRecipeBook();
		}
		
		if (_seller.hasManufactureShop())
		{
			final Iterator<ManufactureItem> it = _seller.getManufactureItems().values().iterator();
			ManufactureItem item;
			while (it.hasNext())
			{
				item = it.next();
				if ((item.isDwarven() != _isDwarven) || !seller.hasRecipeList(item.getRecipeId()))
				{
					it.remove();
				}
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xDE);
		writeD(_seller.getObjectId());
		writeD((int) _seller.getAdena());
		writeD(_isDwarven ? 0x00 : 0x01);
		
		if (_recipes == null)
		{
			writeD(0);
		}
		else
		{
			writeD(_recipes.length);// number of items in recipe book
			
			for (int i = 0; i < _recipes.length; i++)
			{
				RecipeList temp = _recipes[i];
				writeD(temp.getId());
				writeD(i + 1);
			}
		}
		
		if (!_seller.hasManufactureShop())
		{
			writeD(0x00);
		}
		else
		{
			writeD(_seller.getManufactureItems().size());
			for (ManufactureItem item : _seller.getManufactureItems().values())
			{
				writeD(item.getRecipeId());
				writeD(0x00);
				writeQ(item.getCost());
			}
		}
	}
}
