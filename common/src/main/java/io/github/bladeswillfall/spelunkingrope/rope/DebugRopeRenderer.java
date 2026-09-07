package io.github.bladeswillfall.spelunkingrope.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bladeswillfall.spelunkingrope.core.runtime.DenseRopeRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
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

        RappelClientState rappel = RappelClientState.INSTANCE;
        LocalPlayer player = minecraft.player;
        int activeSlot = rappel.active() && player != null ? runtime.slotOf(rappel.spanId()) : -1;

        for (int slot = 0; slot < spanCount; slot++) {
            if (slot == activeSlot) {
                drawLine(
                        consumer, matrix, normalMatrix, cameraPosition,
                        rappel.anchorX(), rappel.anchorY(), rappel.anchorZ(),
                        player.getX(), player.getY(), player.getZ()
                );
                double tailLength = Math.max(0.0, rappel.maxLength() - rappel.currentLength());
                if (tailLength > 1.0e-4) {
                    drawLine(
                            consumer, matrix, normalMatrix, cameraPosition,
                            player.getX(), player.getY(), player.getZ(),
                            player.getX(), player.getY() - tailLength, player.getZ()
                    );
                }
                continue;
            }

            int spanOffset = slot * coordinatesPerSpan;
            for (int segment = 0; segment < segments; segment++) {
                int start = spanOffset + segment * 3;
                int end = start + 3;
                drawLine(
                        consumer, matrix, normalMatrix, cameraPosition,
                        geometry[start], geometry[start + 1], geometry[start + 2],
                        geometry[end], geometry[end + 1], geometry[end + 2]
                );
            }
        }
    }

    private static void drawLine(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            Vec3 cameraPosition,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1
    ) {
        float startX = (float) (x0 - cameraPosition.x);
        float startY = (float) (y0 - cameraPosition.y);
        float startZ = (float) (z0 - cameraPosition.z);
        float endX = (float) (x1 - cameraPosition.x);
        float endY = (float) (y1 - cameraPosition.y);
        float endZ = (float) (z1 - cameraPosition.z);
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
