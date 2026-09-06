package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.rope.RappelServerController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class ForgeRappelEvents {
    private ForgeRappelEvents() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ForgeRappelEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(ForgeRappelEvents::onServerTick);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide
                || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var hookState = event.getLevel().getBlockState(event.getPos());
        if (!hookState.is(Blocks.TRIPWIRE_HOOK)) {
            return;
        }

        if (RappelServerController.attach(
                player,
                event.getPos(),
                hookState.getValue(TripWireHookBlock.FACING),
                ForgeRopeNetworking::sendRappelState
        )) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RappelServerController.tick(event.getServer(), ForgeRopeNetworking::sendRappelState);
        }
    }
}
