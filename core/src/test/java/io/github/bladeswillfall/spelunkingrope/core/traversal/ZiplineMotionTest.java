package io.github.bladeswillfall.spelunkingrope.core.traversal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZiplineMotionTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void gravityAcceleratesDownhillAndInputCanBrakeOrAssist() {
        double downhill = ZiplineMotion.integrateSpeed(0.0, -0.5, (byte) 0);
        double uphill = ZiplineMotion.integrateSpeed(0.0, 0.5, (byte) 0);

        assertTrue(downhill > 0.0);
        assertTrue(uphill < 0.0);
        assertTrue(ZiplineMotion.integrateSpeed(downhill, -0.5, (byte) 1) > downhill);
        assertTrue(ZiplineMotion.integrateSpeed(downhill, -0.5, (byte) -1) < downhill);
        assertTrue(ZiplineMotion.integrateSpeed(0.2, -1.0, (byte) -1) < 0.2);
    }

    @Test
    void speedAndDistanceRemainBoundedAtEndpoints() {
        assertEquals(ZiplineMotion.MAX_SPEED, ZiplineMotion.clampSpeed(99.0), EPSILON);
        assertEquals(-ZiplineMotion.MAX_SPEED, ZiplineMotion.clampSpeed(-99.0), EPSILON);
        assertEquals(10.0, ZiplineMotion.clampDistance(9.8, 10.0, 0.5), EPSILON);
        assertEquals(0.0, ZiplineMotion.stopAtEndpoint(10.0, 10.0, 0.5), EPSILON);
        assertEquals(0.0, ZiplineMotion.clampDistance(0.2, 10.0, -0.5), EPSILON);
        assertEquals(0.0, ZiplineMotion.stopAtEndpoint(0.0, 10.0, -0.5), EPSILON);
    }
}
