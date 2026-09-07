package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PulleyTopologySavedDataTest {
    @Test
    void reusesPulleyNodeAndRejectsThirdStructuralSpan() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment left = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment pulley = BlockAttachment.atWorld(8.5, 60.5, 0.5);
        BlockAttachment right = BlockAttachment.atWorld(16.5, 64.5, 0.5);
        BlockAttachment third = BlockAttachment.atWorld(8.5, 48.5, 0.5);

        RopeSpan first = data.addRouteRope(
                left, RopeNode.Type.FIXED_ANCHOR,
                pulley, RopeNode.Type.PULLEY,
                10.0
        );
        RopeSpan second = data.addRouteRope(
                pulley, RopeNode.Type.PULLEY,
                right, RopeNode.Type.FIXED_ANCHOR,
                11.0
        );

        assertNotNull(first);
        assertNotNull(second);
        RopeNode pulleyNode = onlyPulley(data.nodes());
        assertTrue(incident(first, pulleyNode.id()));
        assertTrue(incident(second, pulleyNode.id()));
        assertEquals(3, data.nodes().size());
        assertEquals(2, data.spans().size());

        assertNull(data.addRouteRope(
                pulley, RopeNode.Type.PULLEY,
                third, RopeNode.Type.FIXED_ANCHOR,
                12.0
        ));
        assertEquals(3, data.nodes().size());
        assertEquals(2, data.spans().size());

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertEquals(RopeNode.Type.PULLEY, onlyPulley(loaded.nodes()).type());
        assertNull(loaded.addRouteRope(
                pulley, RopeNode.Type.PULLEY,
                third, RopeNode.Type.FIXED_ANCHOR,
                12.0
        ));
    }

    @Test
    void schemaTwoNodesWithoutTypeLoadAsFixedAnchors() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        data.addRope(
                BlockAttachment.atWorld(0.5, 64.5, 0.5),
                BlockAttachment.atWorld(0.5, 32.5, 0.5),
                32.0
        );
        CompoundTag tag = data.save(new CompoundTag());
        tag.putInt("schema_version", 2);
        ListTag nodes = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++) {
            nodes.getCompound(i).remove("type");
        }

        FixedRopeSavedData loaded = FixedRopeSavedData.load(tag);
        assertEquals(
                List.of(RopeNode.Type.FIXED_ANCHOR, RopeNode.Type.FIXED_ANCHOR),
                loaded.nodes().stream().map(RopeNode::type).toList()
        );
    }

    private static RopeNode onlyPulley(List<RopeNode> nodes) {
        List<RopeNode> pulleys = nodes.stream().filter(node -> node.type() == RopeNode.Type.PULLEY).toList();
        assertEquals(1, pulleys.size());
        return pulleys.get(0);
    }

    private static boolean incident(RopeSpan span, UUID nodeId) {
        return span.startNodeId().equals(nodeId) || span.endNodeId().equals(nodeId);
    }
}
