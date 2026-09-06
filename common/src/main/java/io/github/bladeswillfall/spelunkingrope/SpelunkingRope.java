package io.github.bladeswillfall.spelunkingrope;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class SpelunkingRope {
    public static final String MOD_ID = "spelunking_rope";
    public static final String MOD_NAME = "Spelunking Rope";

    private static final Logger LOGGER = LogUtils.getLogger();

    private SpelunkingRope() {
    }

    public static void init() {
        LOGGER.info("{} initialized", MOD_NAME);
    }
}
