/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

/**
 * An item that allows for quickly viewing block entity data.
 */
public class BlockEntityViewerItem extends Item {
	public BlockEntityViewerItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		var world = context.getWorld();
		var pos = context.getBlockPos();

		if(world.getBlockEntity(pos) != null) {
			if(world.isClient()) {
				MinecraftClient
					.getInstance()
					.player
					.sendChatMessage(String.format("/data get block %s %s %s", pos.getX(), pos.getY(), pos.getZ()));
			}
			return ActionResult.PASS;
		}
		return ActionResult.FAIL;
	}
}
