package io.github.bladeswillfall.spelunkingrope.rope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(1, RopeCoilItem.DropScan.recoveredCoilsForVerticalBlockDrop(64, 64));
        assertEquals(1, RopeCoilItem.DropScan.recoveredCoilsForVerticalBlockDrop(64, 32));
        assertEquals(2, RopeCoilItem.DropScan.recoveredCoilsForVerticalBlockDrop(64, 31));
        assertEquals(2, RopeCoilItem.DropScan.recoveredCoilsForVerticalBlockDrop(64, 0));
        assertEquals(3, RopeCoilItem.DropScan.recoveredCoilsForVerticalBlockDrop(64, -1));
    }

    @Test
    void allocatesVisibleSlackWithinOneCoil() {
        assertEquals(1.5, RopeCoilItem.RouteLength.allocatedLengthForDistance(1.0));
        assertEquals(21.0, RopeCoilItem.RouteLength.allocatedLengthForDistance(20.0));
        assertTrue(RopeCoilItem.RouteLength.allocatedLengthForDistance(30.47) < 32.0);
        assertEquals(-1.0, RopeCoilItem.RouteLength.allocatedLengthForDistance(30.48));
        assertEquals(-1.0, RopeCoilItem.RouteLength.allocatedLengthForDistance(0.0));
    }
}
