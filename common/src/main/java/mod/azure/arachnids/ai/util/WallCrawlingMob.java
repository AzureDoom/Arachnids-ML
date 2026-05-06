package mod.azure.arachnids.ai.util;

import net.minecraft.world.phys.Vec3;

public interface WallCrawlingMob {

    boolean arachnids$isWallCrawling();

    void arachnids$setWallCrawling(boolean crawling);

    int arachnids$getWallCrawlGraceTicks();

    void arachnids$setWallCrawlGraceTicks(int ticks);

    Vec3 arachnids$getCrawlForward();

    Vec3 arachnids$getOldCrawlForward();

    Vec3 arachnids$getCrawlUp();

    Vec3 arachnids$getOldCrawlUp();

    double arachnids$getCrawlDistFromBlock();

    double arachnids$getOldCrawlDistFromBlock();

    void arachnids$setCrawlOrientation(Vec3 forward, Vec3 up, double distFromBlock);
}
