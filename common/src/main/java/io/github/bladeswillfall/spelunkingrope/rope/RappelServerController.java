package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.core.graph.RopeSpan;
import io.github.bladeswillfall.spelunkingrope.core.traversal.RappelConstraint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private static final double CORRECTION_TOLERANCE = 0.35;
    private static final double ANCHOR_EPSILON = 1.0e-6;
    private static final double FREE_END_EPSILON = 1.0e-4;
    private static final double WALL_PUSH_HORIZONTAL = 0.32;
    private static final double WALL_PUSH_VERTICAL = 0.16;
    private static final int WALL_PUSH_COOLDOWN_TICKS = 6;

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

        double dx = player.getX() - anchorX;
        double dy = player.getY() - anchorY;
        double dz = player.getZ() - anchorZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double maxLength = match.allocatedLength();
        double currentLength = Math.min(maxLength, Math.max(Math.min(MIN_LENGTH, maxLength), distance));
        Session session = new Session(
                level,
                match.id(),
                anchorX,
                anchorY,
                anchorZ,
                maxLength,
                currentLength
        );
        SESSIONS.put(player.getUUID(), session);
        player.fallDistance = 0.0F;
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
        int recoveredCoils = RopeCoilItem.coilsForVerticalDrop(start, end);
        if (!data.disconnectAndRemoveOrphanNodes(match.id())) {
            return false;
        }
        RopeCoilItem.giveRecoveredCoils(player, recoveredCoils);
        return true;
    }

    public static boolean extendActiveRope(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null
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
        // Extension is rare. Update every active user of this one span rather than maintaining another index.
        for (Session active : SESSIONS.values()) {
            if (active.level == session.level && active.spanId.equals(span.id())) {
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
            return;
        }
        if (input.detach()) {
            SESSIONS.remove(player.getUUID());
            stateSender.accept(player, RappelPackets.State.detached());
            return;
        }

        boolean changed = session.vertical != input.vertical();
        session.vertical = input.vertical();
        if (input.push() && player.horizontalCollision && session.pushCooldownTicks == 0) {
            applyWallPush(player);
            session.pushCooldownTicks = WALL_PUSH_COOLDOWN_TICKS;
            changed = true;
        }
        if (changed) {
            // Re-anchor client integration to the server clock only when accepted input changes state.
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
        private final double anchorX;
        private final double anchorY;
        private final double anchorZ;
        private double maxLength;
        private double currentLength;
        private byte vertical;
        private int pushCooldownTicks;

        private Session(
                ServerLevel level,
                UUID spanId,
                double anchorX,
                double anchorY,
                double anchorZ,
                double maxLength,
                double currentLength
        ) {
            this.level = level;
            this.spanId = spanId;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.maxLength = maxLength;
            this.currentLength = currentLength;
        }

        private RappelPackets.State state() {
            return new RappelPackets.State(
                    true,
                    spanId,
                    anchorX,
                    anchorY,
                    anchorZ,
                    currentLength,
                    maxLength
            );
        }
    }
}
