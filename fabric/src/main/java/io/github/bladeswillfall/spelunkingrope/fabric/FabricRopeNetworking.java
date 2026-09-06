package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSavedData;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class FabricRopeNetworking {
    private FabricRopeNetworking() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSnapshot(handler.player));
    }

    private static void sendSnapshot(ServerPlayer player) {
        FixedRopeSnapshot snapshot = FixedRopeSavedData.get(player.serverLevel()).snapshot(
                player.serverLevel().dimension().location()
        );
        FriendlyByteBuf buffer = PacketByteBufs.create();
        FixedRopeSnapshotCodec.encode(snapshot, buffer);
        ServerPlayNetworking.send(player, FixedRopeSnapshotCodec.CHANNEL_ID, buffer);
    }
}
