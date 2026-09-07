package io.github.bladeswillfall.spelunkingrope.rope;

import java.util.Objects;

final class GuideCordColors {
    private GuideCordColors() {
    }

    static int closestPaletteIndex(int rgb, int[] palette) {
        Objects.requireNonNull(palette, "palette");
        if (palette.length == 0) {
            throw new IllegalArgumentException("palette must not be empty");
        }

        int closest = 0;
        long closestDistance = Long.MAX_VALUE;
        for (int index = 0; index < palette.length; index++) {
            int candidateRgb = palette[index];
            long dr = ((rgb >> 16) & 0xFF) - ((candidateRgb >> 16) & 0xFF);
            long dg = ((rgb >> 8) & 0xFF) - ((candidateRgb >> 8) & 0xFF);
            long db = (rgb & 0xFF) - (candidateRgb & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = index;
            }
        }
        return closest;
    }
}
