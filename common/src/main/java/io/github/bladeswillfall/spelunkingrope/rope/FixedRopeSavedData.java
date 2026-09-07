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
    static final int SCHEMA_VERSION = 2;

    private static final String DATA_NAME = "spelunking_rope";
    private static final String TAG_SCHEMA_VERSION = "schema_version";
    private static final String TAG_NODES = "nodes";
    private static final String TAG_SPANS = "spans";
    private static final String TAG_ID = "id";
    private static final String TAG_START = "start";
    private static final String TAG_END = "end";
    private static final String TAG_LENGTH = "length";
    private static final String TAG_GUIDE = "guide";
    private static final String TAG_COLOR = "color";
    private static final String TAG_BLOCK_X = "block_x";
    private static final String TAG_BLOCK_Y = "block_y";
    private static final String TAG_BLOCK_Z = "block_z";
    private static final String TAG_LOCAL_X = "local_x";
    private static final String TAG_LOCAL_Y = "local_y";
    private static final String TAG_LOCAL_Z = "local_z";

    private final RopeNetwork network = new RopeNetwork();
    private final Map<UUID, BlockAttachment> attachments = new HashMap<>();
    // ponytail: structural spans are the common case; only guide spans pay for metadata storage.
    private final Map<UUID, Byte> guideColors = new HashMap<>();

    public static FixedRopeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FixedRopeSavedData::load,
                FixedRopeSavedData::new,
                DATA_NAME
        );
    }

    public RopeSpan addRope(BlockAttachment start, BlockAttachment end, double allocatedLength) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!Double.isFinite(allocatedLength) || allocatedLength <= 0.0) {
            throw new IllegalArgumentException("allocatedLength must be finite and positive");
        }

        RopeNode startNode = network.addNode();
        RopeNode endNode = network.addNode();
        attachments.put(startNode.id(), start);
        attachments.put(endNode.id(), end);
        RopeSpan span = network.connect(startNode.id(), endNode.id(), allocatedLength);
        setDirty();
        return span;
    }

    public RopeSpan addGuideLine(
            BlockAttachment start,
            BlockAttachment end,
            double allocatedLength,
            byte dyeColor
    ) {
        requireGuideColor(dyeColor);
        RopeSpan span = addRope(start, end, allocatedLength);
        guideColors.put(span.id(), dyeColor);
        setDirty();
        return span;
    }

    public RopeNode addNode(BlockAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        RopeNode node = network.addNode();
        attachments.put(node.id(), attachment);
        setDirty();
        return node;
    }

    public boolean removeNode(UUID nodeId) {
        List<UUID> incidentGuideSpans = new ArrayList<>();
        for (RopeSpan span : network.spans()) {
            if ((span.startNodeId().equals(nodeId) || span.endNodeId().equals(nodeId)) && isGuideLine(span.id())) {
                incidentGuideSpans.add(span.id());
            }
        }
        if (!network.removeNode(nodeId)) {
            return false;
        }
        for (UUID spanId : incidentGuideSpans) {
            guideColors.remove(spanId);
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
        guideColors.remove(spanId);
        setDirty();
        return true;
    }

    boolean disconnectAndRemoveOrphanNodes(UUID spanId) {
        RopeSpan current = span(spanId);
        if (current == null) {
            return false;
        }

        network.disconnect(current.id());
        removeNodeIfOrphan(current.startNodeId());
        if (!current.endNodeId().equals(current.startNodeId())) {
            removeNodeIfOrphan(current.endNodeId());
        }
        setDirty();
        return true;
    }

    int removeGuideLinesAt(BlockAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        List<RopeSpan> matches = new ArrayList<>();
        for (RopeSpan span : network.spans()) {
            if (!isGuideLine(span.id())) {
                continue;
            }
            BlockAttachment start = attachments.get(span.startNodeId());
            BlockAttachment end = attachments.get(span.endNodeId());
            if (attachment.equals(start) || attachment.equals(end)) {
                matches.add(span);
            }
        }
        for (RopeSpan match : matches) {
            network.disconnect(match.id());
            guideColors.remove(match.id());
            removeNodeIfOrphan(match.startNodeId());
            removeNodeIfOrphan(match.endNodeId());
        }
        if (!matches.isEmpty()) {
            setDirty();
        }
        return matches.size();
    }

    public BlockAttachment attachment(UUID nodeId) {
        return attachments.get(Objects.requireNonNull(nodeId, "nodeId"));
    }

    RopeSpan span(UUID spanId) {
        Objects.requireNonNull(spanId, "spanId");
        // Guide lines are intentionally invisible to structural traversal/extension callers.
        if (isGuideLine(spanId)) {
            return null;
        }
        // ponytail: extension/retrieval are rare interactions; avoid another long-lived span index until lookup volume justifies it.
        for (RopeSpan span : network.spans()) {
            if (span.id().equals(spanId)) {
                return span;
            }
        }
        return null;
    }

    boolean isGuideLine(UUID spanId) {
        return guideColors.containsKey(Objects.requireNonNull(spanId, "spanId"));
    }

    RopeSpan replaceSpanEnd(UUID spanId, BlockAttachment end, double allocatedLength) {
        Objects.requireNonNull(end, "end");
        if (!Double.isFinite(allocatedLength) || allocatedLength <= 0.0) {
            throw new IllegalArgumentException("allocatedLength must be finite and positive");
        }
        RopeSpan current = span(spanId);
        if (current == null) {
            throw new IllegalArgumentException("Unknown structural rope span: " + spanId);
        }

        network.disconnect(current.id());
        RopeSpan replacement = network.connect(
                current.id(),
                current.startNodeId(),
                current.endNodeId(),
                allocatedLength
        );
        attachments.put(current.endNodeId(), end);
        setDirty();
        return replacement;
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
            Byte guideColor = guideColors.get(span.id());
            snapshotSpans.add(new FixedRopeSnapshot.Span(
                    span.id(),
                    start,
                    end,
                    span.allocatedLength(),
                    guideColor == null ? FixedRopeSnapshot.TYPE_STRUCTURAL : FixedRopeSnapshot.TYPE_GUIDE,
                    guideColor == null ? FixedRopeSnapshot.NO_DYE : guideColor
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
            Byte guideColor = guideColors.get(span.id());
            if (guideColor != null) {
                spanTag.putBoolean(TAG_GUIDE, true);
                spanTag.putByte(TAG_COLOR, guideColor);
            }
            spans.add(spanTag);
        }
        tag.put(TAG_SPANS, spans);
        return tag;
    }

    static FixedRopeSavedData load(CompoundTag tag) {
        int version = tag.getInt(TAG_SCHEMA_VERSION);
        if (version < 1 || version > SCHEMA_VERSION) {
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
            RopeSpan span = data.network.connect(
                    spanTag.getUUID(TAG_ID),
                    spanTag.getUUID(TAG_START),
                    spanTag.getUUID(TAG_END),
                    spanTag.getDouble(TAG_LENGTH)
            );
            if (version >= 2 && spanTag.getBoolean(TAG_GUIDE)) {
                byte color = spanTag.contains(TAG_COLOR, Tag.TAG_BYTE)
                        ? spanTag.getByte(TAG_COLOR)
                        : FixedRopeSnapshot.NO_DYE;
                requireGuideColor(color);
                data.guideColors.put(span.id(), color);
            }
        }
        return data;
    }

    private void removeNodeIfOrphan(UUID nodeId) {
        // ponytail: retrieval is rare; a linear scan is cheaper than another persistent topology index/API.
        for (RopeSpan span : network.spans()) {
            if (span.startNodeId().equals(nodeId) || span.endNodeId().equals(nodeId)) {
                return;
            }
        }
        network.removeNode(nodeId);
        attachments.remove(nodeId);
    }

    private BlockAttachment requireAttachment(UUID nodeId) {
        BlockAttachment attachment = attachments.get(nodeId);
        if (attachment == null) {
            throw new IllegalStateException("Missing attachment for rope node " + nodeId);
        }
        return attachment;
    }

    private static void requireGuideColor(byte dyeColor) {
        if (dyeColor < FixedRopeSnapshot.NO_DYE || dyeColor > 15) {
            throw new IllegalArgumentException("invalid guide dye colour: " + dyeColor);
        }
    }
}
