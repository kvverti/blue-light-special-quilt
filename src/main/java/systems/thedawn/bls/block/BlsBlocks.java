/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.block;

import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;
import systems.thedawn.bls.BlueLightSpecial;

import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.util.registry.Registry;

public final class BlsBlocks {
	public static final Block WIRE = new WireBlockBase(QuiltBlockSettings.of(Material.DECORATION).noCollision().nonOpaque());

	private BlsBlocks() {}

	public static void init() {
		registerBlockItem("wire", WIRE);
		Registry.register(Registry.BLOCK_ENTITY_TYPE, BlueLightSpecial.id("network_node"), NetworkNodeBlockEntity.TYPE);
	}

	private static void registerBlockItem(String name, Block block) {
		var id = BlueLightSpecial.id(name);
		Registry.register(Registry.BLOCK, id, block);
		Registry.register(Registry.ITEM, id, new BlockItem(block, new QuiltItemSettings()));
	}
}
