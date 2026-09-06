package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.RappelClientController;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpelunkingRope.MOD_ID, value = Dist.CLIENT)
public final class ForgeRappelClient {
    private ForgeRappelClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RappelClientController.tick(Minecraft.getInstance(), ForgeRopeNetworking::sendRappelInput);
        }
    }
}
