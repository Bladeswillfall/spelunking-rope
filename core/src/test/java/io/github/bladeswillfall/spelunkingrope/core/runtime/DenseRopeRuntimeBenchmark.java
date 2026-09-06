package io.github.bladeswillfall.spelunkingrope.core.runtime;

import java.util.UUID;

public final class DenseRopeRuntimeBenchmark {
    private static final int SEGMENTS = 16;
    private static final int CLEAN_DRAINS = 100_000;

    private DenseRopeRuntimeBenchmark() {
    }

    public static void main(String[] args) {
        run(1_000, false);
        run(1_000, true);
        run(10_000, true);
    }

    private static void run(int spanCount, boolean report) {
        DenseRopeRuntime runtime = new DenseRopeRuntime(SEGMENTS, spanCount);
        for (int i = 0; i < spanCount; i++) {
            runtime.addSpan(
                    new UUID(3L, i + 1L),
                    0, 64, 0,
                    8, 64 + (i & 1) * 0.25, (i % 7) * 0.01,
                    10
            );
        }

        long dirtyStart = System.nanoTime();
        int recomputed = runtime.recomputeDirty();
        long dirtyNanos = System.nanoTime() - dirtyStart;
        if (recomputed != spanCount) {
            throw new AssertionError("expected " + spanCount + " dirty spans, got " + recomputed);
        }

        long cleanStart = System.nanoTime();
        int cleanWork = 0;
        for (int i = 0; i < CLEAN_DRAINS; i++) {
            cleanWork += runtime.recomputeDirty();
        }
        long cleanNanos = System.nanoTime() - cleanStart;
        if (cleanWork != 0) {
            throw new AssertionError("clean runtime unexpectedly recomputed " + cleanWork + " spans");
        }

        if (report) {
            System.out.printf(
                    "%d spans x %d segments: full dirty %.3f ms, clean drain %.1f ns/op%n",
                    spanCount,
                    SEGMENTS,
                    dirtyNanos / 1_000_000.0,
                    (double) cleanNanos / CLEAN_DRAINS
            );
        }
    }
}
