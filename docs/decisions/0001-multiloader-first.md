# ADR 0001: Multi-loader from the first implementation

**Status:** Accepted

## Decision

The first implementation will target Minecraft 1.20.1 Fabric as the reference environment and Minecraft 1.20.1 Forge as a supported loader from the same shared codebase.

Loader-specific APIs must remain behind thin adapter modules.

## Rationale

The intended modpack use case requires Fabric 1.20.1, while supporting Forge materially increases reuse and prevents foundational Fabric coupling. The rope simulation itself has little reason to depend on a loader.

## Consequences

- Common gameplay code cannot import Fabric/Forge APIs.
- CI should eventually compile/test both targets.
- Loader-specific convenience code is acceptable only inside adapter modules.
