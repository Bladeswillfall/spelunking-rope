package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import net.minecraftforge.fml.common.Mod;

@Mod(SpelunkingRope.MOD_ID)
public final class SpelunkingRopeForge {
    public SpelunkingRopeForge() {
        SpelunkingRope.init();
    }
}
