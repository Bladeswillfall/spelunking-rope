# Architecture

## Goals

1. Treat multi-loader support as a foundational constraint.
2. Keep the rope simulation independent from Minecraft and loader APIs.
3. Make Fabric 1.20.1 the reference implementation without becoming Fabric-specific.
4. Keep Forge 1.20.1 building from the same shared gameplay code.
5. Make newer Minecraft versions a source-compatibility problem, not a loader-abstraction problem.
6. Keep Create and other ecosystem integrations optional and replaceable.

## Dependency direction

```text
fabric ─┐
        ├──> common ───> core
forge  ─┘

integrations/create ───> common/core APIs
```

Dependencies must not point back toward loader-specific modules.

## Module responsibilities

### `core`

Pure Java. It must not import `net.minecraft.*`, Fabric, Forge, NeoForge, Architectury, or Create APIs.

Expected responsibilities:

- Rope graph/network model
- Node and span identifiers
- Catenary / sag geometry
- Sampled rope paths
- Rope-length accounting
- Pulley/sheave solving
- Traversal projection math
- Zipline acceleration/friction calculations
- Serialization DTOs and schema migration primitives
- Unit tests for all of the above

Conceptually:

```text
RopeNetwork
├── RopeNode
│   ├── FIXED_ANCHOR
│   ├── FREE_END
│   ├── PULLEY
│   ├── WINCH
│   ├── MOVING_ANCHOR
│   └── PLAYER_ATTACHMENT
└── RopeSpan
    ├── start node
    ├── end node
    ├── allocated length
    └── material key
```

### `common`

Shared Minecraft-facing implementation for a single Minecraft source line.

Expected responsibilities:

- Blocks and items
- World rope manager
- Persistent world data
- Anchor attachment resolution
- Player interaction logic
- Server-authoritative traversal
- Packets expressed through project networking abstractions
- Shared renderer/mesh preparation where vanilla APIs allow it
- Commands/debugging tools
- Data/resource definitions shared across loaders

Vanilla Minecraft types such as `Block`, `Level`, `Player`, and `Vec3` are allowed here. Loader APIs are not.

### `fabric`

Thin Fabric adapter:

- Entry points
- Registration glue
- Fabric networking/event hooks
- Fabric config/bootstrap differences
- Client initialization hooks where required

No rope gameplay rules should live here.

### `forge`

Thin Forge adapter with the same boundaries as `fabric`.

### `integrations/create`

Optional Create integration only.

Potential responsibilities:

- Drive a project winch from Create rotational power
- Expose kinetic/stress behavior where appropriate
- Resolve Create-specific moving attachment references
- Animate mechanically driven components

The base mod must remain complete and playable without this module.

## Platform services

Only genuinely loader-specific behavior should be abstracted.

Good candidates:

```text
PlatformEnvironment
PlatformRegistration
PlatformNetworking
PlatformEvents
PlatformConfig
```

Avoid wrapping stable vanilla concepts merely for abstraction's sake. We explicitly do **not** want project versions of `Block`, `Item`, `Player`, `Level`, or `Vec3`.

## Runtime performance model

Persistent/topological rope state and hot derived geometry are separate concerns.

- UUID-backed nodes/spans are stable identity and save/network state, not the hot simulation layout.
- Sampled paths are derived runtime data and must not live in the persistent graph model.
- Static spans have zero per-tick geometry work; endpoint/length changes mark only affected spans dirty.
- Hot path geometry uses reusable primitive buffers rather than one object per sample.
- Clients reconstruct ordinary rope curves from authoritative endpoints/length instead of receiving sampled point lists.
- Client and server runtime managers remain separate; shared block/entity ticks must not execute side-specific rope logic.
- Dense integer-indexed runtime storage may be introduced when moving endpoints/traversal justify it; UUID map lookups stay off hot inner loops.
- Multithreading is deferred until profiling shows dirty-geometry work is large enough to repay synchronization/task overhead.

## Multi-version strategy

Multi-loader and multi-version support are separate axes.

Initial line:

```text
1.20.1
├── Fabric (reference)
└── Forge  (supported)
```

Future source lines may look like:

```text
core
├── common-1.20.1
│   ├── fabric-1.20.1
│   └── forge-1.20.1
└── common-1.21.x
    ├── fabric-1.21.x
    └── neoforge-1.21.x
```

The project should not accumulate widespread version conditionals inside gameplay code. When Minecraft APIs meaningfully diverge, the Minecraft-facing source line may diverge while `core` remains shared.

## Persistent data

Rope state should serialize stable references, not live Minecraft objects.

Example conceptual data:

```text
RopeNodeData
├── UUID id
├── logical position
├── attachment reference
└── node properties
```

Attachment references may eventually include:

- Fixed block attachment
- Entity attachment
- Moving contraption attachment
- Free endpoint

World resolution happens in `common` at runtime.

Saved rope data must include an explicit schema version from the first persistent implementation.

## Networking authority

The server owns:

- Rope topology
- Rope lengths
- Attachment validity
- Traversal state
- Player movement resulting from rope interaction
- Winch/pulley state changes

The client owns presentation and sends intent/input. This is necessary for multiplayer consistency and to avoid movement exploits.

## Rendering

Rope rendering is runtime geometry, not a chain of rope blocks.

Pipeline:

```text
node positions
    ↓
core curve solver
    ↓
sampled points
    ↓
common mesh preparation
    ↓
client renderer hook
```

Anchor models expose physically meaningful attachment points so rope geometry begins at the eye/sheave rather than a block center.
