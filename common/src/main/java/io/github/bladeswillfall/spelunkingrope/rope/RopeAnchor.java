package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RopeAnchor {
    private static final double PULLEY_FACE_OFFSET = 0.45;

    private RopeAnchor() {
    }

    public static boolean isPiton(BlockState state) {
        return state.getBlock() instanceof PitonBlock && !isGuideClip(state) && !isPulley(state);
    }

    public static boolean isGuideClip(BlockState state) {
        return state.getBlock() instanceof GuideClipBlock;
    }

    public static boolean isPulley(BlockState state) {
        return state.getBlock() instanceof PulleyBlock;
    }

    public static boolean isRouteAnchor(BlockState state) {
        return isPiton(state) || isPulley(state);
    }

    public static Direction facing(BlockState state) {
        if (isRouteAnchor(state)) {
            return state.getValue(PitonBlock.FACING);
        }
        if (state.is(Blocks.TRIPWIRE_HOOK)) {
            return state.getValue(TripWireHookBlock.FACING);
        }
        return null;
    }

    public static Direction guideFacing(BlockState state) {
        return isGuideClip(state) ? state.getValue(PitonBlock.FACING) : null;
    }

    public static BlockAttachment routeAttachment(BlockPos anchorPos, BlockState state) {
        Direction facing = facing(state);
        if (facing == null) {
            throw new IllegalArgumentException("Block state is not a rope route anchor");
        }
        return isPulley(state) ? pulleyAttachment(anchorPos, facing) : attachment(anchorPos, facing);
    }

    static BlockAttachment pulleyAttachment(BlockPos anchorPos, Direction facing) {
        return BlockAttachment.atWorld(
                anchorPos.getX() + 0.5 + facing.getStepX() * PULLEY_FACE_OFFSET,
                anchorPos.getY() + 0.5,
                anchorPos.getZ() + 0.5 + facing.getStepZ() * PULLEY_FACE_OFFSET
        );
    }

    public static BlockAttachment attachment(BlockPos anchorPos, Direction facing) {
        BlockPos outward = anchorPos.relative(facing);
        return BlockAttachment.atWorld(
                outward.getX() + 0.5,
                anchorPos.getY() + 0.5,
                outward.getZ() + 0.5
        );
    }
}
