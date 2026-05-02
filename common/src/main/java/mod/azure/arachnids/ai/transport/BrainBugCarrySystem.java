package mod.azure.arachnids.ai.transport;

import net.minecraft.world.phys.Vec3;

import java.util.*;

import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;

public final class BrainBugCarrySystem {

    private static final BrainBugCarrySystem INSTANCE = new BrainBugCarrySystem();

    public static BrainBugCarrySystem get() {
        return INSTANCE;
    }

    public static final int MAX_CARRIERS = 4;

    private static final double MAX_SPEED = 0.22D;

    private static final double SPEED_PER_CARRIER = MAX_SPEED;

    private final Map<UUID, Set<UUID>> carriersByBrain = new HashMap<>();

    private final Map<UUID, UUID> brainByCarrier = new HashMap<>();

    private final Map<UUID, Vec3> pendingPush = new HashMap<>();

    private static final long UNDER_ATTACK_TICKS = 100L;

    private final Map<UUID, Long> underAttackUntil = new HashMap<>();

    private static final double MAX_FLEE_DISTANCE = 30.0D;

    private static final double MAX_FLEE_DISTANCE_SQ = MAX_FLEE_DISTANCE * MAX_FLEE_DISTANCE;

    private final Map<UUID, Vec3> threatOriginByBrain = new HashMap<>();

    private final Set<UUID> wantsCarriers = new HashSet<>();

    private static final int MIN_CARRIERS_TO_MOVE = 3;

    private BrainBugCarrySystem() {}

    public boolean tryRegister(BrainBug brain, ChariotBug carrier) {
        if (!brain.isAlive())
            return false;

        var brainId = brain.getUUID();
        var carrierId = carrier.getUUID();

        var carriers = carriersByBrain.computeIfAbsent(brainId, k -> new LinkedHashSet<>());
        if (carriers.size() >= MAX_CARRIERS)
            return false;

        carriers.add(carrierId);
        brainByCarrier.put(carrierId, brainId);
        return true;
    }

    public void unregister(ChariotBug carrier) {
        var carrierId = carrier.getUUID();
        var brainId = brainByCarrier.remove(carrierId);
        if (brainId == null)
            return;

        var carriers = carriersByBrain.get(brainId);
        if (carriers != null) {
            carriers.remove(carrierId);
            if (carriers.isEmpty()) {
                carriersByBrain.remove(brainId);
            }
        }

        pendingPush.remove(brainId);
    }

    public void pushCarried(ChariotBug carrier, Vec3 direction) {
        var brainId = brainByCarrier.get(carrier.getUUID());
        if (brainId == null)
            return;

        var contribution = direction.normalize().scale(SPEED_PER_CARRIER);
        pendingPush.merge(brainId, contribution, Vec3::add);
    }

    public void applyPushes(BrainBug brain) {
        var brainId = brain.getUUID();

        if (!canMoveCarried(brain)) {
            pendingPush.remove(brainId);
            brain.setDeltaMovement(0.0D, brain.getDeltaMovement().y, 0.0D);
            return;
        }

        var push = pendingPush.remove(brainId);
        if (push == null)
            return;

        var horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
        if (horizontal > MAX_SPEED) {
            var scale = MAX_SPEED / horizontal;
            push = new Vec3(push.x * scale, 0.0D, push.z * scale);
        }

        brain.setDeltaMovement(push.x, brain.getDeltaMovement().y, push.z);
        brain.hasImpulse = true;
    }

    public boolean isCarried(BrainBug brain) {
        var carriers = carriersByBrain.get(brain.getUUID());
        return carriers != null && !carriers.isEmpty();
    }

    public boolean canMoveCarried(BrainBug brain) {
        return isUnderAttack(brain)
            && !hasFledFarEnough(brain)
            && carrierCount(brain) >= MIN_CARRIERS_TO_MOVE;
    }

    public int carrierCount(BrainBug brain) {
        var carriers = carriersByBrain.get(brain.getUUID());
        return carriers == null ? 0 : carriers.size();
    }

    public boolean hasOpenSlot(BrainBug brain) {
        var carriers = carriersByBrain.get(brain.getUUID());
        return carriers == null || carriers.size() < MAX_CARRIERS;
    }

    public boolean isRegistered(ChariotBug carrier) {
        return brainByCarrier.containsKey(carrier.getUUID());
    }

    public void removeBrain(BrainBug brain) {
        var brainId = brain.getUUID();
        var carriers = carriersByBrain.remove(brainId);
        if (carriers != null) {
            for (var carrierId : carriers) {
                brainByCarrier.remove(carrierId);
            }
        }
        pendingPush.remove(brainId);
        threatOriginByBrain.remove(brainId);
        underAttackUntil.remove(brainId);
        wantsCarriers.remove(brainId);
    }

    public void signalUnderAttack(BrainBug brain, Vec3 threatOrigin) {
        var brainId = brain.getUUID();

        underAttackUntil.put(brainId, brain.level().getGameTime() + UNDER_ATTACK_TICKS);

        threatOriginByBrain.putIfAbsent(brainId, threatOrigin);
    }

    public boolean hasFledFarEnough(BrainBug brain) {
        var origin = threatOriginByBrain.get(brain.getUUID());
        if (origin == null)
            return false;

        var dx = brain.getX() - origin.x;
        var dz = brain.getZ() - origin.z;
        return (dx * dx + dz * dz) >= MAX_FLEE_DISTANCE_SQ;
    }

    public void clearAttackSignal(BrainBug brain) {
        var brainId = brain.getUUID();
        underAttackUntil.remove(brainId);
        threatOriginByBrain.remove(brainId);
        pendingPush.remove(brainId);
        wantsCarriers.remove(brainId);
    }

    public Vec3 getThreatOrigin(BrainBug brain) {
        return threatOriginByBrain.get(brain.getUUID());
    }

    public boolean isUnderAttack(BrainBug brain) {
        var brainId = brain.getUUID();

        if (hasFledFarEnough(brain)) {
            clearAttackSignal(brain);
            brain.setDeltaMovement(0.0D, brain.getDeltaMovement().y, 0.0D);
            return false;
        }

        var until = underAttackUntil.get(brainId);
        if (until == null)
            return false;

        if (brain.level().getGameTime() > until) {
            underAttackUntil.remove(brainId);
            threatOriginByBrain.remove(brainId);
            return false;
        }

        return true;
    }

    public void signalWantsCarriers(BrainBug brain) {
        wantsCarriers.add(brain.getUUID());
    }

    public void clearWantsCarriers(BrainBug brain) {
        wantsCarriers.remove(brain.getUUID());
    }

    public boolean wantsCarriersExist() {
        return !wantsCarriers.isEmpty();
    }
}
