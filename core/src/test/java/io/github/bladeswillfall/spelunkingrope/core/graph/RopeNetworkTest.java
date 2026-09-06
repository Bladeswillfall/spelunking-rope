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
