package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.rope.DebugRopeRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class FabricRopeDebugRendering {
    private FabricRopeDebugRendering() {
    }

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            MultiBufferSource consumers = context.consumers();
            if (consumers == null) {
                return;
            }
            DebugRopeRenderer.render(
                    context.matrixStack(),
                    consumers.getBuffer(RenderType.lines()),
                    context.camera()
            );
        });
    }
}
