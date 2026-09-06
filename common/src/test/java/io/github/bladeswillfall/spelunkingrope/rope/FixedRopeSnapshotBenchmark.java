package io.github.bladeswillfall.spelunkingrope.rope;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FixedRopeSnapshotBenchmark {
    private static final int SPAN_COUNT = 10_000;
    private static final int VANILLA_CUSTOM_PAYLOAD_LIMIT = 1_048_576;

    private FixedRopeSnapshotBenchmark() {
    }

    public static void main(String[] args) {
        List<FixedRopeSnapshot.Span> spans = new ArrayList<>(SPAN_COUNT);
        for (int i = 0; i < SPAN_COUNT; i++) {
            spans.add(new FixedRopeSnapshot.Span(
                    new UUID(20L, i + 1L),
                    new BlockAttachment(new BlockPos(i, 64, 0), 0.25, 0.5, 0.5),
                    new BlockAttachment(new BlockPos(i + 8, 64, 0), 0.75, 0.5, 0.5),
                    10.0
            ));
        }
        FixedRopeSnapshot snapshot = new FixedRopeSnapshot(
                new ResourceLocation("minecraft", "overworld"),
                spans
        );

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            long encodeStart = System.nanoTime();
            FixedRopeSnapshotCodec.encode(snapshot, buffer);
            long encodeNanos = System.nanoTime() - encodeStart;
            int bytes = buffer.readableBytes();
            if (bytes >= VANILLA_CUSTOM_PAYLOAD_LIMIT) {
                throw new AssertionError("10k fixed-rope snapshot exceeds vanilla custom payload limit: " + bytes);
            }

            long decodeStart = System.nanoTime();
            FixedRopeSnapshot decoded = FixedRopeSnapshotCodec.decode(buffer);
            long decodeNanos = System.nanoTime() - decodeStart;
            if (decoded.spans().size() != SPAN_COUNT) {
                throw new AssertionError("decoded span count: " + decoded.spans().size());
            }

            long hydrateStart = System.nanoTime();
            int recomputed = ClientFixedRopeState.INSTANCE.apply(decoded);
            long hydrateNanos = System.nanoTime() - hydrateStart;
            if (recomputed != SPAN_COUNT || ClientFixedRopeState.INSTANCE.runtime().dirtyCount() != 0) {
                throw new AssertionError("client runtime did not hydrate exactly once per span");
            }

            System.out.printf(
                    "10k fixed-rope snapshot: %,d bytes, encode %.3f ms, decode %.3f ms, hydrate+geometry %.3f ms%n",
                    bytes,
                    encodeNanos / 1_000_000.0,
                    decodeNanos / 1_000_000.0,
                    hydrateNanos / 1_000_000.0
            );
        } finally {
            ClientFixedRopeState.INSTANCE.clear();
            buffer.release();
        }
    }
}
