/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.network;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Stores common data for nodes in a network, such as the positions of adjacent nodes.
 */
public class NetworkComponentData {
	/**
	 * The positions of all adjacent nodes.
	 */
	public final Set<NodePos> adjacentNodes;

	/**
	 * The power level at this node. Power is not lost over distance, but is used up by components.
	 */
	public int powerLevel;

	/**
	 * The connection distance of the nearest signal source at this node, in blocks.
	 */
	public int signalLevel;

	private NetworkComponentData(Set<NodePos> adjacentNodes, int powerLevel, int signalLevel) {
		this.adjacentNodes = adjacentNodes;
		this.powerLevel = powerLevel;
		this.signalLevel = signalLevel;
	}

	public NetworkComponentData() {
		this(new HashSet<>(), 0, 0);
	}

	/**
	 * The position of a node in a network.
	 *
	 * @param pos The position of the node in the world.
	 * @param face The face the node is on, for disambiguating among nodes in the same block space.
	 */
	private record NodePos(BlockPos pos, Direction face) {}
}
