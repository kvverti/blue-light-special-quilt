/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.block;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;

import systems.thedawn.bls.network.NetworkComponentData;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Block entity for network data.
 */
public class NetworkNodeBlockEntity extends BlockEntity {

	public static final BlockEntityType<NetworkNodeBlockEntity> TYPE =
		FabricBlockEntityTypeBuilder.create(NetworkNodeBlockEntity::new, BlsBlocks.WIRE).build();

	private static final String DISCOVERY_STATE = "Discovery";
	private static final String QUEUED_DISCOVERY = "QueuedDiscovery";

	/**
	 * Data about this node and its connections.
	 */
	public final NetworkComponentData componentData;

	/**
	 * Positions of the cursors where connections are currently being found.
	 */
	private final EnumMap<Direction, DiscoveryData> discoveryState;

	/**
	 * Discovery that should begin on the next tick.
	 */
	private final EnumSet<Direction> queuedDiscovery;

	public NetworkNodeBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(TYPE, blockPos, blockState);
		this.componentData = new NetworkComponentData();
		this.discoveryState = new EnumMap<>(Direction.class);
		this.queuedDiscovery = EnumSet.noneOf(Direction.class);
	}

	/**
	 * Initiates the connected node discovery process in the given direction.
	 */
	public void startDiscovery(Direction dir) {
		this.startDiscoveryAt(dir, this.pos.offset(dir), dir.getOpposite(), 1);
		this.queuedDiscovery.remove(dir);
	}

	/**
	 * Initiates the connected node discovery process from the given location.
	 *
	 * @param srcDir   The direction from this node where discovery is occurring.
	 * @param pos      The initial position.
	 * @param dstDir   The direction along the path to this node from the current position.
	 * @param distance The path distance to the current position.
	 */
	public void startDiscoveryAt(Direction srcDir, BlockPos pos, Direction dstDir, int distance) {
		var data = new DiscoveryData();
		data.pos = pos;
		data.fromDir = dstDir;
		data.distance = distance;
		this.discoveryState.put(srcDir, data);
	}

	/**
	 * Queue the connected node discovery process in the given direction for the next tick.
	 */
	public void queueDiscovery(Direction dir) {
		this.queuedDiscovery.add(dir);
	}

	public void updateConnection(Direction dir, NetworkComponentData.Connection connection) {
		this.componentData.updateConnection(dir, connection);
		this.discoveryState.remove(dir);
		this.queuedDiscovery.remove(dir);
	}

	/**
	 * Breaks any connection in the given direction.
	 */
	public void breakConnection(Direction dir) {
		this.componentData.removeConnection(dir);
		this.discoveryState.remove(dir);
		this.queuedDiscovery.remove(dir);
	}

	/**
	 * Whether this block entity doesn't have any state yet.
	 */
	public boolean isEmpty() {
		return this.componentData.connectionCount() == 0 && this.discoveryState.isEmpty();
	}

	public static void tick(World world, BlockPos pos, BlockState state, NetworkNodeBlockEntity self) {
		for(var itr = self.discoveryState.entrySet().iterator(); itr.hasNext(); ) {
			var entry = itr.next();
			var dir = entry.getKey();
			var data = entry.getValue();
			if(self.tickDiscovery(world, dir, data)) {
				itr.remove();
			}
		}
		for(var dir : self.queuedDiscovery) {
			// only start discovery if we actually have a connection
			if(WireBlockBase.hasConnectionInAbsolute(state, dir)) {
				self.startDiscovery(dir);
			}
		}
		self.queuedDiscovery.clear();
	}

	/**
	 * Performs a single step of the discovery process for a particular direction.
	 *
	 * @param sourceDir The direction from the source node.
	 * @param data      Current discovery data.
	 * @return Whether discovery should stop for this direction.
	 */
	private boolean tickDiscovery(World world, Direction sourceDir, DiscoveryData data) {
		var currentState = world.getBlockState(data.pos);
		this.markDirty();
		// todo interface
		if(currentState.getBlock() == BlsBlocks.WIRE) {
			var currentFace = currentState.get(WireBlockBase.FACING);
			// determine whether the current state is a node
			if(WireBlockBase.shouldHaveBlockEntity(currentState)) {
				// it's a node, finish discovery
				var connection = new NetworkComponentData.Connection(data.fromDir, data.pos, currentFace, data.distance);
				this.componentData.updateConnection(sourceDir, connection);
				// update the connection on the other end as well
				var otherNode = TYPE.get(world, data.pos);
				if(otherNode != null) {
					var facing = this.getCachedState().get(WireBlockBase.FACING);
					var reverseConnection = new NetworkComponentData.Connection(sourceDir, this.pos, facing, data.distance);
					otherNode.updateConnection(data.fromDir, reverseConnection);
				}
				return true;
			}
			// if not a node, step to the next block
			var connectionDirs = WireBlockBase.connectionDirs(currentState);
			connectionDirs.remove(data.fromDir);
			if(connectionDirs.size() != 1) {
				// something got corrupted, bail and stop
				this.breakConnection(sourceDir);
				return true;
			}
			var nextDir = connectionDirs.iterator().next();
			data.pos = data.pos.offset(nextDir);
			data.fromDir = nextDir.getOpposite();
			data.distance += 1;
			return false;
		} else {
			// this isn't even a valid connection
			this.breakConnection(sourceDir);
			return true;
		}
	}

	public void notifyConnectionsBeforeRemove(Collection<Direction> currentConnectionDirs) {
		// initiate discovery from this location at all of the connections
		for(var entry : this.componentData.connections()) {
			var dirFromThisNode = entry.getKey();
			var connection = entry.getValue();
			var dirFromOtherNode = connection.dir();
			var otherNodePos = connection.pos();
			var otherNode = TYPE.get(this.world, otherNodePos);
			if(otherNode != null) {
				if(currentConnectionDirs.contains(dirFromThisNode)) {
					// currently connected, should start discovery at this position for that node
					otherNode.startDiscoveryAt(dirFromOtherNode, this.pos, dirFromThisNode, connection.distance());
				} else {
					// not currently connected, should remove this node from its connections
					otherNode.breakConnection(dirFromOtherNode);
				}
			}
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		this.componentData.readNbt(nbt);
		for(var discoveryElement : nbt.getList(DISCOVERY_STATE, NbtElement.COMPOUND_TYPE)) {
			this.readDiscoveryData((NbtCompound)discoveryElement);
		}
		for(var dirId : nbt.getByteArray(QUEUED_DISCOVERY)) {
			this.queuedDiscovery.add(Direction.byId(dirId));
		}
	}

	private void readDiscoveryData(NbtCompound nbt) {
		var data = new DiscoveryData();
		var sourceDir = Direction.byId(nbt.getByte(DiscoveryData.SOURCE_DIR));
		data.pos = new BlockPos(nbt.getInt(DiscoveryData.POS_X), nbt.getInt(DiscoveryData.POS_Y), nbt.getInt(DiscoveryData.POS_Z));
		data.fromDir = Direction.byId(nbt.getByte(DiscoveryData.FROM_DIR));
		data.distance = nbt.getInt(DiscoveryData.DISTANCE);
		this.discoveryState.put(sourceDir, data);
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		this.componentData.writeNbt(nbt);
		var discoveryList = new NbtList();
		for(var entry : this.discoveryState.entrySet()) {
			writeDiscoveryData(discoveryList, entry.getKey(), entry.getValue());
		}
		nbt.put(DISCOVERY_STATE, discoveryList);
		var queued = new byte[this.queuedDiscovery.size()];
		var queuedDiscovery = this.queuedDiscovery.toArray(new Direction[0]);
		for(int i = 0; i < queuedDiscovery.length; i++) {
			queued[i] = (byte)queuedDiscovery[i].getId();
		}
		nbt.put(QUEUED_DISCOVERY, new NbtByteArray(queued));
	}

	private static void writeDiscoveryData(NbtList ls, Direction srcDir, DiscoveryData data) {
		var nbt = new NbtCompound();
		nbt.putByte(DiscoveryData.SOURCE_DIR, (byte)srcDir.getId());
		nbt.putInt(DiscoveryData.POS_X, data.pos.getX());
		nbt.putInt(DiscoveryData.POS_Y, data.pos.getY());
		nbt.putInt(DiscoveryData.POS_Z, data.pos.getZ());
		nbt.putByte(DiscoveryData.FROM_DIR, (byte)data.fromDir.getId());
		nbt.putInt(DiscoveryData.DISTANCE, data.distance);
		ls.add(nbt);
	}

	private static class DiscoveryData {
		static final String SOURCE_DIR = "SourceDir";
		static final String POS_X = "X";
		static final String POS_Y = "Y";
		static final String POS_Z = "Z";
		static final String FROM_DIR = "FromDir";
		static final String DISTANCE = "Distance";

		/**
		 * Current position.
		 */
		BlockPos pos;

		/**
		 * Direction of the previous position.
		 */
		Direction fromDir;

		/**
		 * Wire distance from the source.
		 */
		int distance;
	}
}
