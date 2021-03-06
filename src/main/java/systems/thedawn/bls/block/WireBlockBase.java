/*
 * Blue Light Special
 * Copyright (C) Thalia Nero 2022
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package systems.thedawn.bls.block;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.OrderedTick;

/**
 * The base class for all blocks with wire-like connection behavior
 */
public class WireBlockBase extends BlockWithEntity {
	public static final Property<Direction> FACING = Properties.FACING;
	public static final Property<Boolean> FORWARD = BooleanProperty.of("forward");
	public static final Property<Boolean> BACKWARD = BooleanProperty.of("backward");
	public static final Property<Boolean> LEFT = BooleanProperty.of("left");
	public static final Property<Boolean> RIGHT = BooleanProperty.of("right");

	public static final List<Property<Boolean>> CONNECTIONS = List.of(FORWARD, BACKWARD, LEFT, RIGHT);

	/**
	 * A lookup by facing direction and relative direction using Direction IDs.
	 */
	private static final Direction[][] RELATIVE_DIR_LOOKUP = {
		{ Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST },
		{ Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST },
		{ Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST },
		{ Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST },
		{ Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH },
		{ Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH },
	};

	/**
	 * Lookup for outline shape parts by facing direction and relative horizontal direction.
	 */
	private static final VoxelShape[][] OUTLINE_SHAPES = {
		{
			Block.createCuboidShape(7, 14, 9, 9, 16, 16),
			Block.createCuboidShape(7, 14, 0, 9, 16, 7),
			Block.createCuboidShape(0, 14, 7, 7, 16, 9),
			Block.createCuboidShape(9, 14, 7, 16, 16, 9),
		},
		{
			Block.createCuboidShape(7, 0, 0, 9, 2, 7),
			Block.createCuboidShape(7, 0, 9, 9, 2, 16),
			Block.createCuboidShape(0, 0, 7, 7, 2, 9),
			Block.createCuboidShape(9, 0, 7, 16, 2, 9),
		},
		{
			Block.createCuboidShape(7, 9, 14, 9, 16, 16),
			Block.createCuboidShape(7, 0, 14, 9, 7, 16),
			Block.createCuboidShape(9, 7, 14, 16, 9, 16),
			Block.createCuboidShape(0, 7, 14, 7, 9, 16),
		},
		{
			Block.createCuboidShape(7, 9, 0, 9, 16, 2),
			Block.createCuboidShape(7, 0, 0, 9, 7, 2),
			Block.createCuboidShape(0, 7, 0, 7, 9, 2),
			Block.createCuboidShape(9, 7, 0, 16, 9, 2),
		},
		{
			Block.createCuboidShape(14, 9, 7, 16, 16, 9),
			Block.createCuboidShape(14, 0, 7, 16, 7, 9),
			Block.createCuboidShape(14, 7, 0, 16, 9, 7),
			Block.createCuboidShape(14, 7, 9, 16, 9, 16),
		},
		{
			Block.createCuboidShape(0, 9, 7, 2, 16, 9),
			Block.createCuboidShape(0, 0, 7, 2, 7, 9),
			Block.createCuboidShape(0, 7, 9, 2, 9, 16),
			Block.createCuboidShape(0, 7, 0, 2, 9, 7),
		}
	};

	private static final VoxelShape[] BASE_OUTLINE_SHAPES = {
		Block.createCuboidShape(7, 14, 7, 9, 16, 9),
		Block.createCuboidShape(7, 0, 7, 9, 2, 9),
		Block.createCuboidShape(7, 7, 14, 9, 9, 16),
		Block.createCuboidShape(7, 7, 0, 9, 9, 2),
		Block.createCuboidShape(14, 7, 7, 16, 9, 9),
		Block.createCuboidShape(0, 7, 7, 2, 9, 9),
	};

