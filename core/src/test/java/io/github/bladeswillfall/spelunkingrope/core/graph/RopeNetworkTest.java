package io.github.bladeswillfall.spelunkingrope.core.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RopeNetworkTest {
    @Test
    void connectsAndDisconnectsKnownNodes() {
        RopeNetwork network = new RopeNetwork();
        RopeNode start = network.addNode();
        RopeNode end = network.addNode();
        RopeSpan span = network.connect(start.id(), end.id(), 12.5);

        assertEquals(2, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(start.id(), span.startNodeId());
        assertEquals(end.id(), span.endNodeId());
        assertEquals(12.5, span.allocatedLength());
        assertTrue(network.disconnect(span.id()));
        assertTrue(network.spans().isEmpty());
        assertFalse(network.disconnect(span.id()));
    }

    @Test
    void restoresExplicitStableIdsAndRejectsDuplicates() {
        RopeNetwork network = new RopeNetwork();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID spanId = UUID.randomUUID();

        RopeNode first = network.addNode(firstId);
        RopeNode second = network.addNode(secondId);
        RopeSpan span = network.connect(spanId, firstId, secondId, 14.0);

        assertEquals(firstId, first.id());
        assertEquals(secondId, second.id());
        assertEquals(spanId, span.id());
        assertEquals(14.0, span.allocatedLength());
        assertThrows(IllegalArgumentException.class, () -> network.addNode(firstId));
        assertThrows(IllegalArgumentException.class,
                () -> network.connect(spanId, firstId, secondId, 15.0));
    }

    @Test
    void rejectsUnknownSelfAndInvalidLengthConnections() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();

        assertThrows(IllegalArgumentException.class,
                () -> network.connect(first.id(), UUID.randomUUID(), 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> network.connect(first.id(), first.id(), 10.0));
        for (double invalid : new double[]{0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> network.connect(first.id(), second.id(), invalid));
        }
        assertThrows(IllegalArgumentException.class,
                () -> network.forEachIncidentSpanId(UUID.randomUUID(), ignored -> { }));
    }

    @Test
    void removingNodeRemovesIncidentSpansAndCleansIndex() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode middle = network.addNode();
        RopeNode last = network.addNode();
        network.connect(first.id(), middle.id(), 8.0);
        network.connect(middle.id(), last.id(), 9.0);
        RopeSpan surviving = network.connect(first.id(), last.id(), 10.0);

        assertTrue(network.removeNode(middle.id()));
        assertEquals(2, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(surviving.id(), network.spans().get(0).id());
        assertEquals(Set.of(surviving.id()), incidentIds(network, first.id()));
        assertEquals(Set.of(surviving.id()), incidentIds(network, last.id()));
        assertFalse(network.removeNode(middle.id()));
    }

    @Test
    void incidentIndexTracksParallelSpansWithDifferentLengths() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        RopeSpan one = network.connect(first.id(), second.id(), 7.0);
        RopeSpan two = network.connect(first.id(), second.id(), 11.0);

        assertNotEquals(one.id(), two.id());
        assertEquals(7.0, one.allocatedLength());
        assertEquals(11.0, two.allocatedLength());
        assertEquals(Set.of(one.id(), two.id()), incidentIds(network, first.id()));
        assertEquals(Set.of(one.id(), two.id()), incidentIds(network, second.id()));

        assertTrue(network.disconnect(one.id()));
        assertEquals(Set.of(two.id()), incidentIds(network, first.id()));
        assertEquals(Set.of(two.id()), incidentIds(network, second.id()));
    }

    @Test
    void splitAndJoinRoundTripPreservesStableSpanIdentityAndLength() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode last = network.addNode();
        UUID originalSpanId = UUID.randomUUID();
        UUID insertedNodeId = UUID.randomUUID();
        network.connect(originalSpanId, first.id(), last.id(), 12.0);

        RopeNetwork.SplitResult split = network.splitSpan(originalSpanId, insertedNodeId, 5.0);

        assertEquals(3, network.nodes().size());
        assertEquals(2, network.spans().size());
        assertEquals(insertedNodeId, split.insertedNode().id());
        assertEquals(originalSpanId, split.startSpan().id());
        assertEquals(first.id(), split.startSpan().startNodeId());
        assertEquals(insertedNodeId, split.startSpan().endNodeId());
        assertEquals(5.0, split.startSpan().allocatedLength());
        assertNotEquals(originalSpanId, split.endSpan().id());
        assertEquals(insertedNodeId, split.endSpan().startNodeId());
        assertEquals(last.id(), split.endSpan().endNodeId());
        assertEquals(7.0, split.endSpan().allocatedLength());
        assertEquals(12.0, split.startSpan().allocatedLength() + split.endSpan().allocatedLength());
        assertEquals(Set.of(originalSpanId), incidentIds(network, first.id()));
        assertEquals(Set.of(originalSpanId, split.endSpan().id()), incidentIds(network, insertedNodeId));
        assertEquals(Set.of(split.endSpan().id()), incidentIds(network, last.id()));

        RopeSpan joined = network.joinNode(insertedNodeId);

        assertEquals(2, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(originalSpanId, joined.id());
        assertEquals(first.id(), joined.startNodeId());
        assertEquals(last.id(), joined.endNodeId());
        assertEquals(12.0, joined.allocatedLength());
        assertEquals(Set.of(originalSpanId), incidentIds(network, first.id()));
        assertEquals(Set.of(originalSpanId), incidentIds(network, last.id()));
    }

    @Test
    void invalidSplitsLeaveTopologyAndIncidentIndexesUnchanged() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode last = network.addNode();
        RopeSpan original = network.connect(first.id(), last.id(), 10.0);
        var nodesBefore = network.nodes();
        var spansBefore = network.spans();
        Set<UUID> firstIncidentBefore = incidentIds(network, first.id());
        Set<UUID> lastIncidentBefore = incidentIds(network, last.id());

        assertThrows(IllegalArgumentException.class,
                () -> network.splitSpan(UUID.randomUUID(), UUID.randomUUID(), 5.0));
        assertThrows(IllegalArgumentException.class,
                () -> network.splitSpan(original.id(), first.id(), 5.0));
        for (double invalid : new double[]{0.0, -1.0, 10.0, 11.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> network.splitSpan(original.id(), UUID.randomUUID(), invalid));
        }

        assertEquals(nodesBefore, network.nodes());
        assertEquals(spansBefore, network.spans());
        assertEquals(firstIncidentBefore, incidentIds(network, first.id()));
        assertEquals(lastIncidentBefore, incidentIds(network, last.id()));
    }

    @Test
    void invalidJoinsLeaveTopologyAndIncidentIndexesUnchanged() {
        RopeNetwork degreeOne = new RopeNetwork();
        RopeNode first = degreeOne.addNode();
        RopeNode middle = degreeOne.addNode();
        RopeSpan onlySpan = degreeOne.connect(first.id(), middle.id(), 5.0);
        var degreeOneNodes = degreeOne.nodes();
        var degreeOneSpans = degreeOne.spans();

        assertThrows(IllegalArgumentException.class, () -> degreeOne.joinNode(middle.id()));
        assertEquals(degreeOneNodes, degreeOne.nodes());
        assertEquals(degreeOneSpans, degreeOne.spans());
        assertEquals(Set.of(onlySpan.id()), incidentIds(degreeOne, first.id()));
        assertEquals(Set.of(onlySpan.id()), incidentIds(degreeOne, middle.id()));

        RopeNetwork selfLoop = new RopeNetwork();
        RopeNode outer = selfLoop.addNode();
        RopeNode selfMiddle = selfLoop.addNode();
        RopeSpan firstParallel = selfLoop.connect(outer.id(), selfMiddle.id(), 4.0);
        RopeSpan secondParallel = selfLoop.connect(selfMiddle.id(), outer.id(), 6.0);
        var selfLoopNodes = selfLoop.nodes();
        var selfLoopSpans = selfLoop.spans();

        assertThrows(IllegalArgumentException.class, () -> selfLoop.joinNode(selfMiddle.id()));
        assertEquals(selfLoopNodes, selfLoop.nodes());
        assertEquals(selfLoopSpans, selfLoop.spans());
        assertEquals(Set.of(firstParallel.id(), secondParallel.id()), incidentIds(selfLoop, outer.id()));
        assertEquals(Set.of(firstParallel.id(), secondParallel.id()), incidentIds(selfLoop, selfMiddle.id()));

        RopeNetwork overflow = new RopeNetwork();
        RopeNode overflowFirst = overflow.addNode();
        RopeNode overflowMiddle = overflow.addNode();
        RopeNode overflowLast = overflow.addNode();
        RopeSpan overflowA = overflow.connect(overflowFirst.id(), overflowMiddle.id(), Double.MAX_VALUE);
        RopeSpan overflowB = overflow.connect(overflowMiddle.id(), overflowLast.id(), Double.MAX_VALUE);
        var overflowNodes = overflow.nodes();
        var overflowSpans = overflow.spans();

        assertThrows(IllegalArgumentException.class, () -> overflow.joinNode(overflowMiddle.id()));
        assertEquals(overflowNodes, overflow.nodes());
        assertEquals(overflowSpans, overflow.spans());
        assertEquals(Set.of(overflowA.id()), incidentIds(overflow, overflowFirst.id()));
        assertEquals(Set.of(overflowA.id(), overflowB.id()), incidentIds(overflow, overflowMiddle.id()));
        assertEquals(Set.of(overflowB.id()), incidentIds(overflow, overflowLast.id()));
    }

    @Test
    void returnedCollectionsAreImmutableSnapshots() {
        RopeNetwork network = new RopeNetwork();
        network.addNode();
        var nodes = network.nodes();
        var spans = network.spans();

        assertThrows(UnsupportedOperationException.class, nodes::clear);
        assertThrows(UnsupportedOperationException.class, spans::clear);

        network.addNode();
        assertEquals(1, nodes.size());
        assertEquals(2, network.nodes().size());
    }

    private static Set<UUID> incidentIds(RopeNetwork network, UUID nodeId) {
        var ids = new ArrayList<UUID>();
        network.forEachIncidentSpanId(nodeId, ids::add);
        return Set.copyOf(ids);
    }
}
