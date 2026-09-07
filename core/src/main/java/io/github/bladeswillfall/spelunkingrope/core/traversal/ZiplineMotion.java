package io.github.bladeswillfall.spelunkingrope.core.traversal;

public final class ZiplineMotion {
    public static final double GRAVITY_PER_TICK = 0.08;
    public static final double INPUT_ACCELERATION = 0.018;
    public static final double DAMPING = 0.985;
    public static final double MAX_SPEED = 0.90;
    public static final double STOP_EPSILON = 0.001;

    private ZiplineMotion() {
    }

    public static double integrateSpeed(double speed, double tangentY, byte input) {
        if (!Double.isFinite(speed) || !Double.isFinite(tangentY)) {
            throw new IllegalArgumentException("zipline motion values must be finite");
        }
        if (input < -1 || input > 1) {
            throw new IllegalArgumentException("input must be -1, 0, or 1");
        }

        double next = (speed - GRAVITY_PER_TICK * tangentY + INPUT_ACCELERATION * input) * DAMPING;
        next = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, next));
        return Math.abs(next) < STOP_EPSILON ? 0.0 : next;
    }

    public static double clampDistance(double distance, double pathLength, double speed) {
        if (!Double.isFinite(distance) || !Double.isFinite(pathLength) || !Double.isFinite(speed)
                || pathLength < 0.0) {
            throw new IllegalArgumentException("zipline path values must be finite and path length non-negative");
        }
        return Math.max(0.0, Math.min(pathLength, distance + speed));
    }

    public static double stopAtEndpoint(double distance, double pathLength, double speed) {
        if ((distance <= 0.0 && speed < 0.0) || (distance >= pathLength && speed > 0.0)) {
            return 0.0;
        }
        return speed;
    }

    public static double clampSpeed(double speed) {
        if (!Double.isFinite(speed)) {
            throw new IllegalArgumentException("speed must be finite");
        }
        return Math.max(-MAX_SPEED, Math.min(MAX_SPEED, speed));
    }
}
