package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.runtime.DenseRopeRuntime;
import net.minecraft.resources.ResourceLocation;

public final class ClientFixedRopeState {
    public static final ClientFixedRopeState INSTANCE = new ClientFixedRopeState();

    private static final int ROPE_SEGMENTS = 16;

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

    public void clear() {
        dimension = null;
        runtime = new DenseRopeRuntime(ROPE_SEGMENTS, 0);
    }
}
