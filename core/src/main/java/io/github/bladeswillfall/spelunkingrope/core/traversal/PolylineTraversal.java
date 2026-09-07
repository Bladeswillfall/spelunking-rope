package io.github.bladeswillfall.spelunkingrope.core.traversal;

public final class PolylineTraversal {
    public static final int SAMPLE_OUTPUT_STRIDE = 6;

    private PolylineTraversal() {
    }

    public static double length(double[] points, int offset, int pointCount) {
        validate(points, offset, pointCount);
        double total = 0.0;
        for (int i = 1; i < pointCount; i++) {
            int previous = offset + (i - 1) * 3;
            int current = previous + 3;
            double dx = points[current] - points[previous];
            double dy = points[current + 1] - points[previous + 1];
            double dz = points[current + 2] - points[previous + 2];
            total += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return total;
    }

    public static double projectDistance(
            double[] points,
            int offset,
            int pointCount,
            double x,
            double y,
            double z
    ) {
        validate(points, offset, pointCount);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("point must be finite");
        }

        double bestDistanceSquared = Double.POSITIVE_INFINITY;
        double bestAlong = 0.0;
        double accumulated = 0.0;
        for (int i = 1; i < pointCount; i++) {
            int previous = offset + (i - 1) * 3;
            int current = previous + 3;
            double x0 = points[previous];
            double y0 = points[previous + 1];
            double z0 = points[previous + 2];
            double dx = points[current] - x0;
            double dy = points[current + 1] - y0;
            double dz = points[current + 2] - z0;
            double lengthSquared = dx * dx + dy * dy + dz * dz;
            if (lengthSquared <= 0.0) {
                continue;
            }

            double t = ((x - x0) * dx + (y - y0) * dy + (z - z0) * dz) / lengthSquared;
            t = Math.max(0.0, Math.min(1.0, t));
            double nearestX = x0 + dx * t;
            double nearestY = y0 + dy * t;
            double nearestZ = z0 + dz * t;
            double errorX = x - nearestX;
            double errorY = y - nearestY;
            double errorZ = z - nearestZ;
            double distanceSquared = errorX * errorX + errorY * errorY + errorZ * errorZ;
            double segmentLength = Math.sqrt(lengthSquared);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestAlong = accumulated + segmentLength * t;
            }
            accumulated += segmentLength;
        }
        return bestAlong;
    }

    public static double remapMaterialDistance(
            double currentDistance,
            double newPathLength,
            double deployedLengthDelta,
            boolean changedAtStart
    ) {
        if (!Double.isFinite(currentDistance)
                || !Double.isFinite(newPathLength)
                || !Double.isFinite(deployedLengthDelta)) {
            throw new IllegalArgumentException("material-distance inputs must be finite");
        }
        if (newPathLength < 0.0) {
            throw new IllegalArgumentException("newPathLength must not be negative");
        }

        double remapped = changedAtStart ? currentDistance + deployedLengthDelta : currentDistance;
        return Math.max(0.0, Math.min(newPathLength, remapped));
    }

    public static void sample(
            double[] points,
            int offset,
            int pointCount,
            double distance,
            double[] out,
            int outOffset
    ) {
        validate(points, offset, pointCount);
        if (!Double.isFinite(distance)) {
            throw new IllegalArgumentException("distance must be finite");
        }
        if (outOffset < 0 || outOffset > out.length || SAMPLE_OUTPUT_STRIDE > out.length - outOffset) {
            throw new IllegalArgumentException("output buffer is too small");
        }

        double target = Math.max(0.0, distance);
        double accumulated = 0.0;
        double lastTangentX = 0.0;
        double lastTangentY = 0.0;
        double lastTangentZ = 0.0;

        for (int i = 1; i < pointCount; i++) {
            int previous = offset + (i - 1) * 3;
            int current = previous + 3;
            double x0 = points[previous];
            double y0 = points[previous + 1];
            double z0 = points[previous + 2];
            double dx = points[current] - x0;
            double dy = points[current + 1] - y0;
            double dz = points[current + 2] - z0;
            double segmentLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (segmentLength <= 0.0) {
                continue;
            }

            double tangentX = dx / segmentLength;
            double tangentY = dy / segmentLength;
            double tangentZ = dz / segmentLength;
            lastTangentX = tangentX;
            lastTangentY = tangentY;
            lastTangentZ = tangentZ;
            if (target <= accumulated + segmentLength) {
                double t = Math.max(0.0, Math.min(1.0, (target - accumulated) / segmentLength));
                out[outOffset] = x0 + dx * t;
                out[outOffset + 1] = y0 + dy * t;
                out[outOffset + 2] = z0 + dz * t;
                out[outOffset + 3] = tangentX;
                out[outOffset + 4] = tangentY;
                out[outOffset + 5] = tangentZ;
                return;
            }
            accumulated += segmentLength;
        }

        int last = offset + (pointCount - 1) * 3;
        out[outOffset] = points[last];
        out[outOffset + 1] = points[last + 1];
        out[outOffset + 2] = points[last + 2];
        out[outOffset + 3] = lastTangentX;
        out[outOffset + 4] = lastTangentY;
        out[outOffset + 5] = lastTangentZ;
    }

    private static void validate(double[] points, int offset, int pointCount) {
        if (pointCount < 2) {
            throw new IllegalArgumentException("polyline needs at least two points");
        }
        int required = Math.multiplyExact(pointCount, 3);
        if (offset < 0 || offset > points.length || required > points.length - offset) {
            throw new IllegalArgumentException("polyline buffer is too small");
        }
    }
}
