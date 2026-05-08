package mod.azure.arachnids.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public final class ColonyDomeBuilder {

    public static final int DOME_RADIUS = 8;

    private static final int REPLAN_DISTANCE = 6;

    private final Deque<BlockPos> buildQueue = new ArrayDeque<>();

    private final Set<BlockPos> claimed = new HashSet<>();

    private final Set<BlockPos> built = new HashSet<>();

    private BlockPos lastOrigin = null;

    private boolean planned = false;

    public void plan(ServerLevel level, BlockPos origin) {
        if (
            planned && lastOrigin != null
                && lastOrigin.distSqr(origin) < REPLAN_DISTANCE * REPLAN_DISTANCE
        ) {
            return;
        }

        buildQueue.clear();
        claimed.clear();
        built.clear();
        planned = true;
        lastOrigin = origin;

        var surfY = level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX(), origin.getZ());

        var r = DOME_RADIUS;
        var rSq = r * r;
        var innerR = r - 1;
        var innerRSq = innerR * innerR;

        for (var dx = -r; dx <= r; dx++) {
            for (var dz = -r; dz <= r; dz++) {
                for (var dy = 0; dy <= r; dy++) {
                    var distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > rSq || distSq < innerRSq)
                        continue;

                    if (dz >= r - 2 && Math.abs(dx) <= 2 && dy <= 3)
                        continue;

                    var pos = origin.offset(dx, dy, dz);

                    if (pos.getY() < surfY)
                        continue;

                    buildQueue.add(pos);
                }
            }
        }
    }

    public BuildTask claimNextBuildTask(ServerLevel level, ColonyBlockStore store) {
        if (store.isEmpty())
            return null;

        while (!buildQueue.isEmpty()) {
            var pos = buildQueue.poll();

            if (!level.isLoaded(pos))
                continue;

            var state = level.getBlockState(pos);

            if (!state.isAir()) {
                built.add(pos);
                continue;
            }

            var block = store.withdraw();
            if (block == null)
                return null;

            claimed.add(pos);
            return new BuildTask(pos, block);
        }
        return null;
    }

    public void completeTask(BlockPos pos) {
        claimed.remove(pos);
        built.add(pos);
    }

    public void returnTask(BlockPos pos, Block block, ColonyBlockStore store) {
        if (pos == null)
            return;
        claimed.remove(pos);
        buildQueue.addFirst(pos);
        store.deposit(block.defaultBlockState());
    }

    public boolean isDone() {
        return planned && buildQueue.isEmpty() && claimed.isEmpty();
    }

    public boolean isPlanned() {
        return planned;
    }

    public int pendingCount() {
        return buildQueue.size() + claimed.size();
    }

    public CompoundTag save() {
        var tag = new CompoundTag();

        tag.putBoolean("Planned", planned);

        if (lastOrigin != null) {
            tag.put("LastOrigin", NbtUtils.writeBlockPos(lastOrigin));
        }

        var queueTag = new ListTag();
        for (var pos : buildQueue) {
            queueTag.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("Queue", queueTag);

        var builtTag = new ListTag();
        for (var pos : built) {
            builtTag.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("Built", builtTag);

        return tag;
    }

    public void load(CompoundTag tag) {
        planned = tag.getBoolean("Planned");

        if (tag.contains("LastOrigin", Tag.TAG_COMPOUND)) {
            NbtUtils.readBlockPos(tag, "LastOrigin").ifPresent(pos -> lastOrigin = pos);
        }

        buildQueue.clear();
        if (tag.contains("Queue", Tag.TAG_LIST)) {
            var list = tag.getList("Queue", Tag.TAG_COMPOUND);
            for (var i = 0; i < list.size(); i++) {
                NbtUtils.readBlockPos(list.getCompound(i), "").ifPresent(buildQueue::add);
            }
        }

        built.clear();
        if (tag.contains("Built", Tag.TAG_LIST)) {
            var list = tag.getList("Built", Tag.TAG_COMPOUND);
            for (var i = 0; i < list.size(); i++) {
                NbtUtils.readBlockPos(list.getCompound(i), "").ifPresent(built::add);
            }
        }
    }

    public record BuildTask(
        BlockPos pos,
        Block block
    ) {}
}
