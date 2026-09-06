# Contributing

The project is currently in foundation/pre-code status.

## Architectural rules

- Keep `core` free of Minecraft and loader APIs.
- Keep loader APIs out of `common`.
- Treat Create as optional integration only.
- Prefer deterministic/server-authoritative gameplay state.
- Add an ADR under `docs/decisions/` for decisions that materially change module boundaries, persistent data, networking authority, or public extension APIs.
- Avoid adding content breadth before the rope engine is proven.

## Art rules

- Prototype art may reuse vanilla-compatible visual language.
- Readable silhouettes and mechanically correct attachment geometry come before detail.
- Avoid styling generic base equipment so strongly that it only suits Create or military packs.
