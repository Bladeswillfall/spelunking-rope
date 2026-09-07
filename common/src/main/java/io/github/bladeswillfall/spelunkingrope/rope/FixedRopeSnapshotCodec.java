package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FixedRopeSnapshotCodec {
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(SpelunkingRope.MOD_ID, "fixed_rope_snapshot");

    private FixedRopeSnapshotCodec() {
    }

    public static void encode(FixedRopeSnapshot snapshot, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(snapshot.dimension());
        List<FixedRopeSnapshot.Span> spans = snapshot.spans();
        buffer.writeVarInt(spans.size());
        for (FixedRopeSnapshot.Span span : spans) {
            buffer.writeUUID(span.id());
            writeAttachment(buffer, span.start());
            writeAttachment(buffer, span.end());
            buffer.writeDouble(span.allocatedLength());
            buffer.writeByte(span.lineType());
            buffer.writeByte(span.dyeColor());
        }
    }

    public static FixedRopeSnapshot decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int spanCount = buffer.readVarInt();
        if (spanCount < 0 || spanCount > FixedRopeSnapshot.MAX_SPANS) {
            throw new IllegalArgumentException("Invalid fixed rope snapshot span count: " + spanCount);
        }

        List<FixedRopeSnapshot.Span> spans = new ArrayList<>(spanCount);
        for (int i = 0; i < spanCount; i++) {
            UUID id = buffer.readUUID();
            BlockAttachment start = readAttachment(buffer);
            BlockAttachment end = readAttachment(buffer);
            double allocatedLength = buffer.readDouble();
            spans.add(new FixedRopeSnapshot.Span(
                    id,
                    start,
                    end,
                    allocatedLength,
                    buffer.readByte(),
                    buffer.readByte()
            ));
        }
        return new FixedRopeSnapshot(dimension, spans);
    }

    private static void writeAttachment(FriendlyByteBuf buffer, BlockAttachment attachment) {
        buffer.writeBlockPos(attachment.blockPos());
        buffer.writeDouble(attachment.localX());
        buffer.writeDouble(attachment.localY());
        buffer.writeDouble(attachment.localZ());
    }

    private static BlockAttachment readAttachment(FriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        return new BlockAttachment(
                blockPos,
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
