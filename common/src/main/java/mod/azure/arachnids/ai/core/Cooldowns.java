package mod.azure.arachnids.ai.core;

import java.util.HashMap;
import java.util.Map;

public final class Cooldowns {

    private final Map<String, Integer> cooldowns = new HashMap<>();

    public void tick() {
        cooldowns.entrySet().removeIf(entry -> {
            int next = entry.getValue() - 1;
            entry.setValue(next);
            return next <= 0;
        });
    }

    public boolean ready(String key) {
        return !cooldowns.containsKey(key);
    }

    public boolean isOnCooldown(String key) {
        return !ready(key);
    }

    public void set(String key, int ticks) {
        cooldowns.put(key, ticks);
    }
}
