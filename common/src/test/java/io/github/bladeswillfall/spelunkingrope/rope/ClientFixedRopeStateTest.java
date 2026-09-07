package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.traversal.PolylineTraversal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientFixedRopeStateTest {
    @AfterEach
    void clearClientState() {
        ClientFixedRopeState.INSTANCE.clear();
    }

    @Test
    void findsStructuralRopesButIgnoresGuideLinesAcrossSweptMotion() {
        UUID vertical = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        UUID guide = UUID.randomUUID();
        ClientFixedRopeState.INSTANCE.apply(new FixedRopeSnapshot(
                new ResourceLocation("minecraft", "overworld"),
                List.of(
                        new FixedRopeSnapshot.Span(
                                vertical,
                                BlockAttachment.atWorld(0.5, 10.5, 0.5),
                                BlockAttachment.atWorld(0.5, 0.5, 0.5),
                                10.0
                        ),
                        new FixedRopeSnapshot.Span(
                                route,
                                BlockAttachment.atWorld(5.5, 10.5, 0.5),
                                BlockAttachment.atWorld(10.5, 5.5, 0.5),
                                8.0
                        ),
                        new FixedRopeSnapshot.Span(
                                guide,
                                BlockAttachment.atWorld(15.5, 10.5, 0.5),
                                BlockAttachment.atWorld(15.5, 5.5, 0.5),
                                5.5,
                                FixedRopeSnapshot.TYPE_GUIDE,
                                FixedRopeSnapshot.NO_DYE
                        )
                )
        ));

        assertEquals(
                vertical,
                ClientFixedRopeState.INSTANCE.grabCandidate(
                        new Vec3(-2.0, 5.5, 0.5),
                        new Vec3(2.0, 5.5, 0.5),
                        0.3
                )
        );

        double[] routePoint = new double[PolylineTraversal.SAMPLE_OUTPUT_STRIDE];
        assertTrue(ClientFixedRopeState.INSTANCE.sampleSpan(route, 4.0, routePoint, 0));
        assertEquals(
                route,
                ClientFixedRopeState.INSTANCE.grabCandidate(
                        new Vec3(routePoint[0], routePoint[1], routePoint[2] - 1.0),
                        new Vec3(routePoint[0], routePoint[1], routePoint[2] + 1.0),
                        0.3
                )
        );

        assertEquals(FixedRopeSnapshot.TYPE_GUIDE, ClientFixedRopeState.INSTANCE.lineTypeAt(2));
        assertNull(ClientFixedRopeState.INSTANCE.grabCandidate(
                new Vec3(14.0, 8.0, 0.5),
                new Vec3(17.0, 8.0, 0.5),
                0.3
        ));
        assertNull(ClientFixedRopeState.INSTANCE.grabCandidate(
                new Vec3(3.0, 5.5, 3.0),
                new Vec3(3.0, 4.5, 3.0),
                0.3
        ));
    }
}
