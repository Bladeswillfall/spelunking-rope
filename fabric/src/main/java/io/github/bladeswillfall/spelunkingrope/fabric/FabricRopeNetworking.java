package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSavedData;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import io.github.bladeswillfall.spelunkingrope.rope.RappelPackets;
import io.github.bladeswillfall.spelunkingrope.rope.RappelServerController;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;

public final class FabricRopeNetworking {
    private FabricRopeNetworking() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSnapshot(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(
                RappelPackets.INPUT_CHANNEL,
                (server, player, handler, buffer, responseSender) -> {
                    RappelPackets.Input input = RappelPackets.decodeInput(buffer);
                    server.execute(() -> RappelServerController.handleInput(
                            player,
                            input,
                            FabricRopeNetworking::sendRappelState
                    ));
                }
        );
        ServerTickEvents.END_SERVER_TICK.register(
                server -> RappelServerController.tick(server, FabricRopeNetworking::sendRappelState)
        );
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.PASS;
            }
            BlockPos hookPos = hitResult.getBlockPos();
            var hookState = level.getBlockState(hookPos);
            if (!hookState.is(Blocks.TRIPWIRE_HOOK)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            ServerPlayer serverPlayer = (ServerPlayer) player;
            var facing = hookState.getValue(TripWireHookBlock.FACING);
            if (player.isShiftKeyDown()) {
                if (!RappelServerController.retrieveAtHook(serverPlayer, hookPos, facing)) {
                    return InteractionResult.PASS;
                }
                broadcastSnapshot(serverPlayer.serverLevel());
                return InteractionResult.SUCCESS;
            }

            boolean attached = RappelServerController.attach(
                    serverPlayer,
                    hookPos,
                    facing,
                    FabricRopeNetworking::sendRappelState
            );
            return attached ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    public static void broadcastSnapshot(ServerLevel level) {
        FixedRopeSnapshot snapshot = snapshot(level);
        for (ServerPlayer player : level.players()) {
            sendSnapshot(player, snapshot);
        }
    }

    private static void sendSnapshot(ServerPlayer player) {
        sendSnapshot(player, snapshot(player.serverLevel()));
    }

    private static void sendSnapshot(ServerPlayer player, FixedRopeSnapshot snapshot) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        FixedRopeSnapshotCodec.encode(snapshot, buffer);
        ServerPlayNetworking.send(player, FixedRopeSnapshotCodec.CHANNEL_ID, buffer);
    }

    private static void sendRappelState(ServerPlayer player, RappelPackets.State state) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        RappelPackets.encodeState(state, buffer);
        ServerPlayNetworking.send(player, RappelPackets.STATE_CHANNEL, buffer);
    }

    private static FixedRopeSnapshot snapshot(ServerLevel level) {
        return FixedRopeSavedData.get(level).snapshot(level.dimension().location());
    }
}
