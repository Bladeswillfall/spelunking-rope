package io.github.bladeswillfall.spelunkingrope.rope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RopeCoilItemTest {
    @Test
    void selectsFloorTopOrPrototypeLimit() {
        assertEquals(24, RopeCoilItem.DropScan.findDropEndBlockY(24, -64, y -> y == 23));
        assertEquals(17, RopeCoilItem.DropScan.findDropEndBlockY(24, -64, y -> y == 16));
        assertEquals(-8, RopeCoilItem.DropScan.findDropEndBlockY(24, -64, y -> false));
        assertEquals(-63, RopeCoilItem.DropScan.findDropEndBlockY(-60, -64, y -> y == -64));
        assertEquals(-64, RopeCoilItem.DropScan.findDropEndBlockY(-60, -64, y -> false));
    }
}
