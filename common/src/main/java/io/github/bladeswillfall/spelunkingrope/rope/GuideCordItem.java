package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class GuideCordItem extends Item implements DyeableLeatherItem {
    private static final int MAX_START_BLOCK_DISTANCE = RopeCoilItem.MAX_DEPLOY_BLOCKS + 2;
    private static final String TAG_DIMENSION = "spelunking_rope_guide_dimension";
    private static final String TAG_POS = "spelunking_rope_guide_pos";
    private static final ResourceLocation GUIDE_CORD_ID = new ResourceLocation(SpelunkingRope.MOD_ID, "guide_cord");

    private final Consumer<ServerLevel> snapshotBroadcaster;

    public GuideCordItem(Properties properties, Consumer<ServerLevel> snapshotBroadcaster) {
        super(properties);
        this.snapshotBroadcaster = Objects.requireNonNull(snapshotBroadcaster, "snapshotBroadcaster");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return readSelection(stack) != null || super.isFoil(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        byte dyeId = dyeId(stack);
        if (dyeId == FixedRopeSnapshot.NO_DYE) {
            return super.getName(stack);
        }
        DyeColor color = DyeColor.byId(dyeId);
        return Component.translatable(
                "item.spelunking_rope.guide_cord.colored",
                Component.translatable("color.minecraft." + color.getName())
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos currentPos = context.getClickedPos();
        BlockState currentState = level.getBlockState(currentPos);
        Direction currentFacing = RopeAnchor.guideFacing(currentState);
        Player player = context.getPlayer();
        if (currentFacing == null || player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack stack = context.getItemInHand();
        String dimension = serverLevel.dimension().location().toString();
        Selection selection = readSelection(stack);
        if (selection == null || !selection.dimension().equals(dimension)) {
            select(stack, dimension, currentPos);
            message(serverPlayer, "message.spelunking_rope.guide_selected");
            return InteractionResult.CONSUME;
        }

        BlockPos startPos = BlockPos.of(selection.pos());
        if (startPos.equals(currentPos)) {
            clearSelection(stack);
            message(serverPlayer, "message.spelunking_rope.guide_cancelled");
            return InteractionResult.CONSUME;
        }

        long dx = (long) startPos.getX() - currentPos.getX();
        long dy = (long) startPos.getY() - currentPos.getY();
        long dz = (long) startPos.getZ() - currentPos.getZ();
        long maxDistanceSquared = (long) MAX_START_BLOCK_DISTANCE * MAX_START_BLOCK_DISTANCE;
        if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) {
            message(serverPlayer, "message.spelunking_rope.guide_too_long");
            return InteractionResult.CONSUME;
        }

        // Creative-edited item NBT must not force-load an arbitrary chunk.
        if (!serverLevel.hasChunkAt(startPos)) {
            select(stack, dimension, currentPos);
            message(serverPlayer, "message.spelunking_rope.guide_selected");
            return InteractionResult.CONSUME;
        }

        BlockState startState = serverLevel.getBlockState(startPos);
        Direction startFacing = RopeAnchor.guideFacing(startState);
        if (startFacing == null) {
            select(stack, dimension, currentPos);
            message(serverPlayer, "message.spelunking_rope.guide_selected");
            return InteractionResult.CONSUME;
        }

        BlockAttachment start = RopeAnchor.guideAttachment(startPos, startFacing);
        BlockAttachment end = RopeAnchor.guideAttachment(currentPos, currentFacing);
        double worldDx = end.worldX() - start.worldX();
        double worldDy = end.worldY() - start.worldY();
        double worldDz = end.worldZ() - start.worldZ();
        double straightDistance = Math.sqrt(worldDx * worldDx + worldDy * worldDy + worldDz * worldDz);
        double allocatedLength = RopeCoilItem.RouteLength.allocatedLengthForDistance(straightDistance);
        if (allocatedLength < 0.0) {
            message(serverPlayer, "message.spelunking_rope.guide_too_long");
            return InteractionResult.CONSUME;
        }

        FixedRopeSavedData.get(serverLevel).addGuideLine(
                start,
                end,
                allocatedLength,
                dyeId(stack)
        );
        clearSelection(stack);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        snapshotBroadcaster.accept(serverLevel);
        message(serverPlayer, "message.spelunking_rope.guide_created");
        return InteractionResult.CONSUME;
    }

    public static boolean retrieveAtClip(ServerPlayer player, BlockPos clipPos, Direction facing) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        FixedRopeSavedData data = FixedRopeSavedData.get(player.serverLevel());
        List<Byte> recoveredColors = data.removeGuideLinesAt(RopeAnchor.guideAttachment(clipPos, facing));
        if (recoveredColors.isEmpty()) {
            return false;
        }
        giveRecoveredCord(player, recoveredColors);
        return true;
    }

    static byte dyeId(ItemStack stack) {
        if (!(stack.getItem() instanceof DyeableLeatherItem dyeable) || !dyeable.hasCustomColor(stack)) {
            return FixedRopeSnapshot.NO_DYE;
        }
        return closestDyeId(dyeable.getColor(stack));
    }

    static byte closestDyeId(int rgb) {
        return (byte) GuideCordColors.closestPaletteIndex(rgb, DyePalette.RGB);
    }

    static int dyeRgb(byte dyeId) {
        if (dyeId < 0 || dyeId >= DyePalette.RGB.length) {
            throw new IllegalArgumentException("invalid dye id: " + dyeId);
        }
        return DyePalette.RGB[dyeId];
    }

    private static int[] createDyePalette() {
        DyeColor[] colors = DyeColor.values();
        int[] palette = new int[colors.length];
        for (DyeColor color : colors) {
            float[] diffuse = color.getTextureDiffuseColors();
            int red = Math.round(diffuse[0] * 255.0F);
            int green = Math.round(diffuse[1] * 255.0F);
            int blue = Math.round(diffuse[2] * 255.0F);
            palette[color.getId()] = red << 16 | green << 8 | blue;
        }
        return palette;
    }

    private static Selection readSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DIMENSION, Tag.TAG_STRING) || !tag.contains(TAG_POS, Tag.TAG_LONG)) {
            return null;
        }
        return new Selection(tag.getString(TAG_DIMENSION), tag.getLong(TAG_POS));
    }

    private static void select(ItemStack stack, String dimension, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_DIMENSION, dimension);
        tag.putLong(TAG_POS, pos.asLong());
    }

    private static void clearSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(TAG_DIMENSION);
        tag.remove(TAG_POS);
    }

    private static void message(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private static void giveRecoveredCord(ServerPlayer player, List<Byte> recoveredColors) {
        if (player.getAbilities().instabuild) {
            return;
        }
        Item guideCord = BuiltInRegistries.ITEM.get(GUIDE_CORD_ID);
        if (!(guideCord instanceof GuideCordItem guideCordItem)) {
            throw new IllegalStateException("Guide cord item is not registered");
        }

        int[] counts = new int[17];
        for (byte color : recoveredColors) {
            counts[color + 1]++;
        }
        for (int colorIndex = 0; colorIndex < counts.length; colorIndex++) {
            int remaining = counts[colorIndex];
            byte dyeId = (byte) (colorIndex - 1);
            while (remaining > 0) {
                int batch = Math.min(remaining, guideCord.getMaxStackSize());
                ItemStack recovered = new ItemStack(guideCord, batch);
                if (dyeId != FixedRopeSnapshot.NO_DYE) {
                    guideCordItem.setColor(recovered, dyeRgb(dyeId));
                }
                if (!player.addItem(recovered)) {
                    player.drop(recovered, false);
                }
                remaining -= batch;
            }
        }
    }

    private static final class DyePalette {
        private static final int[] RGB = createDyePalette();
    }

    private record Selection(String dimension, long pos) {
    }
}
