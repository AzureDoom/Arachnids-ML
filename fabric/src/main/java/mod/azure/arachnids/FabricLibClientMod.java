package mod.azure.arachnids;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import mod.azure.arachnids.client.renderer.*;
import mod.azure.arachnids.registry.EntityRegistry;

public class FabricLibClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityRegistry.WARRIORBUG.get(), WarriorBugRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.WORKERBUG.get(), WorkerBugRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.HOPPERBUG.get(), HopperBugRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.CHARIOTBUG.get(), ChariotBugRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.BRAINBUG.get(), BrainBugRenderer::new);
    }
}
