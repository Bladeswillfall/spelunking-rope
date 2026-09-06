package io.github.bladeswillfall.spelunkingrope.core.graph;

import java.util.Objects;
import java.util.UUID;

public record RopeSpan(UUID id, UUID startNodeId, UUID endNodeId, double allocatedLength) {
    public RopeSpan {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(startNodeId, "startNodeId");
        Objects.requireNonNull(endNodeId, "endNodeId");
        if (startNodeId.equals(endNodeId)) {
            throw new IllegalArgumentException("A rope span must connect two different nodes");
        }
        if (!Double.isFinite(allocatedLength) || allocatedLength <= 0.0) {
            throw new IllegalArgumentException("allocatedLength must be finite and positive");
        }
    }
}
