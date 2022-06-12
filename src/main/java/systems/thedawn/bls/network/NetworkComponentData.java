/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.network;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Stores common data for nodes in a network, such as the positions of adjacent nodes.
 */
public class NetworkComponentData {
	private static final String CONNECTIONS = "Connections";
	private static final String POWER_LEVEL = "Power";
	private static final String SIGNAL_LEVEL = "Signal";

	/**
	 * Connection data for each of the six possible directions.
	 */
	private final EnumMap<Direction, Connection> connections;

	/**
	 * The power level at this node. Power is not lost over distance, but is used up by components.
	 */
	private int powerLevel;

	/**
	 * The connection distance of the nearest signal source at this node, in blocks.
	 */
	private int signalLevel;

	public NetworkComponentData() {
		this.connections = new EnumMap<>(Direction.class);
	}

	public void updateConnection(Direction dir, Connection connection) {
		this.connections.put(dir, connection);
	}

	public void removeConnection(Direction dir) {
		this.connections.remove(dir);
	}

	public int connectionCount() {
		return this.connections.size();
	}

	public Iterable<Map.Entry<Direction, Connection>> connections() {
		return this.connections.entrySet();
	}

	/**
	 * Reads this data from NBT.
	 */
	public void readNbt(NbtCompound nbt) {
		var connectionNbt = nbt.getList(CONNECTIONS, NbtElement.COMPOUND_TYPE);
		for(var element : connectionNbt) {
			this.readConnection((NbtCompound)element);
		}
		this.powerLevel = Math.max(0, nbt.getByte(POWER_LEVEL));
		this.signalLevel = Math.max(0, nbt.getByte(SIGNAL_LEVEL));
	}

	private void readConnection(NbtCompound nodeNbt) {
		var sourceDir = Direction.byId(nodeNbt.getByte(Connection.SOURCE_DIR));
		var destDir = Direction.byId(nodeNbt.getByte(Connection.DEST_DIR));
		var pos = new BlockPos(nodeNbt.getInt(Connection.POS_X), nodeNbt.getInt(Connection.POS_Y), nodeNbt.getInt(Connection.POS_Z));
		var face = Direction.byId(nodeNbt.getByte(Connection.FACE));
		var distance = Math.max(0, nodeNbt.getInt(Connection.DISTANCE));
		this.connections.put(sourceDir, new Connection(destDir, pos, face, distance));
	}

	/**
	 * Writes this data to NBT.
	 */
	public void writeNbt(NbtCompound nbt) {
		var nodeList = new NbtList();
		for(var entry : this.connections.entrySet()) {
			writeConnection(nodeList, entry.getKey(), entry.getValue());
		}
		nbt.put(CONNECTIONS, nodeList);
		nbt.putByte(POWER_LEVEL, (byte)this.powerLevel);
		nbt.putByte(SIGNAL_LEVEL, (byte)this.signalLevel);
	}

	private static void writeConnection(NbtList nodeList, Direction dir, Connection connection) {
		var nodeNbt = new NbtCompound();
		nodeNbt.putByte(Connection.SOURCE_DIR, (byte)dir.getId());
		nodeNbt.putByte(Connection.DEST_DIR, (byte)connection.dir().getId());
		nodeNbt.putInt(Connection.POS_X, connection.pos().getX());
		nodeNbt.putInt(Connection.POS_Y, connection.pos().getY());
		nodeNbt.putInt(Connection.POS_Z, connection.pos().getZ());
		nodeNbt.putByte(Connection.FACE, (byte)connection.face().getId());
		nodeNbt.putInt(Connection.DISTANCE, connection.distance());
		nodeList.add(nodeNbt);
	}

	/**
	 * Information about the connection between this node and another.
	 *
	 * @param dir      The (absolute) direction from which the other node connects to the source node.
	 * @param pos      The position of the other node in the world.
	 * @param face     The face the other node is on, for disambiguating among nodes in the same block space.
	 * @param distance The distance in blocks along the connection to the other node.
	 */
	public record Connection(Direction dir, BlockPos pos, Direction face, int distance) {
		static final String SOURCE_DIR = "SourceDir";
		static final String DEST_DIR = "DestDir";
		static final String POS_X = "X";
		static final String POS_Y = "Y";
		static final String POS_Z = "Z";
		static final String FACE = "Face";
		static final String DISTANCE = "Distance";
	}
}
