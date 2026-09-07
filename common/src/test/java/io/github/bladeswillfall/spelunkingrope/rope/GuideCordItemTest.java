package io.github.bladeswillfall.spelunkingrope.rope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideCordItemTest {
    private static final int[] PALETTE = {
            0x101010,
            0xCC2222,
            0x22CC22,
            0x2222CC
    };

    @Test
    void exactPaletteColoursKeepTheirIndex() {
        for (int index = 0; index < PALETTE.length; index++) {
            assertEquals(index, GuideCordColors.closestPaletteIndex(PALETTE[index], PALETTE));
        }
    }

    @Test
    void mixedRgbChoosesNearestPaletteEntry() {
        assertEquals(1, GuideCordColors.closestPaletteIndex(0xF01818, PALETTE));
        assertEquals(2, GuideCordColors.closestPaletteIndex(0x18F018, PALETTE));
        assertEquals(3, GuideCordColors.closestPaletteIndex(0x1818F0, PALETTE));
    }
}
