package org.vicky.vspe.systems.dimension.Generator.utils.Locator.Locators.Patterns;

import org.bukkit.Material;
import org.vicky.vspe.systems.dimension.Exceptions.MisconfigurationException;
import org.vicky.vspe.systems.dimension.Generator.utils.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Pattern {
    private final Type type;
    private Range patternRange = new Range(-64, 319);
    private final Map<String, Integer> blocks = new HashMap<>();
    private final List<Pattern> patterns = new ArrayList<>(); // for AND/OR/XOR
    private Pattern singlePattern;                             // for NOT
    private int offset;
    private boolean offsetSet = false;

    public Pattern(Type patternType) {
        this.type = patternType;
    }

    public Pattern addBlock(Material block, int offset) {
        String key = block.isAir() ? "air"
                : block.getKey().getNamespace() + ":" + block.getKey().getKey();
        this.blocks.put(key, offset);
        return this;
    }

    public Map<String, Integer> getBlocks() {
        return this.blocks;
    }

    public Pattern setRange(Range range) {
        this.patternRange = range;
        return this;
    }

    public Pattern setOffset(int offset) {
        this.offset = offset;
        this.offsetSet = true;
        return this;
    }

    public Pattern addSubPattern(Pattern pattern) {
        if (this.type == Type.NOT) {
            this.singlePattern = pattern;
        } else {
            this.patterns.add(pattern);
        }
        return this;
    }

    public StringBuilder getYml() throws MisconfigurationException {
        validateConfiguration();
        StringBuilder builder = new StringBuilder();
        switch (type) {
            case MATCH_AIR:
            case MATCH_SOLID:
                builder.append("type: ").append(type).append("\n");
                builder.append("offset: ").append(offset).append("\n");
                appendRange(builder);
                break;

            case MATCH:
                builder.append("type: MATCH").append("\n");
                appendRange(builder);
                for (Entry<String, Integer> entry : blocks.entrySet()) {
                    builder.append("block: ").append(entry.getKey()).append("\n");
                    builder.append("offset: ").append(entry.getValue()).append("\n");
                }
                break;

            case MATCH_SET:
                builder.append("type: MATCH_SET\n");
                appendRange(builder);
                builder.append("blocks:\n");
                for (Map.Entry<String, Integer> key : blocks.entrySet()) {
                    builder.append("  - ").append(key.getKey()).append("\n");
                }
                builder.append("offset: ").append(offset).append("\n");
                break;

            case AND:
            case OR:
            case XOR:
                builder.append("type: ").append(type).append("\n");
                builder.append("patterns:\n");
                for (Pattern p : patterns) {
                    StringBuilder sub = p.getYml();
                    // indent each line by two spaces
                    for (String line : sub.toString().split("\n")) {
                        builder.append("  ").append(line).append("\n");
                    }
                }
                break;

            case NOT:
                builder.append("type: NOT\n");
                appendRange(builder);
                builder.append("pattern:\n");
                StringBuilder sub = singlePattern.getYml();
                for (String line : sub.toString().split("\n")) {
                    builder.append("  ").append(line).append("\n");
                }
                break;

            default:
                throw new MisconfigurationException("Unsupported pattern type: " + type);
        }
        return builder;
    }

    private void appendRange(StringBuilder builder) {
        if (patternRange != null) {
            builder.append("range:\n");
            builder.append("  min: ").append(patternRange.getMin()).append("\n");
            builder.append("  max: ").append(patternRange.getMax()).append("\n");
        }
    }

    private void validateConfiguration() throws MisconfigurationException {
        switch (type) {
            case MATCH_AIR:
            case MATCH_SOLID:
            case MATCH:
                if (patternRange == null) {
                    throw new MisconfigurationException("Pattern type " + type + " requires a range");
                }
                break;

            case MATCH_SET:
                if (!offsetSet) {
                    throw new MisconfigurationException("Pattern type MATCH_SET requires offset to be set");
                }
                break;

            case AND:
            case OR:
            case XOR:
                if (patterns.isEmpty()) {
                    throw new MisconfigurationException("Pattern type " + type + " requires at least one sub-pattern");
                }
                break;

            case NOT:
                if (singlePattern == null) {
                    throw new MisconfigurationException("Pattern type NOT requires a single sub-pattern");
                }
                break;

            default:
                // No additional validation
        }
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
