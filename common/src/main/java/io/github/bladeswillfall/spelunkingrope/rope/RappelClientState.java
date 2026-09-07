package io.github.bladeswillfall.spelunkingrope.rope;

import java.util.UUID;

public final class RappelClientState {
    public static final RappelClientState INSTANCE = new RappelClientState();
    private static final double FREE_END_EPSILON = 1.0e-4;

    private boolean active;
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
        spanId = state.spanId();
        anchorX = state.anchorX();
        anchorY = state.anchorY();
        anchorZ = state.anchorZ();
        currentLength = state.currentLength();
        maxLength = state.maxLength();
    }

    public void clear() {
        active = false;
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
        return active && currentLength >= maxLength - FREE_END_EPSILON;
    }

    public void updateMaxLength(UUID updatedSpanId, double updatedMaxLength) {
        if (active
                && spanId.equals(updatedSpanId)
                && Double.isFinite(updatedMaxLength)
                && updatedMaxLength >= currentLength) {
            maxLength = updatedMaxLength;
        }
    }

    public void advanceLength(byte vertical) {
        if (active) {
            currentLength = RappelServerController.adjustLength(currentLength, maxLength, vertical);
        }
    }
}
