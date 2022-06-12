/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.thedawn.bls.block.BlsBlocks;
import systems.thedawn.bls.item.BlsItems;

import net.minecraft.util.Identifier;

public class BlueLightSpecial implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Blue Light Special");

	private static String modId;

	@Override
	public void onInitialize(ModContainer mod) {
		modId = mod.metadata().id();
		BlsBlocks.init();
		BlsItems.init();
	}

	public static Identifier id(String path) {
		return new Identifier(modId, path);
	}
}
