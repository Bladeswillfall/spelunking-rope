package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoBindingSavedDataTest {
    @Test
    void roundTripsBindingAndKeepsNoOpsClean() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan span = data.addRope(
                BlockAttachment.atWorld(0.5, 64.5, 0.5),
                BlockAttachment.atWorld(12.5, 64.5, 0.5),
                14.0
        );
        UUID entityId = UUID.randomUUID();
        data.setDirty(false);

        assertTrue(data.bindCargo(entityId, span.id(), 6.25));
        assertEquals(
                new FixedRopeSavedData.CargoBinding(entityId, span.id(), 6.25),
                data.cargoBinding(entityId)
        );

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertFalse(loaded.isDirty());
        assertEquals(
                new FixedRopeSavedData.CargoBinding(entityId, span.id(), 6.25),
                loaded.cargoBinding(entityId)
        );

        assertFalse(loaded.bindCargo(entityId, span.id(), 6.25));
        assertFalse(loaded.isDirty());
        assertTrue(loaded.bindCargo(entityId, span.id(), 7.0));
        assertTrue(loaded.isDirty());
    }

    @Test
    void schemaThreeLoadsWithoutCargoBindings() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan span = data.addRope(
                BlockAttachment.atWorld(0.5, 64.5, 0.5),
                BlockAttachment.atWorld(8.5, 64.5, 0.5),
                10.0
        );
        data.bindCargo(UUID.randomUUID(), span.id(), 4.0);
        CompoundTag tag = data.save(new CompoundTag());
        tag.putInt("schema_version", 3);

        FixedRopeSavedData loaded = FixedRopeSavedData.load(tag);

        assertTrue(loaded.cargoBindings().isEmpty());
        assertFalse(loaded.isDirty());
    }

    @Test
    void rejectsInvalidBindingsWithoutMutation() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan structural = data.addRope(
                BlockAttachment.atWorld(0.5, 64.5, 0.5),
                BlockAttachment.atWorld(8.5, 64.5, 0.5),
                10.0
        );
        RopeSpan guide = data.addGuideLine(
                BlockAttachment.atWorld(0.5, 63.5, 0.5),
                BlockAttachment.atWorld(8.5, 63.5, 0.5),
                10.0,
                FixedRopeSnapshot.NO_DYE
        );
        UUID entityId = UUID.randomUUID();
        data.setDirty(false);

        assertThrows(IllegalArgumentException.class,
                () -> data.bindCargo(entityId, UUID.randomUUID(), 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> data.bindCargo(entityId, guide.id(), 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> data.bindCargo(entityId, structural.id(), -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> data.bindCargo(entityId, structural.id(), Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> data.bindCargo(entityId, structural.id(), 11.0));

        assertTrue(data.cargoBindings().isEmpty());
        assertFalse(data.isDirty());
    }

    @Test
    void removingSpanRemovesItsCargoBindings() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan span = data.addRope(
                BlockAttachment.atWorld(0.5, 64.5, 0.5),
                BlockAttachment.atWorld(8.5, 64.5, 0.5),
                10.0
        );
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        data.bindCargo(first, span.id(), 2.0);
        data.bindCargo(second, span.id(), 7.0);
        data.setDirty(false);

        assertTrue(data.disconnect(span.id()));
        assertNull(data.cargoBinding(first));
        assertNull(data.cargoBinding(second));
        assertTrue(data.cargoBindings().isEmpty());
        assertTrue(data.isDirty());

        FixedRopeSavedData loaded = FixedRopeSavedData.load(data.save(new CompoundTag()));
        assertTrue(loaded.cargoBindings().isEmpty());
    }

    @Test
    void unbindIsIdempotent() {
        FixedRopeSavedData data = new FixedRopeSavedData();
        RopeSpan span = data.addRope(
                BlockAttachment.atWorld(0.5, 64.5, 0.5),
                BlockAttachment.atWorld(8.5, 64.5, 0.5),
                10.0
        );
        UUID entityId = UUID.randomUUID();
        data.bindCargo(entityId, span.id(), 3.0);
        data.setDirty(false);

        assertTrue(data.unbindCargo(entityId));
        assertTrue(data.isDirty());

        data.setDirty(false);
        assertFalse(data.unbindCargo(entityId));
        assertFalse(data.isDirty());
    }
}
