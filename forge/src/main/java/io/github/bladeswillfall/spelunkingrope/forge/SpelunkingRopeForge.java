package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.GuideClipBlock;
import io.github.bladeswillfall.spelunkingrope.rope.GuideCordItem;
import io.github.bladeswillfall.spelunkingrope.rope.PitonBlock;
import io.github.bladeswillfall.spelunkingrope.rope.PulleyBlock;
import io.github.bladeswillfall.spelunkingrope.rope.RopeCoilItem;
import io.github.bladeswillfall.spelunkingrope.rope.WinchBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SpelunkingRope.MOD_ID)
public final class SpelunkingRopeForge {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            SpelunkingRope.MOD_ID
    );
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            SpelunkingRope.MOD_ID
    );

    private static final RegistryObject<PitonBlock> PITON = BLOCKS.register("piton", PitonBlock::new);
    private static final RegistryObject<GuideClipBlock> GUIDE_CLIP = BLOCKS.register("guide_clip", GuideClipBlock::new);
    private static final RegistryObject<PulleyBlock> PULLEY = BLOCKS.register("pulley", PulleyBlock::new);
    private static final RegistryObject<WinchBlock> WINCH = BLOCKS.register(
            "winch",
            () -> new WinchBlock(ForgeRopeNetworking::broadcastSnapshot)
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> PITON_ITEM = ITEMS.register(
            "piton",
            () -> new BlockItem(PITON.get(), new Item.Properties())
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> GUIDE_CLIP_ITEM = ITEMS.register(
            "guide_clip",
            () -> new BlockItem(GUIDE_CLIP.get(), new Item.Properties())
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> PULLEY_ITEM = ITEMS.register(
            "pulley",
            () -> new BlockItem(PULLEY.get(), new Item.Properties())
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> WINCH_ITEM = ITEMS.register(
            "winch",
            () -> new BlockItem(WINCH.get(), new Item.Properties())
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> ROPE_COIL = ITEMS.register(
            "rope_coil",
            () -> new RopeCoilItem(new Item.Properties().stacksTo(16), ForgeRopeNetworking::broadcastSnapshot)
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> GUIDE_CORD = ITEMS.register(
            "guide_cord",
            () -> new GuideCordItem(new Item.Properties().stacksTo(16), ForgeRopeNetworking::broadcastSnapshot)
    );

    public SpelunkingRopeForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        SpelunkingRope.init();
        ForgeRopeNetworking.init();
        ForgeRappelEvents.init();
    }
}
