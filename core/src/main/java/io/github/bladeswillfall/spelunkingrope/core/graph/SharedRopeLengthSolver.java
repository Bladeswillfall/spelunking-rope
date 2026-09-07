package io.github.bladeswillfall.spelunkingrope.core.graph;

public final class SharedRopeLengthSolver {
    private SharedRopeLengthSolver() {
    }

    public record Transfer(double firstLength, double secondLength, double transferredToFirst) {
    }

    public static Transfer transfer(
            double firstLength,
            double secondLength,
            double firstMinimum,
            double secondMinimum,
            double requestedToFirst
    ) {
        requireFinitePositive(firstLength, "firstLength");
        requireFinitePositive(secondLength, "secondLength");
        requireFinitePositive(firstMinimum, "firstMinimum");
        requireFinitePositive(secondMinimum, "secondMinimum");
        if (!Double.isFinite(requestedToFirst)) {
            throw new IllegalArgumentException("requestedToFirst must be finite");
        }
        if (firstLength < firstMinimum || secondLength < secondMinimum) {
            throw new IllegalArgumentException("current span allocation is shorter than its minimum");
        }

        double total = firstLength + secondLength;
        if (!Double.isFinite(total)) {
            throw new IllegalArgumentException("combined rope length must be finite");
        }

        double actual = Math.max(
                firstMinimum - firstLength,
                Math.min(requestedToFirst, secondLength - secondMinimum)
        );
        if (actual == 0.0) {
            return new Transfer(firstLength, secondLength, 0.0);
        }

        double newFirst;
        double newSecond;
        if (actual > 0.0) {
            newSecond = secondLength - actual;
            newFirst = total - newSecond;
        } else {
            newFirst = firstLength + actual;
            newSecond = total - newFirst;
        }

        double transferred = newFirst - firstLength;
        if (transferred == 0.0) {
            return new Transfer(firstLength, secondLength, 0.0);
        }
        return new Transfer(newFirst, newSecond, transferred);
    }

    private static void requireFinitePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
