package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.ClientFixedRopeState;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSavedData;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshot;
import io.github.bladeswillfall.spelunkingrope.rope.FixedRopeSnapshotCodec;
import net.minecraft.resources.ResourceLocation;
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
    private static final String PROTOCOL_VERSION = "1";
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
        MinecraftForge.EVENT_BUS.addListener(ForgeRopeNetworking::onPlayerLoggedIn);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FixedRopeSnapshot snapshot = FixedRopeSavedData.get(player.serverLevel()).snapshot(
                player.serverLevel().dimension().location()
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }

    private static void handleSnapshot(
            FixedRopeSnapshot snapshot,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientFixedRopeState.INSTANCE.apply(snapshot));
        context.setPacketHandled(true);
    }
}
