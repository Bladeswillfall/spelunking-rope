package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;
import java.util.function.Consumer;

public final class WinchBlock extends PitonBlock {
    private static final double STEP = 1.0;

    private final Consumer<ServerLevel> snapshotBroadcaster;

    public WinchBlock(Consumer<ServerLevel> snapshotBroadcaster) {
        this.snapshotBroadcaster = Objects.requireNonNull(snapshotBroadcaster, "snapshotBroadcaster");
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        double deployedDelta = player.isShiftKeyDown() ? STEP : -STEP;
        FixedRopeSavedData.WinchAdjustment result = FixedRopeSavedData.get(serverLevel).adjustWinch(
                RopeAnchor.routeAttachment(pos, state),
                deployedDelta
        );

        if (result == FixedRopeSavedData.WinchAdjustment.CHANGED) {
            snapshotBroadcaster.accept(serverLevel);
            message(player, player.isShiftKeyDown()
                    ? "message.spelunking_rope.winch_paid_out"
                    : "message.spelunking_rope.winch_reeled_in");
        } else if (result == FixedRopeSavedData.WinchAdjustment.NO_ROPE) {
            message(player, "message.spelunking_rope.winch_no_rope");
        } else {
            message(player, "message.spelunking_rope.winch_limit");
        }
        return InteractionResult.CONSUME;
    }

    private static void message(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }
}