	public WireBlockBase(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState()
			.with(FACING, Direction.UP)
			.with(FORWARD, false)
			.with(BACKWARD, false)
			.with(LEFT, false)
			.with(RIGHT, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FACING, FORWARD, BACKWARD, LEFT, RIGHT);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		var facing = ctx.getSide();
		var world = ctx.getWorld();
		var pos = ctx.getBlockPos();

		var state = this.getDefaultState().with(FACING, facing);
		var connectionDirs = relativeHorizontal(facing);
		for(int i = 0; i < connectionDirs.length; i++) {
			var adjacentState = world.getBlockState(pos.offset(connectionDirs[i]));
			// todo replace with interface
			if(canConnectTo(state, adjacentState)) {
				state = state.with(CONNECTIONS.get(i), true);
			}
		}

		return state;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if(world.isClient()) {
			return;
		}

		// initialize block entity state
		var node = NetworkNodeBlockEntity.TYPE.get(world, pos);
		if(node != null) {
			for(var dir : connectionDirs(state)) {
				node.startDiscovery(dir);
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		var relativeConnectionDir = toRelative(state.get(FACING), direction);
		if(!relativeConnectionDir.getAxis().isHorizontal()) {
			return state;
		}
		var connected = canConnectTo(state, neighborState) &&
			neighborState.get(toProperty(relativeConnectionDir.getOpposite()));
		return state.with(toProperty(relativeConnectionDir), connected);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
		super.neighborUpdate(state, world, pos, block, fromPos, notify);
		if(world.isClient()) {
			return;
		}

		// speculatively queue discovery in the direction that changed
		var blockEntity = NetworkNodeBlockEntity.TYPE.get(world, pos);
		if(blockEntity != null) {
			var updateDir = Direction.fromVector(fromPos.subtract(pos));
			if(updateDir != null) {
				blockEntity.queueDiscovery(updateDir);
			}
		}

		// second part of the update requires current state
		world.getBlockTickScheduler().scheduleTick(OrderedTick.create(BlsBlocks.WIRE, pos));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		var blockEntity = NetworkNodeBlockEntity.TYPE.get(world, pos);
		if(blockEntity == null) {
			return;
		}

		if(!shouldHaveBlockEntity(state)) {
			// remove BE since we're no longer a node
			blockEntity.notifyConnectionsBeforeRemove(connectionDirs(state));
			world.removeBlockEntity(pos);
		} else if(blockEntity.isEmpty()) {
			// start discovery for all directions
			for(var dir : connectionDirs(state)) {
				blockEntity.startDiscovery(dir);
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		var facing = state.get(FACING);
		var shape = BASE_OUTLINE_SHAPES[facing.getId()];
		var outlines = OUTLINE_SHAPES[facing.getId()];
		for(int i = 0; i < 4; i++) {
			if(state.get(CONNECTIONS.get(i))) {
				shape = VoxelShapes.union(shape, outlines[i]);
			}
		}
		return shape;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		if(shouldHaveBlockEntity(state)) {
			return new NetworkNodeBlockEntity(pos, state);
		}
		return null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		var ticker = checkType(type, NetworkNodeBlockEntity.TYPE, NetworkNodeBlockEntity::tick);
		return shouldHaveBlockEntity(state) ? ticker : null;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	/**
	 * Whether the block state should store a block entity.
	 */
	public static boolean shouldHaveBlockEntity(BlockState state) {
		// block states with one, three, or four connections store data
		int connections = 0;
		for(var prop : CONNECTIONS) {
			if(state.get(prop)) {
				connections++;
			}
		}
		return connections != 0 && connections != 2;
	}

	/**
	 * Computes the (absolute) directions in which the given state is connected.
	 */
	public static EnumSet<Direction> connectionDirs(BlockState state) {
		var dirs = EnumSet.noneOf(Direction.class);
		var facing = state.get(FACING);
		for(var dir : relativeHorizontal(facing)) {
			if(hasConnectionInAbsolute(state, dir)) {
				dirs.add(dir);
			}
		}
		return dirs;
	}

	/**
	 * Whether this state can possibly connect to another state.
	 *
	 * @param state This state.
	 * @param other The other state.
	 */
	public static boolean canConnectTo(BlockState state, BlockState other) {
		// todo replace with interface
		return other.getBlock() instanceof WireBlockBase && other.get(FACING) == state.get(FACING);
	}

	public static boolean hasConnectionInAbsolute(BlockState state, Direction absolute) {
		var facing = state.get(FACING);
		if(facing.getAxis() == absolute.getAxis()) {
			return false;
		}
		return state.get(toProperty(toRelative(facing, absolute)));
	}

	private static Direction toAbsolute(Direction facing, Direction relative) {
		return RELATIVE_DIR_LOOKUP[facing.getId()][relative.getId()];
	}

	private static Direction[] toAbsolute(Direction facing, Direction... relative) {
		for(int i = 0; i < relative.length; i++) {
			relative[i] = toAbsolute(facing, relative[i]);
		}
		return relative;
	}

	private static Direction toRelative(Direction facing, Direction absolute) {
		var dirs = RELATIVE_DIR_LOOKUP[facing.getId()];
		for(int i = 0; i < 6; i++) {
			if(dirs[i] == absolute) {
				return Direction.byId(i);
			}
		}
		throw new IllegalArgumentException("Impossible direction: " + absolute);
	}

	private static Direction[] relativeHorizontal(Direction facing) {
		return toAbsolute(facing, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
	}

	private static Property<Boolean> toProperty(Direction relative) {
		return switch(relative) {
			case NORTH -> FORWARD;
			case SOUTH -> BACKWARD;
			case WEST -> LEFT;
			case EAST -> RIGHT;
			default -> throw new IllegalArgumentException(String.valueOf(relative));
		};
	}
}
