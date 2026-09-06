package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;

import java.util.Objects;

public record BlockAttachment(BlockPos blockPos, double localX, double localY, double localZ) {
    public BlockAttachment {
        Objects.requireNonNull(blockPos, "blockPos");
    }

    public static BlockAttachment atWorld(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("World attachment coordinates must be finite");
        }
        BlockPos blockPos = BlockPos.containing(x, y, z);
        return new BlockAttachment(
                blockPos,
                x - blockPos.getX(),
                y - blockPos.getY(),
                z - blockPos.getZ()
        );
    }

    public double worldX() {
        return blockPos.getX() + localX;
    }

    public double worldY() {
        return blockPos.getY() + localY;
    }

    public double worldZ() {
        return blockPos.getZ() + localZ;
    }
}
