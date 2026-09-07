package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.traversal.RappelConstraint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public final class RappelClientController {
    private static final double WALL_PUSH_HORIZONTAL = 0.32;
    private static final double WALL_PUSH_VERTICAL = 0.16;
    private static final double[] CONSTRAINT_OUTPUT = new double[RappelConstraint.OUTPUT_STRIDE];

    private static byte lastVertical;
    private static boolean jumpWasDown;
    private static boolean wasActive;

    private RappelClientController() {
    }

    public static void tick(Minecraft client, Consumer<RappelPackets.Input> inputSender) {
        RappelClientState state = RappelClientState.INSTANCE;
        LocalPlayer player = client.player;
        if (player == null || !state.active()) {
            if (wasActive) {
                lastVertical = 0;
                jumpWasDown = false;
            }
            wasActive = false;
            return;
        }

        if (!wasActive) {
            lastVertical = 0;
            jumpWasDown = false;
            wasActive = true;
        }

        if (client.options.keyShift.isDown()) {
            inputSender.accept(new RappelPackets.Input((byte) 0, true, false));
            state.clear();
            wasActive = false;
            return;
        }

        byte vertical = (byte) ((client.options.keyUp.isDown() ? 1 : 0)
                - (client.options.keyDown.isDown() ? 1 : 0));
        if (vertical != lastVertical) {
            lastVertical = vertical;
            inputSender.accept(new RappelPackets.Input(vertical, false, false));
        }
        state.advanceLength(vertical);

        boolean jumpDown = client.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown && player.horizontalCollision) {
            applyWallPush(player);
            inputSender.accept(new RappelPackets.Input(vertical, false, true));
        }
        jumpWasDown = jumpDown;

        player.fallDistance = 0.0F;
        Vec3 velocity = player.getDeltaMovement();
        if (RappelConstraint.constrain(
                state.anchorX(),
                state.anchorY(),
                state.anchorZ(),
                player.getX(),
                player.getY(),
                player.getZ(),
                velocity.x,
                velocity.y,
                velocity.z,
                state.currentLength(),
                CONSTRAINT_OUTPUT,
                0
        )) {
            player.setPos(CONSTRAINT_OUTPUT[0], CONSTRAINT_OUTPUT[1], CONSTRAINT_OUTPUT[2]);
            player.setDeltaMovement(CONSTRAINT_OUTPUT[3], CONSTRAINT_OUTPUT[4], CONSTRAINT_OUTPUT[5]);
        }
    }

    private static void applyWallPush(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0e-6) {
            return;
        }
        player.addDeltaMovement(new Vec3(
                -look.x / horizontal * WALL_PUSH_HORIZONTAL,
                WALL_PUSH_VERTICAL,
                -look.z / horizontal * WALL_PUSH_HORIZONTAL
        ));
    }
}
