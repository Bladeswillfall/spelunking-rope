package io.github.bladeswillfall.spelunkingrope.core.graph;

import java.util.Objects;
import java.util.UUID;

public record RopeNode(UUID id, Type type) {
    public enum Type {
        FIXED_ANCHOR,
        PULLEY
    }

    public RopeNode(UUID id) {
        this(id, Type.FIXED_ANCHOR);
    }

    public RopeNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }
}
