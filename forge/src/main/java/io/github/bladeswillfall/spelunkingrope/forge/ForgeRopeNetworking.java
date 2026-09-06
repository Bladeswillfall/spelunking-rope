package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.ClientFixedRopeState;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSavedData;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import io.github.bladeswillfall.spelunkingrope.rope.RappelClientState;
import io.github.bladeswillfall.spelunkingrope.rope.RappelPackets;
import io.github.bladeswillfall.spelunkingrope.rope.RappelServerController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class ForgeRopeNetworking {
    private static final String PROTOCOL_VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SpelunkingRope.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ForgeRopeNetworking() {
    }

    public static void init() {
        CHANNEL.registerMessage(
                0,
                FixedRopeSnapshot.class,
                FixedRopeSnapshotCodec::encode,
                FixedRopeSnapshotCodec::decode,
                ForgeRopeNetworking::handleSnapshot,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                1,
                RappelPackets.State.class,
                RappelPackets::encodeState,
                RappelPackets::decodeState,
                ForgeRopeNetworking::handleRappelState,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                2,
                RappelPackets.Input.class,
                RappelPackets::encodeInput,
                RappelPackets::decodeInput,
                ForgeRopeNetworking::handleRappelInput,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        MinecraftForge.EVENT_BUS.addListener(ForgeRopeNetworking::onPlayerLoggedIn);
    }

    public static void broadcastSnapshot(ServerLevel level) {
        FixedRopeSnapshot snapshot = snapshot(level);
        for (ServerPlayer player : level.players()) {
            sendSnapshot(player, snapshot);
        }
    }

    public static void sendRappelState(ServerPlayer player, RappelPackets.State state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
    }

    public static void sendRappelInput(RappelPackets.Input input) {
        CHANNEL.sendToServer(input);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendSnapshot(player, snapshot(player.serverLevel()));
        }
    }

    private static void sendSnapshot(ServerPlayer player, FixedRopeSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }

    private static FixedRopeSnapshot snapshot(ServerLevel level) {
        return FixedRopeSavedData.get(level).snapshot(level.dimension().location());
    }

    private static void handleSnapshot(
            FixedRopeSnapshot snapshot,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientFixedRopeState.INSTANCE.apply(snapshot));
        context.setPacketHandled(true);
    }

    private static void handleRappelState(
            RappelPackets.State state,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> RappelClientState.INSTANCE.apply(state));
        context.setPacketHandled(true);
    }

    private static void handleRappelInput(
            RappelPackets.Input input,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> RappelServerController.handleInput(
                    player,
                    input,
                    ForgeRopeNetworking::sendRappelState
            ));
        }
        context.setPacketHandled(true);
    }
}
