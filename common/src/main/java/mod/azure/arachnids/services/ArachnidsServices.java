package mod.azure.arachnids.services;

import java.util.ServiceLoader;

public class ArachnidsServices {

    public static final CommonRegistry COMMON_REGISTRY = load(CommonRegistry.class);

    private ArachnidsServices() {
        throw new UnsupportedOperationException();
    }

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
