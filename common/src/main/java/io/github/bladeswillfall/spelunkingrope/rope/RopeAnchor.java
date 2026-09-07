package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RopeAnchor {
    private RopeAnchor() {
    }

    public static boolean isPiton(BlockState state) {
        return state.getBlock() instanceof PitonBlock;
    }

    public static Direction facing(BlockState state) {
        if (isPiton(state)) {
            return state.getValue(PitonBlock.FACING);
        }
        if (state.is(Blocks.TRIPWIRE_HOOK)) {
            return state.getValue(TripWireHookBlock.FACING);
        }
        return null;
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
