package io.github.bladeswillfall.spelunkingrope.core.graph;

import java.util.Objects;
import java.util.UUID;

public record RopeNode(UUID id) {
    public RopeNode {
        Objects.requireNonNull(id, "id");
    }
}
