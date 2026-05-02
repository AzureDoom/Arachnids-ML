package mod.azure.arachnids;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;

import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.mob.hopperbug.HopperBug;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.registry.EntityRegistry;
import mod.azure.arachnids.registry.ItemRegistry;
import mod.azure.arachnids.util.ModTags;
import mod.azure.arachnids.util.SpawnUtils;

public final class FabricLibMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonMod.initRegistries();
        FabricDefaultAttributeRegistry.register(
            EntityRegistry.WARRIORBUG.get(),
            WarriorBug.createMobAttributes()
        );
        FabricDefaultAttributeRegistry.register(
            EntityRegistry.WORKERBUG.get(),
            WorkerBug.createMobAttributes()
        );
        FabricDefaultAttributeRegistry.register(
            EntityRegistry.HOPPERBUG.get(),
            HopperBug.createMobAttributes()
        );
        FabricDefaultAttributeRegistry.register(
            EntityRegistry.CHARIOTBUG.get(),
            ChariotBug.createMobAttributes()
        );
        FabricDefaultAttributeRegistry.register(
            EntityRegistry.BRAINBUG.get(),
            BrainBug.createMobAttributes()
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(ItemRegistry.WARRIOR_SPAWN_EGG.get());
            entries.accept(ItemRegistry.WORKER_SPAWN_EGG.get());
            entries.accept(ItemRegistry.HOPPER_SPAWN_EGG.get());
            entries.accept(ItemRegistry.CHARIOT_SPAWN_EGG.get());
            entries.accept(ItemRegistry.BRAIN_SPAWN_EGG.get());
        });
        ServerTickEvents.END_WORLD_TICK.register(serverLevel -> {
            ColonyManager.get().tick(serverLevel);
        });
        SpawnPlacements.register(
            EntityRegistry.WORKERBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ModTags.SPAWN_ARACHNIDS),
            MobCategory.MONSTER,
            EntityRegistry.WORKERBUG.get(),
            20,
            1,
            4
        );
        SpawnPlacements.register(
            EntityRegistry.WARRIORBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ModTags.SPAWN_ARACHNIDS),
            MobCategory.MONSTER,
            EntityRegistry.WARRIORBUG.get(),
            20,
            1,
            4
        );
        SpawnPlacements.register(
            EntityRegistry.HOPPERBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ModTags.SPAWN_ARACHNIDS),
            MobCategory.MONSTER,
            EntityRegistry.HOPPERBUG.get(),
            20,
            1,
            4
        );
        SpawnPlacements.register(
            EntityRegistry.BRAINBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ModTags.SPAWN_ARACHNIDS),
            MobCategory.MONSTER,
            EntityRegistry.BRAINBUG.get(),
            15,
            1,
            1
        );
    }
}
