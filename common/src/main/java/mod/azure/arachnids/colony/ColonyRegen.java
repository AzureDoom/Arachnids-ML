package mod.azure.arachnids.colony;

import net.minecraft.nbt.CompoundTag;

public final class ColonyRegen {

    private static final float FAST_RATE = 0.08F;

    private static final float SLOW_RATE = 0.025F;

    private float workerAccum = 0F;

    private float warriorAccum = 0F;

    private float hopperAccum = 0F;

    private float chariotAccum = 0F;

    public RegenResult tick(ColonyState state, boolean workersDead, boolean brainSafe) {
        var rate = computeRate(state, workersDead, brainSafe);

        if (rate <= 0) {
            return RegenResult.NONE;
        }

        workerAccum += rate;
        warriorAccum += rate;
        hopperAccum += rate;
        chariotAccum += rate;

        var workers = consume(workerAccum);
        workerAccum -= workers;
        var warriors = consume(warriorAccum);
        warriorAccum -= warriors;
        var hoppers = consume(hopperAccum);
        hopperAccum -= hoppers;
        var chariots = consume(chariotAccum);
        chariotAccum -= chariots;

        return new RegenResult(workers, warriors, hoppers, chariots);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.putFloat("WorkerAccum", workerAccum);
        tag.putFloat("WarriorAccum", warriorAccum);
        tag.putFloat("HopperAccum", hopperAccum);
        tag.putFloat("ChariotAccum", chariotAccum);

        return tag;
    }

    public void load(CompoundTag tag) {
        workerAccum = tag.getFloat("WorkerAccum");
        warriorAccum = tag.getFloat("WarriorAccum");
        hopperAccum = tag.getFloat("HopperAccum");
        chariotAccum = tag.getFloat("ChariotAccum");
    }

    private float computeRate(ColonyState state, boolean workersDead, boolean brainSafe) {
        if (state == ColonyState.PANIC && workersDead)
            return 0F;
        if (workersDead && state != ColonyState.PEACEFUL)
            return 0F;
        if (brainSafe && !workersDead)
            return FAST_RATE;
        return SLOW_RATE;
    }

    private int consume(float accum) {
        return (int) accum;
    }

    public record RegenResult(
        int workers,
        int warriors,
        int hoppers,
        int chariots
    ) {

        public static final RegenResult NONE = new RegenResult(0, 0, 0, 0);

        public boolean isEmpty() {
            return workers == 0 && warriors == 0 && hoppers == 0 && chariots == 0;
        }
    }
}
