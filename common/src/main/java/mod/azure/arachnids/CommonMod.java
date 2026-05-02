package mod.azure.arachnids;

import mod.azure.azurelib.AzureLibMod;
import mod.azure.azurelib.common.config.format.ConfigFormats;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mod.azure.arachnids.config.ArachnidsConfig;
import mod.azure.arachnids.registry.EntityRegistry;
import mod.azure.arachnids.registry.ItemRegistry;
import mod.azure.arachnids.registry.SoundRegistry;

public class CommonMod {

    public static ArachnidsConfig ARACHNIDS_CONFIG;

    public static final String MOD_ID = "arachnids";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static ResourceLocation modResource(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    public static void initRegistries() {
        ARACHNIDS_CONFIG = AzureLibMod.registerConfig(ArachnidsConfig.class, ConfigFormats.json()).getConfigInstance();
        EntityRegistry.initialize();
        SoundRegistry.initialize();
        ItemRegistry.initialize();
    }

    public static ArachnidsConfig getConfig() {
        return ARACHNIDS_CONFIG;
    }
}
