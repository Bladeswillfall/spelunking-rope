package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideCordItemTest {
    @Test
    void resolvesAllVanillaDyeColours() {
        for (DyeColor color : DyeColor.values()) {
            assertEquals(
                    (byte) color.getId(),
                    GuideCordItem.closestDyeId(GuideCordItem.dyeRgb((byte) color.getId()))
            );
        }
    }

    @Test
    void mixedRgbChoosesNearestMinecraftDye() {
        assertEquals((byte) DyeColor.RED.getId(), GuideCordItem.closestDyeId(0xFF1010));
    }
}
