package mod.azure.arachnids.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

import mod.azure.arachnids.ai.group.SquadRegistry;
import mod.azure.arachnids.ai.group.TacticalRole;
import mod.azure.arachnids.mob.brainbug.BrainBug;
import mod.azure.arachnids.mob.chariotbug.ChariotBug;
import mod.azure.arachnids.mob.hopperbug.HopperBug;
import mod.azure.arachnids.mob.warriorbug.WarriorBug;
import mod.azure.arachnids.mob.worker.WorkerBug;

public final class BugColony {

    public static final int MAX_WORKERS = 6;

    public static final int MAX_WARRIORS = 10;

    public static final int MAX_HOPPERS = 4;

    public static final int MAX_CHARIOTS = 4;

    private static final int REGEN_INTERVAL = 20;

    private static final int DESPAWN_CHECK_INTERVAL = 60;

    private static final int PATROL_UPDATE_INTERVAL = 80;

    private final BrainBug brain;

    private ColonyBounds bounds;

    private final ThreatEvaluator threatEval = new ThreatEvaluator();

    private final VirtualPopulation virtualPop = new VirtualPopulation();

    private final ColonyRegen regen = new ColonyRegen();

    private final ColonySpawner spawner = new ColonySpawner();

    private final ColonyTunnelDigger tunnelDigger = new ColonyTunnelDigger();

    private ColonyState state = ColonyState.PEACEFUL;

    private boolean disbandPending = false;

    private boolean initialSpawnDone = false;

    private final Set<UUID> activeWorkers = new LinkedHashSet<>();

    private final Set<UUID> activeWarriors = new LinkedHashSet<>();

    private final Set<UUID> activeHoppers = new LinkedHashSet<>();

    private final Set<UUID> activeChariots = new LinkedHashSet<>();

    private LivingEntity warriorDirective = null;

