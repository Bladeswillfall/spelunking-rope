package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import net.fabricmc.api.ModInitializer;

public final class SpelunkingRopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SpelunkingRope.init();
    }
}
