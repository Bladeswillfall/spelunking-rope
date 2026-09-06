package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNetwork;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class FixedRopeSavedData extends SavedData {
    static final int SCHEMA_VERSION = 1;

    private static final String DATA_NAME = "spelunking_rope";
    private static final String TAG_SCHEMA_VERSION = "schema_version";
    private static final String TAG_NODES = "nodes";
    private static final String TAG_SPANS = "spans";
    private static final String TAG_ID = "id";
    private static final String TAG_START = "start";
    private static final String TAG_END = "end";
    private static final String TAG_LENGTH = "length";
    private static final String TAG_BLOCK_X = "block_x";
    private static final String TAG_BLOCK_Y = "block_y";
    private static final String TAG_BLOCK_Z = "block_z";
    private static final String TAG_LOCAL_X = "local_x";
    private static final String TAG_LOCAL_Y = "local_y";
    private static final String TAG_LOCAL_Z = "local_z";

    private final RopeNetwork network = new RopeNetwork();
    private final Map<UUID, BlockAttachment> attachments = new HashMap<>();

    public static FixedRopeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FixedRopeSavedData::load,
                FixedRopeSavedData::new,
                DATA_NAME
        );
    }

    public RopeNode addNode(BlockAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        RopeNode node = network.addNode();
        attachments.put(node.id(), attachment);
        setDirty();
        return node;
    }

    public boolean removeNode(UUID nodeId) {
        if (!network.removeNode(nodeId)) {
            return false;
        }
        attachments.remove(nodeId);
        setDirty();
        return true;
    }

    public RopeSpan connect(UUID startNodeId, UUID endNodeId, double allocatedLength) {
        RopeSpan span = network.connect(startNodeId, endNodeId, allocatedLength);
        setDirty();
        return span;
    }

    public boolean disconnect(UUID spanId) {
        if (!network.disconnect(spanId)) {
            return false;
        }
        setDirty();
        return true;
    }

    public BlockAttachment attachment(UUID nodeId) {
        return attachments.get(Objects.requireNonNull(nodeId, "nodeId"));
    }

    public List<RopeNode> nodes() {
        return network.nodes();
    }

    public List<RopeSpan> spans() {
        return network.spans();
    }

    public FixedRopeSnapshot snapshot(ResourceLocation dimension) {
        List<RopeSpan> currentSpans = network.spans();
        List<FixedRopeSnapshot.Span> snapshotSpans = new ArrayList<>(currentSpans.size());
        for (RopeSpan span : currentSpans) {
            BlockAttachment start = requireAttachment(span.startNodeId());
            BlockAttachment end = requireAttachment(span.endNodeId());
            snapshotSpans.add(new FixedRopeSnapshot.Span(
                    span.id(),
                    start,
                    end,
                    span.allocatedLength()
            ));
        }
        return new FixedRopeSnapshot(dimension, snapshotSpans);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(TAG_SCHEMA_VERSION, SCHEMA_VERSION);

        ListTag nodes = new ListTag();
        for (RopeNode node : network.nodes()) {
            BlockAttachment attachment = requireAttachment(node.id());
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putUUID(TAG_ID, node.id());
            nodeTag.putInt(TAG_BLOCK_X, attachment.blockPos().getX());
            nodeTag.putInt(TAG_BLOCK_Y, attachment.blockPos().getY());
            nodeTag.putInt(TAG_BLOCK_Z, attachment.blockPos().getZ());
            nodeTag.putDouble(TAG_LOCAL_X, attachment.localX());
            nodeTag.putDouble(TAG_LOCAL_Y, attachment.localY());
            nodeTag.putDouble(TAG_LOCAL_Z, attachment.localZ());
            nodes.add(nodeTag);
        }
        tag.put(TAG_NODES, nodes);

        ListTag spans = new ListTag();
        for (RopeSpan span : network.spans()) {
            CompoundTag spanTag = new CompoundTag();
            spanTag.putUUID(TAG_ID, span.id());
            spanTag.putUUID(TAG_START, span.startNodeId());
            spanTag.putUUID(TAG_END, span.endNodeId());
            spanTag.putDouble(TAG_LENGTH, span.allocatedLength());
            spans.add(spanTag);
        }
        tag.put(TAG_SPANS, spans);
        return tag;
    }

    static FixedRopeSavedData load(CompoundTag tag) {
        int version = tag.getInt(TAG_SCHEMA_VERSION);
        if (version != SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported rope data schema version: " + version);
        }

        FixedRopeSavedData data = new FixedRopeSavedData();
        ListTag nodes = tag.getList(TAG_NODES, Tag.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++) {
            CompoundTag nodeTag = nodes.getCompound(i);
            UUID nodeId = nodeTag.getUUID(TAG_ID);
            data.network.addNode(nodeId);
            data.attachments.put(nodeId, new BlockAttachment(
                    new BlockPos(
                            nodeTag.getInt(TAG_BLOCK_X),
                            nodeTag.getInt(TAG_BLOCK_Y),
                            nodeTag.getInt(TAG_BLOCK_Z)
                    ),
                    nodeTag.getDouble(TAG_LOCAL_X),
                    nodeTag.getDouble(TAG_LOCAL_Y),
                    nodeTag.getDouble(TAG_LOCAL_Z)
            ));
        }

        ListTag spans = tag.getList(TAG_SPANS, Tag.TAG_COMPOUND);
        for (int i = 0; i < spans.size(); i++) {
            CompoundTag spanTag = spans.getCompound(i);
            data.network.connect(
                    spanTag.getUUID(TAG_ID),
                    spanTag.getUUID(TAG_START),
                    spanTag.getUUID(TAG_END),
                    spanTag.getDouble(TAG_LENGTH)
            );
        }
        return data;
    }

    private BlockAttachment requireAttachment(UUID nodeId) {
        BlockAttachment attachment = attachments.get(nodeId);
        if (attachment == null) {
            throw new IllegalStateException("Missing attachment for rope node " + nodeId);
        }
        return attachment;
    }
}
