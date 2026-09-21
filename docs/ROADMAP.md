# Roadmap

The roadmap is intentionally ordered around proving the reusable rope engine before adding content breadth.

## M0 — Foundation

- [x] Establish multi-loader-first architecture
- [x] Establish module boundaries
- [x] Establish art direction and prototype policy
- [x] Define Create as optional integration
- [x] Define server-authoritative networking policy
- [x] Define persistent-data versioning requirement
- [x] Select build tooling / mappings strategy
- [x] Create compilable Fabric 1.20.1 + Forge 1.20.1 skeleton
- [x] Set up CI matrix

## M1 — Rope engine

- [x] Rope node/span data model
- [x] Stable IDs and graph operations
- [x] Incident-span index for dirty updates
- [x] Authoritative allocated span length
- [x] Fixed arbitrary world attachment points
- [x] Catenary/sag solver
- [x] Sampled rope path
- [x] Persistent save/load
- [x] Dense dirty-span runtime and 1k/10k benchmark
- [x] Fixed-rope client/server snapshot synchronization
- [x] Add/remove nodes
- [x] Split/join rope networks
- [x] Debug renderer
- [x] Core unit tests

## M2 — Spelunking

- [x] Rope coil/reel interaction
- [x] Piton
- [x] Reusable anchor
- [x] Hanging rope deployment
- [x] Climbing
- [x] Controlled rappelling
- [x] Rope retrieval
- [x] Basic custom hardware models

### M2.6 — Lead climbing / piton wall scaling

- [ ] Add wall-climbing traversal as an extension of anchored rope traversal rather than free wall-crawling
- [ ] Allow piton placement up to 5 blocks away while climbing, requiring a valid solid face and unobstructed line of sight
- [ ] Keep upward climbing progression limited to approximately 2–2.5 blocks beyond the current active protection point
- [ ] Do not promote a remotely placed piton to active protection immediately; require the player to reach/clip it at close range (target approximately 1–1.5 blocks, subject to playtesting)
- [ ] On loss of wall contact, transition into the existing rope fall/swing behavior and catch from the most recent active piton
- [ ] Support lateral route-finding and later overhang traversal without introducing a separate stamina or ladder-style climbing system
- [ ] Preserve completed routes as reusable piton/rope infrastructure for later players
- [ ] Add focused multiplayer and collision tests for placement reach, protection promotion, fall catches, and wall/ledge edge cases

## M3 — Route infrastructure

- [x] Anchor-to-anchor rope placement
- [x] Guide cord / guide clips
- [x] Colourable guide lines
- [x] Horizontal traversal
- [x] Gravity-driven zipline traversal
- [x] Carabiner/trolley visual language

## M4 — Rope engineering

- [x] Explicit pulley/sheave node
- [x] Shared total-rope-length solver
- [x] Movable endpoint
- [x] Manual winch
- [x] Player hauling
- [ ] Cargo hauling
- [x] Persistence tests for routed systems

## M5 — Ecosystem integrations

- [ ] Create kinetic winch adapter
- [ ] Create stress/rotation behavior as appropriate
- [ ] Evaluate moving Create contraption anchors
- [ ] Rope/material extension API
- [ ] Optional interoperability with other rope/material mods

## Deferred until justified

- Automatic rope wrapping around arbitrary blocks
- Fully simulated rope-body physics
- Broad material catalog
- Conventional grappling hook
- Large multi-version release matrix
