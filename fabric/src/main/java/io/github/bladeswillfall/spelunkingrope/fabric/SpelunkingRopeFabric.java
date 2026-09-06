package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.RopeCoilItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class SpelunkingRopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SpelunkingRope.init();
        FabricRopeNetworking.init();
        Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation(SpelunkingRope.MOD_ID, "rope_coil"),
                new RopeCoilItem(new Item.Properties().stacksTo(16), FabricRopeNetworking::broadcastSnapshot)
        );
    }
}
