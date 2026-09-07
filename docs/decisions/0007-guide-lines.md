# 0007 — Guide lines

## Decision

Guide lines reuse the existing persistent rope graph for geometry, but their gameplay/rendering metadata stays outside the pure-Java graph.

- A guide line is a normal span plus saved/synchronized metadata identifying it as non-structural and carrying an optional Minecraft dye colour.
- Structural spans remain the default for existing worlds and APIs.
- Guide clips are small wall-mounted, non-load-bearing anchors dedicated to guide cord. Structural rope cannot use them, and guide lines cannot be grabbed, rappelled, or used as ziplines.
- Using guide cord on one guide clip selects it; using the same stack on another clip in the same dimension creates one span and consumes one cord outside creative mode.
- The existing 32-block route-placement ceiling and catenary/slack calculation are reused.
- Sneak + empty-hand use on a guide clip retrieves every guide span attached to that clip and refunds one guide cord per removed span. This keeps junction recovery deterministic without adding a line-selection UI.
- A selected stack is visibly enchanted/foiled, matching structural route placement feedback.
- Guide lines render thinner and visually distinct from structural rope. Colour is player-defined and has no built-in semantic meaning.

## Persistence and networking

Minecraft-facing saved data stores guide metadata keyed by span UUID. The core `RopeSpan` remains only topology + allocated length.

Schema v2 accepts v1 data and treats all legacy spans as structural. Full snapshots carry guide/type metadata; Forge protocol is bumped when that packet layout changes.

## Deferred

- No clip inventory, junction UI, labels, names, arrows, routefinding, or semantic colour rules.
- No dynamic line-body physics.
- No separate guide-line graph/runtime; the existing dense rope runtime remains the geometry source.
