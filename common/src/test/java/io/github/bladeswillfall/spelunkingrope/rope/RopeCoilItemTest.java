package io.github.bladeswillfall.spelunkingrope.rope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RopeCoilItemTest {
    @Test
    void selectsFloorTopOrOneCoilLimit() {
        assertEquals(24, RopeCoilItem.DropScan.findDropEndBlockY(24, -64, y -> y == 23));
        assertEquals(17, RopeCoilItem.DropScan.findDropEndBlockY(24, -64, y -> y == 16));
        assertEquals(-8, RopeCoilItem.DropScan.findDropEndBlockY(24, -64, y -> false));
        assertEquals(-40, RopeCoilItem.DropScan.findDropEndBlockY(-8, -64, y -> false));
        assertEquals(-19, RopeCoilItem.DropScan.findDropEndBlockY(-8, -64, y -> y == -20));
        assertEquals(-63, RopeCoilItem.DropScan.findDropEndBlockY(-60, -64, y -> y == -64));
        assertEquals(-64, RopeCoilItem.DropScan.findDropEndBlockY(-60, -64, y -> false));
    }

    @Test
    void countsRecoveredCoilsFromVerticalBlockDrop() {
        BlockAttachment start = BlockAttachment.atWorld(0.5, 64.5, 0.5);

        assertEquals(1, RopeCoilItem.coilsForVerticalDrop(start, BlockAttachment.atWorld(0.5, 64.05, 0.5)));
        assertEquals(1, RopeCoilItem.coilsForVerticalDrop(start, BlockAttachment.atWorld(0.5, 32.05, 0.5)));
        assertEquals(2, RopeCoilItem.coilsForVerticalDrop(start, BlockAttachment.atWorld(0.5, 31.05, 0.5)));
        assertEquals(2, RopeCoilItem.coilsForVerticalDrop(start, BlockAttachment.atWorld(0.5, 0.05, 0.5)));
        assertEquals(3, RopeCoilItem.coilsForVerticalDrop(start, BlockAttachment.atWorld(0.5, -0.95, 0.5)));
    }
}
