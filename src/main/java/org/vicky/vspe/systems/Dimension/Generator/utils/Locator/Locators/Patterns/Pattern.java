package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class Pattern {
    public Type type;
    public Type blockType;
    private final Map<String, Integer> blocks;

    public Pattern(Type patternType, Type blockType) {
        this.type = patternType;
        this.blockType = blockType;
        this.blocks = new HashMap<>();
    }

    public Map<String, Integer> getBlocks() {
        return blocks;
    }

    public void addBlock(Material block, int offset) {
        if (block.isAir()) {
            blocks.put("air", offset);
        } else if (block.isBlock()) {
            blocks.put(block.getKey().getNamespace() + ":" + block.getKey().getKey(), offset);
        }
    }

    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("type: ").append(type).append("\n");
        builder.append("patterns: ").append("\n");
        if (blockType.equals(Pattern.Type.MATCH)) {
            for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
                builder.append("  - type: MATCH").append("\n");
                builder.append("    block: ").append(entry.getKey()).append("\n")
                        .append("    offset: ").append(entry.getValue()).append("\n");
            }
        } else if (blockType.equals(Type.MATCH_SET)) {
            builder.append("  - type: MATCH_SET").append("\n");
            for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
                builder.append("   - block: ").append(entry.getKey()).append("\n");
            }
            builder.append("   - block: ").append(1).append("\n");
        }

        return builder;
    }

    public enum Type {
        AND, OR, XOR, NOT, MATCH_SET, MATCH, MATCH_SOLID, MATCH_AIR
    }
}
