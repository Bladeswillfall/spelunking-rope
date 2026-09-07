package io.github.bladeswillfall.spelunkingrope.core.traversal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolylineTraversalTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void measuresProjectsAndSamplesWithoutAllocatingPathObjects() {
        double[] points = {
                0.0, 0.0, 0.0,
                4.0, 0.0, 0.0,
                4.0, 3.0, 0.0
        };
        double[] sample = new double[PolylineTraversal.SAMPLE_OUTPUT_STRIDE];

        assertEquals(7.0, PolylineTraversal.length(points, 0, 3), EPSILON);
        assertEquals(6.0, PolylineTraversal.projectDistance(points, 0, 3, 4.25, 2.0, 0.0), EPSILON);

        PolylineTraversal.sample(points, 0, 3, 5.0, sample, 0);
        assertEquals(4.0, sample[0], EPSILON);
        assertEquals(1.0, sample[1], EPSILON);
        assertEquals(0.0, sample[2], EPSILON);
        assertEquals(0.0, sample[3], EPSILON);
        assertEquals(1.0, sample[4], EPSILON);
        assertEquals(0.0, sample[5], EPSILON);

        PolylineTraversal.sample(points, 0, 3, 99.0, sample, 0);
        assertEquals(4.0, sample[0], EPSILON);
        assertEquals(3.0, sample[1], EPSILON);
        assertEquals(0.0, sample[2], EPSILON);
        assertEquals(0.0, sample[3], EPSILON);
        assertEquals(1.0, sample[4], EPSILON);
        assertEquals(0.0, sample[5], EPSILON);
    }

    @Test
    void remapsMaterialDistanceFromTheEndpointThatFeedsRope() {
        assertEquals(
                4.0,
                PolylineTraversal.remapMaterialDistance(6.0, 10.0, -2.0, true),
                EPSILON
        );
        assertEquals(
                9.0,
                PolylineTraversal.remapMaterialDistance(6.0, 10.0, 3.0, true),
                EPSILON
        );
        assertEquals(
                6.0,
                PolylineTraversal.remapMaterialDistance(6.0, 10.0, -2.0, false),
                EPSILON
        );
        assertEquals(
                6.0,
                PolylineTraversal.remapMaterialDistance(6.0, 10.0, 3.0, false),
                EPSILON
        );
    }

    @Test
    void clampsMaterialDistanceToTheAdjustedPath() {
        assertEquals(
                0.0,
                PolylineTraversal.remapMaterialDistance(0.5, 10.0, -2.0, true),
                EPSILON
        );
        assertEquals(
                8.0,
                PolylineTraversal.remapMaterialDistance(7.0, 8.0, 5.0, true),
                EPSILON
        );
        assertEquals(
                8.0,
                PolylineTraversal.remapMaterialDistance(9.0, 8.0, 0.0, false),
                EPSILON
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PolylineTraversal.remapMaterialDistance(1.0, -1.0, 0.0, false)
        );
    }
}
