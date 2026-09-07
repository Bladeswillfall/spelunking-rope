package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FixedRopeSnapshot(ResourceLocation dimension, List<Span> spans) {
    public static final int MAX_SPANS = 10_000;
    public static final byte TYPE_STRUCTURAL = 0;
    public static final byte TYPE_GUIDE = 1;
    public static final byte NO_DYE = -1;

    public FixedRopeSnapshot {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(spans, "spans");
        if (spans.size() > MAX_SPANS) {
            throw new IllegalArgumentException("Fixed rope snapshot exceeds " + MAX_SPANS + " spans");
        }
        spans = List.copyOf(spans);
    }

    public record Span(
            UUID id,
            BlockAttachment start,
            BlockAttachment end,
            double allocatedLength,
            byte lineType,
            byte dyeColor
    ) {
        public Span(UUID id, BlockAttachment start, BlockAttachment end, double allocatedLength) {
            this(id, start, end, allocatedLength, TYPE_STRUCTURAL, NO_DYE);
        }

        public Span {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            if (!Double.isFinite(allocatedLength) || allocatedLength <= 0.0) {
                throw new IllegalArgumentException("allocatedLength must be finite and positive");
            }
            if (lineType != TYPE_STRUCTURAL && lineType != TYPE_GUIDE) {
                throw new IllegalArgumentException("unknown rope line type: " + lineType);
            }
            if (dyeColor < NO_DYE || dyeColor > 15 || (lineType == TYPE_STRUCTURAL && dyeColor != NO_DYE)) {
                throw new IllegalArgumentException("invalid rope dye colour: " + dyeColor);
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
