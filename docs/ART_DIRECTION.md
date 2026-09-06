# Art Direction

## Working style: Vanilla Utility

The mod should look plausibly at home beside vanilla Minecraft while using restrained mining, climbing, and mechanical-equipment influences.

Target qualities:

- Vanilla-compatible pixel density
- Readable silhouettes
- Dark iron / steel-like hardware
- Tan natural-fibre rope as the baseline
- Restrained wood + iron machinery
- Mechanical state communicated through geometry/animation
- No hyper-realistic climbing equipment
- No pervasive brass/cog styling that makes the mod visually dependent on Create
- No overt military styling in generic base items

## Prototype art

Vanilla-compatible placeholders are encouraged while mechanics are being proven.

Examples:

- Lead-inspired rope texture/palette
- Tripwire-hook-like temporary piton model
- Simple iron wheel for pulley prototypes
- Crude barrel/spool silhouette for winches
- Debug lines for early rope rendering

These are greybox assets, not final visual identity.

## Important rule: geometry before polish

Do not postpone representative model geometry until feature-complete.

Rope endpoints depend on physical attachment positions. A piton eye, anchor ring, pulley sheave, or winch drum may affect rope paths and therefore gameplay/rendering code.

By mechanical alpha, custom models should have roughly final proportions and attachment points even if textures remain crude.

## Equipment hierarchy

### Piton

Small, cheap expedition anchor. Minimal iron spike/eye silhouette. Should not retain the final silhouette of a tripwire hook because that reads as redstone/trap equipment.

### Anchor

More deliberate reusable fixture. Visually distinct from the piton and readable on walls, floors, or ceilings where supported.

### Pulley / sheave

The rope path should visibly relate to the wheel. Avoid decorative gearing that obscures the actual mechanical function.

### Winch

Readable drum/spool plus crank/drive point. The base visual should work standalone; Create integration may animate or power the same conceptual machine.

## Rope rendering

The rope itself is the major non-block visual element.

- Runtime-rendered curve
- Apparent thickness roughly consistent with vanilla leads at gameplay distance
- Avoid visually noisy braided-strand detail
- Sag/tension should communicate state without HUD text

A slack span and a tensioned span should visibly differ through geometry.

## Structural rope vs guide cord

Keep them visually related but distinct.

- Structural rope: thicker, load-bearing visual
- Guide cord: thin route-marking line

Future colour variants should use Minecraft's dye language where feasible, without assigning mandatory semantic meanings to colours.

## Animation

Animate only when movement communicates state.

Good candidates:

- Pulley rotation while rope moves
- Winch drum rotation
- Manual crank motion
- Zipline trolley wheel motion
- Subtle loose-rope movement

Avoid constant decorative motion on static hardware.

## Art production stages

1. **Prototype:** vanilla placeholders and debug geometry
2. **Mechanical alpha:** crude custom models with correct dimensions/attachment points
3. **Gameplay alpha:** baseline 16× textures and clear silhouettes
4. **Beta:** cohesive palette, animation, particles, sound pass
5. **Release candidate:** final item sprites, polish, documentation imagery
