package io.github.bladeswillfall.spelunkingrope.core.graph;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class RopeNetwork {
    private final Map<UUID, RopeNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, RopeSpan> spans = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> incidentSpanIds = new LinkedHashMap<>();

    public RopeNode addNode() {
        RopeNode node = new RopeNode(UUID.randomUUID());
        nodes.put(node.id(), node);
        incidentSpanIds.put(node.id(), new LinkedHashSet<>());
        return node;
    }

    public boolean removeNode(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (nodes.remove(nodeId) == null) {
            return false;
        }

        LinkedHashSet<UUID> incident = incidentSpanIds.remove(nodeId);
        for (UUID spanId : incident) {
            RopeSpan span = spans.remove(spanId);
            UUID otherNodeId = span.startNodeId().equals(nodeId) ? span.endNodeId() : span.startNodeId();
            incidentSpanIds.get(otherNodeId).remove(spanId);
        }
        return true;
    }

    public RopeSpan connect(UUID startNodeId, UUID endNodeId) {
        requireKnownNode(startNodeId);
        requireKnownNode(endNodeId);

        RopeSpan span = new RopeSpan(UUID.randomUUID(), startNodeId, endNodeId);
        spans.put(span.id(), span);
        incidentSpanIds.get(startNodeId).add(span.id());
        incidentSpanIds.get(endNodeId).add(span.id());
        return span;
    }

    public boolean disconnect(UUID spanId) {
        RopeSpan span = spans.remove(Objects.requireNonNull(spanId, "spanId"));
        if (span == null) {
            return false;
        }
        incidentSpanIds.get(span.startNodeId()).remove(spanId);
        incidentSpanIds.get(span.endNodeId()).remove(spanId);
        return true;
    }

    public void forEachIncidentSpanId(UUID nodeId, Consumer<UUID> action) {
        requireKnownNode(nodeId);
        Objects.requireNonNull(action, "action");
        incidentSpanIds.get(nodeId).forEach(action);
    }

    public List<RopeNode> nodes() {
        return List.copyOf(nodes.values());
    }

    public List<RopeSpan> spans() {
        return List.copyOf(spans.values());
    }

    private void requireKnownNode(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown rope node: " + nodeId);
        }
    }
}
