package io.github.bladeswillfall.spelunkingrope.core.graph;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
