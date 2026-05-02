package mod.azure.arachnids.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Supplier;

import mod.azure.arachnids.services.ArachnidsServices;

public class ItemRegistry {

    public static final Supplier<SpawnEggItem> WARRIOR_SPAWN_EGG = registerItem(
        "warrior_spawn_egg",
        ArachnidsServices.COMMON_REGISTRY.makeSpawnEggFor(
            EntityRegistry.WARRIORBUG,
            0xa6380a,
            0xc15224,
            new Item.Properties()
        )
    );

    public static final Supplier<SpawnEggItem> WORKER_SPAWN_EGG = registerItem(
        "worker_spawn_egg",
        ArachnidsServices.COMMON_REGISTRY.makeSpawnEggFor(
            EntityRegistry.WORKERBUG,
            0x3b2c80,
            0x55469b,
            new Item.Properties()
        )
    );

    public static final Supplier<SpawnEggItem> HOPPER_SPAWN_EGG = registerItem(
        "hopper_spawn_egg",
        ArachnidsServices.COMMON_REGISTRY.makeSpawnEggFor(
            EntityRegistry.HOPPERBUG,
            0x214c0e,
            0x6f9e5a,
            new Item.Properties()
        )
    );

    public static final Supplier<SpawnEggItem> CHARIOT_SPAWN_EGG = registerItem(
        "chariot_spawn_egg",
        ArachnidsServices.COMMON_REGISTRY.makeSpawnEggFor(
            EntityRegistry.CHARIOTBUG,
            0x5a001e,
            0x770016,
            new Item.Properties()
        )
    );

    public static final Supplier<SpawnEggItem> BRAIN_SPAWN_EGG = registerItem(
        "brainbug_spawn_egg",
        ArachnidsServices.COMMON_REGISTRY.makeSpawnEggFor(
            EntityRegistry.BRAINBUG,
            0x634e33,
            0xf8dec1,
            new Item.Properties()
        )
    );

    static <T extends Item> Supplier<T> registerItem(String itemName, Supplier<T> item) {
        return ArachnidsServices.COMMON_REGISTRY.register(BuiltInRegistries.ITEM, itemName, item);
    }

    public static void initialize() {}
}
