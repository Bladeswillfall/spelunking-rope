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

    public record SplitResult(RopeNode insertedNode, RopeSpan startSpan, RopeSpan endSpan) {
    }

    public RopeNode addNode() {
        return addNode(UUID.randomUUID(), RopeNode.Type.FIXED_ANCHOR);
    }

    public RopeNode addNode(RopeNode.Type type) {
        return addNode(UUID.randomUUID(), type);
    }

    public RopeNode addNode(UUID nodeId) {
        return addNode(nodeId, RopeNode.Type.FIXED_ANCHOR);
    }

    public RopeNode addNode(UUID nodeId, RopeNode.Type type) {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(type, "type");
        if (nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Duplicate rope node: " + nodeId);
        }

        RopeNode node = new RopeNode(nodeId, type);
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

    public RopeSpan connect(UUID startNodeId, UUID endNodeId, double allocatedLength) {
        return connect(UUID.randomUUID(), startNodeId, endNodeId, allocatedLength);
    }

    public RopeSpan connect(UUID spanId, UUID startNodeId, UUID endNodeId, double allocatedLength) {
        Objects.requireNonNull(spanId, "spanId");
        if (spans.containsKey(spanId)) {
            throw new IllegalArgumentException("Duplicate rope span: " + spanId);
        }
        RopeNode startNode = requireKnownNode(startNodeId);
        RopeNode endNode = requireKnownNode(endNodeId);
        requireNodeCapacity(startNode);
        if (!endNode.id().equals(startNode.id())) {
            requireNodeCapacity(endNode);
        }

        RopeSpan span = new RopeSpan(spanId, startNodeId, endNodeId, allocatedLength);
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

    public RopeSpan replaceSpanLength(UUID spanId, double allocatedLength) {
        RopeSpan current = requireKnownSpan(spanId);
        RopeSpan replacement = new RopeSpan(
                current.id(),
                current.startNodeId(),
                current.endNodeId(),
                allocatedLength
        );
        spans.put(replacement.id(), replacement);
        return replacement;
    }

    public SplitResult splitSpan(UUID spanId, UUID insertedNodeId, double startLength) {
        RopeSpan original = requireKnownSpan(spanId);
        Objects.requireNonNull(insertedNodeId, "insertedNodeId");
        if (nodes.containsKey(insertedNodeId)) {
            throw new IllegalArgumentException("Duplicate rope node: " + insertedNodeId);
        }
        if (!Double.isFinite(startLength) || startLength <= 0.0 || startLength >= original.allocatedLength()) {
            throw new IllegalArgumentException("startLength must be finite, positive, and shorter than the original span");
        }

        double endLength = original.allocatedLength() - startLength;
        if (!Double.isFinite(endLength) || endLength <= 0.0) {
            throw new IllegalArgumentException("remaining split length must be finite and positive");
        }

        UUID endSpanId;
        do {
            endSpanId = UUID.randomUUID();
        } while (spans.containsKey(endSpanId));

        disconnect(original.id());
        RopeNode insertedNode = addNode(insertedNodeId);
        RopeSpan startSpan = connect(
                original.id(),
                original.startNodeId(),
                insertedNode.id(),
                startLength
        );
        RopeSpan endSpan = connect(
                endSpanId,
                insertedNode.id(),
                original.endNodeId(),
                endLength
        );
        return new SplitResult(insertedNode, startSpan, endSpan);
    }

    public RopeSpan joinNode(UUID nodeId) {
        requireKnownNode(nodeId);
        LinkedHashSet<UUID> incident = incidentSpanIds.get(nodeId);
        if (incident.size() != 2) {
            throw new IllegalArgumentException("A joined rope node must have exactly two incident spans");
        }

        var iterator = incident.iterator();
        RopeSpan first = spans.get(iterator.next());
        RopeSpan second = spans.get(iterator.next());
        UUID startNodeId = otherNodeId(first, nodeId);
        UUID endNodeId = otherNodeId(second, nodeId);
        if (startNodeId.equals(endNodeId)) {
            throw new IllegalArgumentException("Joining this node would create a self-loop span");
        }

        double joinedLength = first.allocatedLength() + second.allocatedLength();
        if (!Double.isFinite(joinedLength) || joinedLength <= 0.0) {
            throw new IllegalArgumentException("joined allocated length must be finite and positive");
        }

        UUID retainedSpanId = first.id();
        removeNode(nodeId);
        return connect(retainedSpanId, startNodeId, endNodeId, joinedLength);
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

    private RopeSpan requireKnownSpan(UUID spanId) {
        RopeSpan span = spans.get(Objects.requireNonNull(spanId, "spanId"));
        if (span == null) {
            throw new IllegalArgumentException("Unknown rope span: " + spanId);
        }
        return span;
    }

    private RopeNode requireKnownNode(UUID nodeId) {
        RopeNode node = nodes.get(Objects.requireNonNull(nodeId, "nodeId"));
        if (node == null) {
            throw new IllegalArgumentException("Unknown rope node: " + nodeId);
        }
        return node;
    }

    private void requireNodeCapacity(RopeNode node) {
        int maxIncidentSpans = switch (node.type()) {
            case FIXED_ANCHOR -> Integer.MAX_VALUE;
            case PULLEY -> 2;
            case MOVABLE_ENDPOINT -> 1;
        };
        if (incidentSpanIds.get(node.id()).size() >= maxIncidentSpans) {
            throw new IllegalArgumentException(
                    node.type() + " node supports at most " + maxIncidentSpans + " incident spans: " + node.id()
            );
        }
    }

    private static UUID otherNodeId(RopeSpan span, UUID nodeId) {
        return span.startNodeId().equals(nodeId) ? span.endNodeId() : span.startNodeId();
    }
}
