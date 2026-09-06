# ADR 0002: Pure-Java rope core

**Status:** Accepted

## Decision

Rope graph state, geometry, catenary solving, pulley/length logic, and traversal mathematics will live in a pure-Java `core` module without Minecraft dependencies.

## Rationale

These systems are mathematical/stateful rather than Minecraft-specific. Separating them makes them unit-testable and substantially reduces loader/version migration cost.

## Consequences

- `core` cannot import `net.minecraft.*`.
- Minecraft positions/entities must be converted into project-owned data forms at the boundary.
- Core algorithms can be tested without launching Minecraft.
