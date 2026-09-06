package io.github.bladeswillfall.spelunkingrope.core.geometry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatenarySamplerTest {
    @Test
    void samplesTautArbitrary3dSpanAsLine() {
        int segments = 4;
        double[] out = new double[CatenarySampler.coordinateCount(segments)];
        double distance = Math.sqrt(61.0);

        CatenarySampler.sample(1, 2, 3, 4, 6, 9, distance, segments, out);

        assertPoint(out, 0, 1, 2, 3);
        assertPoint(out, 2, 2.5, 4, 6);
        assertPoint(out, 4, 4, 6, 9);
    }

    @Test
    void rejectsRopeShorterThanEndpointDistance() {
        double[] out = new double[CatenarySampler.coordinateCount(8)];
        assertThrows(IllegalArgumentException.class,
                () -> CatenarySampler.sample(0, 0, 0, 3, 4, 0, 4.99, 8, out));
    }

    @Test
    void slackHorizontalSpanSagsAndMatchesLength() {
        int segments = 512;
        double[] out = new double[CatenarySampler.coordinateCount(segments)];

        CatenarySampler.sample(0, 10, 0, 10, 10, 0, 12, segments, out);

        assertPoint(out, 0, 0, 10, 0);
        assertPoint(out, segments, 10, 10, 0);
        assertTrue(out[(segments / 2) * 3 + 1] < 10.0);
        assertEquals(12.0, polylineLength(out, segments), 1e-4);
    }

    @Test
    void samplesSlackArbitrary3dSpan() {
        int segments = 512;
        double[] out = new double[CatenarySampler.coordinateCount(segments)];

        CatenarySampler.sample(0, 5, 0, 10, 0, 10, 18, segments, out);

        assertPoint(out, 0, 0, 5, 0);
        assertPoint(out, segments, 10, 0, 10);
        assertEquals(18.0, polylineLength(out, segments), 1e-4);
    }

    @Test
    void nearTautSpanUsesStableStraightFastPath() {
        int segments = 2;
        double[] out = new double[CatenarySampler.coordinateCount(segments)];
        double distance = Math.sqrt(2.0);

        CatenarySampler.sample(0, 0, 0, 1, 0, 1, distance + 1e-12, segments, out);

        assertPoint(out, 1, 0.5, 0, 0.5);
    }

    @Test
    void verticalSpanIsStableEvenWithSlack() {
        int segments = 8;
        double[] out = new double[CatenarySampler.coordinateCount(segments)];

        CatenarySampler.sample(2, 10, 3, 2, 0, 3, 14, segments, out);

        for (int i = 0; i <= segments; i++) {
            double expectedY = 10.0 - 10.0 * i / segments;
            assertPoint(out, i, 2, expectedY, 3);
        }
    }

    @Test
    void writesIntoCallerProvidedOffsetWithoutTouchingNeighbors() {
        int segments = 2;
        int coordinateCount = CatenarySampler.coordinateCount(segments);
        double[] out = new double[coordinateCount + 8];
        Arrays.fill(out, -99.0);

        CatenarySampler.sample(1, 2, 3, 5, 2, 3, 5, segments, out, 4);

        assertEquals(-99.0, out[3], 0.0);
        assertEquals(-99.0, out[4 + coordinateCount], 0.0);
        assertEquals(1.0, out[4], 0.0);
        assertEquals(5.0, out[4 + coordinateCount - 3], 0.0);
    }

    @Test
    void validatesCallerOwnedBuffer() {
        assertEquals(15, CatenarySampler.coordinateCount(4));
        assertThrows(IllegalArgumentException.class, () -> CatenarySampler.coordinateCount(0));
        assertThrows(IllegalArgumentException.class,
                () -> CatenarySampler.sample(0, 0, 0, 1, 0, 0, 1, 4, new double[14]));
        assertThrows(IllegalArgumentException.class,
                () -> CatenarySampler.sample(0, 0, 0, 1, 0, 0, 1, 4, new double[20], 6));
        assertThrows(IllegalArgumentException.class,
                () -> CatenarySampler.sample(0, 0, 0, 1, 0, 0, 1, 4, new double[20], -1));
    }

    private static void assertPoint(double[] coordinates, int point, double x, double y, double z) {
        int offset = point * 3;
        assertEquals(x, coordinates[offset], 1e-12);
        assertEquals(y, coordinates[offset + 1], 1e-12);
        assertEquals(z, coordinates[offset + 2], 1e-12);
    }

    private static double polylineLength(double[] coordinates, int segments) {
        double length = 0.0;
        for (int i = 1; i <= segments; i++) {
            int previous = (i - 1) * 3;
            int current = i * 3;
            double dx = coordinates[current] - coordinates[previous];
            double dy = coordinates[current + 1] - coordinates[previous + 1];
            double dz = coordinates[current + 2] - coordinates[previous + 2];
            length += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return length;
    }
}
