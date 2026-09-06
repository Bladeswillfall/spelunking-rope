package io.github.bladeswillfall.spelunkingrope.core.runtime;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DenseRopeRuntimeTest {
    @Test
    void tenThousandCleanSpansDoNoGeometryWork() {
        int count = 10_000;
        DenseRopeRuntime runtime = new DenseRopeRuntime(4, count);
        UUID target = null;

        for (int i = 0; i < count; i++) {
            UUID id = new UUID(0L, i + 1L);
            runtime.addSpan(id, 0, 64, 0, 8, 64, 0, 10);
            if (i == count / 2) {
                target = id;
            }
        }

        double[] geometry = runtime.geometryBuffer();
        assertEquals(count, runtime.recomputeDirty());
        assertSame(geometry, runtime.geometryBuffer());
        assertEquals(0, runtime.recomputeDirty());

        assertTrue(runtime.markDirty(target));
        assertFalse(runtime.markDirty(target));
        assertEquals(1, runtime.dirtyCount());
        assertEquals(1, runtime.recomputeDirty());
        assertEquals(0, runtime.recomputeDirty());
    }

    @Test
    void updatesQueueOnlyActualChanges() {
        DenseRopeRuntime runtime = new DenseRopeRuntime(4, 1);
        UUID id = new UUID(1L, 1L);
        runtime.addSpan(id, 0, 0, 0, 8, 0, 0, 10);
        runtime.recomputeDirty();

        assertFalse(runtime.updateSpan(id, 0, 0, 0, 8, 0, 0, 10));
        assertEquals(0, runtime.dirtyCount());
        assertTrue(runtime.updateSpan(id, 0, 0, 0, 8, 1, 0, 10));
        assertEquals(1, runtime.dirtyCount());
        assertFalse(runtime.markDirty(id));
        assertEquals(1, runtime.recomputeDirty());
    }

    @Test
    void swapRemoveKeepsDenseLookupGeometryAndDirtyQueueCorrect() {
        DenseRopeRuntime runtime = new DenseRopeRuntime(2, 3);
        UUID first = new UUID(2L, 1L);
        UUID middle = new UUID(2L, 2L);
        UUID last = new UUID(2L, 3L);
        runtime.addSpan(first, 0, 0, 0, 8, 0, 0, 10);
        runtime.addSpan(middle, 10, 0, 0, 18, 0, 0, 10);
        runtime.addSpan(last, 20, 0, 0, 28, 0, 0, 10);
        runtime.recomputeDirty();

        runtime.markDirty(middle);
        runtime.markDirty(last);
        assertTrue(runtime.removeSpan(middle));

        assertEquals(2, runtime.size());
        assertEquals(-1, runtime.slotOf(middle));
        assertEquals(1, runtime.slotOf(last));
        assertEquals(last, runtime.spanIdAt(1));
        assertEquals(1, runtime.dirtyCount());
        assertEquals(1, runtime.recomputeDirty());

        int offset = runtime.coordinateOffset(1);
        double[] geometry = runtime.geometryBuffer();
        assertEquals(20.0, geometry[offset], 0.0);
        assertEquals(28.0, geometry[offset + runtime.coordinatesPerSpan() - 3], 0.0);
    }
}
