package mod.azure.arachnids.colony;

import net.minecraft.nbt.CompoundTag;

public final class VirtualPopulation {

    private int virtualChariots = 0;

    private int virtualWorkers = 0;

    private int virtualWarriors = 0;

    private int virtualHoppers = 0;

    public int getVirtualChariots() {
        return virtualChariots;
    }

    public int getVirtualWorkers() {
        return virtualWorkers;
    }

    public int getVirtualWarriors() {
        return virtualWarriors;
    }

    public int getVirtualHoppers() {
        return virtualHoppers;
    }

    public void setVirtualChariots(int n) {
        virtualChariots = Math.max(0, n);
    }

    public void setVirtualWorkers(int n) {
        virtualWorkers = Math.max(0, n);
    }

    public void setVirtualWarriors(int n) {
        virtualWarriors = Math.max(0, n);
    }

    public void setVirtualHoppers(int n) {
        virtualHoppers = Math.max(0, n);
    }

    public void killVirtualChariots() {
        virtualChariots = Math.max(0, virtualChariots - 1);
    }

    public void killVirtualWorker() {
        virtualWorkers = Math.max(0, virtualWorkers - 1);
    }

    public void killVirtualWarrior() {
        virtualWarriors = Math.max(0, virtualWarriors - 1);
    }

    public void killVirtualHopper() {
        virtualHoppers = Math.max(0, virtualHoppers - 1);
    }

    public void regenChariot(int count) {
        virtualChariots += count;
    }

    public void regenWorker(int count) {
        virtualWorkers += count;
    }

    public void regenWarrior(int count) {
        virtualWarriors += count;
    }

    public void regenHopper(int count) {
        virtualHoppers += count;
    }

    public int total() {
        return virtualWorkers + virtualWarriors + virtualHoppers;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.putInt("Workers", virtualWorkers);
        tag.putInt("Warriors", virtualWarriors);
        tag.putInt("Hoppers", virtualHoppers);
        tag.putInt("Chariots", virtualChariots);

        return tag;
    }

    public void load(CompoundTag tag) {
        setVirtualWorkers(tag.getInt("Workers"));
        setVirtualWarriors(tag.getInt("Warriors"));
        setVirtualHoppers(tag.getInt("Hoppers"));
        setVirtualChariots(tag.getInt("Chariots"));
    }

    @Override
    public String toString() {
        return String.format("VirtualPop{w=%d, wa=%d, h=%d}", virtualWorkers, virtualWarriors, virtualHoppers);
    }
}
