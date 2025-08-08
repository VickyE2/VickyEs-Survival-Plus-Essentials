package org.vicky.vspe.platform.systems.platformquestingintegration;

import java.util.Map;

public record QuestContext(
        String sourceType,             // e.g., "json", "ai", "script", "manual"
        Map<String, Object> metadata,  // Hints or configs for generation
        String rawContent              // Raw text, json, yaml, etc.
) {}
