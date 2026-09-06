package io.github.bladeswillfall.spelunkingrope.core.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RopeNetwork {
    private final Map<UUID, RopeNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, RopeSpan> spans = new LinkedHashMap<>();

    public RopeNode addNode() {
        RopeNode node = new RopeNode(UUID.randomUUID());
        nodes.put(node.id(), node);
        return node;
    }

    public boolean removeNode(UUID nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (nodes.remove(nodeId) == null) {
            return false;
        }
        spans.values().removeIf(span -> span.touches(nodeId));
        return true;
    }

    public RopeSpan connect(UUID startNodeId, UUID endNodeId) {
        requireKnownNode(startNodeId);
        requireKnownNode(endNodeId);

        RopeSpan span = new RopeSpan(UUID.randomUUID(), startNodeId, endNodeId);
        spans.put(span.id(), span);
        return span;
    }

    public boolean disconnect(UUID spanId) {
        return spans.remove(Objects.requireNonNull(spanId, "spanId")) != null;
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