    private final Random rng = new Random();

    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.literal("Bug Colony"),
        BossEvent.BossBarColor.GREEN,
        BossEvent.BossBarOverlay.NOTCHED_10
    );

    BugColony(BrainBug brain) {
        this.brain = brain;
        this.bounds = new ColonyBounds(brain.blockPosition());

        virtualPop.setVirtualChariots(0);
        virtualPop.setVirtualWorkers(0);
        virtualPop.setVirtualWarriors(0);
        virtualPop.setVirtualHoppers(0);
    }

    void initialSpawn(ServerLevel level) {
        if (initialSpawnDone) {
            return;
        }

        initialSpawnDone = true;
        doInitialSpawn(level);
        tickBossBar(level);
    }

    private void doInitialSpawn(ServerLevel level) {
        for (var i = 0; i < MAX_CHARIOTS && !isAtActiveCap(); i++) {
            var c = spawner.trySpawnChariot(level, bounds, brain, rng);
            if (c != null)
                registerChariot(c);
            else
                virtualPop.regenChariot(1);
        }
        for (var i = 0; i < MAX_WORKERS && !isAtActiveCap(); i++) {
            var w = spawner.trySpawnWorker(level, bounds, brain, rng);
            if (w != null)
                registerWorker(w);
            else
                virtualPop.regenWorker(1);
        }
        for (int i = 0; i < MAX_WARRIORS && !isAtActiveCap(); i++) {
            var wa = spawner.trySpawnWarrior(level, bounds, brain, rng);
            if (wa != null)
                registerWarrior(wa);
            else
                virtualPop.regenWarrior(1);
        }
        if (isAboveGround()) {
            for (var i = 0; i < MAX_HOPPERS && !isAtActiveCap(); i++) {
                var h = spawner.trySpawnHopper(level, bounds, brain, rng);
                if (h != null)
                    registerHopper(h);
                else
                    virtualPop.regenHopper(1);
            }
        }
        updateBossBar();
    }

    public void tick(ServerLevel level) {
        if (disbandPending)
            return;
        if (!brain.isAlive()) {
            disband();
            return;
        }

        if (!initialSpawnDone) {
            initialSpawn(level);
        }

        bounds.updateCentre(brain.blockPosition());

        var now = level.getGameTime();

        threatEval.tick(level, bounds.territoryAABB());
        var newState = threatEval.evaluateState();

        if (!isBrainProtected(level))
            newState = ColonyState.PANIC;
        state = newState;

        if (now % PATROL_UPDATE_INTERVAL == 0) {
            updateWarriorDirectives(level);
        }

        if (now % REGEN_INTERVAL == 0) {
            tickRegen(level);
        }

        tickConstruction(level);

        if (now % DESPAWN_CHECK_INTERVAL == 0) {
            despawnFarMembers(level);
        }

        pruneDeadMembers(level);

        tickBossBar(level);
    }

    public void disband() {
        disbandPending = true;
        state = ColonyState.DISBANDED;
        bossBar.removeAllPlayers();
        bossBar.setVisible(false);
    }

    public void registerWorker(WorkerBug bug) {
        activeWorkers.add(bug.getUUID());
    }

    public void registerWarrior(WarriorBug bug) {
        activeWarriors.add(bug.getUUID());
    }

    public void registerHopper(HopperBug bug) {
        activeHoppers.add(bug.getUUID());
    }

    public void registerChariot(ChariotBug bug) {
        activeChariots.add(bug.getUUID());
    }

    public boolean isMember(Mob mob) {
        var id = mob.getUUID();
        return activeWorkers.contains(id) || activeWarriors.contains(id)
            || activeHoppers.contains(id) || activeChariots.contains(id);
    }

    public void onMemberHurt(Mob member, DamageSource source, float amount) {
        threatEval.onMemberHurt(source, amount);
    }

    public void onMemberDeath(Mob member) {
        var id = member.getUUID();
        if (activeChariots.remove(id)) {
            virtualPop.killVirtualChariots();
        } else if (activeWorkers.remove(id)) {
            virtualPop.killVirtualWorker();
        } else if (activeWarriors.remove(id)) {
            virtualPop.killVirtualWarrior();
        } else if (activeHoppers.remove(id)) {
            virtualPop.killVirtualHopper();
        } else {
            activeChariots.remove(id);
        }
    }

    public void onExplosion() {
        threatEval.onExplosion();
    }

    public ColonyState getState() {
        return state;
    }

    public BrainBug getBrain() {
        return brain;
    }

    public ColonyBounds getBounds() {
        return bounds;
    }

    public boolean isDisbanded() {
        return disbandPending;
    }

    public LivingEntity getWarriorDirective() {
        return warriorDirective;
    }

    public int activeWorkerCount() {
        return activeWorkers.size();
    }

    public int activeWarriorCount() {
        return activeWarriors.size();
    }

    public int activeHopperCount() {
        return activeHoppers.size();
    }

    public int activeChariotCount() {
        return activeChariots.size();
    }

    public boolean isAtActiveCap() {
        int current =
            activeWorkers.size()
                + activeWarriors.size()
                + activeChariots.size()
                + activeHoppers.size();

        int max =
            MAX_WORKERS
                + MAX_WARRIORS
                + MAX_CHARIOTS
                + MAX_HOPPERS;

        return current >= max;
    }

    public BlockPos patrolCentre() {
        return bounds.centre();
    }

    public ColonyTunnelDigger getTunnelDigger() {
        return tunnelDigger;
    }

    public boolean hasLivingWorkers() {
        return !activeWorkers.isEmpty() || virtualPop.getVirtualWorkers() > 0;
    }

    private static ListTag saveUuidSet(Set<UUID> ids) {
        var list = new ListTag();

        for (var id : ids) {
            var entry = new CompoundTag();
            entry.putUUID("Id", id);
            list.add(entry);
        }

        return list;
    }

    private static void loadUuidSet(CompoundTag tag, String key, Set<UUID> target) {
        target.clear();

        if (!tag.contains(key, Tag.TAG_LIST))
            return;

        var list = tag.getList(key, Tag.TAG_COMPOUND);

        for (var i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);

            if (entry.hasUUID("Id")) {
                target.add(entry.getUUID("Id"));
            }
        }
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.put("Bounds", bounds.save());
        tag.putString("State", state.name());
        tag.putBoolean("DisbandPending", disbandPending);
        tag.putBoolean("InitialSpawnDone", initialSpawnDone);

        tag.put("ActiveWorkers", saveUuidSet(activeWorkers));
        tag.put("ActiveWarriors", saveUuidSet(activeWarriors));
        tag.put("ActiveHoppers", saveUuidSet(activeHoppers));
        tag.put("ActiveChariots", saveUuidSet(activeChariots));

        tag.put("VirtualPopulation", virtualPop.save());
        tag.put("Regen", regen.save());
        tag.put("Threats", threatEval.save());

        return tag;
    }

    public static BugColony load(BrainBug brain, CompoundTag tag, ServerLevel level) {
        BugColony colony = new BugColony(brain);

        if (tag.contains("Bounds", Tag.TAG_COMPOUND)) {
            colony.bounds = ColonyBounds.load(tag.getCompound("Bounds"));
        }

        if (tag.contains("State", Tag.TAG_STRING)) {
            try {
                colony.state = ColonyState.valueOf(tag.getString("State"));
            } catch (IllegalArgumentException ignored) {
                colony.state = ColonyState.PEACEFUL;
            }
        }

        colony.disbandPending = tag.getBoolean("DisbandPending");
        colony.initialSpawnDone = tag.getBoolean("InitialSpawnDone");

        loadUuidSet(tag, "ActiveWorkers", colony.activeWorkers);
        loadUuidSet(tag, "ActiveWarriors", colony.activeWarriors);
        loadUuidSet(tag, "ActiveHoppers", colony.activeHoppers);
        loadUuidSet(tag, "ActiveChariots", colony.activeChariots);

        if (tag.contains("VirtualPopulation", Tag.TAG_COMPOUND)) {
            colony.virtualPop.load(tag.getCompound("VirtualPopulation"));
        }

        if (tag.contains("Regen", Tag.TAG_COMPOUND)) {
            colony.regen.load(tag.getCompound("Regen"));
        }

        if (tag.contains("Threats", Tag.TAG_COMPOUND)) {
            colony.threatEval.load(tag.getCompound("Threats"), level);
        }

        colony.updateBossBar();

        return colony;
    }

    private void updateWarriorDirectives(ServerLevel level) {
        var highest = threatEval.highestThreat();

        if (state == ColonyState.PEACEFUL || highest == null) {
            warriorDirective = null;
            return;
        }

        warriorDirective = highest;

        for (var wid : activeWarriors) {
            var entity = level.getEntity(wid);
            if (entity instanceof WarriorBug warrior) {
                var board = SquadRegistry.get().getOrJoinSquad(warrior);
                if (board != null && highest.isAlive()) {
                    board.targetPriority.remove(highest);
                    board.targetPriority.addFirst(highest);
                    board.roleTargets.put(TacticalRole.FRONTLINE, highest);
                    board.roleTargets.put(TacticalRole.FLANKER, highest);
                    board.roleTargets.put(TacticalRole.SUPPORT, highest);
                }
            }
        }
    }

    private void tickRegen(ServerLevel level) {
        var workersDead = !hasLivingWorkers();
        var brainSafe = (state != ColonyState.PANIC);

        var result = regen.tick(state, workersDead, brainSafe);
        if (result.isEmpty())
            return;

        var aboveGround = isAboveGround();
        var playerNearby = isPlayerNear(level);

        var wantChariots = (MAX_CHARIOTS - activeChariots.size()) - virtualPop.getVirtualChariots();
        for (var i = 0; i < result.workers() && wantChariots > 0 && !isAtActiveCap(); i++, wantChariots--) {
            if (playerNearby) {
                var w = spawner.trySpawnChariot(level, bounds, brain, rng);
                if (w != null)
                    registerChariot(w);
            } else {
                virtualPop.regenChariot(1);
            }
        }

        var wantWorkers = (MAX_WORKERS - activeWorkers.size()) - virtualPop.getVirtualWorkers();
        for (var i = 0; i < result.workers() && wantWorkers > 0 && !isAtActiveCap(); i++, wantWorkers--) {
            if (playerNearby) {
                var w = spawner.trySpawnWorker(level, bounds, brain, rng);
                if (w != null)
                    registerWorker(w);
            } else {
                virtualPop.regenWorker(1);
            }
        }

        int wantWarriors = (MAX_WARRIORS - activeWarriors.size()) - virtualPop.getVirtualWarriors();
        for (var i = 0; i < result.warriors() && wantWarriors > 0 && !isAtActiveCap(); i++, wantWarriors--) {
            if (playerNearby) {
                var wa = spawner.trySpawnWarrior(level, bounds, brain, rng);
                if (wa != null)
                    registerWarrior(wa);
            } else {
                virtualPop.regenWarrior(1);
            }
        }

        if (aboveGround) {
            var wantHoppers = (MAX_HOPPERS - activeHoppers.size()) - virtualPop.getVirtualHoppers();
            for (var i = 0; i < result.hoppers() && wantHoppers > 0 && !isAtActiveCap(); i++, wantHoppers--) {
                if (playerNearby) {
                    var h = spawner.trySpawnHopper(level, bounds, brain, rng);
                    if (h != null)
                        registerHopper(h);
                } else {
                    virtualPop.regenHopper(1);
                }
            }
        }
    }

    private void tickConstruction(ServerLevel level) {
        tunnelDigger.plan(level, bounds.centre(), rng);
    }

    private void despawnFarMembers(ServerLevel level) {
        despawnSet(level, activeChariots);
        despawnSet(level, activeWorkers);
        despawnSet(level, activeWarriors);
        despawnSet(level, activeHoppers);
    }

    private void despawnSet(ServerLevel level, Set<UUID> members) {
        List<Entity> discardEntities = new ArrayList<>();

        var iterator = members.iterator();

        while (iterator.hasNext()) {
            var id = iterator.next();
            var entity = level.getEntity(id);

            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            if (!isPlayerNear(level) && bounds.isFarAway(entity)) {
                discardEntities.add(entity);
                iterator.remove();
            }
        }

        for (var entity : discardEntities) {
            entity.discard();
        }
    }

    private void pruneDeadMembers(ServerLevel level) {
        pruneSet(level, activeWorkers);
        pruneSet(level, activeWarriors);
        pruneSet(level, activeHoppers);
        pruneSet(level, activeChariots);
    }

    private void pruneSet(ServerLevel level, Set<UUID> members) {
        members.removeIf(id -> {
            var entity = level.getEntity(id);
            return entity == null || !entity.isAlive();
        });
    }

    private boolean isAboveGround() {
        var surfY = brain.level()
            .getHeight(
                Heightmap.Types.WORLD_SURFACE,
                brain.getBlockX(),
                brain.getBlockZ()
            );
        return brain.getBlockY() >= surfY - 2;
    }

    private boolean isBrainProtected(ServerLevel level) {
        if (!isAboveGround())
            return true;
        var brainPos = brain.blockPosition();
        return bounds.isInsideTerritory(brainPos);
    }

    private boolean isPlayerNear(ServerLevel level) {
        var expanded = bounds.territoryAABB().inflate(32);
        return !level.getEntitiesOfClass(
            Player.class,
            expanded,
            p -> !p.isSpectator() && !p.isCreative()
        ).isEmpty();
    }

    private void tickBossBar(ServerLevel level) {
        var expanded = bounds.territoryAABB().inflate(32);
        var nearby = level.getEntitiesOfClass(
            ServerPlayer.class,
            expanded,
            p -> !p.isSpectator()
        );

        for (var player : nearby) {
            bossBar.addPlayer(player);
        }

        for (var player : List.copyOf(bossBar.getPlayers())) {
            if (!nearby.contains(player)) {
                bossBar.removePlayer(player);
            }
        }

        updateBossBar();
    }

    private String formatState() {
        return switch (state) {
            case PEACEFUL -> "Peaceful";
            case PANIC -> "Under Attack";
            case DISBANDED -> "Disbanded";
            default -> state.name();
        };
    }

    private BossEvent.BossBarColor colorState() {
        return switch (state) {
            case PANIC -> BossEvent.BossBarColor.YELLOW;
            case DISBANDED -> BossEvent.BossBarColor.RED;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    private void updateBossBar() {
        bossBar.setName(Component.literal("Bug Colony - " + formatState()));
        bossBar.setColor(colorState());

        var maxMembers = MAX_WORKERS + MAX_WARRIORS + MAX_CHARIOTS + MAX_HOPPERS;

        var current = activeChariots.size() + virtualPop.getVirtualChariots()
            + activeWorkers.size() + virtualPop.getVirtualWorkers()
            + activeWarriors.size() + virtualPop.getVirtualWarriors()
            + activeHoppers.size() + virtualPop.getVirtualHoppers();

        var progress = Math.clamp((float) current / maxMembers, 0.0f, 1.0f);
        bossBar.setProgress(progress);
        bossBar.setVisible(!disbandPending);
    }
}
