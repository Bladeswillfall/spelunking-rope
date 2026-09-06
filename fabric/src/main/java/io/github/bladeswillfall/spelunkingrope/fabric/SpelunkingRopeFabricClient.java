package io.github.bladeswillfall.spelunkingrope.fabric;

import net.fabricmc.api.ClientModInitializer;

public final class SpelunkingRopeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricRopeClientNetworking.init();
        FabricRopeDebugRendering.init();
    }
}
