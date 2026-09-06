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
- [ ] Create compilable Fabric 1.20.1 + Forge 1.20.1 skeleton
- [ ] Set up CI matrix

## M1 — Rope engine

- [ ] Rope node/span data model
- [ ] Stable IDs and graph operations
- [ ] Two arbitrary world attachment points
- [ ] Catenary/sag solver
- [ ] Sampled rope path
- [ ] Persistent save/load
- [ ] Client/server synchronization
- [ ] Add/remove nodes
- [ ] Split/join rope networks
- [ ] Debug renderer
- [ ] Core unit tests

## M2 — Spelunking

- [ ] Rope coil/reel interaction
- [ ] Piton
- [ ] Reusable anchor
- [ ] Hanging rope deployment
- [ ] Climbing
- [ ] Controlled rappelling
- [ ] Rope retrieval
- [ ] Basic custom hardware models

## M3 — Route infrastructure

- [ ] Anchor-to-anchor rope placement
- [ ] Guide cord / guide clips
- [ ] Colourable guide lines
- [ ] Horizontal traversal
- [ ] Gravity-driven zipline traversal
- [ ] Carabiner/trolley visual language

## M4 — Rope engineering

- [ ] Explicit pulley/sheave node
- [ ] Shared total-rope-length solver
- [ ] Movable endpoint
- [ ] Manual winch
- [ ] Player/cargo hauling
- [ ] Persistence tests for routed systems

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
