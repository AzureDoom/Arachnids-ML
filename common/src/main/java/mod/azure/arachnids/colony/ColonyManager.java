package mod.azure.arachnids.colony;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.*;

import mod.azure.arachnids.CommonMod;
import mod.azure.arachnids.mob.brainbug.BrainBug;

public final class ColonyManager {

    private static final ColonyManager INSTANCE = new ColonyManager();

    public static ColonyManager get() {
        return INSTANCE;
    }

    private final Map<UUID, BugColony> colonies = new HashMap<>();

    private static final int TICK_INTERVAL = 20;

    private long lastTick = -1;

    private ColonyManager() {}

    public void getOrCreate(BrainBug brain, ServerLevel level) {
        var id = brain.getUUID();

        if (colonies.containsKey(id)) {
            colonies.get(id);
            return;
        }

        var brainPos = brain.blockPosition();
        for (var existing : colonies.values()) {
            if (!existing.isDisbanded() && existing.getBounds().isTooCloseToAnotherColony(level, brainPos)) {
                CommonMod.LOGGER.info(
                    "Colony already exists near {}, existing colony centre is {}",
                    brainPos,
                    existing.getBounds().centre()
                );
                brain.discard();
                return;
            }

            if (!existing.isDisbanded() && existing.getBounds().isInsideTerritory(brainPos)) {
                return;
            }
        }

        var colony = new BugColony(brain);
        colonies.put(id, colony);
        colony.initialSpawn(level);
    }

    public void load(BrainBug brain, CompoundTag tag, ServerLevel level) {
        var colony = BugColony.load(brain, tag, level);
        colonies.put(brain.getUUID(), colony);
    }

    public BugColony get(BrainBug brain) {
        return colonies.get(brain.getUUID());
    }

    public BugColony colonyOf(Mob mob) {
        for (var colony : colonies.values()) {
            if (colony.isMember(mob))
                return colony;
        }
        return null;
    }

    public void remove(BrainBug brain) {
        var colony = colonies.remove(brain.getUUID());
        if (colony != null)
            colony.disband();
    }

    public void tick(ServerLevel level) {
        var now = level.getGameTime();
        if (now - lastTick < TICK_INTERVAL)
            return;
        lastTick = now;

        Iterator<BugColony> it = colonies.values().iterator();
        while (it.hasNext()) {
            var colony = it.next();
            if (colony.isDisbanded()) {
                it.remove();
            } else {
                colony.tick(level);
            }
        }
    }

    public Collection<BugColony> allColonies() {
        return Collections.unmodifiableCollection(colonies.values());
    }
}
