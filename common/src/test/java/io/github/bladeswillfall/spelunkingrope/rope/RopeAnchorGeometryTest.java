package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RopeAnchorGeometryTest {
    private static final double EPSILON = 1.0e-12;
    private static final BlockPos POS = new BlockPos(10, 64, 20);

    @Test
    void pitonEyeRotatesWithWallFacing() {
        assertAttachment(RopeAnchor.attachment(POS, Direction.NORTH), 10.5, 64.5, 20.375);
        assertAttachment(RopeAnchor.attachment(POS, Direction.EAST), 10.625, 64.5, 20.5);
        assertAttachment(RopeAnchor.attachment(POS, Direction.SOUTH), 10.5, 64.5, 20.625);
        assertAttachment(RopeAnchor.attachment(POS, Direction.WEST), 10.375, 64.5, 20.5);
    }

    @Test
    void guideEyePulleyGrooveAndWinchDrumUseTheirModelContactPoints() {
        assertAttachment(RopeAnchor.guideAttachment(POS, Direction.NORTH), 10.5, 64.5, 20.5);

        assertAttachment(RopeAnchor.pulleyAttachment(POS, Direction.NORTH), 10.5, 64.1875, 20.5625);
        assertAttachment(RopeAnchor.pulleyAttachment(POS, Direction.EAST), 10.4375, 64.1875, 20.5);
        assertAttachment(RopeAnchor.pulleyAttachment(POS, Direction.SOUTH), 10.5, 64.1875, 20.4375);
        assertAttachment(RopeAnchor.pulleyAttachment(POS, Direction.WEST), 10.5625, 64.1875, 20.5);

        assertAttachment(RopeAnchor.winchAttachment(POS, Direction.NORTH), 10.5, 64.25, 20.625);
        assertAttachment(RopeAnchor.winchAttachment(POS, Direction.EAST), 10.375, 64.25, 20.5);
        assertAttachment(RopeAnchor.winchAttachment(POS, Direction.SOUTH), 10.5, 64.25, 20.375);
        assertAttachment(RopeAnchor.winchAttachment(POS, Direction.WEST), 10.625, 64.25, 20.5);
    }

    @Test
    void wallAttachmentsRejectVerticalFacing() {
        assertThrows(IllegalArgumentException.class, () -> RopeAnchor.attachment(POS, Direction.UP));
        assertThrows(IllegalArgumentException.class, () -> RopeAnchor.pulleyAttachment(POS, Direction.DOWN));
    }

    private static void assertAttachment(BlockAttachment attachment, double x, double y, double z) {
        assertEquals(x, attachment.worldX(), EPSILON);
        assertEquals(y, attachment.worldY(), EPSILON);
        assertEquals(z, attachment.worldZ(), EPSILON);
    }
}
