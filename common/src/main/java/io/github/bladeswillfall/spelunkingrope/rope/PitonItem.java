package io.github.bladeswillfall.spelunkingrope.rope;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PitonItem extends BlockItem {
    static final double REMOTE_REACH = 5.0;

    public PitonItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!remotePlacementEligible(isRappelling(level, player), player.horizontalCollision)) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 target = eye.add(player.getViewVector(1.0F).scale(REMOTE_REACH));
        BlockHitResult hit = level.clip(new ClipContext(
                eye,
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getDirection() == net.minecraft.core.Direction.UP
                || hit.getDirection() == net.minecraft.core.Direction.DOWN) {
            return InteractionResultHolder.pass(stack);
        }

        InteractionResult result = useOn(new UseOnContext(player, hand, hit));
        return new InteractionResultHolder<>(result, stack);
    }

    static boolean remotePlacementEligible(boolean rappelling, boolean wallContact) {
        return rappelling && wallContact;
    }

    private static boolean isRappelling(Level level, Player player) {
        if (level.isClientSide) {
            return RappelClientState.INSTANCE.rappelling();
        }
        return player instanceof ServerPlayer serverPlayer
                && RappelServerController.isRappelling(serverPlayer);
    }
}
