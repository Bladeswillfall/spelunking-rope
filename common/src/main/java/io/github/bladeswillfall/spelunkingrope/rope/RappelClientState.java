package io.github.bladeswillfall.spelunkingrope.rope;

import java.util.UUID;

public final class RappelClientState {
    public static final RappelClientState INSTANCE = new RappelClientState();
    private static final double FREE_END_EPSILON = 1.0e-4;

    private boolean active;
    private byte mode;
    private UUID spanId;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private double currentLength;
    private double maxLength;

    private RappelClientState() {
    }

    public void apply(RappelPackets.State state) {
        if (!state.active()) {
            clear();
            return;
        }
        active = true;
        mode = state.mode();
        spanId = state.spanId();
        anchorX = state.anchorX();
        anchorY = state.anchorY();
        anchorZ = state.anchorZ();
        currentLength = state.currentLength();
        maxLength = state.maxLength();
    }

    public void clear() {
        active = false;
        mode = 0;
        spanId = null;
        anchorX = 0.0;
        anchorY = 0.0;
        anchorZ = 0.0;
        currentLength = 0.0;
        maxLength = 0.0;
    }

    public boolean active() {
        return active;
    }

    public boolean rappelling() {
        return active && mode == RappelPackets.MODE_RAPPEL;
    }

    public boolean traversing() {
        return active && mode == RappelPackets.MODE_TRAVERSE;
    }

    public UUID spanId() {
        return spanId;
    }

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public double anchorZ() {
        return anchorZ;
    }

    public double currentLength() {
        return currentLength;
    }

    public double maxLength() {
        return maxLength;
    }

    public boolean atFreeEnd() {
        return rappelling() && currentLength >= maxLength - FREE_END_EPSILON;
    }

    public void updateMaxLength(UUID updatedSpanId, double updatedMaxLength) {
        if (rappelling()
                && spanId.equals(updatedSpanId)
                && Double.isFinite(updatedMaxLength)
                && updatedMaxLength >= currentLength) {
            maxLength = updatedMaxLength;
        }
    }

    public void advanceLength(byte vertical) {
        if (rappelling()) {
            currentLength = RappelServerController.adjustLength(currentLength, maxLength, vertical);
        }
    }

    public void setTraverseDistance(double distance) {
        if (traversing() && Double.isFinite(distance)) {
            currentLength = Math.max(0.0, Math.min(maxLength, distance));
        }
    }
}
