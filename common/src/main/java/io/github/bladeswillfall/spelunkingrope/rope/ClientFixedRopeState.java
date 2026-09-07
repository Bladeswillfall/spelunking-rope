package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.runtime.DenseRopeRuntime;
import io.github.bladeswillfall.spelunkingrope.core.traversal.PolylineTraversal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ClientFixedRopeState {
    public static final ClientFixedRopeState INSTANCE = new ClientFixedRopeState();
    public static final int ROPE_SEGMENTS = 16;

    private ResourceLocation dimension;
    private DenseRopeRuntime runtime = new DenseRopeRuntime(ROPE_SEGMENTS, 0);

    private ClientFixedRopeState() {
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    public DenseRopeRuntime runtime() {
        return runtime;
    }

    public int apply(FixedRopeSnapshot snapshot) {
        DenseRopeRuntime next = new DenseRopeRuntime(ROPE_SEGMENTS, snapshot.spans().size());
        RappelClientState rappel = RappelClientState.INSTANCE;
        for (FixedRopeSnapshot.Span span : snapshot.spans()) {
            BlockAttachment start = span.start();
            BlockAttachment end = span.end();
            next.addSpan(
                    span.id(),
                    start.worldX(), start.worldY(), start.worldZ(),
                    end.worldX(), end.worldY(), end.worldZ(),
                    span.allocatedLength()
            );
            rappel.updateMaxLength(span.id(), span.allocatedLength());
        }
        int recomputed = next.recomputeDirty();
        dimension = snapshot.dimension();
        runtime = next;
        return recomputed;
    }

    public UUID grabCandidate(Vec3 from, Vec3 to, double radius) {
        if (!(radius > 0.0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }

        double[] geometry = runtime.geometryBuffer();
        int coordinatesPerSpan = runtime.coordinatesPerSpan();

        // ponytail: O(span count * 16) only while prompting/grabbing; add a spatial index if profiling makes this hot.
        for (int slot = 0; slot < runtime.size(); slot++) {
            int spanOffset = slot * coordinatesPerSpan;
            for (int segment = 0; segment < runtime.segments(); segment++) {
                int start = spanOffset + segment * 3;
                int end = start + 3;
                AABB tube = new AABB(
                        Math.min(geometry[start], geometry[end]),
                        Math.min(geometry[start + 1], geometry[end + 1]),
                        Math.min(geometry[start + 2], geometry[end + 2]),
                        Math.max(geometry[start], geometry[end]),
                        Math.max(geometry[start + 1], geometry[end + 1]),
                        Math.max(geometry[start + 2], geometry[end + 2])
                ).inflate(radius);
                if (tube.contains(from) || tube.contains(to) || tube.clip(from, to).isPresent()) {
                    return runtime.spanIdAt(slot);
                }
            }
        }
        return null;
    }

    public boolean sampleSpan(UUID spanId, double distance, double[] out, int outOffset) {
        int slot = runtime.slotOf(spanId);
        if (slot < 0) {
            return false;
        }
        PolylineTraversal.sample(
                runtime.geometryBuffer(),
                runtime.coordinateOffset(slot),
                runtime.segments() + 1,
                distance,
                out,
                outOffset
        );
        return true;
    }

    public void clear() {
        dimension = null;
        runtime = new DenseRopeRuntime(ROPE_SEGMENTS, 0);
    }
}
