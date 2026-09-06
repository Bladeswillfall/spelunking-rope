package io.github.bladeswillfall.spelunkingrope.forge;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import io.github.bladeswillfall.spelunkingrope.rope.RopeCoilItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SpelunkingRope.MOD_ID)
public final class SpelunkingRopeForge {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            SpelunkingRope.MOD_ID
    );
    @SuppressWarnings("unused")
    private static final RegistryObject<Item> ROPE_COIL = ITEMS.register(
            "rope_coil",
            () -> new RopeCoilItem(new Item.Properties().stacksTo(16), ForgeRopeNetworking::broadcastSnapshot)
    );

    public SpelunkingRopeForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        SpelunkingRope.init();
        ForgeRopeNetworking.init();
    }
}
