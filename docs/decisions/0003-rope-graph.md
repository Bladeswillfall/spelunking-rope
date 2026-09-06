# ADR 0003: Model rope as a graph, not rope blocks

**Status:** Accepted

## Decision

Persistent rope infrastructure will be represented as nodes connected by rope spans.

## Rationale

A graph naturally supports hanging ropes, anchor-to-anchor spans, guide lines, pulleys, winches, moving endpoints, network splitting, and hauling without replacing the architecture for each feature.

## Consequences

- Rope is rendered from runtime geometry rather than block chains.
- Explicit topology changes must be synchronized and persisted.
- World attachments resolve through stable logical references.
