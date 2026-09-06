package io.github.bladeswillfall.spelunkingrope.fabric;

import io.github.bladeswillfall.spelunkingrope.rope.ClientFixedRopeState;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientFixedRopeState.INSTANCE.clear()
        );
    }
}
