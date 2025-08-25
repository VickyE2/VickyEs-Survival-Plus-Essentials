package org.vicky.vspe.platform;

import java.util.Map;

public class NativeTypeMapper {
    private static final Map<String, String> nativeMaps = Map.of(
            "vspe:magenta_frost_leaves", "minecraft:cherry_leaves",
            "vspe:magenta_frost_vines", "minecraft:weeping_vines",
            "vspe:magenta_frost_logs", "minecraft:cherry_wood",
            "vspe:pink_sand", "minecraft:pink_concrete_powder"
    );

    private static final String DEFAULT_FALLBACK = "minecraft:grass_block";

    public static String getFor(String id) {
        if (VSPEPlatformPlugin.isNative()) {
            String mapped = nativeMaps.getOrDefault(id, DEFAULT_FALLBACK);
            if ("minecraft:grass".equals(mapped)) {
                return DEFAULT_FALLBACK;
            }
            return mapped;
        }
        return id;
    }
}

