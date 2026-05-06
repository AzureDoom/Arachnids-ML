package mod.azure.arachnids.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class CrawlingManager {

    private CrawlingManager() {}

    public static boolean canWallCrawl(Mob mob) {
        return mob instanceof WallCrawlingMob && MovementUtils.canWallCrawl(mob);
    }

    public static void setWallCrawling(Mob mob, boolean crawling) {
        if (mob instanceof WallCrawlingMob wallCrawler) {
            wallCrawler.arachnids$setWallCrawling(crawling);
        }
    }

    /**
     * Returns true if the mob was wall-crawling recently — either it is currently crawling, or it still has grace ticks
     * remaining from a recent crawl. Use this in actions that take over from CrawlToTargetAction so they can inherit
     * the crawling state even after the previous action cleared the flag.
     */
    public static boolean wasRecentlyWallCrawling(Mob mob) {
        if (!(mob instanceof WallCrawlingMob wallCrawler))
            return false;
        return wallCrawler.arachnids$isWallCrawling()
            || wallCrawler.arachnids$getWallCrawlGraceTicks() > 0;
    }

    public static boolean isWallCrawling(Mob mob) {
        return mob instanceof WallCrawlingMob wallCrawler
            && wallCrawler.arachnids$isWallCrawling();
    }

    public static boolean shouldUseWallCrawlingTo(Mob mob, BlockPos destination) {
        if (!canWallCrawl(mob) || destination == null) {
            return false;
        }

        var destVec = Vec3.atBottomCenterOf(destination);

        if (MovementUtils.needsWallCrawl(mob, destVec)) {
            return true;
        }

        var yDiff = destination.getY() - mob.blockPosition().getY();

        if (Math.abs(yDiff) >= 2) {
            return true;
        }

        return MovementUtils.isClimbable(mob.level(), destination, false);
    }

    public static boolean shouldUseWallCrawlingTo(Mob mob, LivingEntity target) {
        if (!canWallCrawl(mob) || target == null || !target.isAlive()) {
            return false;
        }

        if (MovementUtils.needsWallCrawl(mob, target.position())) {
            return true;
        }

        var yDiff = target.blockPosition().getY() - mob.blockPosition().getY();

        if (Math.abs(yDiff) >= 2) {
            return true;
        }

        return MovementUtils.isClimbable(mob.level(), target.blockPosition(), false);
    }

    public static void updateWallCrawlingPhysics(Mob mob) {
        if (!(mob instanceof WallCrawlingMob wallCrawler)) {
            mob.setNoGravity(false);
            return;
        }

        if (wallCrawler.arachnids$isWallCrawling()) {
            wallCrawler.arachnids$setWallCrawlGraceTicks(3);
        } else if (wallCrawler.arachnids$getWallCrawlGraceTicks() > 0) {
            wallCrawler.arachnids$setWallCrawlGraceTicks(
                wallCrawler.arachnids$getWallCrawlGraceTicks() - 1
            );
        }

        var isCrawling = wallCrawler.arachnids$isWallCrawling();
        var graceTicks = wallCrawler.arachnids$getWallCrawlGraceTicks();

        var touchingSurface = isAdjacentToAnySurface(mob);
        var active = (isCrawling || graceTicks > 0) && touchingSurface;

        mob.setNoGravity(active);

        if (active) {
            mob.fallDistance = 0.0F;
        }
    }

    public static void updateCrawlOrientation(Mob mob, Vec3 movement) {
        if (!(mob instanceof WallCrawlingMob wallCrawler)) {
            return;
        }

        var up = findSurfaceNormal(mob);

        if (up == null) {
            up = new Vec3(0.0D, 1.0D, 0.0D);
        }

        var forward = new Vec3(movement.x, movement.y, movement.z);

        if (forward.lengthSqr() < 0.0001D) {
            forward = wallCrawler.arachnids$getCrawlForward();
        }

        forward = forward.subtract(up.scale(forward.dot(up)));

        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 1.0D, 0.0D).subtract(up.scale(up.y));
        }

        if (forward.lengthSqr() < 0.0001D) {
            forward = wallCrawler.arachnids$getCrawlForward();
        }

        wallCrawler.arachnids$setCrawlOrientation(
            forward.normalize(),
            up.normalize(),
            distanceToSurface(mob, up.scale(-1.0D))
        );
    }

    private static Vec3 findSurfaceNormal(Mob mob) {
        var level = mob.level();
        var box = mob.getBoundingBox();

        Vec3 bestSurfaceUp = null;
        var bestDistance = Double.MAX_VALUE;

        Vec3 currentUp = null;
        double hysteresisBonus = 0.0D;
        if (mob instanceof WallCrawlingMob wc) {
            var up = wc.arachnids$getCrawlUp();
            if (up != null && up.lengthSqr() > 0.0001D) {
                currentUp = up;
                hysteresisBonus = 0.8D;
            }
        }

        var horizontalDirections = new Direction[] {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
        };

        var detectionProbe = 0.25D;

        for (var direction : horizontalDirections) {
            var intoSurface = Vec3.atLowerCornerOf(direction.getNormal());
            var movedBox = box.move(intoSurface.scale(detectionProbe));

            if (!level.noBlockCollision(mob, movedBox)) {
                var distance = distanceToSurface(mob, intoSurface);
                var candidateUp = intoSurface.scale(-1.0D);

                var effectiveDistance = distance;
                if (currentUp != null && candidateUp.dot(currentUp) < 0.5D) {
                    effectiveDistance += hysteresisBonus;
                }

                if (effectiveDistance < bestDistance) {
                    bestDistance = effectiveDistance;
                    bestSurfaceUp = candidateUp;
                }
            }
        }

        if (bestSurfaceUp != null) {
            return bestSurfaceUp;
        }

        if (!mob.onGround()) {
            var intoCeiling = new Vec3(0.0D, 1.0D, 0.0D);
            if (!level.noBlockCollision(mob, box.move(intoCeiling.scale(0.15D)))) {
                return new Vec3(0.0D, -1.0D, 0.0D);
            }

            var intoFloor = new Vec3(0.0D, -1.0D, 0.0D);
            if (!level.noBlockCollision(mob, box.move(intoFloor.scale(0.15D)))) {
                return new Vec3(0.0D, 1.0D, 0.0D);
            }
        }

        if (mob instanceof WallCrawlingMob wc) {
            var lastUp = wc.arachnids$getCrawlUp();
            if (lastUp != null && lastUp.lengthSqr() > 0.0001D) {
                return lastUp;
            }
        }

        return new Vec3(0.0D, 1.0D, 0.0D);
    }

    private static boolean isAdjacentToAnySurface(Mob mob) {
        var level = mob.level();
        var box = mob.getBoundingBox();

        var probe = (mob.getBbWidth() / 2.0D) + 0.5D;

        return !level.noBlockCollision(mob, box.move(probe, 0, 0))
            || !level.noBlockCollision(mob, box.move(-probe, 0, 0))
            || !level.noBlockCollision(mob, box.move(0, 0, probe))
            || !level.noBlockCollision(mob, box.move(0, 0, -probe))
            || !level.noBlockCollision(mob, box.move(0, probe, 0));
    }

    private static double distanceToSurface(Mob mob, Vec3 normal) {
        var level = mob.level();
        var box = mob.getBoundingBox();

        for (var distance = 0.0D; distance <= 1.5D; distance += 0.05D) {
            var movedBox = box.move(normal.scale(distance));

            if (!level.noBlockCollision(mob, movedBox)) {
                return distance;
            }
        }

        return mob.getBbHeight() / 2.0D;
    }
}
