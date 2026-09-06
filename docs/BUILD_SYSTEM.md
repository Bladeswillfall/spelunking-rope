# Build System

This document records how the 1.20.1 source line should be bootstrapped. It implements the decision in [ADR 0006](decisions/0006-build-tooling-and-mappings.md).

## Initial source line

```text
Minecraft 1.20.1
Java 17
├── Fabric (reference)
└── Forge  (supported)
```

The first goal is not feature code. The first goal is proving that the same shared Minecraft-facing code can boot on both loaders while `core` remains ordinary Java.

## Tooling

- Gradle wrapper, exact version pinned in the repository
- Architectury Plugin
- Architectury Loom for Minecraft-facing modules
- Mojang official mappings
- JUnit for pure-Java `core` tests

Architectury API is not assumed to be a runtime dependency.

## Module ownership

### `core`

Normal Java 17 module.

Must not apply Loom or declare Minecraft/Fabric/Forge/Create dependencies.

### `common`

Applies Architectury Loom and compiles shared Minecraft-facing code using Mojang mappings.

May depend on `core`.

Must not import Fabric, Forge, or Create APIs.

### `fabric`

Fabric bootstrap and adapter module.

Depends on shared production output from `common` and on `core` transitively through common where appropriate.

Owns:

- Fabric Loader dependency
- Fabric API dependency
- `fabric.mod.json`
- Fabric entry points
- Fabric-specific event/network registration adapters

### `forge`

Forge bootstrap and adapter module.

Depends on shared production output from `common`.

Owns:

- Forge dependency
- `mods.toml`
- Forge entry point
- Forge-specific event/network registration adapters

### `gametest`

Test harness for Minecraft-level behavioural tests once M1 begins.

It should not become a dumping ground for implementation code. Pure graph/geometry/solver tests stay in `core`.

### `integrations/create/*`

Optional build targets only.

Create compile-time dependencies must never be declared in `common`, `fabric`, or `forge` merely to make integration code convenient.

The preferred future shape is:

```text
integrations/create/
├── common/   # optional; only if code truly works against both Create distributions
├── fabric/
└── forge/
```

Base loader jars must build and run when the entire `integrations/create` tree is absent.

## Version ownership

Minecraft build-environment versions are repository-root properties.

Expected root properties include conceptually:

```properties
minecraft_version=1.20.1
java_version=17
architectury_plugin_version=<pinned>
architectury_loom_version=<pinned>
fabric_loader_version=<pinned>
fabric_api_version=<pinned>
forge_version=<pinned>
mod_version=<pinned project version>
maven_group=<project package group>
archives_base_name=spelunking-rope
```

The exact property names may be adjusted during bootstrap, but ownership should not move into individual loader modules.

Rules:

1. no dynamic `+` versions in committed build files;
2. loader modules read centrally owned versions;
3. optional integration versions live with the integration or in a clearly named root section, never mixed into base-mod requirements;
4. dependency bumps should be isolated enough to review what changed;
5. CI must build with the checked-in Gradle wrapper rather than an ambient Gradle installation.

## Mappings

Use:

```groovy
mappings loom.officialMojangMappings()
```

Do not enable layered Parchment mappings during the first bootstrap.

Parchment may be reconsidered after both Fabric and Forge builds, IDE sources, remapping, and optional Create development dependencies are demonstrably stable.

## Platform abstraction

Shared code should depend on project-owned interfaces such as:

```text
PlatformEnvironment
PlatformRegistration
PlatformNetworking
PlatformEvents
PlatformConfig
```

Do not create wrappers for ordinary Minecraft concepts such as `Block`, `Item`, `Player`, `Level`, or `Vec3`.

The bootstrap implementation may use either explicit loader initialization of service implementations or a small service-discovery mechanism. The choice should favour debuggability over cleverness.

## Artifact policy

The build produces separate artifacts:

```text
spelunking-rope-<version>+fabric-1.20.1.jar
spelunking-rope-<version>+forge-1.20.1.jar
```

Exact naming can be refined during M0.2, but loader identity must be obvious.

A universal combined jar is not a target.

## Bootstrap acceptance test

M0.2 is complete only when all of the following are true:

1. `core` compiles and tests without Minecraft on its classpath.
2. Fabric client launches.
3. Fabric dedicated server launches far enough to initialize the mod.
4. Forge client launches.
5. Forge dedicated server launches far enough to initialize the mod.
6. Both loaders execute one shared `common` initialization path.
7. No Fabric/Forge imports exist in `common`.
8. No Minecraft imports exist in `core`.
9. No Create dependency is required.
10. `./gradlew build` succeeds from a clean checkout.

Only after this passes should M1 rope-engine implementation begin.

## Future versions

Do not add Stonecutter or a preprocessor until there is a real second Minecraft source line.

When 1.21.x work starts, the preferred conceptual boundary remains:

```text
core
├── common-1.20.1
│   ├── fabric-1.20.1
│   └── forge-1.20.1
└── common-1.21.x
    ├── fabric-1.21.x
    └── neoforge-1.21.x
```

Whether that becomes branches, source sets, or a multi-version build is a later decision based on the actual amount of source divergence.
