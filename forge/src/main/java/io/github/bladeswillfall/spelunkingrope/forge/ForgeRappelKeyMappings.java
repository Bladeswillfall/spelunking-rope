package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.RappelClientController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpelunkingRope.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeRappelKeyMappings {
    private ForgeRappelKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(RappelClientController.DETACH_KEY);
    }
}
