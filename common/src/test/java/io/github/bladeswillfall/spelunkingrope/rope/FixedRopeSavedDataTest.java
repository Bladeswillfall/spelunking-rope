package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedRopeSavedDataTest {
    @Test
    void roundTripsFixedAttachmentsAndTopology() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment firstAttachment = new BlockAttachment(new BlockPos(-3, 64, 7), 0.125, 0.75, 0.875);
        BlockAttachment secondAttachment = new BlockAttachment(new BlockPos(12, -20, 31), 0.5, 1.25, 0.25);
        RopeNode first = data.addNode(firstAttachment);
        RopeNode second = data.addNode(secondAttachment);
        RopeSpan span = data.connect(first.id(), second.id(), 18.25);

        CompoundTag tag = data.save(new CompoundTag());
        assertEquals(FixedRopeSavedData.SCHEMA_VERSION, tag.getInt("schema_version"));

        FixedRopeSavedData loaded = FixedRopeSavedData.load(tag);
        assertFalse(loaded.isDirty());
        assertEquals(Set.of(first.id(), second.id()), nodeIds(loaded));
        assertEquals(firstAttachment, loaded.attachment(first.id()));
        assertEquals(secondAttachment, loaded.attachment(second.id()));
        assertEquals(1, loaded.spans().size());
        RopeSpan loadedSpan = loaded.spans().get(0);
        assertEquals(span.id(), loadedSpan.id());
        assertEquals(first.id(), loadedSpan.startNodeId());
        assertEquals(second.id(), loadedSpan.endNodeId());
        assertEquals(18.25, loadedSpan.allocatedLength());

        assertTrue(loaded.removeNode(first.id()));
        assertNull(loaded.attachment(first.id()));
        assertTrue(loaded.spans().isEmpty());
        assertTrue(loaded.isDirty());
    }

    @Test
    void replacesSpanEndWithoutChangingIdentity() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment start = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment end = BlockAttachment.atWorld(0.5, 32.05, 0.5);
        RopeSpan original = data.addRope(start, end, 32.45);
        BlockAttachment extendedEnd = BlockAttachment.atWorld(0.5, 0.05, 0.5);

        RopeSpan replacement = data.replaceSpanEnd(original.id(), extendedEnd, 64.45);

        assertEquals(original.id(), replacement.id());
        assertEquals(original.startNodeId(), replacement.startNodeId());
        assertEquals(original.endNodeId(), replacement.endNodeId());
        assertEquals(extendedEnd, data.attachment(original.endNodeId()));
        assertEquals(64.45, replacement.allocatedLength());

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        RopeSpan persisted = loaded.spans().get(0);
        assertEquals(original.id(), persisted.id());
        assertEquals(64.45, persisted.allocatedLength());
        assertEquals(extendedEnd, loaded.attachment(original.endNodeId()));
    }

    @Test
    void disconnectsSpanWithoutRemovingSharedEndpoint() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        BlockAttachment firstAttachment = BlockAttachment.atWorld(0.5, 64.5, 0.5);
        BlockAttachment sharedAttachment = BlockAttachment.atWorld(0.5, 48.5, 0.5);
        BlockAttachment thirdAttachment = BlockAttachment.atWorld(0.5, 32.5, 0.5);
        RopeNode first = data.addNode(firstAttachment);
        RopeNode shared = data.addNode(sharedAttachment);
        RopeNode third = data.addNode(thirdAttachment);
        RopeSpan upper = data.connect(first.id(), shared.id(), 16.0);
        RopeSpan lower = data.connect(shared.id(), third.id(), 16.0);

        assertTrue(data.disconnectAndRemoveOrphanNodes(upper.id()));
        assertNull(data.attachment(first.id()));
        assertEquals(sharedAttachment, data.attachment(shared.id()));
        assertEquals(thirdAttachment, data.attachment(third.id()));
        assertEquals(Set.of(shared.id(), third.id()), nodeIds(data));
        assertEquals(1, data.spans().size());
        assertEquals(lower.id(), data.spans().get(0).id());

        assertTrue(data.disconnectAndRemoveOrphanNodes(lower.id()));
        assertTrue(data.nodes().isEmpty());
        assertTrue(data.spans().isEmpty());
        assertNull(data.attachment(shared.id()));
        assertNull(data.attachment(third.id()));
    }

    @Test
    void readsAndFailedMutationsDoNotDirtyFreshData() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        UUID unknown = UUID.randomUUID();

        assertFalse(data.isDirty());
        assertTrue(data.nodes().isEmpty());
        assertTrue(data.spans().isEmpty());
        assertNull(data.attachment(unknown));
        assertFalse(data.removeNode(unknown));
        assertFalse(data.disconnect(UUID.randomUUID()));
        assertFalse(data.isDirty());
    }

    @Test
    void rejectsUnsupportedSchema() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", FixedRopeSavedData.SCHEMA_VERSION + 1);
        assertThrows(IllegalStateException.class, () -> FixedRopeSavedData.load(tag));
    }

    private static Set<UUID> nodeIds(FixedRopeSavedData data) {
        return data.nodes().stream().map(RopeNode::id).collect(Collectors.toSet());
    }
}
