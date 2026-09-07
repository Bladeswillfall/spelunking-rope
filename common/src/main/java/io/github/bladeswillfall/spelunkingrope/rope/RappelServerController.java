package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.geometry.CatenarySampler;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import io.github.bladeswillfall.spelunkingrope.core.traversal.PolylineTraversal;
import io.github.bladeswillfall.spelunkingrope.core.traversal.RappelConstraint;
import io.github.bladeswillfall.spelunkingrope.core.traversal.ZiplineMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class RappelServerController {
    private static final double MIN_LENGTH = 1.25;
    private static final double CLIMB_PER_TICK = 0.12;
    private static final double DESCEND_PER_TICK = 0.18;
    static final double TRAVERSE_HANG_OFFSET = 1.25;
    private static final double CORRECTION_TOLERANCE = 0.35;
    private static final double TRAVERSE_CORRECTION_TOLERANCE = 0.20;
    private static final double ANCHOR_EPSILON = 1.0e-6;
    private static final double FREE_END_EPSILON = 1.0e-4;
    private static final double GRAB_RADIUS = 1.15;
    private static final double GRAB_VERTICAL_EPSILON = 1.0e-4;
    private static final double WALL_PUSH_HORIZONTAL = 0.32;
    private static final double WALL_PUSH_VERTICAL = 0.16;
    private static final int WALL_PUSH_COOLDOWN_TICKS = 6;
    // ponytail: traversal follows the same fixed 16-segment prototype used by the client runtime;
    // promote this to shared/configured rope resolution only if segment count becomes user-configurable.
    private static final int TRAVERSE_SEGMENTS = 16;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final double[] CONSTRAINT_OUTPUT = new double[RappelConstraint.OUTPUT_STRIDE];

    private RappelServerController() {
    }

    public static boolean attach(
            ServerPlayer player,
            BlockPos hookPos,
            Direction facing,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos column = hookPos.relative(facing);
        double anchorX = column.getX() + 0.5;
        double anchorY = hookPos.getY() + 0.5;
        double anchorZ = column.getZ() + 0.5;
        FixedRopeSavedData data = FixedRopeSavedData.get(level);
        RopeSpan match = findHookSpan(data, anchorX, anchorY, anchorZ);
        if (match == null) {
            return false;
        }
        BlockAttachment start = data.attachment(match.startNodeId());
        BlockAttachment end = data.attachment(match.endNodeId());
        if (start == null || end == null) {
            return false;
        }
        if (!isVerticalRappelSpan(start, end)) {
            return grabSpan(player, match.id(), stateSender);
        }
        return beginRappelSession(player, match, anchorX, anchorY, anchorZ, false, stateSender);
    }

    public static boolean grabSpan(
            ServerPlayer player,
            UUID spanId,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        FixedRopeSavedData data = FixedRopeSavedData.get(level);
        RopeSpan span = data.span(spanId);
        if (span == null) {
            return false;
        }
        BlockAttachment start = data.attachment(span.startNodeId());
        BlockAttachment end = data.attachment(span.endNodeId());
        if (start == null || end == null) {
            return false;
        }

        double[] geometry = sampleSpan(span, start, end);
        Vec3 movement = player.getDeltaMovement();
        AABB sweptPlayer = player.getBoundingBox()
                .expandTowards(movement)
                .expandTowards(movement.scale(-1.0));
        if (!intersectsGeometry(geometry, sweptPlayer, GRAB_RADIUS)) {
            return false;
        }

        if (isVerticalRappelSpan(start, end)) {
            return beginRappelSession(
                    player,
                    span,
                    start.worldX(),
                    start.worldY(),
                    start.worldZ(),
                    true,
                    stateSender
            );
        }
        return beginTraverseSession(player, span, geometry, stateSender);
    }

    private static boolean beginRappelSession(
            ServerPlayer player,
            RopeSpan span,
            double anchorX,
            double anchorY,
            double anchorZ,
            boolean arrestRadialVelocity,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        double dx = player.getX() - anchorX;
        double dy = player.getY() - anchorY;
        double dz = player.getZ() - anchorZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double maxLength = span.allocatedLength();
        double currentLength = Math.min(maxLength, Math.max(Math.min(MIN_LENGTH, maxLength), distance));
        Session session = new Session(
                player.serverLevel(),
                span.id(),
                RappelPackets.MODE_RAPPEL,
                anchorX,
                anchorY,
                anchorZ,
                maxLength,
                currentLength,
                null,
                0.0
        );
        SESSIONS.put(player.getUUID(), session);
        player.fallDistance = 0.0F;

        if (arrestRadialVelocity) {
            Vec3 velocity = player.getDeltaMovement();
            RappelConstraint.constrainTaut(
                    anchorX,
                    anchorY,
                    anchorZ,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    currentLength,
                    CONSTRAINT_OUTPUT,
                    0
            );
            player.teleportTo(CONSTRAINT_OUTPUT[0], CONSTRAINT_OUTPUT[1], CONSTRAINT_OUTPUT[2]);
            player.setDeltaMovement(CONSTRAINT_OUTPUT[3], CONSTRAINT_OUTPUT[4], CONSTRAINT_OUTPUT[5]);
            player.hurtMarked = true;
        }

        stateSender.accept(player, session.state());
        return true;
    }

    private static boolean beginTraverseSession(
            ServerPlayer player,
            RopeSpan span,
            double[] geometry,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        double pathLength = PolylineTraversal.length(geometry, 0, TRAVERSE_SEGMENTS + 1);
        if (!(pathLength > 0.0)) {
            return false;
        }
        double referenceY = player.getY() + player.getBbHeight() * 0.75;
        double pathDistance = PolylineTraversal.projectDistance(
                geometry,
                0,
                TRAVERSE_SEGMENTS + 1,
                player.getX(),
                referenceY,
                player.getZ()
        );
        PolylineTraversal.sample(
                geometry,
                0,
                TRAVERSE_SEGMENTS + 1,
                pathDistance,
                CONSTRAINT_OUTPUT,
                0
        );
        double targetX = CONSTRAINT_OUTPUT[0];
        double targetY = CONSTRAINT_OUTPUT[1] - TRAVERSE_HANG_OFFSET;
        double targetZ = CONSTRAINT_OUTPUT[2];
        if (!canOccupy(player, targetX, targetY, targetZ)) {
            return false;
        }

        Vec3 velocity = player.getDeltaMovement();
        double pathSpeed = ZiplineMotion.clampSpeed(
                velocity.x * CONSTRAINT_OUTPUT[3]
                        + velocity.y * CONSTRAINT_OUTPUT[4]
                        + velocity.z * CONSTRAINT_OUTPUT[5]
        );
        Session session = new Session(
                player.serverLevel(),
                span.id(),
                RappelPackets.MODE_TRAVERSE,
                0.0,
                0.0,
                0.0,
                pathLength,
                pathDistance,
                geometry,
                pathSpeed
        );
        SESSIONS.put(player.getUUID(), session);
        player.teleportTo(targetX, targetY, targetZ);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        stateSender.accept(player, session.state());
        return true;
    }

    public static boolean retrieveAtHook(ServerPlayer player, BlockPos hookPos, Direction facing) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockPos column = hookPos.relative(facing);
        double anchorX = column.getX() + 0.5;
        double anchorY = hookPos.getY() + 0.5;
        double anchorZ = column.getZ() + 0.5;
        FixedRopeSavedData data = FixedRopeSavedData.get(level);
        RopeSpan match = findHookSpan(data, anchorX, anchorY, anchorZ);
        if (match == null || spanInUse(level, match.id())) {
            return false;
        }

        BlockAttachment start = data.attachment(match.startNodeId());
        BlockAttachment end = data.attachment(match.endNodeId());
        if (start == null || end == null) {
            return false;
        }
        int recoveredCoils = RopeCoilItem.DropScan.recoveredCoilsForVerticalBlockDrop(
                start.blockPos().getY(),
                end.blockPos().getY()
        );
        if (!data.disconnectAndRemoveOrphanNodes(match.id())) {
            return false;
        }
        RopeCoilItem.giveRecoveredCoils(player, recoveredCoils);
        return true;
    }

    public static boolean extendActiveRope(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null
                || session.mode != RappelPackets.MODE_RAPPEL
                || player.serverLevel() != session.level
                || session.currentLength < session.maxLength - FREE_END_EPSILON) {
            return false;
        }

        FixedRopeSavedData data = FixedRopeSavedData.get(session.level);
        RopeSpan span = data.span(session.spanId);
        if (span == null) {
            return false;
        }
        BlockAttachment end = data.attachment(span.endNodeId());
        if (end == null) {
            return false;
        }

        int x = end.blockPos().getX();
        int z = end.blockPos().getZ();
        int endBlockY = RopeCoilItem.DropScan.findDropEndBlockY(
                end.blockPos().getY(),
                session.level.getMinBuildHeight(),
                y -> {
                    BlockPos pos = new BlockPos(x, y, z);
                    return !session.level.getBlockState(pos).getCollisionShape(session.level, pos).isEmpty();
                }
        );
        double newEndY = endBlockY + end.localY();
        double extension = end.worldY() - newEndY;
        if (extension <= ANCHOR_EPSILON) {
            return false;
        }

        double newLength = span.allocatedLength() + extension;
        data.replaceSpanEnd(
                span.id(),
                BlockAttachment.atWorld(end.worldX(), newEndY, end.worldZ()),
                newLength
        );
        // Extension is rare. Update every active rappel user of this one span rather than maintaining another index.
        for (Session active : SESSIONS.values()) {
            if (active.mode == RappelPackets.MODE_RAPPEL
                    && active.level == session.level
                    && active.spanId.equals(span.id())) {
                active.maxLength = newLength;
            }
        }
        return true;
    }

    public static void handleInput(
            ServerPlayer player,
            RappelPackets.Input input,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            if (input.grabSpanId() != null) {
                grabSpan(player, input.grabSpanId(), stateSender);
            }
            return;
        }
        if (input.detach()) {
            SESSIONS.remove(player.getUUID());
            stateSender.accept(player, RappelPackets.State.detached());
            return;
        }

        boolean changed = session.vertical != input.vertical();
        session.vertical = input.vertical();
        if (session.mode == RappelPackets.MODE_RAPPEL
                && input.push()
                && player.horizontalCollision
                && session.pushCooldownTicks == 0) {
            applyWallPush(player);
            session.pushCooldownTicks = WALL_PUSH_COOLDOWN_TICKS;
            changed = true;
        }
        if (changed) {
            // Re-anchor client prediction to the server clock only when accepted input changes state.
            stateSender.accept(player, session.state());
        }
    }

    public static void tick(
            MinecraftServer server,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (!player.isAlive() || player.isSpectator() || player.serverLevel() != session.level) {
                iterator.remove();
                stateSender.accept(player, RappelPackets.State.detached());
                continue;
            }

            if (session.mode == RappelPackets.MODE_TRAVERSE) {
                tickTraverse(player, session, stateSender);
                continue;
            }

            if (session.pushCooldownTicks > 0) {
                session.pushCooldownTicks--;
            }
            session.currentLength = adjustLength(session.currentLength, session.maxLength, session.vertical);
            player.fallDistance = 0.0F;

            double dx = player.getX() - session.anchorX;
            double dy = player.getY() - session.anchorY;
            double dz = player.getZ() - session.anchorZ;
            double allowed = session.currentLength + CORRECTION_TOLERANCE;
            if (dx * dx + dy * dy + dz * dz <= allowed * allowed) {
                continue;
            }

            Vec3 velocity = player.getDeltaMovement();
            RappelConstraint.constrain(
                    session.anchorX,
                    session.anchorY,
                    session.anchorZ,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    session.currentLength,
                    CONSTRAINT_OUTPUT,
                    0
            );
            player.teleportTo(CONSTRAINT_OUTPUT[0], CONSTRAINT_OUTPUT[1], CONSTRAINT_OUTPUT[2]);
            player.setDeltaMovement(CONSTRAINT_OUTPUT[3], CONSTRAINT_OUTPUT[4], CONSTRAINT_OUTPUT[5]);
        }
    }

    public static double adjustLength(double currentLength, double maxLength, byte vertical) {
        if (vertical > 0) {
            return Math.max(Math.min(MIN_LENGTH, maxLength), currentLength - CLIMB_PER_TICK);
        }
        if (vertical < 0) {
            return Math.min(maxLength, currentLength + DESCEND_PER_TICK);
        }
        return currentLength;
    }

    private static void tickTraverse(
            ServerPlayer player,
            Session session,
            BiConsumer<ServerPlayer, RappelPackets.State> stateSender
    ) {
        PolylineTraversal.sample(
                session.pathGeometry,
                0,
                TRAVERSE_SEGMENTS + 1,
                session.currentLength,
                CONSTRAINT_OUTPUT,
                0
        );
        double nextSpeed = ZiplineMotion.integrateSpeed(
                session.traverseSpeed,
                CONSTRAINT_OUTPUT[4],
                session.vertical
        );
        double nextDistance = ZiplineMotion.clampDistance(session.currentLength, session.maxLength, nextSpeed);
        PolylineTraversal.sample(
                session.pathGeometry,
                0,
                TRAVERSE_SEGMENTS + 1,
                nextDistance,
                CONSTRAINT_OUTPUT,
                0
        );
        double targetX = CONSTRAINT_OUTPUT[0];
        double targetY = CONSTRAINT_OUTPUT[1] - TRAVERSE_HANG_OFFSET;
        double targetZ = CONSTRAINT_OUTPUT[2];
        player.fallDistance = 0.0F;
        if (!canOccupy(player, targetX, targetY, targetZ)) {
            if (session.traverseSpeed != 0.0) {
                session.traverseSpeed = 0.0;
                stateSender.accept(player, session.state());
            }
            return;
        }

        session.currentLength = nextDistance;
        session.traverseSpeed = ZiplineMotion.stopAtEndpoint(nextDistance, session.maxLength, nextSpeed);
        if (session.traverseSpeed != nextSpeed) {
            stateSender.accept(player, session.state());
        }

        double dx = targetX - player.getX();
        double dy = targetY - player.getY();
        double dz = targetZ - player.getZ();
        if (dx * dx + dy * dy + dz * dz <= TRAVERSE_CORRECTION_TOLERANCE * TRAVERSE_CORRECTION_TOLERANCE) {
            return;
        }
        player.teleportTo(targetX, targetY, targetZ);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static double[] sampleSpan(RopeSpan span, BlockAttachment start, BlockAttachment end) {
        double[] geometry = new double[CatenarySampler.coordinateCount(TRAVERSE_SEGMENTS)];
        CatenarySampler.sample(
                start.worldX(), start.worldY(), start.worldZ(),
                end.worldX(), end.worldY(), end.worldZ(),
                span.allocatedLength(),
                TRAVERSE_SEGMENTS,
                geometry
        );
        return geometry;
    }

    private static boolean intersectsGeometry(double[] geometry, AABB sweptPlayer, double radius) {
        for (int segment = 0; segment < TRAVERSE_SEGMENTS; segment++) {
            int start = segment * 3;
            int end = start + 3;
            AABB tube = new AABB(
                    Math.min(geometry[start], geometry[end]),
                    Math.min(geometry[start + 1], geometry[end + 1]),
                    Math.min(geometry[start + 2], geometry[end + 2]),
                    Math.max(geometry[start], geometry[end]),
                    Math.max(geometry[start + 1], geometry[end + 1]),
                    Math.max(geometry[start + 2], geometry[end + 2])
            ).inflate(radius);
            if (tube.intersects(sweptPlayer)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canOccupy(ServerPlayer player, double x, double y, double z) {
        AABB target = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
        return player.serverLevel().noCollision(player, target);
    }

    private static RopeSpan findHookSpan(FixedRopeSavedData data, double anchorX, double anchorY, double anchorZ) {
        for (RopeSpan span : data.spans()) {
            BlockAttachment start = data.attachment(span.startNodeId());
            if (start != null
                    && close(start.worldX(), anchorX)
                    && close(start.worldY(), anchorY)
                    && close(start.worldZ(), anchorZ)) {
                return span;
            }
        }
        return null;
    }

    private static boolean isVerticalRappelSpan(BlockAttachment start, BlockAttachment end) {
        return Math.abs(start.worldX() - end.worldX()) <= GRAB_VERTICAL_EPSILON
                && Math.abs(start.worldZ() - end.worldZ()) <= GRAB_VERTICAL_EPSILON
                && end.worldY() < start.worldY() - GRAB_VERTICAL_EPSILON;
    }

    private static boolean spanInUse(ServerLevel level, UUID spanId) {
        for (Session session : SESSIONS.values()) {
            if (session.level == level && session.spanId.equals(spanId)) {
                return true;
            }
        }
        return false;
    }

    private static void applyWallPush(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0e-6) {
            return;
        }
        player.addDeltaMovement(new Vec3(
                -look.x / horizontal * WALL_PUSH_HORIZONTAL,
                WALL_PUSH_VERTICAL,
                -look.z / horizontal * WALL_PUSH_HORIZONTAL
        ));
        player.hurtMarked = true;
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) <= ANCHOR_EPSILON;
    }

    private static final class Session {
        private final ServerLevel level;
        private final UUID spanId;
        private final byte mode;
        private final double anchorX;
        private final double anchorY;
        private final double anchorZ;
        private final double[] pathGeometry;
        private double maxLength;
        private double currentLength;
        private double traverseSpeed;
        private byte vertical;
        private int pushCooldownTicks;

        private Session(
                ServerLevel level,
                UUID spanId,
                byte mode,
                double anchorX,
                double anchorY,
                double anchorZ,
                double maxLength,
                double currentLength,
                double[] pathGeometry,
                double traverseSpeed
        ) {
            this.level = level;
            this.spanId = spanId;
            this.mode = mode;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.maxLength = maxLength;
            this.currentLength = currentLength;
            this.pathGeometry = pathGeometry;
            this.traverseSpeed = traverseSpeed;
        }

        private RappelPackets.State state() {
            if (mode == RappelPackets.MODE_TRAVERSE) {
                return RappelPackets.State.traverse(spanId, currentLength, maxLength, traverseSpeed);
            }
            return new RappelPackets.State(
                    true,
                    RappelPackets.MODE_RAPPEL,
                    spanId,
                    anchorX,
                    anchorY,
                    anchorZ,
                    currentLength,
                    maxLength,
                    0.0
            );
        }
    }
}
