/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.item;

import systems.thedawn.bls.BlueLightSpecial;

import net.minecraft.item.Item;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

public final class BlsItems {
	private static final Item BLOCK_ENTITY_VIEWER = new BlockEntityViewerItem(new Item.Settings().rarity(Rarity.EPIC));

	private BlsItems() {
	}

	public static void init() {
		Registry.register(Registry.ITEM, BlueLightSpecial.id("block_entity_viewer"), BLOCK_ENTITY_VIEWER);
	}
}
