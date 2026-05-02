package mod.azure.arachnids.ai.core;

import java.util.HashMap;
import java.util.Map;

public final class Blackboard {

    private final Map<String, Object> values = new HashMap<>();

    public <T> void set(String key, T value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        var value = values.get(key);
        return type.isInstance(value) ? (T) value : null;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public void remove(String key) {
        values.remove(key);
    }
}
