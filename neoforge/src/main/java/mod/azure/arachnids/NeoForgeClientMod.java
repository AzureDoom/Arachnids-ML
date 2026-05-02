package mod.azure.arachnids;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import mod.azure.arachnids.client.renderer.*;
import mod.azure.arachnids.registry.EntityRegistry;

@EventBusSubscriber(modid = CommonMod.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClientMod {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.WARRIORBUG.get(), WarriorBugRenderer::new);
        event.registerEntityRenderer(EntityRegistry.WORKERBUG.get(), WorkerBugRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HOPPERBUG.get(), HopperBugRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CHARIOTBUG.get(), ChariotBugRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BRAINBUG.get(), BrainBugRenderer::new);
    }
}
