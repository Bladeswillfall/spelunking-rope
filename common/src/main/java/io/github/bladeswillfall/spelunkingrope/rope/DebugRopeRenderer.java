package io.github.bladeswillfall.spelunkingrope.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bladeswillfall.spelunkingrope.core.runtime.DenseRopeRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class DebugRopeRenderer {
    private static final int RED = 214;
    private static final int GREEN = 166;
    private static final int BLUE = 77;
    private static final int ALPHA = 255;

    private DebugRopeRenderer() {
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer, Camera camera) {
        ClientLevel level = Minecraft.getInstance().level;
        ClientFixedRopeState clientState = ClientFixedRopeState.INSTANCE;
        if (level == null || !level.dimension().location().equals(clientState.dimension())) {
            return;
        }

        DenseRopeRuntime runtime = clientState.runtime();
        int spanCount = runtime.size();
        if (spanCount == 0) {
            return;
        }

        double[] geometry = runtime.geometryBuffer();
        int coordinatesPerSpan = runtime.coordinatesPerSpan();
        int segments = runtime.segments();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (int slot = 0; slot < spanCount; slot++) {
            int spanOffset = slot * coordinatesPerSpan;
            for (int segment = 0; segment < segments; segment++) {
                int start = spanOffset + segment * 3;
                int end = start + 3;

                float startX = (float) (geometry[start] - cameraPosition.x);
                float startY = (float) (geometry[start + 1] - cameraPosition.y);
                float startZ = (float) (geometry[start + 2] - cameraPosition.z);
                float endX = (float) (geometry[end] - cameraPosition.x);
                float endY = (float) (geometry[end + 1] - cameraPosition.y);
                float endZ = (float) (geometry[end + 2] - cameraPosition.z);
                float normalX = endX - startX;
                float normalY = endY - startY;
                float normalZ = endZ - startZ;

                consumer.vertex(matrix, startX, startY, startZ)
                        .color(RED, GREEN, BLUE, ALPHA)
                        .normal(normalMatrix, normalX, normalY, normalZ)
                        .endVertex();
                consumer.vertex(matrix, endX, endY, endZ)
                        .color(RED, GREEN, BLUE, ALPHA)
                        .normal(normalMatrix, normalX, normalY, normalZ)
                        .endVertex();
            }
        }
    }
}
