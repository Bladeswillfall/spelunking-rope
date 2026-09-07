package io.github.bladeswillfall.spelunkingrope.core.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovableEndpointNodeTest {
    @Test
    void movableEndpointAcceptsExactlyOneIncidentSpan() {
        RopeNetwork network = new RopeNetwork();
        RopeNode movable = network.addNode(RopeNode.Type.MOVABLE_ENDPOINT);
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        RopeSpan span = network.connect(movable.id(), first.id(), 8.0);

        assertEquals(RopeNode.Type.MOVABLE_ENDPOINT, movable.type());
        assertEquals(Set.of(span.id()), incidentIds(network, movable.id()));
        assertThrows(IllegalArgumentException.class,
                () -> network.connect(movable.id(), second.id(), 9.0));
        assertEquals(3, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(Set.of(span.id()), incidentIds(network, movable.id()));
    }

    private static Set<UUID> incidentIds(RopeNetwork network, UUID nodeId) {
        var ids = new ArrayList<UUID>();
        network.forEachIncidentSpanId(nodeId, ids::add);
        return Set.copyOf(ids);
    }
}
