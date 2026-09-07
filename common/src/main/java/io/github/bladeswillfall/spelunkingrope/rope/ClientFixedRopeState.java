package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.runtime.DenseRopeRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ClientFixedRopeState {
    public static final ClientFixedRopeState INSTANCE = new ClientFixedRopeState();

    private static final int ROPE_SEGMENTS = 16;
    private static final double VERTICAL_EPSILON = 1.0e-4;

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
        int endPointOffset = runtime.segments() * 3;

        // ponytail: O(n) only while the player is near/trying to grab a rope; add a spatial index if this becomes hot.
        for (int slot = 0; slot < runtime.size(); slot++) {
            int start = slot * coordinatesPerSpan;
            int end = start + endPointOffset;
            double x0 = geometry[start];
            double y0 = geometry[start + 1];
            double z0 = geometry[start + 2];
            double x1 = geometry[end];
            double y1 = geometry[end + 1];
            double z1 = geometry[end + 2];
            if (Math.abs(x0 - x1) > VERTICAL_EPSILON || Math.abs(z0 - z1) > VERTICAL_EPSILON) {
                continue;
            }

            AABB tube = new AABB(
                    Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1),
                    Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1)
            ).inflate(radius);
            if (tube.contains(from) || tube.contains(to) || tube.clip(from, to).isPresent()) {
                return runtime.spanIdAt(slot);
            }
        }
        return null;
    }

    public void clear() {
        dimension = null;
        runtime = new DenseRopeRuntime(ROPE_SEGMENTS, 0);
    }
}
