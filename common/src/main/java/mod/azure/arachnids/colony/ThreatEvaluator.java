package mod.azure.arachnids.colony;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class ThreatEvaluator {

    private static final float SCORE_PER_HIT = 3.0F;

    private static final float SCORE_PLAYER_PROXIMITY = 5.0F;

    private static final float SCORE_EXPLOSION = 40.0F;

    private static final float SCORE_DECAY_PER_TICK = 0.5F;

    public static final float THRESHOLD_DEFEND = 80.0F;

    public static final float THRESHOLD_PANIC = 300.0F;

    private final Map<UUID, Float> entityScores = new LinkedHashMap<>();

    private final Map<UUID, LivingEntity> entityRefs = new HashMap<>();

    private float baseScore = 0.0F;

    public void onMemberHurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            addEntityScore(attacker, amount * SCORE_PER_HIT);
        } else {
            baseScore += amount * SCORE_PER_HIT;
        }
    }

    public void onExplosion() {
        baseScore += SCORE_EXPLOSION;
    }

    public void tick(ServerLevel level, AABB colonyAABB) {
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, colonyAABB.inflate(8));
        for (Player p : nearbyPlayers) {
            if (!p.isSpectator() && !p.isCreative()) {
                addEntityScore(p, SCORE_PLAYER_PROXIMITY);
            }
        }

        baseScore = Math.max(0, baseScore - SCORE_DECAY_PER_TICK);

        Iterator<Map.Entry<UUID, Float>> it = entityScores.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var decayed = entry.getValue() - SCORE_DECAY_PER_TICK;
            if (decayed <= 0) {
                it.remove();
                entityRefs.remove(entry.getKey());
            } else {
                entry.setValue(decayed);
            }
        }
    }

    public float totalScore() {
        var total = baseScore;
        for (float s : entityScores.values())
            total += s;
        return total;
    }

    public ColonyState evaluateState() {
        var total = totalScore();
        if (total >= THRESHOLD_PANIC)
            return ColonyState.PANIC;
        if (total >= THRESHOLD_DEFEND)
            return ColonyState.DEFEND;
        return ColonyState.PEACEFUL;
    }

    public LivingEntity highestThreat() {
        UUID best = null;
        var bestScore = -1F;
        for (var entry : entityScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                best = entry.getKey();
                bestScore = entry.getValue();
            }
        }
        if (best == null)
            return null;
        var e = entityRefs.get(best);
        return (e != null && e.isAlive()) ? e : null;
    }

    public List<LivingEntity> threatsByPriority() {
        List<Map.Entry<UUID, Float>> entries = new ArrayList<>(entityScores.entrySet());
        entries.sort(Map.Entry.<UUID, Float>comparingByValue().reversed());

        List<LivingEntity> result = new ArrayList<>();
        for (var entry : entries) {
            var e = entityRefs.get(entry.getKey());
            if (e != null && e.isAlive())
                result.add(e);
        }
        return result;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.putFloat("BaseScore", baseScore);

        var scores = new ListTag();

        for (var entry : entityScores.entrySet()) {
            var scoreTag = new CompoundTag();
            scoreTag.putUUID("Id", entry.getKey());
            scoreTag.putFloat("Score", entry.getValue());
            scores.add(scoreTag);
        }

        tag.put("EntityScores", scores);

        return tag;
    }

    public void load(CompoundTag tag, ServerLevel level) {
        baseScore = tag.getFloat("BaseScore");

        entityScores.clear();
        entityRefs.clear();

        if (!tag.contains("EntityScores", Tag.TAG_LIST))
            return;

        var scores = tag.getList("EntityScores", Tag.TAG_COMPOUND);

        for (var i = 0; i < scores.size(); i++) {
            var scoreTag = scores.getCompound(i);

            if (!scoreTag.hasUUID("Id"))
                continue;

            var id = scoreTag.getUUID("Id");
            var score = scoreTag.getFloat("Score");

            if (score <= 0)
                continue;

            entityScores.put(id, score);

            var entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                entityRefs.put(id, living);
            }
        }
    }

    private void addEntityScore(LivingEntity entity, float delta) {
        entityScores.merge(entity.getUUID(), delta, Float::sum);
        entityRefs.put(entity.getUUID(), entity);
    }
}
