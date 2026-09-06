package io.github.bladeswillfall.spelunkingrope.core.geometry;

public final class CatenarySampler {
    private static final double EPSILON = 1e-9;

    private CatenarySampler() {
    }

    public static int coordinateCount(int segments) {
        if (segments < 1) {
            throw new IllegalArgumentException("segments must be positive");
        }
        return Math.multiplyExact(segments + 1, 3);
    }

    public static void sample(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double ropeLength,
            int segments,
            double[] out
    ) {
        int required = coordinateCount(segments);
        if (out.length < required) {
            throw new IllegalArgumentException("output buffer is too small");
        }
        if (!Double.isFinite(x0) || !Double.isFinite(y0) || !Double.isFinite(z0)
                || !Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(z1)
                || !Double.isFinite(ropeLength) || ropeLength < 0.0) {
            throw new IllegalArgumentException("coordinates and rope length must be finite and rope length non-negative");
        }

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double horizontal = Math.hypot(dx, dz);
        double distance = Math.hypot(horizontal, dy);
        double tolerance = EPSILON * Math.max(1.0, distance);

        if (ropeLength + tolerance < distance) {
            throw new IllegalArgumentException("rope length is shorter than endpoint distance");
        }
        if (horizontal <= tolerance || ropeLength - distance <= tolerance) {
            sampleLine(x0, y0, z0, dx, dy, dz, segments, out);
            return;
        }

        double horizontalArc = Math.sqrt(Math.max(0.0, ropeLength * ropeLength - dy * dy));
        double u = solveDimensionless(horizontalArc / horizontal);
        double a = horizontal / (2.0 * u);
        double midpointSlope = 0.5 * (Math.log1p(dy / ropeLength) - Math.log1p(-dy / ropeLength));
        double s0 = midpointSlope - u;
        double coshS0 = Math.cosh(s0);

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            int offset = i * 3;
            out[offset] = x0 + dx * t;
            out[offset + 1] = y0 + a * (Math.cosh(s0 + 2.0 * u * t) - coshS0);
            out[offset + 2] = z0 + dz * t;
        }

        out[0] = x0;
        out[1] = y0;
        out[2] = z0;
        int last = segments * 3;
        out[last] = x1;
        out[last + 1] = y1;
        out[last + 2] = z1;
    }

    private static void sampleLine(
            double x0, double y0, double z0,
            double dx, double dy, double dz,
            int segments,
            double[] out
    ) {
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            int offset = i * 3;
            out[offset] = x0 + dx * t;
            out[offset + 1] = y0 + dy * t;
            out[offset + 2] = z0 + dz * t;
        }
    }

    private static double solveDimensionless(double ratio) {
        double u = ratio < 1.5
                ? Math.sqrt(6.0 * (ratio - 1.0))
                : Math.log(2.0 * ratio) + Math.log(Math.max(1.0, Math.log(2.0 * ratio)));

        for (int i = 0; i < 12; i++) {
            double sinh = Math.sinh(u);
            double delta = (sinh - ratio * u) / (Math.cosh(u) - ratio);
            double next = u - delta;
            if (!(next > 0.0) || !Double.isFinite(next)) {
                next = u * 0.5;
            }
            if (Math.abs(next - u) <= 1e-12 * Math.max(1.0, u)) {
                return next;
            }
            u = next;
        }
        return u;
    }
}
