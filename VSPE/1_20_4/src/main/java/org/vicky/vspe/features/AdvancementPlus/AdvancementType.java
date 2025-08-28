package org.vicky.vspe.features.AdvancementPlus;

import java.util.HashMap;
import java.util.Map;

public enum AdvancementType {
    GOAL("#00aa00"),
    CHALLENGE("#ffd700"),
    MASTERY("#800080"),
    PROGRESSION("#ffa500"),
    CUSTOM("");

    private static final Map<String, AdvancementType> customAdvancements = new HashMap<>();
    private final String advancementColor;

    AdvancementType(String advancementColor) {
        this.advancementColor = advancementColor;
    }

    public static void registerCustomAdvancement(String name) {
        String upperName = name.toUpperCase();
        if (!customAdvancements.containsKey(upperName)) {
            customAdvancements.put(upperName, valueOf(upperName));
        } else {
            throw new IllegalArgumentException("Advancement type '" + name + "' is already registered.");
        }
    }

    public static boolean isCustomAdvancement(String name) {
        return customAdvancements.containsKey(name.toUpperCase());
    }

    public String getAdvancementColor() {
        return this.advancementColor;
    }
}
