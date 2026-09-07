package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideCordItemTest {
    @Test
    void resolvesUndyedAndAllVanillaDyeColours() {
        GuideCordItem item = new GuideCordItem(new Item.Properties(), ignored -> { });
        ItemStack stack = new ItemStack(item);

        assertEquals(FixedRopeSnapshot.NO_DYE, GuideCordItem.dyeId(stack));
        for (DyeColor color : DyeColor.values()) {
            item.setColor(stack, GuideCordItem.dyeRgb((byte) color.getId()));
            assertEquals((byte) color.getId(), GuideCordItem.dyeId(stack));
        }
    }

    @Test
    void mixedRgbChoosesNearestMinecraftDye() {
        GuideCordItem item = new GuideCordItem(new Item.Properties(), ignored -> { });
        ItemStack stack = new ItemStack(item);
        item.setColor(stack, 0xFF1010);

        assertEquals((byte) DyeColor.RED.getId(), GuideCordItem.dyeId(stack));
    }
}
