# ADR 0005: Placeholder-first art with early geometry lock-in

**Status:** Accepted

## Decision

Prototype using vanilla-compatible placeholder assets, but replace them with crude custom models once physical attachment geometry matters. Final texture polish may wait until later milestones.

## Rationale

Placeholder art keeps development fast, while anchor eyes, pulley centers, and winch drums influence rope path geometry and cannot safely remain arbitrary until feature-complete.

## Consequences

- Lead-like rope and tripwire-hook-like pitons are acceptable prototypes.
- Final hardware silhouettes must be visually distinct from vanilla redstone/trap components.
- Attachment points become explicit model/gameplay data.
