package io.github.bladeswillfall.spelunkingrope.core.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WinchNodeTest {
    @Test
    void winchAcceptsExactlyOneIncidentSpan() {
        RopeNetwork network = new RopeNetwork();
        RopeNode winch = network.addNode(RopeNode.Type.WINCH);
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        RopeSpan span = network.connect(winch.id(), first.id(), 8.0);

        assertEquals(RopeNode.Type.WINCH, winch.type());
        assertEquals(Set.of(span.id()), incidentIds(network, winch.id()));
        assertThrows(IllegalArgumentException.class,
                () -> network.connect(winch.id(), second.id(), 9.0));
        assertEquals(3, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(Set.of(span.id()), incidentIds(network, winch.id()));
    }

    private static Set<UUID> incidentIds(RopeNetwork network, UUID nodeId) {
        var ids = new ArrayList<UUID>();
        network.forEachIncidentSpanId(nodeId, ids::add);
        return Set.copyOf(ids);
    }
}
