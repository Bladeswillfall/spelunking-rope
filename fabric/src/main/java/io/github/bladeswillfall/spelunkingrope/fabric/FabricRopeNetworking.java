package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSavedData;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FabricRopeNetworking {
    private FabricRopeNetworking() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSnapshot(handler.player));
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

    private static FixedRopeSnapshot snapshot(ServerLevel level) {
        return FixedRopeSavedData.get(level).snapshot(level.dimension().location());
    }
}
