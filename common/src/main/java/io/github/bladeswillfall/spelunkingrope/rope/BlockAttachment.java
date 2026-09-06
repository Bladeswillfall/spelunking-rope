package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;

import java.util.Objects;

public record BlockAttachment(BlockPos blockPos, double localX, double localY, double localZ) {
    public BlockAttachment {
        Objects.requireNonNull(blockPos, "blockPos");
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
