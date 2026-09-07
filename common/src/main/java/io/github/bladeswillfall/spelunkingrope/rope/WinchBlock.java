package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class WinchBlock extends PitonBlock {
    private static final double STEP = 1.0;
    private static final VoxelShape NORTH_SHAPE = Block.box(3.0, 3.0, 7.0, 15.0, 13.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(1.0, 3.0, 0.0, 13.0, 13.0, 9.0);
    private static final VoxelShape WEST_SHAPE = Block.box(7.0, 3.0, 1.0, 16.0, 13.0, 13.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 3.0, 3.0, 9.0, 13.0, 15.0);

    private final Consumer<ServerLevel> snapshotBroadcaster;
    private final BiConsumer<ServerPlayer, RappelPackets.State> stateSender;

    public WinchBlock(
            Consumer<ServerLevel> snapshotBroadcaster,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        this.snapshotBroadcaster = Objects.requireNonNull(snapshotBroadcaster, "snapshotBroadcaster");
        this.stateSender = Objects.requireNonNull(stateSender, "stateSender");
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
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
        FixedRopeSavedData.WinchAdjustment result = RappelServerController.adjustWinch(
                serverLevel,
                RopeAnchor.routeAttachment(pos, state),
                deployedDelta,
                stateSender
        );

        if (result == FixedRopeSavedData.WinchAdjustment.CHANGED) {
            snapshotBroadcaster.accept(serverLevel);
            message(player, player.isShiftKeyDown()
                    ? "message.spelunking_rope.winch_paid_out"
                    : "message.spelunking_rope.winch_reeled_in");
        } else if (result == FixedRopeSavedData.WinchAdjustment.NO_ROPE) {
            message(player, "message.spelunking_rope.winch_no_rope");
        } else if (result == FixedRopeSavedData.WinchAdjustment.BLOCKED) {
            message(player, "message.spelunking_rope.winch_blocked");
        } else {
            message(player, "message.spelunking_rope.winch_limit");
        }
        return InteractionResult.CONSUME;
    }

    private static void message(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }
}
