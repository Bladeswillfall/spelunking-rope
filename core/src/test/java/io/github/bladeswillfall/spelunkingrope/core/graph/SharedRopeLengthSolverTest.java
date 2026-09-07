package io.github.bladeswillfall.spelunkingrope.core.graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedRopeLengthSolverTest {
    @Test
    void transfersInBothDirectionsAndConservesTotal() {
        SharedRopeLengthSolver.Transfer toFirst = SharedRopeLengthSolver.transfer(8.0, 12.0, 5.0, 6.0, 3.0);
        assertEquals(11.0, toFirst.firstLength());
        assertEquals(9.0, toFirst.secondLength());
        assertEquals(3.0, toFirst.transferredToFirst());
        assertEquals(20.0, toFirst.firstLength() + toFirst.secondLength());

        SharedRopeLengthSolver.Transfer toSecond = SharedRopeLengthSolver.transfer(8.0, 12.0, 5.0, 6.0, -2.5);
        assertEquals(5.5, toSecond.firstLength());
        assertEquals(14.5, toSecond.secondLength());
        assertEquals(-2.5, toSecond.transferredToFirst());
        assertEquals(20.0, toSecond.firstLength() + toSecond.secondLength());
    }

    @Test
    void clampsAtEitherDonorMinimum() {
        SharedRopeLengthSolver.Transfer fromSecond = SharedRopeLengthSolver.transfer(8.0, 12.0, 5.0, 6.0, 100.0);
        assertEquals(14.0, fromSecond.firstLength());
        assertEquals(6.0, fromSecond.secondLength());
        assertEquals(6.0, fromSecond.transferredToFirst());

        SharedRopeLengthSolver.Transfer fromFirst = SharedRopeLengthSolver.transfer(8.0, 12.0, 5.0, 6.0, -100.0);
        assertEquals(5.0, fromFirst.firstLength());
        assertEquals(15.0, fromFirst.secondLength());
        assertEquals(-3.0, fromFirst.transferredToFirst());
    }

    @Test
    void zeroAndFullyClampedTransfersPreserveOriginalValues() {
        double first = 8.1;
        double second = 12.2;
        SharedRopeLengthSolver.Transfer zero = SharedRopeLengthSolver.transfer(first, second, 5.0, 6.0, 0.0);
        assertEquals(Double.doubleToRawLongBits(first), Double.doubleToRawLongBits(zero.firstLength()));
        assertEquals(Double.doubleToRawLongBits(second), Double.doubleToRawLongBits(zero.secondLength()));
        assertEquals(0.0, zero.transferredToFirst());

        SharedRopeLengthSolver.Transfer clamped = SharedRopeLengthSolver.transfer(first, 6.0, 5.0, 6.0, 1.0);
        assertEquals(Double.doubleToRawLongBits(first), Double.doubleToRawLongBits(clamped.firstLength()));
        assertEquals(Double.doubleToRawLongBits(6.0), Double.doubleToRawLongBits(clamped.secondLength()));
        assertEquals(0.0, clamped.transferredToFirst());
    }

    @Test
    void rejectsInvalidOrAlreadyImpossibleInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> SharedRopeLengthSolver.transfer(4.0, 8.0, 5.0, 6.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> SharedRopeLengthSolver.transfer(8.0, 5.0, 5.0, 6.0, -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> SharedRopeLengthSolver.transfer(8.0, 12.0, 0.0, 6.0, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> SharedRopeLengthSolver.transfer(8.0, 12.0, 5.0, 6.0, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> SharedRopeLengthSolver.transfer(Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 1.0, 0.0));
    }

    @Test
    void replacingSpanLengthPreservesIdentityAndTopology() {
        RopeNetwork network = new RopeNetwork();
        RopeNode first = network.addNode();
        RopeNode second = network.addNode();
        RopeSpan original = network.connect(first.id(), second.id(), 8.0);
        Set<java.util.UUID> firstIncident = incidentIds(network, first.id());
        Set<java.util.UUID> secondIncident = incidentIds(network, second.id());

        RopeSpan replacement = network.replaceSpanLength(original.id(), 11.5);

        assertEquals(original.id(), replacement.id());
        assertEquals(original.startNodeId(), replacement.startNodeId());
        assertEquals(original.endNodeId(), replacement.endNodeId());
        assertEquals(11.5, replacement.allocatedLength());
        assertEquals(2, network.nodes().size());
        assertEquals(1, network.spans().size());
        assertEquals(firstIncident, incidentIds(network, first.id()));
        assertEquals(secondIncident, incidentIds(network, second.id()));

        assertThrows(IllegalArgumentException.class, () -> network.replaceSpanLength(original.id(), 0.0));
        assertEquals(11.5, network.spans().get(0).allocatedLength());
    }

    private static Set<java.util.UUID> incidentIds(RopeNetwork network, java.util.UUID nodeId) {
        var ids = new ArrayList<java.util.UUID>();
        network.forEachIncidentSpanId(nodeId, ids::add);
        return Set.copyOf(ids);
    }
}
