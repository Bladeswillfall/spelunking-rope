# ADR 0006 — Build tooling and mappings for the 1.20.1 source line

- **Status:** Accepted
- **Date:** 2026-09-06
- **Related:** #2

## Context

The project must support Fabric and Forge on Minecraft 1.20.1 from the beginning while keeping the rope engine independent from Minecraft and loader APIs. The build must also leave a sensible route to later Minecraft source lines without turning loader compatibility into pervasive conditional code.

The main decisions are:

1. whether to use Architectury's build toolchain or maintain separate Fabric/Forge build systems ourselves;
2. which mappings namespace common Minecraft code should use;
3. how project modules and optional integrations are represented in Gradle;
4. where dependency versions are owned;
5. whether Architectury becomes a runtime dependency or only build infrastructure.

## Decision

### 1. Use Gradle with Architectury Plugin + Architectury Loom

The Minecraft-facing 1.20.1 modules will use the Architectury build toolchain so `common` can be compiled once against Minecraft and transformed/remapped into separate Fabric and Forge artifacts.

Architectury is a **build implementation detail**, not the project's architectural API.

We will not design gameplay around Architectury API classes. Loader differences remain behind project-owned platform service interfaces. Architectury API will not be a mandatory runtime dependency unless a later concrete requirement justifies it.

`@ExpectPlatform` is permitted only when it clearly reduces glue code; ordinary project-owned interfaces with loader implementations are preferred because they are easier to test, replace, and understand.

### 2. Use official Mojang mappings for the initial 1.20.1 line

All shared Minecraft-facing source uses Mojang names.

Parchment will **not** be layered into the initial mapping set. It can be reconsidered later if a verified toolchain combination gives us useful parameter names/Javadocs without remapping problems.

Reasons:

- Mojang mappings provide the same naming baseline across Fabric and Forge development.
- They reduce friction with Forge/Create-facing code and future migration away from Yarn.
- Parchment is quality-of-life metadata rather than a functional requirement.
- Avoiding layered mappings removes one source of remap/toolchain failure while the multiloader skeleton is being proven.

### 3. Keep `core` outside Loom

`core` is a normal Java 17 Gradle subproject and must not apply Loom or depend on Minecraft.

This invariant should be mechanically obvious from its build file and testable in CI.

### 4. Initial Gradle module graph

```text
root
├── core
├── common
├── fabric
├── forge
├── gametest
└── integrations
    └── create
        ├── common      (only if genuinely shared Create-facing code exists)
        ├── fabric
        └── forge
```

Dependency direction:

```text
fabric ─┐
        ├──> common ───> core
forge  ─┘

create-fabric ──> fabric/common/core APIs
create-forge  ──> forge/common/core APIs
```

The Create integration modules are **not** dependencies of the base Fabric/Forge artifacts. They compile only when the integration is being built/tested.

If there is no meaningful cross-loader Create code, `integrations/create/common` should not be created merely for symmetry.

### 5. Central version ownership

For the 1.20.1 line, versions that define the Minecraft build environment are owned at the repository root and referenced by subprojects:

- Minecraft version
- Java target
- Architectury plugin version
- Architectury Loom version
- Fabric Loader version
- Fabric API version
- Forge version
- mod ID / group / base artifact name / project version

No dynamic Gradle versions such as `1.7.+` are allowed in committed build files.

The exact compatible plugin/loader pins are established and smoke-tested in M0.2 (#3). Changing them later is a normal dependency update, not an architecture change unless it changes module boundaries or runtime requirements.

A Gradle version catalog may be introduced for ordinary libraries/test tooling once it provides real value. We will not add one solely to duplicate a small set of Minecraft build properties.

### 6. Produce separate loader artifacts

The project will publish distinct Fabric and Forge jars. We are not targeting a combined universal jar.

Artifact names must include enough information to distinguish loader and Minecraft source line.

### 7. Keep multi-version work outside the initial build matrix

The initial repository builds one Minecraft source line: 1.20.1.

Future 1.21.x support may introduce a second Minecraft-facing source line while retaining `core`. We will not introduce Stonecutter or widespread version preprocessors before the second source line actually exists.

## Consequences

### Positive

- Fabric and Forge are continuously built from the same gameplay source.
- `core` remains portable and unit-testable without Minecraft.
- Mojang names minimize loader-specific naming translation in shared code.
- Create remains optional and can fail independently of the base mod.
- The project is not forced to carry Architectury API as a user-facing dependency.
- Future Minecraft versions can diverge at the Minecraft-facing layer without rewriting the rope engine.

### Costs / risks

- Architectury Loom is another build dependency we must pin and maintain.
- Forge support still requires loader-specific bootstrap/event/networking work; Architectury does not remove that complexity.
- Mojang mappings provide less parameter/Javadoc information than Parchment.
- Optional Create integration needs loader-specific dependency declarations and testing.
- A later second Minecraft source line may require build restructuring; we intentionally defer that complexity until it exists.

## Rejected alternatives

### Separate Fabric Loom + ForgeGradle projects with manually shared sources

Rejected for the initial implementation. It gives maximum independence but duplicates more configuration and makes remapping/dependency parity our problem before we have gameplay code.

### Runtime Architectury API as the primary abstraction layer

Rejected. Our loader abstraction should belong to this project so gameplay code is not structurally coupled to a third-party runtime API.

### Yarn mappings in common

Rejected. Yarn would make Fabric the naming authority for shared Minecraft code and add unnecessary mapping friction on Forge and in future mapping transitions.

### Mojang + Parchment from day one

Deferred rather than permanently rejected. The additional documentation is useful, but it is not worth adding mapping-layer risk before both loader builds are proven.

### Stonecutter from day one

Rejected for now. It solves multi-version source management, but the project currently has only one Minecraft source line.
