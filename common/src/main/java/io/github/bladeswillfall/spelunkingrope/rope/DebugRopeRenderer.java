package io.github.bladeswillfall.spelunkingrope.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bladeswillfall.spelunkingrope.core.runtime.DenseRopeRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class DebugRopeRenderer {
    private static final int RED = 214;
    private static final int GREEN = 166;
    private static final int BLUE = 77;
    private static final int GUIDE_RED = 202;
    private static final int GUIDE_GREEN = 202;
    private static final int GUIDE_BLUE = 190;
    private static final int METAL_RED = 72;
    private static final int METAL_GREEN = 77;
    private static final int METAL_BLUE = 83;
    private static final int ALPHA = 255;
    private static final double TROLLEY_HALF_WIDTH = 0.14;
    private static final double HARNESS_HEIGHT = 0.95;
    private static final double[] TROLLEY_SAMPLE = new double[6];

    private DebugRopeRenderer() {
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ClientFixedRopeState clientState = ClientFixedRopeState.INSTANCE;
        if (level == null || !level.dimension().location().equals(clientState.dimension())) {
            return;
        }

        DenseRopeRuntime runtime = clientState.runtime();
        int spanCount = runtime.size();
        double[] geometry = runtime.geometryBuffer();
        int coordinatesPerSpan = runtime.coordinatesPerSpan();
        int segments = runtime.segments();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        RappelClientState rappel = RappelClientState.INSTANCE;
        LocalPlayer player = minecraft.player;
        if (rappel.rappelling() && player != null) {
            double playerX = Mth.lerp(partialTick, player.xOld, player.getX());
            double playerY = Mth.lerp(partialTick, player.yOld, player.getY());
            double playerZ = Mth.lerp(partialTick, player.zOld, player.getZ());
            drawLine(
                    consumer, matrix, normalMatrix, cameraPosition,
                    rappel.anchorX(), rappel.anchorY(), rappel.anchorZ(),
                    playerX, playerY, playerZ,
                    RED, GREEN, BLUE
            );
            // ponytail: the prototype has no simulated free-tail geometry. A second rigid ray from the
            // player looked like render ghosting during swings; restore the tail once it has segmented geometry.
        } else if (rappel.traversing() && player != null
                && clientState.sampleSpan(rappel.spanId(), rappel.currentLength(), TROLLEY_SAMPLE, 0)) {
            double sideX = -TROLLEY_SAMPLE[5];
            double sideZ = TROLLEY_SAMPLE[3];
            double sideLength = Math.hypot(sideX, sideZ);
            if (sideLength > 1.0e-6) {
                sideX = sideX / sideLength * TROLLEY_HALF_WIDTH;
                sideZ = sideZ / sideLength * TROLLEY_HALF_WIDTH;
            } else {
                sideX = TROLLEY_HALF_WIDTH;
                sideZ = 0.0;
            }
            double playerX = Mth.lerp(partialTick, player.xOld, player.getX());
            double playerY = Mth.lerp(partialTick, player.yOld, player.getY());
            double playerZ = Mth.lerp(partialTick, player.zOld, player.getZ());

            drawLine(
                    consumer, matrix, normalMatrix, cameraPosition,
                    TROLLEY_SAMPLE[0] - sideX, TROLLEY_SAMPLE[1], TROLLEY_SAMPLE[2] - sideZ,
                    TROLLEY_SAMPLE[0] + sideX, TROLLEY_SAMPLE[1], TROLLEY_SAMPLE[2] + sideZ,
                    METAL_RED, METAL_GREEN, METAL_BLUE
            );
            drawLine(
                    consumer, matrix, normalMatrix, cameraPosition,
                    TROLLEY_SAMPLE[0], TROLLEY_SAMPLE[1], TROLLEY_SAMPLE[2],
                    playerX, playerY + HARNESS_HEIGHT, playerZ,
                    METAL_RED, METAL_GREEN, METAL_BLUE
            );
        }

        for (int slot = 0; slot < spanCount; slot++) {
            if (rappel.rappelling() && runtime.spanIdAt(slot).equals(rappel.spanId())) {
                continue;
            }

            boolean guide = clientState.lineTypeAt(slot) == FixedRopeSnapshot.TYPE_GUIDE;
            int red = guide ? GUIDE_RED : RED;
            int green = guide ? GUIDE_GREEN : GREEN;
            int blue = guide ? GUIDE_BLUE : BLUE;
            int spanOffset = slot * coordinatesPerSpan;
            for (int segment = 0; segment < segments; segment++) {
                int start = spanOffset + segment * 3;
                int end = start + 3;
                drawLine(
                        consumer, matrix, normalMatrix, cameraPosition,
                        geometry[start], geometry[start + 1], geometry[start + 2],
                        geometry[end], geometry[end + 1], geometry[end + 2],
                        red, green, blue
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
            double z1,
            int red,
            int green,
            int blue
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
                .color(red, green, blue, ALPHA)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
        consumer.vertex(matrix, endX, endY, endZ)
                .color(red, green, blue, ALPHA)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }
}
