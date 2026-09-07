package io.github.bladeswillfall.spelunkingrope.core.traversal;

/**
 * Applies the distance constraints used while a player is hanging from a rope.
 *
 * <p>The one-sided constraint allows slack: it only projects positions that exceed the available
 * rope length and only removes outward radial velocity. The taut constraint projects onto the
 * requested rope length and removes all radial velocity while preserving tangential swing momentum.</p>
 */
public final class RappelConstraint {
    public static final int OUTPUT_STRIDE = 6;
    private static final double DIRECTION_EPSILON_SQUARED = 1.0e-12;

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
        validate(maxLength, output, offset);

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

    public static void constrainTaut(
            double anchorX,
            double anchorY,
            double anchorZ,
            double predictedX,
            double predictedY,
            double predictedZ,
            double velocityX,
            double velocityY,
            double velocityZ,
            double targetLength,
            double[] output,
            int offset
    ) {
        validate(targetLength, output, offset);

        double dx = predictedX - anchorX;
        double dy = predictedY - anchorY;
        double dz = predictedZ - anchorZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double nx;
        double ny;
        double nz;
        if (distanceSquared <= DIRECTION_EPSILON_SQUARED) {
            nx = 0.0;
            ny = -1.0;
            nz = 0.0;
        } else {
            double inverseDistance = 1.0 / Math.sqrt(distanceSquared);
            nx = dx * inverseDistance;
            ny = dy * inverseDistance;
            nz = dz * inverseDistance;
        }

        double radialVelocity = velocityX * nx + velocityY * ny + velocityZ * nz;
        velocityX -= nx * radialVelocity;
        velocityY -= ny * radialVelocity;
        velocityZ -= nz * radialVelocity;

        write(
                output,
                offset,
                anchorX + nx * targetLength,
                anchorY + ny * targetLength,
                anchorZ + nz * targetLength,
                velocityX,
                velocityY,
                velocityZ
        );
    }

    private static void validate(double length, double[] output, int offset) {
        if (!(length > 0.0) || !Double.isFinite(length)) {
            throw new IllegalArgumentException("rope length must be finite and positive");
        }
        if (offset < 0 || output.length - offset < OUTPUT_STRIDE) {
            throw new IllegalArgumentException("output needs six values from offset");
        }
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
