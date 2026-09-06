package io.github.bladeswillfall.spelunkingrope.rope;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FixedRopeSnapshotCodecTest {
    @AfterEach
    void clearClientState() {
        ClientFixedRopeState.INSTANCE.clear();
    }

    @Test
    void roundTripsAuthoritativeStateWithoutGeometry() {
        FixedRopeSnapshot snapshot = sampleSnapshot();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FixedRopeSnapshotCodec.encode(snapshot, buffer);
            FixedRopeSnapshot decoded = FixedRopeSnapshotCodec.decode(buffer);

            assertEquals(snapshot, decoded);
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void hydratesDenseRuntimeOnceAndLeavesItClean() {
        FixedRopeSnapshot snapshot = sampleSnapshot();

        int recomputed = ClientFixedRopeState.INSTANCE.apply(snapshot);

        assertEquals(2, recomputed);
        assertEquals(snapshot.dimension(), ClientFixedRopeState.INSTANCE.dimension());
        assertEquals(2, ClientFixedRopeState.INSTANCE.runtime().size());
        assertEquals(0, ClientFixedRopeState.INSTANCE.runtime().dirtyCount());
        assertEquals(0, ClientFixedRopeState.INSTANCE.runtime().recomputeDirty());

        UUID firstId = snapshot.spans().get(0).id();
        int slot = ClientFixedRopeState.INSTANCE.runtime().slotOf(firstId);
        int offset = ClientFixedRopeState.INSTANCE.runtime().coordinateOffset(slot);
        double[] geometry = ClientFixedRopeState.INSTANCE.runtime().geometryBuffer();
        assertEquals(10.25, geometry[offset], 0.0);
        assertEquals(18.75,
                geometry[offset + ClientFixedRopeState.INSTANCE.runtime().coordinatesPerSpan() - 3],
                0.0);
        assertSame(geometry, ClientFixedRopeState.INSTANCE.runtime().geometryBuffer());
    }

    private static FixedRopeSnapshot sampleSnapshot() {
        return new FixedRopeSnapshot(
                new ResourceLocation("minecraft", "overworld"),
                List.of(
                        new FixedRopeSnapshot.Span(
                                new UUID(10L, 1L),
                                new BlockAttachment(new BlockPos(10, 64, 5), 0.25, 0.5, 0.75),
                                new BlockAttachment(new BlockPos(18, 62, 5), 0.75, 0.25, 0.75),
                                10.0
                        ),
                        new FixedRopeSnapshot.Span(
                                new UUID(10L, 2L),
                                new BlockAttachment(new BlockPos(-4, 40, 12), 0.5, 0.75, 0.5),
                                new BlockAttachment(new BlockPos(-4, 30, 12), 0.5, 0.25, 0.5),
                                12.0
                        )
                )
        );
    }
}
