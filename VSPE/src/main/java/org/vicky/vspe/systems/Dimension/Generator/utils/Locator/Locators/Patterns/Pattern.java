package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Pattern {
    private final Map<String, Integer> blocks;
    public Type type;
    public Type blockType;

    public Pattern(Type patternType, Type blockType) {
        this.type = patternType;
        this.blockType = blockType;
        this.blocks = new HashMap<>();
    }

    public Map<String, Integer> getBlocks() {
        return this.blocks;
    }

    public void addBlock(Material block, int offset) {
        if (block.isAir()) {
            this.blocks.put("air", offset);
        } else if (block.isBlock()) {
            this.blocks.put(block.getKey().getNamespace() + ":" + block.getKey().getKey(), offset);
        }
    }

    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("type: ").append(this.type).append("\n");
        builder.append("patterns: ").append("\n");
        if (this.blockType.equals(Type.MATCH)) {
            for (Entry<String, Integer> entry : this.blocks.entrySet()) {
                if (entry.getKey().equals("air")) {
                    builder.append("  - type: MATCH_AIR").append("\n").append("    offset: ").append(entry.getValue()).append("\n");
                } else {
                    builder.append("  - type: MATCH").append("\n");
                    builder.append("    block: ").append(entry.getKey()).append("\n").append("    offset: ").append(entry.getValue()).append("\n");
                }
            }
        } else if (this.blockType.equals(Type.MATCH_SET)) {
            builder.append("  - type: MATCH_SET").append("\n");

            for (Entry<String, Integer> entryx : this.blocks.entrySet()) {
                builder.append("   - block: ").append(entryx.getKey()).append("\n");
            }

            builder.append("   - block: ").append(1).append("\n");
        }

        return builder;
    }

    public enum Type {
        AND,
        OR,
        XOR,
        NOT,
        MATCH_SET,
        MATCH,
        MATCH_SOLID,
        MATCH_AIR
    }
}
