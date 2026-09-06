# ADR 0004: Create is an optional integration

**Status:** Accepted

## Decision

The base mod will provide complete manual rope, pulley, and winch mechanics without Create. Create support will adapt its kinetic system onto project-owned mechanical interfaces.

## Rationale

The mod should remain useful across vanilla-style, Fabric, spelunking, tactical, and non-Create packs. It also reduces dependency and version churn risk.

## Consequences

- Base gameplay cannot require Create classes.
- Create integration lives behind an optional compatibility boundary.
- The same conceptual winch should work manually and, when available, mechanically.
