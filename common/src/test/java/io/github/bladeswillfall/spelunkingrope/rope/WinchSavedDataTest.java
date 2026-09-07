package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WinchSavedDataTest {
    @Test
    void reusesWinchNodeAndRejectsSecondStructuralRoute() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment left = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment winch = BlockAttachment.atWorld(10.5, 64.5, 0.5);
        BlockAttachment right = BlockAttachment.atWorld(20.5, 64.5, 0.5);

        RopeSpan first = data.addRouteRope(
                left, RopeNode.Type.FIXED_ANCHOR,
                winch, RopeNode.Type.WINCH,
                12.0
        );

        assertNotNull(first);
        RopeNode winchNode = onlyWinch(data.nodes());
        assertTrue(incident(first, winchNode.id()));
        assertEquals(2, data.nodes().size());
        assertEquals(1, data.spans().size());

        assertNull(data.addRouteRope(
                winch, RopeNode.Type.WINCH,
                right, RopeNode.Type.FIXED_ANCHOR,
                12.0
        ));
        assertEquals(2, data.nodes().size());
        assertEquals(1, data.spans().size());

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertEquals(RopeNode.Type.WINCH, onlyWinch(loaded.nodes()).type());
        assertNull(loaded.addRouteRope(
                winch, RopeNode.Type.WINCH,
                right, RopeNode.Type.FIXED_ANCHOR,
                12.0
        ));
        assertEquals(2, loaded.nodes().size());
        assertEquals(1, loaded.spans().size());
    }

    @Test
    void previewsWithoutMutationAndReportsTheWinchEndpoint() {
        BlockAttachment winch = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment anchor = BlockAttachment.atWorld(10.5, 64.5, 0.5);
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan original = data.addRouteRope(
                winch, RopeNode.Type.WINCH,
                anchor, RopeNode.Type.FIXED_ANCHOR,
                12.0
        );
        assertNotNull(original);

        FixedRopeSavedData working = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertFalse(working.isDirty());
        FixedRopeSavedData.WinchPreview preview = working.previewWinch(winch, -1.0);
        assertEquals(FixedRopeSavedData.WinchAdjustment.CHANGED, preview.adjustment());
        assertEquals(original.id(), preview.spanId());
        assertTrue(preview.winchAtStart());
        assertEquals(12.0, preview.oldLength(), 1.0e-12);
        assertEquals(11.0, preview.newLength(), 1.0e-12);
        assertEquals(-1.0, preview.deployedLengthDelta(), 1.0e-12);
        assertEquals(12.0, structuralSpan(working, original.id()).allocatedLength(), 1.0e-12);
        assertFalse(working.isDirty());

        working.applyWinch(preview);
        RopeSpan applied = structuralSpan(working, original.id());
        assertEquals(11.0, applied.allocatedLength(), 1.0e-12);
        assertEquals(original.startNodeId(), applied.startNodeId());
        assertEquals(original.endNodeId(), applied.endNodeId());
        assertTrue(working.isDirty());

        FixedRopeSavedData reverse = new FixedRopeSavedData();
        RopeSpan reverseSpan = reverse.addRouteRope(
                anchor, RopeNode.Type.FIXED_ANCHOR,
                winch, RopeNode.Type.WINCH,
                12.0
        );
        assertNotNull(reverseSpan);
        reverse = FixedRopeSavedData.load(reverse.save(new CompoundTag()));
        FixedRopeSavedData.WinchPreview endPreview = reverse.previewWinch(winch, 1.0);
        assertEquals(FixedRopeSavedData.WinchAdjustment.CHANGED, endPreview.adjustment());
        assertEquals(reverseSpan.id(), endPreview.spanId());
        assertFalse(endPreview.winchAtStart());
        assertEquals(12.0, endPreview.oldLength(), 1.0e-12);
        assertEquals(13.0, endPreview.newLength(), 1.0e-12);
        assertFalse(reverse.isDirty());
    }

    @Test
    void adjustsLengthWithinGeometryAndSingleCoilCapacityWithoutChangingIdentity() {
        BlockAttachment winch = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment anchor = BlockAttachment.atWorld(10.5, 64.5, 0.5);
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan original = data.addRouteRope(
                winch, RopeNode.Type.WINCH,
                anchor, RopeNode.Type.FIXED_ANCHOR,
                12.0
        );
        assertNotNull(original);

        UUID spanId = original.id();
        UUID startNodeId = original.startNodeId();
        UUID endNodeId = original.endNodeId();
        FixedRopeSavedData working = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertFalse(working.isDirty());

        assertEquals(FixedRopeSavedData.WinchAdjustment.CHANGED, working.adjustWinch(winch, -1.0));
        RopeSpan reeled = structuralSpan(working, spanId);
        assertEquals(11.0, reeled.allocatedLength(), 1.0e-12);
        assertEquals(startNodeId, reeled.startNodeId());
        assertEquals(endNodeId, reeled.endNodeId());
        assertTrue(working.isDirty());

        working = FixedRopeSavedData.load(working.save(new CompoundTag()));
        assertEquals(FixedRopeSavedData.WinchAdjustment.CHANGED, working.adjustWinch(winch, -100.0));
        assertEquals(10.0, structuralSpan(working, spanId).allocatedLength(), 1.0e-12);

        working = FixedRopeSavedData.load(working.save(new CompoundTag()));
        assertFalse(working.isDirty());
        assertEquals(FixedRopeSavedData.WinchAdjustment.LIMIT, working.adjustWinch(winch, -1.0));
        assertFalse(working.isDirty());
        assertEquals(10.0, structuralSpan(working, spanId).allocatedLength(), 1.0e-12);

        assertEquals(FixedRopeSavedData.WinchAdjustment.CHANGED, working.adjustWinch(winch, 100.0));
        RopeSpan paidOut = structuralSpan(working, spanId);
        assertEquals(RopeCoilItem.MAX_DEPLOY_BLOCKS, paidOut.allocatedLength(), 1.0e-12);
        assertEquals(spanId, paidOut.id());
        assertEquals(startNodeId, paidOut.startNodeId());
        assertEquals(endNodeId, paidOut.endNodeId());

        FixedRopeSavedData persisted = FixedRopeSavedData.load(working.save(new CompoundTag()));
        assertEquals(RopeNode.Type.WINCH, onlyWinch(persisted.nodes()).type());
        assertEquals(RopeCoilItem.MAX_DEPLOY_BLOCKS, structuralSpan(persisted, spanId).allocatedLength(), 1.0e-12);
        assertFalse(persisted.isDirty());
        assertEquals(FixedRopeSavedData.WinchAdjustment.LIMIT, persisted.adjustWinch(winch, 1.0));
        assertFalse(persisted.isDirty());

        FixedRopeSnapshot snapshot = persisted.snapshot(new ResourceLocation("spelunking_rope_test", "winch"));
        assertEquals(1, snapshot.spans().size());
        assertEquals(spanId, snapshot.spans().get(0).id());
        assertEquals(RopeCoilItem.MAX_DEPLOY_BLOCKS, snapshot.spans().get(0).allocatedLength(), 1.0e-12);
    }

    private static RopeNode onlyWinch(List<RopeNode> nodes) {
        List<RopeNode> winches = nodes.stream().filter(node -> node.type() == RopeNode.Type.WINCH).toList();
        assertEquals(1, winches.size());
        return winches.get(0);
    }

    private static RopeSpan structuralSpan(FixedRopeSavedData data, UUID spanId) {
        RopeSpan span = data.span(spanId);
        assertNotNull(span);
        return span;
    }

    private static boolean incident(RopeSpan span, UUID nodeId) {
        return span.startNodeId().equals(nodeId) || span.endNodeId().equals(nodeId);
    }
}
