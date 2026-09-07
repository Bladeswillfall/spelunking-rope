package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GuideClipBlock extends PitonBlock {
    private static final VoxelShape NORTH_SHAPE = Block.box(6.0, 6.0, 7.0, 10.0, 10.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 9.0);
    private static final VoxelShape WEST_SHAPE = Block.box(7.0, 6.0, 6.0, 16.0, 10.0, 10.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 6.0, 6.0, 9.0, 10.0, 10.0);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }
}
