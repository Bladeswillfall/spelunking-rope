package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class RopeCoilItem extends Item {
    static final int MAX_DEPLOY_BLOCKS = 32;

    private final Consumer<ServerLevel> snapshotBroadcaster;

    public RopeCoilItem(Properties properties, Consumer<ServerLevel> snapshotBroadcaster) {
        super(properties);
        this.snapshotBroadcaster = Objects.requireNonNull(snapshotBroadcaster, "snapshotBroadcaster");
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
        var level = context.getLevel();
        BlockPos hookPos = context.getClickedPos();
        var hookState = level.getBlockState(hookPos);
        if (!hookState.is(Blocks.TRIPWIRE_HOOK)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Direction facing = hookState.getValue(TripWireHookBlock.FACING);
        BlockPos column = hookPos.relative(facing);
        int endBlockY = DropScan.findDropEndBlockY(
                hookPos.getY(),
                serverLevel.getMinBuildHeight(),
                y -> {
                    BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                    return !serverLevel.getBlockState(pos).getCollisionShape(serverLevel, pos).isEmpty();
                }
        );

        double x = column.getX() + 0.5;
        double z = column.getZ() + 0.5;
        double startY = hookPos.getY() + 0.5;
        double endY = endBlockY + 0.05;
        double allocatedLength = startY - endY;
        if (allocatedLength <= 0.0) {
            return InteractionResult.FAIL;
        }

        // ponytail: FixedRopeSavedData only has world-point BlockAttachment endpoints today.
        // Treat the lower air-cell point as a free end until typed attachment references are required.
        FixedRopeSavedData.get(serverLevel).addRope(
                BlockAttachment.atWorld(x, startY, z),
                BlockAttachment.atWorld(x, endY, z),
                allocatedLength
        );

        var player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        // ponytail: placement is rare; reuse the proven full snapshot until mutation volume justifies deltas.
        snapshotBroadcaster.accept(serverLevel);
        return InteractionResult.CONSUME;
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
    }
}
