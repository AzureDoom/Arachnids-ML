package mod.azure.arachnids;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import mod.azure.arachnids.colony.ColonyManager;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.mob.hopperbug.HopperBug;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;
import mod.azure.arachnids.mob.worker.WorkerBug;
import mod.azure.arachnids.registry.EntityRegistry;
import mod.azure.arachnids.registry.ItemRegistry;
import mod.azure.arachnids.util.SpawnUtils;

@Mod(CommonMod.MOD_ID)
public final class NeoForgeMod {

    public static DeferredRegister<EntityType<?>> entityTypeDeferredRegister = DeferredRegister.create(
        BuiltInRegistries.ENTITY_TYPE,
        CommonMod.MOD_ID
    );

    public static DeferredRegister<Item> itemDeferredRegister = DeferredRegister.create(
        BuiltInRegistries.ITEM,
        CommonMod.MOD_ID
    );

    public static DeferredRegister<SoundEvent> soundEventDeferredRegister = DeferredRegister.create(
        BuiltInRegistries.SOUND_EVENT,
        CommonMod.MOD_ID
    );

    public NeoForgeMod(IEventBus modEventBus) {
        CommonMod.initRegistries();
        entityTypeDeferredRegister.register(modEventBus);
        itemDeferredRegister.register(modEventBus);
        soundEventDeferredRegister.register(modEventBus);
        modEventBus.addListener(this::createEntityAttributes);
        modEventBus.addListener(this::addSpawnPlacements);
        ModEntitySpawn.SERIALIZER.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabs);
        NeoForge.EVENT_BUS.addListener(
            (LevelTickEvent.Post event) -> {
                if (event.getLevel() instanceof ServerLevel serverLevel) {
                    ColonyManager.get().tick(serverLevel);
                }
            }
        );
    }

    public void createEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.WARRIORBUG.get(), WarriorBug.createMobAttributes().build());
        event.put(EntityRegistry.WORKERBUG.get(), WorkerBug.createMobAttributes().build());
        event.put(EntityRegistry.HOPPERBUG.get(), HopperBug.createMobAttributes().build());
        event.put(EntityRegistry.CHARIOTBUG.get(), ChariotBug.createMobAttributes().build());
        event.put(EntityRegistry.BRAINBUG.get(), BrainBug.createMobAttributes().build());
    }

    public void addCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ItemRegistry.WARRIOR_SPAWN_EGG.get());
            event.accept(ItemRegistry.WORKER_SPAWN_EGG.get());
            event.accept(ItemRegistry.HOPPER_SPAWN_EGG.get());
            event.accept(ItemRegistry.CHARIOT_SPAWN_EGG.get());
            event.accept(ItemRegistry.BRAIN_SPAWN_EGG.get());
        }
    }

    public void addSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            EntityRegistry.WORKERBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ((entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)),
            RegisterSpawnPlacementsEvent.Operation.AND
        );
        event.register(
            EntityRegistry.WARRIORBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ((entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)),
            RegisterSpawnPlacementsEvent.Operation.AND
        );
        event.register(
            EntityRegistry.HOPPERBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ((entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)),
            RegisterSpawnPlacementsEvent.Operation.AND
        );
        event.register(
            EntityRegistry.BRAINBUG.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ((entityType, world, reason, pos, random) -> SpawnUtils.isDesertSpawn(world, pos)),
            RegisterSpawnPlacementsEvent.Operation.AND
        );
    }

    record ModEntitySpawn(
        HolderSet<Biome> biomes,
        MobSpawnSettings.SpawnerData spawn
    ) implements BiomeModifier {

        public static DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZER = DeferredRegister.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            CommonMod.MOD_ID
        );

        static Supplier<MapCodec<ModEntitySpawn>> SPAWN_CODEC = SERIALIZER.register(
            "mobspawns",
            () -> RecordCodecBuilder.mapCodec(
                builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(ModEntitySpawn::biomes),
                    MobSpawnSettings.SpawnerData.CODEC.fieldOf("spawn")
                        .forGetter(
                            ModEntitySpawn::spawn
                        )
                ).apply(builder, ModEntitySpawn::new)
            )
        );

        @Override
        public void modify(
            @NotNull Holder<Biome> biome,
            @NotNull Phase phase,
            ModifiableBiomeInfo.BiomeInfo.@NotNull Builder builder
        ) {
            if (phase == Phase.ADD && this.biomes.contains(biome)) {
                builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, this.spawn);
            }
        }

        @Override
        public @NotNull MapCodec<? extends BiomeModifier> codec() {
            return SPAWN_CODEC.get();
        }
    }
}
