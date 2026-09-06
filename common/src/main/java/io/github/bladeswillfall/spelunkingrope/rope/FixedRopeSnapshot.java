package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FixedRopeSnapshot(ResourceLocation dimension, List<Span> spans) {
    public static final int MAX_SPANS = 10_000;

    public FixedRopeSnapshot {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(spans, "spans");
        if (spans.size() > MAX_SPANS) {
            throw new IllegalArgumentException("Fixed rope snapshot exceeds " + MAX_SPANS + " spans");
        }
        spans = List.copyOf(spans);
    }

    public record Span(UUID id, BlockAttachment start, BlockAttachment end, double allocatedLength) {
        public Span {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            if (!Double.isFinite(allocatedLength) || allocatedLength <= 0.0) {
                throw new IllegalArgumentException("allocatedLength must be finite and positive");
            }
            requireFinite(start);
            requireFinite(end);
        }

        private static void requireFinite(BlockAttachment attachment) {
            if (!Double.isFinite(attachment.localX())
                    || !Double.isFinite(attachment.localY())
                    || !Double.isFinite(attachment.localZ())) {
                throw new IllegalArgumentException("attachment offsets must be finite");
            }
        }
    }
}
