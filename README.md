# Spelunking Rope

> Working title for a multi-loader Minecraft ropework, rappelling, traversal, and winching mod.

## Vision

Spelunking Rope is intended to make hostile and vertical environments something players can **prepare, traverse, and turn into reusable infrastructure** rather than bypass with a single movement gadget.

The core gameplay loop is:

**explore → anchor → deploy → descend/cross → mark → haul → recover/reconfigure**

The system is spelunking-first, but its primitives should also fit industrial, expedition, rescue, adventure, and tactical/milsim modpacks without requiring separate themed mechanics.

## Planned capabilities

- Hanging and retrievable rope
- Pitons and reusable anchors
- Controlled climbing and rappelling
- Anchor-to-anchor rope spans
- Physical guide lines for route marking
- Gravity-driven ziplines and traversable lines
- Explicit pulleys/sheaves
- Manual winching and hauling
- Optional Create kinetic integration
- Future moving-anchor compatibility where practical

## Technical direction

- **Reference target:** Minecraft 1.20.1 Fabric
- **Secondary target from the start:** Minecraft 1.20.1 Forge
- **Future targets:** Fabric / NeoForge on newer Minecraft versions
- **Java:** 17 for the 1.20.1 line
- **Architecture:** multi-loader first, multi-version capable
- **Core rope simulation:** pure Java, with no `net.minecraft.*` dependency
- **Minecraft gameplay layer:** shared common module
- **Loader integrations:** thin Fabric / Forge adapters
- **Create:** optional compatibility, never a hard dependency
- **Networking:** server-authoritative gameplay state

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the detailed module design.

## Art direction

Development starts with vanilla-compatible placeholders where useful (lead-like rope, tripwire-hook-like piton silhouette), but final geometry should be established before mechanics depend on incorrect attachment positions.

The intended visual language is **Vanilla Utility**: vanilla survival equipment with restrained old-mining / climbing hardware influences. Readability and mechanical communication come before decorative detail.

See [docs/ART_DIRECTION.md](docs/ART_DIRECTION.md).

## Project status

**Foundation / pre-code.** Architecture, scope, terminology, module boundaries, and visual rules are being established before gameplay implementation begins.

## Repository modules

| Path | Responsibility |
| --- | --- |
| `core/` | Pure-Java rope graph, geometry, solvers, and traversal math |
| `common/` | Shared Minecraft-facing gameplay implementation |
| `fabric/` | Fabric-specific bootstrap and platform adapters |
| `forge/` | Forge-specific bootstrap and platform adapters |
| `integrations/create/` | Optional Create compatibility boundary |
| `gametest/` | Cross-loader gameplay/integration test scenarios |
| `docs/` | Architecture, design rules, roadmap, and ADRs |

## Non-goals for the first implementation

- A superhero-style grappling-hook movement mod
- Fully simulated particle/constraint rope physics
- Automatic rope wrapping around arbitrary world geometry
- Mandatory Create dependency
- Immediate support for every Minecraft/loader combination
- Large material/content catalogs before the rope system is proven

## License

No license has been selected yet. Until one is added, all rights are reserved by the repository owner.
