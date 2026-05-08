package mod.azure.arachnids.colony;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class ColonyBlockStore {

    private static final int MAX_STOCKPILE = 256;

    private static final Set<Block> BLACKLISTED = Set.of(
        Blocks.AIR,
        Blocks.CAVE_AIR,
        Blocks.VOID_AIR,
        Blocks.BEDROCK,
        Blocks.BARRIER,
        Blocks.LIGHT
    );

    private final Map<Block, Integer> stock = new LinkedHashMap<>();

    private int total = 0;

    public boolean deposit(BlockState state) {
        var block = state.getBlock();
        if (BLACKLISTED.contains(block))
            return false;
        if (total >= MAX_STOCKPILE)
            return false;

        stock.merge(block, 1, Integer::sum);
        total++;
        return true;
    }

    public Block withdraw() {
        var it = stock.entrySet().iterator();

        if (it.hasNext()) {
            var entry = it.next();
            var count = entry.getValue() - 1;

            if (count <= 0)
                it.remove();
            else
                entry.setValue(count);

            total = Math.max(0, total - 1);
            return entry.getKey();
        }

        return null;
    }

    public Block peek() {
        Block best = null;
        var bestCount = 0;
        for (var entry : stock.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    public boolean isEmpty() {
        return total == 0;
    }

    public boolean isFull() {
        return total >= MAX_STOCKPILE;
    }

    public int total() {
        return total;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        var list = new ListTag();
        for (var entry : stock.entrySet()) {
            var key = BuiltInRegistries.BLOCK.getKey(entry.getKey());
            var entry_tag = new CompoundTag();
            entry_tag.putString("Block", key.toString());
            entry_tag.putInt("Count", entry.getValue());
            list.add(entry_tag);
        }
        tag.put("Stock", list);
        tag.putInt("Total", total);
        return tag;
    }

    public void load(CompoundTag tag) {
        stock.clear();
        total = 0;

        if (!tag.contains("Stock"))
            return;

        var list = tag.getList("Stock", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            var loc = ResourceLocation.tryParse(entry.getString("Block"));
            if (loc == null)
                continue;
            var block = BuiltInRegistries.BLOCK.get(loc);
            if (block == Blocks.AIR)
                continue;
            var count = entry.getInt("Count");
            if (count > 0) {
                stock.put(block, count);
                total += count;
            }
        }
    }
}
