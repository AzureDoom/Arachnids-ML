package mod.azure.arachnids.ai.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

import mod.azure.arachnids.util.ModTags;

public final class TargetingUtils {

    private TargetingUtils() {}

    public static Predicate<LivingEntity> validTarget(Mob mob) {
        return baseValid(mob)
            .and(notAnnoyingMobs())
            .and(notExploding())
            .and(inRangeOrVisible(mob));
    }

    public static Predicate<LivingEntity> baseValid(Mob mob) {
        return e -> e != null &&
            e.isAlive() &&
            e != mob &&
            !e.isSpectator() &&
            !(e instanceof Player p && (p.isCreative() || p.isSpectator())) &&
            e.getType() != mob.getType() &&
            !e.getType().is(ModTags.ARACHNIDS);
    }

    public static Predicate<LivingEntity> notAnnoyingMobs() {
        return e -> !(e instanceof Bat) &&
            !(e instanceof WaterAnimal) &&
            !e.isInWater();
    }

    public static Predicate<LivingEntity> notExploding() {
        return e -> !(e instanceof Creeper c && (c.isIgnited() || c.getSwellDir() > 0));
    }

    public static Predicate<LivingEntity> inRangeOrVisible(Mob mob) {
        return e -> {
            double dist = mob.distanceToSqr(e);
            return dist <= 4096 || mob.hasLineOfSight(e);
        };
    }

    public static boolean isInAttackRange(Mob mob, LivingEntity target, double reach) {
        return mob.getBoundingBox()
            .inflate(reach)
            .intersects(target.getBoundingBox());
    }
}
