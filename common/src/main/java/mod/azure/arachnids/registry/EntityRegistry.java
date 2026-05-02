package mod.azure.arachnids.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.mob.hopperbug.HopperBug;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.services.ArachnidsServices;
import mod.azure.arachnids.util.SilencedEntityTypeBuilder;

public class EntityRegistry {

    private EntityRegistry() {}

    public static final Supplier<EntityType<WarriorBug>> WARRIORBUG = registerEntity(
        "warrior",
        WarriorBug::new,
        MobCategory.MONSTER,
        2.3f,
        2.15F
    );

    public static final Supplier<EntityType<WorkerBug>> WORKERBUG = registerEntity(
        "worker",
        WorkerBug::new,
        MobCategory.MONSTER,
        2.3f,
        2.15F
    );

    public static final Supplier<EntityType<HopperBug>> HOPPERBUG = registerEntity(
        "hopper",
        HopperBug::new,
        MobCategory.MONSTER,
        1.6f,
        1.15F
    );

    public static final Supplier<EntityType<ChariotBug>> CHARIOTBUG = registerEntity(
        "chariot",
        ChariotBug::new,
        MobCategory.MONSTER,
        1.1f,
        0.85F
    );

    public static final Supplier<EntityType<BrainBug>> BRAINBUG = registerEntity(
        "brainbug",
        BrainBug::new,
        MobCategory.MONSTER,
        5.6f,
        4.05F
    );

    static <T extends Entity> Supplier<EntityType<T>> registerEntity(
        String entityName,
        EntityType.EntityFactory<T> entity,
        MobCategory mobCategory,
        float width,
        float height
    ) {
        return ArachnidsServices.COMMON_REGISTRY.register(
            BuiltInRegistries.ENTITY_TYPE,
            entityName,
            () -> create(entity, mobCategory, width, height).buildWithoutDataFixerCheck()
        );
    }

    static <T extends Entity> SilencedEntityTypeBuilder create(
        EntityType.EntityFactory<T> entity,
        MobCategory mobCategory,
        float width,
        float height
    ) {
        return (SilencedEntityTypeBuilder) EntityType.Builder.of(entity, mobCategory).sized(width, height);
    }

    public static void initialize() {}
}
