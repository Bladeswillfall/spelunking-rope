package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.GuideClipBlock;
import io.github.bladeswillfall.spelunkingrope.rope.GuideCordItem;
import io.github.bladeswillfall.spelunkingrope.rope.PitonBlock;
import io.github.bladeswillfall.spelunkingrope.rope.PulleyBlock;
import io.github.bladeswillfall.spelunkingrope.rope.RopeCoilItem;
import io.github.bladeswillfall.spelunkingrope.rope.WinchBlock;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class SpelunkingRopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SpelunkingRope.init();

        ResourceLocation pitonId = new ResourceLocation(SpelunkingRope.MOD_ID, "piton");
        PitonBlock piton = Registry.register(BuiltInRegistries.BLOCK, pitonId, new PitonBlock());
        Registry.register(BuiltInRegistries.ITEM, pitonId, new BlockItem(piton, new Item.Properties()));

        ResourceLocation guideClipId = new ResourceLocation(SpelunkingRope.MOD_ID, "guide_clip");
        GuideClipBlock guideClip = Registry.register(BuiltInRegistries.BLOCK, guideClipId, new GuideClipBlock());
        Registry.register(BuiltInRegistries.ITEM, guideClipId, new BlockItem(guideClip, new Item.Properties()));

        ResourceLocation pulleyId = new ResourceLocation(SpelunkingRope.MOD_ID, "pulley");
        PulleyBlock pulley = Registry.register(BuiltInRegistries.BLOCK, pulleyId, new PulleyBlock());
        Registry.register(BuiltInRegistries.ITEM, pulleyId, new BlockItem(pulley, new Item.Properties()));

        ResourceLocation winchId = new ResourceLocation(SpelunkingRope.MOD_ID, "winch");
        WinchBlock winch = Registry.register(
                BuiltInRegistries.BLOCK,
                winchId,
                new WinchBlock(FabricRopeNetworking::broadcastSnapshot)
        );
        Registry.register(BuiltInRegistries.ITEM, winchId, new BlockItem(winch, new Item.Properties()));

        Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation(SpelunkingRope.MOD_ID, "rope_coil"),
                new RopeCoilItem(new Item.Properties().stacksTo(16), FabricRopeNetworking::broadcastSnapshot)
        );
        Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation(SpelunkingRope.MOD_ID, "guide_cord"),
                new GuideCordItem(new Item.Properties().stacksTo(16), FabricRopeNetworking::broadcastSnapshot)
        );

        FabricRopeNetworking.init();
    }
}
