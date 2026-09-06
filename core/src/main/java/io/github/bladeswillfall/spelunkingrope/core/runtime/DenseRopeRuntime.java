package io.github.bladeswillfall.spelunkingrope.core.runtime;

import io.github.bladeswillfall.spelunkingrope.core.geometry.CatenarySampler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DenseRopeRuntime {
    private static final int STATE_STRIDE = 7;
    private static final int X0 = 0;
    private static final int Y0 = 1;
    private static final int Z0 = 2;
    private static final int X1 = 3;
    private static final int Y1 = 4;
    private static final int Z1 = 5;
    private static final int LENGTH = 6;

    private final int segments;
    private final int coordinatesPerSpan;
    private final Map<UUID, Integer> slotById;

    private UUID[] spanIds;
    private double[] state;
    private double[] geometry;
    private int[] dirtySlots;
    private int[] dirtyQueueIndex;
    private int size;
    private int dirtyCount;

    public DenseRopeRuntime(int segments, int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        this.segments = segments;
        this.coordinatesPerSpan = CatenarySampler.coordinateCount(segments);
        this.slotById = new HashMap<>(Math.max(16, initialCapacity * 4 / 3 + 1));
        this.spanIds = new UUID[initialCapacity];
        this.state = new double[Math.multiplyExact(initialCapacity, STATE_STRIDE)];
        this.geometry = new double[Math.multiplyExact(initialCapacity, coordinatesPerSpan)];
        this.dirtySlots = new int[initialCapacity];
        this.dirtyQueueIndex = new int[initialCapacity];
        Arrays.fill(dirtyQueueIndex, -1);
    }

    public int size() {
        return size;
    }

    public int dirtyCount() {
        return dirtyCount;
    }

    public int segments() {
        return segments;
    }

    public int coordinatesPerSpan() {
        return coordinatesPerSpan;
    }

    public double[] geometryBuffer() {
        return geometry;
    }

    public int coordinateOffset(int slot) {
        requireSlot(slot);
        return slot * coordinatesPerSpan;
    }

    public UUID spanIdAt(int slot) {
        requireSlot(slot);
        return spanIds[slot];
    }

    public int slotOf(UUID spanId) {
        Integer slot = slotById.get(Objects.requireNonNull(spanId, "spanId"));
        return slot == null ? -1 : slot;
    }

    public int addSpan(
            UUID spanId,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double ropeLength
    ) {
        Objects.requireNonNull(spanId, "spanId");
        validateState(x0, y0, z0, x1, y1, z1, ropeLength);
        if (slotById.containsKey(spanId)) {
            throw new IllegalArgumentException("Duplicate runtime rope span: " + spanId);
        }

        ensureCapacity(size + 1);
        int slot = size++;
        spanIds[slot] = spanId;
        slotById.put(spanId, slot);
        writeState(slot, x0, y0, z0, x1, y1, z1, ropeLength);
        markSlotDirty(slot);
        return slot;
    }

    public boolean updateSpan(
            UUID spanId,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double ropeLength
    ) {
        validateState(x0, y0, z0, x1, y1, z1, ropeLength);
        int slot = requireSpan(spanId);
        int offset = slot * STATE_STRIDE;
        if (same(state[offset + X0], x0)
                && same(state[offset + Y0], y0)
                && same(state[offset + Z0], z0)
                && same(state[offset + X1], x1)
                && same(state[offset + Y1], y1)
                && same(state[offset + Z1], z1)
                && same(state[offset + LENGTH], ropeLength)) {
            return false;
        }

        writeState(slot, x0, y0, z0, x1, y1, z1, ropeLength);
        markSlotDirty(slot);
        return true;
    }

    public boolean markDirty(UUID spanId) {
        Integer slot = slotById.get(Objects.requireNonNull(spanId, "spanId"));
        if (slot == null) {
            return false;
        }
        return markSlotDirty(slot);
    }

    public boolean removeSpan(UUID spanId) {
        Integer slotValue = slotById.remove(Objects.requireNonNull(spanId, "spanId"));
        if (slotValue == null) {
            return false;
        }

        int slot = slotValue;
        removeDirtySlot(slot);
        int lastSlot = size - 1;
        if (slot != lastSlot) {
            UUID movedId = spanIds[lastSlot];
            spanIds[slot] = movedId;
            slotById.put(movedId, slot);
            System.arraycopy(state, lastSlot * STATE_STRIDE, state, slot * STATE_STRIDE, STATE_STRIDE);
            System.arraycopy(
                    geometry,
                    lastSlot * coordinatesPerSpan,
                    geometry,
                    slot * coordinatesPerSpan,
                    coordinatesPerSpan
            );

            int movedDirtyIndex = dirtyQueueIndex[lastSlot];
            if (movedDirtyIndex >= 0) {
                dirtySlots[movedDirtyIndex] = slot;
                dirtyQueueIndex[slot] = movedDirtyIndex;
            } else {
                dirtyQueueIndex[slot] = -1;
            }
            dirtyQueueIndex[lastSlot] = -1;
        }

        spanIds[lastSlot] = null;
        size = lastSlot;
        return true;
    }

    public int recomputeDirty() {
        int count = dirtyCount;
        for (int i = 0; i < count; i++) {
            int slot = dirtySlots[i];
            int stateOffset = slot * STATE_STRIDE;
            CatenarySampler.sample(
                    state[stateOffset + X0], state[stateOffset + Y0], state[stateOffset + Z0],
                    state[stateOffset + X1], state[stateOffset + Y1], state[stateOffset + Z1],
                    state[stateOffset + LENGTH],
                    segments,
                    geometry,
                    slot * coordinatesPerSpan
            );
            dirtyQueueIndex[slot] = -1;
        }
        dirtyCount = 0;
        return count;
    }

    private void writeState(
            int slot,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double ropeLength
    ) {
        int offset = slot * STATE_STRIDE;
        state[offset + X0] = x0;
        state[offset + Y0] = y0;
        state[offset + Z0] = z0;
        state[offset + X1] = x1;
        state[offset + Y1] = y1;
        state[offset + Z1] = z1;
        state[offset + LENGTH] = ropeLength;
    }

    private boolean markSlotDirty(int slot) {
        if (dirtyQueueIndex[slot] >= 0) {
            return false;
        }
        int queueIndex = dirtyCount++;
        dirtySlots[queueIndex] = slot;
        dirtyQueueIndex[slot] = queueIndex;
        return true;
    }

    private void removeDirtySlot(int slot) {
        int queueIndex = dirtyQueueIndex[slot];
        if (queueIndex < 0) {
            return;
        }

        int lastQueueIndex = --dirtyCount;
        int movedSlot = dirtySlots[lastQueueIndex];
        if (queueIndex != lastQueueIndex) {
            dirtySlots[queueIndex] = movedSlot;
            dirtyQueueIndex[movedSlot] = queueIndex;
        }
        dirtyQueueIndex[slot] = -1;
    }

    private void ensureCapacity(int minimum) {
        if (minimum <= spanIds.length) {
            return;
        }

        int oldCapacity = spanIds.length;
        int newCapacity = Math.max(minimum, Math.max(16, oldCapacity * 2));
        spanIds = Arrays.copyOf(spanIds, newCapacity);
        state = Arrays.copyOf(state, Math.multiplyExact(newCapacity, STATE_STRIDE));
        geometry = Arrays.copyOf(geometry, Math.multiplyExact(newCapacity, coordinatesPerSpan));
        dirtySlots = Arrays.copyOf(dirtySlots, newCapacity);
        dirtyQueueIndex = Arrays.copyOf(dirtyQueueIndex, newCapacity);
        Arrays.fill(dirtyQueueIndex, oldCapacity, newCapacity, -1);
    }

    private int requireSpan(UUID spanId) {
        int slot = slotOf(spanId);
        if (slot < 0) {
            throw new IllegalArgumentException("Unknown runtime rope span: " + spanId);
        }
        return slot;
    }

    private void requireSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("rope slot: " + slot);
        }
    }

    private static void validateState(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double ropeLength
    ) {
        if (!Double.isFinite(x0) || !Double.isFinite(y0) || !Double.isFinite(z0)
                || !Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(z1)
                || !Double.isFinite(ropeLength) || ropeLength <= 0.0) {
            throw new IllegalArgumentException("runtime rope state must be finite and rope length positive");
        }
    }

    private static boolean same(double left, double right) {
        return Double.doubleToLongBits(left) == Double.doubleToLongBits(right);
    }
}
