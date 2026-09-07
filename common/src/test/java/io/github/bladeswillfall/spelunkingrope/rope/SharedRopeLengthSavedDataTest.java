package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import io.github.bladeswillfall.spelunkingrope.core.graph.SharedRopeLengthSolver;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedRopeLengthSavedDataTest {
    @Test
    void transfersPersistsAndPreservesTopology() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment left = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment pulley = BlockAttachment.atWorld(8.5, 60.5, 0.5);
        BlockAttachment right = BlockAttachment.atWorld(16.5, 64.5, 0.5);
        RopeSpan first = data.addRouteRope(
                left, RopeNode.Type.FIXED_ANCHOR,
                pulley, RopeNode.Type.PULLEY,
                10.0
        );
        RopeSpan second = data.addRouteRope(
                pulley, RopeNode.Type.PULLEY,
                right, RopeNode.Type.FIXED_ANCHOR,
                12.0
        );
        RopeNode pulleyNode = pulleyAt(data, pulley);
        Set<UUID> nodeIds = ids(data.nodes());
        Set<UUID> spanIds = spanIds(data);
        data.setDirty(false);

        SharedRopeLengthSolver.Transfer transfer = data.transferAcrossPulley(
                pulleyNode.id(), first.id(), 8.0, 9.0, 2.5
        );

        assertEquals(12.5, transfer.firstLength());
        assertEquals(9.5, transfer.secondLength());
        assertEquals(2.5, transfer.transferredToFirst());
        assertEquals(22.0, transfer.firstLength() + transfer.secondLength());
        assertTrue(data.isDirty());
        assertEquals(nodeIds, ids(data.nodes()));
        assertEquals(spanIds, spanIds(data));
        assertEquals(12.5, data.span(first.id()).allocatedLength());
        assertEquals(9.5, data.span(second.id()).allocatedLength());

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertEquals(nodeIds, ids(loaded.nodes()));
        assertEquals(spanIds, spanIds(loaded));
        assertEquals(12.5, loaded.span(first.id()).allocatedLength());
        assertEquals(9.5, loaded.span(second.id()).allocatedLength());
    }

    @Test
    void zeroAndInvalidTransfersDoNotDirtyOrPartiallyMutate() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment left = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment pulley = BlockAttachment.atWorld(8.5, 60.5, 0.5);
        BlockAttachment right = BlockAttachment.atWorld(16.5, 64.5, 0.5);
        RopeSpan first = data.addRouteRope(
                left, RopeNode.Type.FIXED_ANCHOR,
                pulley, RopeNode.Type.PULLEY,
                10.0
        );
        RopeSpan second = data.addRouteRope(
                pulley, RopeNode.Type.PULLEY,
                right, RopeNode.Type.FIXED_ANCHOR,
                12.0
        );
        RopeNode pulleyNode = pulleyAt(data, pulley);
        data.setDirty(false);

        SharedRopeLengthSolver.Transfer zero = data.transferAcrossPulley(
                pulleyNode.id(), first.id(), 8.0, 9.0, 0.0
        );
        assertEquals(0.0, zero.transferredToFirst());
        assertFalse(data.isDirty());
        assertEquals(10.0, data.span(first.id()).allocatedLength());
        assertEquals(12.0, data.span(second.id()).allocatedLength());

        assertThrows(IllegalArgumentException.class, () -> data.transferAcrossPulley(
                pulleyNode.id(), first.id(), 11.0, 9.0, 1.0
        ));
        assertFalse(data.isDirty());
        assertEquals(10.0, data.span(first.id()).allocatedLength());
        assertEquals(12.0, data.span(second.id()).allocatedLength());
    }

    @Test
    void pairwiseTransfersComposeAcrossMultiplePulleys() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment left = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment firstPulley = BlockAttachment.atWorld(8.5, 60.5, 0.5);
        BlockAttachment secondPulley = BlockAttachment.atWorld(16.5, 60.5, 0.5);
        BlockAttachment right = BlockAttachment.atWorld(24.5, 64.5, 0.5);
        RopeSpan first = data.addRouteRope(
                left, RopeNode.Type.FIXED_ANCHOR,
                firstPulley, RopeNode.Type.PULLEY,
                10.0
        );
        RopeSpan middle = data.addRouteRope(
                firstPulley, RopeNode.Type.PULLEY,
                secondPulley, RopeNode.Type.PULLEY,
                12.0
        );
        RopeSpan last = data.addRouteRope(
                secondPulley, RopeNode.Type.PULLEY,
                right, RopeNode.Type.FIXED_ANCHOR,
                14.0
        );
        double total = 36.0;

        data.transferAcrossPulley(pulleyAt(data, firstPulley).id(), first.id(), 8.0, 10.0, 5.0);
        assertEquals(12.0, data.span(first.id()).allocatedLength());
        assertEquals(10.0, data.span(middle.id()).allocatedLength());
        assertEquals(14.0, data.span(last.id()).allocatedLength());
        assertEquals(total, totalLength(data));

        data.transferAcrossPulley(pulleyAt(data, secondPulley).id(), middle.id(), 7.0, 8.0, -4.0);
        assertEquals(12.0, data.span(first.id()).allocatedLength());
        assertEquals(7.0, data.span(middle.id()).allocatedLength());
        assertEquals(17.0, data.span(last.id()).allocatedLength());
        assertEquals(total, totalLength(data));
    }

    private static RopeNode pulleyAt(FixedRopeSavedData data, BlockAttachment attachment) {
        return data.nodes().stream()
                .filter(node -> node.type() == RopeNode.Type.PULLEY)
                .filter(node -> attachment.equals(data.attachment(node.id())))
                .findFirst()
                .orElseThrow();
    }

    private static Set<UUID> ids(java.util.List<RopeNode> nodes) {
        return nodes.stream().map(RopeNode::id).collect(Collectors.toSet());
    }

    private static Set<UUID> spanIds(FixedRopeSavedData data) {
        return data.spans().stream().map(RopeSpan::id).collect(Collectors.toSet());
    }

    private static double totalLength(FixedRopeSavedData data) {
        return data.spans().stream().mapToDouble(RopeSpan::allocatedLength).sum();
    }
}
