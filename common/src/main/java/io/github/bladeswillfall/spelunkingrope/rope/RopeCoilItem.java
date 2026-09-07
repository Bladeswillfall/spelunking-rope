package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.core.graph.RopeNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class RopeCoilItem extends Item {
    static final int MAX_DEPLOY_BLOCKS = 32;
    private static final int MAX_ROUTE_START_BLOCK_DISTANCE = MAX_DEPLOY_BLOCKS + 2;
    private static final String TAG_ROUTE_DIMENSION = "spelunking_rope_route_dimension";
    private static final String TAG_ROUTE_POS = "spelunking_rope_route_pos";
    private static final ResourceLocation ROPE_COIL_ID = new ResourceLocation(SpelunkingRope.MOD_ID, "rope_coil");

    private final Consumer<ServerLevel> snapshotBroadcaster;

    public RopeCoilItem(Properties properties, Consumer<ServerLevel> snapshotBroadcaster) {
        super(properties);
        this.snapshotBroadcaster = Objects.requireNonNull(snapshotBroadcaster, "snapshotBroadcaster");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return readRouteSelection(stack) != null || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return RappelClientState.INSTANCE.atFreeEnd()
                    ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.pass(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !RappelServerController.extendActiveRope(serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        snapshotBroadcaster.accept((ServerLevel) level);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos anchorPos = context.getClickedPos();
        BlockState anchorState = level.getBlockState(anchorPos);
        Player player = context.getPlayer();

        if (RopeAnchor.isRouteAnchor(anchorState) && player != null && player.isShiftKeyDown()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.FAIL;
            }
            return useForRoute(
                    (ServerLevel) level,
                    serverPlayer,
                    context.getItemInHand(),
                    anchorPos,
                    anchorState
            );
        }

        // M4.1 pulleys are routed topology only; hanging/free-end pulley behavior belongs with the shared-length solver.
        if (RopeAnchor.isPulley(anchorState)) {
            return InteractionResult.PASS;
        }

        Direction facing = RopeAnchor.facing(anchorState);
        if (facing == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos column = anchorPos.relative(facing);
        int endBlockY = DropScan.findDropEndBlockY(
                anchorPos.getY(),
                serverLevel.getMinBuildHeight(),
                y -> {
                    BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                    return !serverLevel.getBlockState(pos).getCollisionShape(serverLevel, pos).isEmpty();
                }
        );

        BlockAttachment start = RopeAnchor.attachment(anchorPos, facing);
        double endY = endBlockY + 0.05;
        double allocatedLength = start.worldY() - endY;
        if (allocatedLength <= 0.0) {
            return InteractionResult.FAIL;
        }

        // ponytail: FixedRopeSavedData only has world-point BlockAttachment endpoints today.
        // Treat the lower air-cell point as a free end until typed attachment references are required.
        FixedRopeSavedData.get(serverLevel).addRope(
                start,
                BlockAttachment.atWorld(start.worldX(), endY, start.worldZ()),
                allocatedLength
        );

        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        // ponytail: placement is rare; reuse the proven full snapshot until mutation volume justifies deltas.
        snapshotBroadcaster.accept(serverLevel);
        return InteractionResult.CONSUME;
    }

    private InteractionResult useForRoute(
            ServerLevel level,
            ServerPlayer player,
            ItemStack stack,
            BlockPos currentPos,
            BlockState currentState
    ) {
        String dimension = level.dimension().location().toString();
        RouteSelection selection = readRouteSelection(stack);
        if (selection == null || !selection.dimension().equals(dimension)) {
            selectRouteStart(stack, dimension, currentPos);
            routeMessage(player, "message.spelunking_rope.route_selected");
            return InteractionResult.CONSUME;
        }

        BlockPos startPos = BlockPos.of(selection.pos());
        if (startPos.equals(currentPos)) {
            clearRouteSelection(stack);
            routeMessage(player, "message.spelunking_rope.route_cancelled");
            return InteractionResult.CONSUME;
        }

        long dx = (long) startPos.getX() - currentPos.getX();
        long dy = (long) startPos.getY() - currentPos.getY();
        long dz = (long) startPos.getZ() - currentPos.getZ();
        long maxBlockDistanceSquared = (long) MAX_ROUTE_START_BLOCK_DISTANCE * MAX_ROUTE_START_BLOCK_DISTANCE;
        if (dx * dx + dy * dy + dz * dz > maxBlockDistanceSquared) {
            routeMessage(player, "message.spelunking_rope.route_too_long");
            return InteractionResult.CONSUME;
        }

        // The selection NBT is player-controlled in creative mode. Never let it force-load an arbitrary chunk.
        if (!level.hasChunkAt(startPos)) {
            selectRouteStart(stack, dimension, currentPos);
            routeMessage(player, "message.spelunking_rope.route_selected");
            return InteractionResult.CONSUME;
        }

        BlockState startState = level.getBlockState(startPos);
        if (!RopeAnchor.isRouteAnchor(startState)) {
            selectRouteStart(stack, dimension, currentPos);
            routeMessage(player, "message.spelunking_rope.route_selected");
            return InteractionResult.CONSUME;
        }

        BlockAttachment start = RopeAnchor.attachment(startPos, RopeAnchor.facing(startState));
        BlockAttachment end = RopeAnchor.attachment(currentPos, RopeAnchor.facing(currentState));
        double worldDx = end.worldX() - start.worldX();
        double worldDy = end.worldY() - start.worldY();
        double worldDz = end.worldZ() - start.worldZ();
        double straightDistance = Math.sqrt(worldDx * worldDx + worldDy * worldDy + worldDz * worldDz);
        double allocatedLength = RouteLength.allocatedLengthForDistance(straightDistance);
        if (allocatedLength < 0.0) {
            routeMessage(player, "message.spelunking_rope.route_too_long");
            return InteractionResult.CONSUME;
        }

        RopeSpanResult result = addRouteSpan(level, start, startState, end, currentState, allocatedLength);
        if (!result.created()) {
            routeMessage(player, "message.spelunking_rope.pulley_full");
            return InteractionResult.CONSUME;
        }

        clearRouteSelection(stack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        snapshotBroadcaster.accept(level);
        routeMessage(player, "message.spelunking_rope.route_created");
        return InteractionResult.CONSUME;
    }

    private static RopeSpanResult addRouteSpan(
            ServerLevel level,
            BlockAttachment start,
            BlockState startState,
            BlockAttachment end,
            BlockState endState,
            double allocatedLength
    ) {
        return new RopeSpanResult(FixedRopeSavedData.get(level).addRouteRope(
                start,
                routeNodeType(startState),
                end,
                routeNodeType(endState),
                allocatedLength
        ) != null);
    }

    private static RopeNode.Type routeNodeType(BlockState state) {
        return RopeAnchor.isPulley(state) ? RopeNode.Type.PULLEY : RopeNode.Type.FIXED_ANCHOR;
    }

    private static RouteSelection readRouteSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null
                || !tag.contains(TAG_ROUTE_DIMENSION, Tag.TAG_STRING)
                || !tag.contains(TAG_ROUTE_POS, Tag.TAG_LONG)) {
            return null;
        }
        return new RouteSelection(tag.getString(TAG_ROUTE_DIMENSION), tag.getLong(TAG_ROUTE_POS));
    }

    private static void selectRouteStart(ItemStack stack, String dimension, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_ROUTE_DIMENSION, dimension);
        tag.putLong(TAG_ROUTE_POS, pos.asLong());
    }

    private static void clearRouteSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(TAG_ROUTE_DIMENSION);
        tag.remove(TAG_ROUTE_POS);
    }

    private static void routeMessage(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    static void giveRecoveredCoils(ServerPlayer player, int count) {
        Objects.requireNonNull(player, "player");
        if (count <= 0 || player.getAbilities().instabuild) {
            return;
        }

        Item ropeCoil = BuiltInRegistries.ITEM.get(ROPE_COIL_ID);
        if (ropeCoil == Items.AIR) {
            throw new IllegalStateException("Rope coil item is not registered");
        }
        int maxStack = ropeCoil.getMaxStackSize();
        int remaining = count;
        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemStack recovered = new ItemStack(ropeCoil, batch);
            if (!player.addItem(recovered)) {
                player.drop(recovered, false);
            }
            remaining -= batch;
        }
    }

    private record RouteSelection(String dimension, long pos) {
    }

    private record RopeSpanResult(boolean created) {
    }

    static final class RouteLength {
        private static final double MIN_SLACK = 0.5;
        private static final double SLACK_FRACTION = 0.05;
        private static final double MIN_ROUTE_DISTANCE = 1.0e-4;

        private RouteLength() {
        }

        static double allocatedLengthForDistance(double straightDistance) {
            if (!Double.isFinite(straightDistance) || straightDistance <= MIN_ROUTE_DISTANCE) {
                return -1.0;
            }
            double required = straightDistance + Math.max(MIN_SLACK, straightDistance * SLACK_FRACTION);
            return required <= MAX_DEPLOY_BLOCKS ? required : -1.0;
        }
    }

    static final class DropScan {
        private DropScan() {
        }

        static int findDropEndBlockY(int anchorBlockY, int minBuildHeight, IntPredicate isBlocked) {
            Objects.requireNonNull(isBlocked, "isBlocked");
            int lowestY = Math.max(minBuildHeight, anchorBlockY - MAX_DEPLOY_BLOCKS);
            for (int y = anchorBlockY - 1; y >= lowestY; y--) {
                if (isBlocked.test(y)) {
                    return y + 1;
                }
            }
            return lowestY;
        }

        static int recoveredCoilsForVerticalBlockDrop(int startBlockY, int endBlockY) {
            int blockDrop = Math.max(0, startBlockY - endBlockY);
            return Math.max(1, (blockDrop + MAX_DEPLOY_BLOCKS - 1) / MAX_DEPLOY_BLOCKS);
        }
    }
}
