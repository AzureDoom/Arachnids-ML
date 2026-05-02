package mod.azure.arachnids.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import mod.azure.arachnids.CommonMod;

public record ModTags() {

    public static final TagKey<Block> WEAK_BLOCKS = TagKey.create(
        Registries.BLOCK,
        CommonMod.modResource("weak_blocks")
    );

    public static final TagKey<Block> DANGER_BLOCKS = TagKey.create(
        Registries.BLOCK,
        CommonMod.modResource("danger_blocks")
    );

    public static final TagKey<Fluid> DANGER_FLUIDS = TagKey.create(
        Registries.FLUID,
        CommonMod.modResource("danger_fluids")
    );

    public static final TagKey<EntityType<?>> ARACHNIDS = TagKey.create(
        Registries.ENTITY_TYPE,
        CommonMod.modResource("arachnids")
    );

    public static final TagKey<Biome> SPAWN_ARACHNIDS = TagKey.create(
        Registries.BIOME,
        CommonMod.modResource("spawns_arachnids")
    );
}
