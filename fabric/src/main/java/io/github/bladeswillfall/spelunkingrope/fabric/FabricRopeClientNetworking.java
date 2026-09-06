package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.rope.ClientFixedRopeState;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import io.github.bladeswillfall.spelunkingrope.rope.RappelClientController;
import io.github.bladeswillfall.spelunkingrope.rope.RappelClientState;
import io.github.bladeswillfall.spelunkingrope.rope.RappelPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public final class FabricRopeClientNetworking {
    private FabricRopeClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(
                FixedRopeSnapshotCodec.CHANNEL_ID,
                (client, handler, buffer, responseSender) -> {
                    FixedRopeSnapshot snapshot = FixedRopeSnapshotCodec.decode(buffer);
                    client.execute(() -> ClientFixedRopeState.INSTANCE.apply(snapshot));
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                RappelPackets.STATE_CHANNEL,
                (client, handler, buffer, responseSender) -> {
                    RappelPackets.State state = RappelPackets.decodeState(buffer);
                    client.execute(() -> RappelClientState.INSTANCE.apply(state));
                }
        );
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> RappelClientController.tick(client, FabricRopeClientNetworking::sendRappelInput)
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientFixedRopeState.INSTANCE.clear();
            RappelClientState.INSTANCE.clear();
        });
    }

    private static void sendRappelInput(RappelPackets.Input input) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        RappelPackets.encodeInput(input, buffer);
        ClientPlayNetworking.send(RappelPackets.INPUT_CHANNEL, buffer);
    }
}
