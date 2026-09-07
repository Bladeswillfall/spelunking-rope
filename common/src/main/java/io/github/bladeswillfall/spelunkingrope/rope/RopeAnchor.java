package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RopeAnchor {
    private static final double SIXTEENTH = 1.0 / 16.0;
    private static final double PITON_ROPE_Y = 8.0 * SIXTEENTH;
    private static final double PITON_ROPE_DEPTH = 6.0 * SIXTEENTH;
    private static final double GUIDE_ROPE_Y = 8.0 * SIXTEENTH;
    private static final double GUIDE_ROPE_DEPTH = 8.0 * SIXTEENTH;
    private static final double PULLEY_ROPE_Y = 3.0 * SIXTEENTH;
    private static final double PULLEY_ROPE_DEPTH = 9.0 * SIXTEENTH;
    private static final double WINCH_ROPE_Y = 4.0 * SIXTEENTH;
    private static final double WINCH_ROPE_DEPTH = 10.0 * SIXTEENTH;

    private RopeAnchor() {
    }

    public static boolean isPiton(BlockState state) {
        return state.getBlock() instanceof PitonBlock && !isGuideClip(state) && !isPulley(state) && !isWinch(state);
    }

    public static boolean isGuideClip(BlockState state) {
        return state.getBlock() instanceof GuideClipBlock;
    }

    public static boolean isPulley(BlockState state) {
        return state.getBlock() instanceof PulleyBlock;
    }

    public static boolean isWinch(BlockState state) {
        return state.getBlock() instanceof WinchBlock;
    }

    public static boolean isRouteAnchor(BlockState state) {
        return isPiton(state) || isPulley(state) || isWinch(state);
    }

    public static boolean isGuideAnchor(BlockState state) {
        return isGuideClip(state) || isPiton(state);
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
        if (isPulley(state)) {
            return pulleyAttachment(anchorPos, facing);
        }
        if (isWinch(state)) {
            return winchAttachment(anchorPos, facing);
        }
        return attachment(anchorPos, facing);
    }

    static BlockAttachment guideAttachment(BlockPos anchorPos, BlockState state) {
        if (isGuideClip(state)) {
            return guideAttachment(anchorPos, state.getValue(PitonBlock.FACING));
        }
        if (isPiton(state)) {
            return attachment(anchorPos, state.getValue(PitonBlock.FACING));
        }
        throw new IllegalArgumentException("Block state is not a guide anchor");
    }

    static BlockAttachment pulleyAttachment(BlockPos anchorPos, Direction facing) {
        return wallAttachment(anchorPos, facing, PULLEY_ROPE_Y, PULLEY_ROPE_DEPTH);
    }

    static BlockAttachment winchAttachment(BlockPos anchorPos, Direction facing) {
        return wallAttachment(anchorPos, facing, WINCH_ROPE_Y, WINCH_ROPE_DEPTH);
    }

    static BlockAttachment guideAttachment(BlockPos anchorPos, Direction facing) {
        return wallAttachment(anchorPos, facing, GUIDE_ROPE_Y, GUIDE_ROPE_DEPTH);
    }

    public static BlockAttachment attachment(BlockPos anchorPos, Direction facing) {
        return wallAttachment(anchorPos, facing, PITON_ROPE_Y, PITON_ROPE_DEPTH);
    }

    private static BlockAttachment wallAttachment(
            BlockPos anchorPos,
            Direction facing,
            double localY,
            double localDepth
    ) {
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Wall hardware facing must be horizontal");
        }
        double outward = 0.5 - localDepth;
        return BlockAttachment.atWorld(
                anchorPos.getX() + 0.5 + facing.getStepX() * outward,
                anchorPos.getY() + localY,
                anchorPos.getZ() + 0.5 + facing.getStepZ() * outward
        );
    }
}
