package org.vicky.vspe.platform;

import java.util.Map;

public class NativeTypeMapper {
    private static final Map<String, String> nativeMaps = Map.of(
            "vspe:magenta_frost_leaves", "minecraft:cherry_leaves",
            "vspe:magenta_frost_vines", "minecraft:weeping_vines",
            "vspe:magenta_frost_logs", "minecraft:cherry_wood",
            "vspe:pink_sand", "minecraft:pink_concrete_powder"
    );

    public static String getFor(String forgifabible) {
        if (VSPEPlatformPlugin.isNative()) {
            return nativeMaps.getOrDefault(forgifabible, "minecraft:grass");
        }
        return forgifabible;
    }
}
