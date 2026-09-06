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
        RopeSpan span = network.connect(start.id(), end.id());

        assertEquals(2, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(start.id(), span.startNodeId());
        assertEquals(end.id(), span.endNodeId());
        assertTrue(network.disconnect(span.id()));
        assertTrue(network.spans().isEmpty());
        assertFalse(network.disconnect(span.id()));
    }

    @Test
    void rejectsUnknownAndSelfConnections() {
        RopeNetwork network = new RopeNetwork();
        RopeNode node = network.addNode();

        assertThrows(IllegalArgumentException.class, () -> network.connect(node.id(), UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> network.connect(node.id(), node.id()));
        assertThrows(IllegalArgumentException.class,
                () -> network.forEachIncidentSpanId(UUID.randomUUID(), ignored -> { }));
    }

    @Test
    void removingNodeRemovesIncidentSpansAndCleansIndex() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode middle = network.addNode();
        RopeNode last = network.addNode();
        network.connect(first.id(), middle.id());
        network.connect(middle.id(), last.id());
        RopeSpan surviving = network.connect(first.id(), last.id());

        assertTrue(network.removeNode(middle.id()));
        assertEquals(2, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(surviving.id(), network.spans().get(0).id());
        assertEquals(Set.of(surviving.id()), incidentIds(network, first.id()));
        assertEquals(Set.of(surviving.id()), incidentIds(network, last.id()));
        assertFalse(network.removeNode(middle.id()));
    }

    @Test
    void incidentIndexTracksParallelSpansAndDisconnects() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        RopeSpan one = network.connect(first.id(), second.id());
        RopeSpan two = network.connect(first.id(), second.id());

        assertNotEquals(one.id(), two.id());
        assertEquals(Set.of(one.id(), two.id()), incidentIds(network, first.id()));
        assertEquals(Set.of(one.id(), two.id()), incidentIds(network, second.id()));

        assertTrue(network.disconnect(one.id()));
        assertEquals(Set.of(two.id()), incidentIds(network, first.id()));
        assertEquals(Set.of(two.id()), incidentIds(network, second.id()));
    }

    @Test
    void allowsParallelSpansWithStableDistinctIds() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        UUID firstNodeId = first.id();
        RopeSpan one = network.connect(first.id(), second.id());
        RopeSpan two = network.connect(first.id(), second.id());

        assertEquals(firstNodeId, first.id());
        assertNotEquals(one.id(), two.id());
        assertEquals(2, network.spans().size());
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
