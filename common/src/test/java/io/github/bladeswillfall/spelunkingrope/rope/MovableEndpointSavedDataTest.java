package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovableEndpointSavedDataTest {
    @Test
    void movesEndpointWithoutChangingIdentityLengthOrTopology() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment fixed = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment initial = BlockAttachment.atWorld(4.5, 64.5, 0.5);
        BlockAttachment moved = BlockAttachment.atWorld(6.5, 64.5, 0.5);
        RopeSpan span = data.addRouteRope(
                fixed, RopeNode.Type.FIXED_ANCHOR,
                initial, RopeNode.Type.MOVABLE_ENDPOINT,
                8.0
        );
        RopeNode movable = onlyMovable(data);
        Set<UUID> nodeIds = nodeIds(data);
        Set<UUID> spanIds = spanIds(data);
        data.setDirty(false);

        assertTrue(data.moveEndpoint(movable.id(), moved));

        assertTrue(data.isDirty());
        assertEquals(moved, data.attachment(movable.id()));
        assertEquals(nodeIds, nodeIds(data));
        assertEquals(spanIds, spanIds(data));
        assertEquals(8.0, data.span(span.id()).allocatedLength());

        FixedRopeSnapshot.Span snapshotSpan = data.snapshot(new ResourceLocation("minecraft", "overworld"))
                .spans().stream()
                .filter(candidate -> candidate.id().equals(span.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(moved, snapshotSpan.end());

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        RopeNode loadedMovable = loaded.nodes().stream()
                .filter(node -> node.id().equals(movable.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(RopeNode.Type.MOVABLE_ENDPOINT, loadedMovable.type());
        assertEquals(moved, loaded.attachment(loadedMovable.id()));
        assertEquals(span.id(), loaded.span(span.id()).id());
        assertEquals(8.0, loaded.span(span.id()).allocatedLength());
    }

    @Test
    void rejectsNoOpOverLengthAndNonMovableTargetsWithoutMutation() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment fixed = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment initial = BlockAttachment.atWorld(4.5, 64.5, 0.5);
        RopeSpan span = data.addRouteRope(
                fixed, RopeNode.Type.FIXED_ANCHOR,
                initial, RopeNode.Type.MOVABLE_ENDPOINT,
                8.0
        );
        RopeNode movable = onlyMovable(data);
        RopeNode fixedNode = data.nodes().stream()
                .filter(node -> node.type() == RopeNode.Type.FIXED_ANCHOR)
                .findFirst()
                .orElseThrow();
        data.setDirty(false);

        assertFalse(data.moveEndpoint(movable.id(), initial));
        assertFalse(data.isDirty());

        assertFalse(data.moveEndpoint(movable.id(), BlockAttachment.atWorld(20.5, 64.5, 0.5)));
        assertFalse(data.isDirty());
        assertEquals(initial, data.attachment(movable.id()));
        assertEquals(8.0, data.span(span.id()).allocatedLength());

        assertThrows(IllegalArgumentException.class,
                () -> data.moveEndpoint(fixedNode.id(), BlockAttachment.atWorld(1.5, 64.5, 0.5)));
        assertFalse(data.isDirty());
        assertEquals(initial, data.attachment(movable.id()));
        assertEquals(1, data.spans().size());
    }

    private static RopeNode onlyMovable(FixedRopeSavedData data) {
        return data.nodes().stream()
                .filter(node -> node.type() == RopeNode.Type.MOVABLE_ENDPOINT)
                .findFirst()
                .orElseThrow();
    }

    private static Set<UUID> nodeIds(FixedRopeSavedData data) {
        return data.nodes().stream().map(RopeNode::id).collect(Collectors.toSet());
    }

    private static Set<UUID> spanIds(FixedRopeSavedData data) {
        return data.spans().stream().map(RopeSpan::id).collect(Collectors.toSet());
    }
}
