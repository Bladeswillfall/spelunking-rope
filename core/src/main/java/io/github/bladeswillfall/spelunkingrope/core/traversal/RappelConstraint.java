package io.github.bladeswillfall.spelunkingrope.core.traversal;

/**
 * Applies the one-sided distance constraint for a player hanging from a rope.
 *
 * <p>The caller supplies the predicted player position after gravity/input. When that position would
 * exceed the available rope length, it is projected back onto the constraint sphere and only outward
 * radial velocity is removed. Tangential velocity is preserved, which is the momentum needed for a
 * pendulum swing. Inward velocity is intentionally retained because a rope can go slack; it cannot push.</p>
 */
public final class RappelConstraint {
    public static final int OUTPUT_STRIDE = 6;

    private RappelConstraint() {
    }

    public static boolean constrain(
            double anchorX,
            double anchorY,
            double anchorZ,
            double predictedX,
            double predictedY,
            double predictedZ,
            double velocityX,
            double velocityY,
            double velocityZ,
            double maxLength,
            double[] output,
            int offset
    ) {
        if (!(maxLength > 0.0) || !Double.isFinite(maxLength)) {
            throw new IllegalArgumentException("maxLength must be finite and positive");
        }
        if (offset < 0 || output.length - offset < OUTPUT_STRIDE) {
            throw new IllegalArgumentException("output needs six values from offset");
        }

        double dx = predictedX - anchorX;
        double dy = predictedY - anchorY;
        double dz = predictedZ - anchorZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double maxLengthSquared = maxLength * maxLength;

        if (distanceSquared <= maxLengthSquared) {
            write(output, offset, predictedX, predictedY, predictedZ, velocityX, velocityY, velocityZ);
            return false;
        }

        double inverseDistance = 1.0 / Math.sqrt(distanceSquared);
        double nx = dx * inverseDistance;
        double ny = dy * inverseDistance;
        double nz = dz * inverseDistance;

        double constrainedX = anchorX + nx * maxLength;
        double constrainedY = anchorY + ny * maxLength;
        double constrainedZ = anchorZ + nz * maxLength;

        double radialVelocity = velocityX * nx + velocityY * ny + velocityZ * nz;
        if (radialVelocity > 0.0) {
            velocityX -= nx * radialVelocity;
            velocityY -= ny * radialVelocity;
            velocityZ -= nz * radialVelocity;
        }

        write(output, offset, constrainedX, constrainedY, constrainedZ, velocityX, velocityY, velocityZ);
        return true;
    }

    private static void write(
            double[] output,
            int offset,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ
    ) {
        output[offset] = x;
        output[offset + 1] = y;
        output[offset + 2] = z;
        output[offset + 3] = velocityX;
        output[offset + 4] = velocityY;
        output[offset + 5] = velocityZ;
    }
}
