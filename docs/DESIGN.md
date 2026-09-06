# Gameplay Design Principles

## Product identity

This mod is **expedition infrastructure**, not primarily a grappling-hook mod.

A movement gadget helps a player cross a ravine once. This mod should let the player turn the ravine into infrastructure that remains useful for return trips, teammates, cargo, and later mechanical systems.

## Player agency

Prefer systems that present options over arbitrary restrictions.

Examples:

- Zipline performance should emerge from slope, friction, and gravity rather than a narrow valid-angle rule.
- Rope should be recoverable and reusable rather than disposable by default.
- Guide-line colours should not have hard-coded meanings; players and modpacks can establish conventions.
- Create should enhance machinery, not gate the base functionality.

## Rope as a graph

The fundamental abstraction is a persistent network of nodes and rope spans, not placed rope blocks.

That one abstraction should eventually power:

- Hanging rope
- Rappelling
- Guide lines
- Anchor-to-anchor spans
- Ziplines
- Traversal lines
- Pulleys/sheaves
- Winches
- Hauling systems

## Explicit engineering beats hidden automation

For routed mechanical rope, prefer explicit pulleys/sheaves over automatic wrapping around arbitrary blocks.

Benefits:

- More readable player-built systems
- Easier persistence and collision rules
- Better multiplayer determinism
- Lower simulation complexity
- Stronger engineering gameplay

## Rope economy

Do not balance rope by making it annoyingly scarce.

Its meaningful advantages over ladders are that it can be deployed downward, suspended away from walls, routed between anchors, and recovered. Hardware is the more appropriate equipment investment.

Initial material philosophy:

- Rope/cord: economical and reusable
- Pitons: cheap expedition hardware
- Anchors: deliberate reusable infrastructure
- Pulleys: moderate mechanical investment
- Winches: substantial infrastructure

No mandatory wear/degradation is planned for the default experience.

## Guide lines

Guide lines are a first-class feature, not a cosmetic side mechanic.

They should support physical cave navigation through small clips/anchors and eventually dye/tint variants. Structural rope and guide cord should remain visually distinct.

## Tactical / milsim compatibility

Do not add separate military duplicates of generic mechanics. Keep primitives generic:

- Vertical rope can serve as a fast-rope
- Rappelling works for cave or tactical insertion
- Guide lines work in caves, tunnels, and structures
- Winches work for rescue, cargo, industry, or vehicles

Theme should emerge from context and resource packs rather than duplicated systems.
