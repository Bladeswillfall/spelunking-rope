package io.github.bladeswillfall.spelunkingrope.core.traversal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RappelConstraintTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void constrainsOnlyOutwardMotionAndPreservesTangentialMomentum() {
        double[] output = new double[RappelConstraint.OUTPUT_STRIDE];

        assertFalse(RappelConstraint.constrain(
                0.0, 0.0, 0.0,
                1.0, -2.0, 0.0,
                0.25, -0.5, 0.0,
                5.0,
                output, 0
        ));
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(-2.0, output[1], EPSILON);
        assertEquals(0.25, output[3], EPSILON);
        assertEquals(-0.5, output[4], EPSILON);

        assertTrue(RappelConstraint.constrain(
                0.0, 0.0, 0.0,
                4.8, -3.6, 0.0,
                2.0, 1.0, 0.0,
                5.0,
                output, 0
        ));
        assertEquals(4.0, output[0], EPSILON);
        assertEquals(-3.0, output[1], EPSILON);
        assertEquals(0.0, output[2], EPSILON);
        assertEquals(1.2, output[3], EPSILON);
        assertEquals(1.6, output[4], EPSILON);
        assertEquals(0.0, output[5], EPSILON);
        assertEquals(0.0, output[3] * 0.8 + output[4] * -0.6, EPSILON);

        assertTrue(RappelConstraint.constrain(
                0.0, 0.0, 0.0,
                4.8, -3.6, 0.0,
                -1.0, 0.0, 0.0,
                5.0,
                output, 0
        ));
        assertEquals(-1.0, output[3], EPSILON);
        assertEquals(0.0, output[4], EPSILON);
    }
}
