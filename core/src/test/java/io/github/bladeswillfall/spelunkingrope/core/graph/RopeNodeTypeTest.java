package io.github.bladeswillfall.spelunkingrope.core.graph;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RopeNodeTypeTest {
    @Test
    void defaultsToFixedAndPreservesExplicitPulleyType() {
        RopeNetwork network = new RopeNetwork();
        RopeNode fixed = network.addNode();
        UUID pulleyId = UUID.randomUUID();
        RopeNode pulley = network.addNode(pulleyId, RopeNode.Type.PULLEY);

        assertEquals(RopeNode.Type.FIXED_ANCHOR, fixed.type());
        assertEquals(pulleyId, pulley.id());
        assertEquals(RopeNode.Type.PULLEY, pulley.type());
        assertEquals(RopeNode.Type.PULLEY, network.nodes().get(1).type());
    }

    @Test
    void pulleyRejectsThirdIncidentSpanWithoutMutatingTopology() {
        RopeNetwork network = new RopeNetwork();
        RopeNode pulley = network.addNode(RopeNode.Type.PULLEY);
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        RopeNode third = network.addNode();
        network.connect(first.id(), pulley.id(), 5.0);
        network.connect(pulley.id(), second.id(), 6.0);

        assertThrows(IllegalArgumentException.class, () -> network.connect(pulley.id(), third.id(), 7.0));
        assertEquals(4, network.nodes().size());
        assertEquals(2, network.spans().size());
    }
}
