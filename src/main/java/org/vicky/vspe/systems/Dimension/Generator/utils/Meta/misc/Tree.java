package org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc;

import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import java.util.*;

public class Tree implements Ymlable {
    private final String name;
    private final Map<String, Object> values;
    private final List<Tree> nestedTrees;

    public Tree(String name) {
        this.name = name;
        this.values = new LinkedHashMap<>();
        this.nestedTrees = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public List<Tree> getNestedTrees() {
        return nestedTrees;
    }

    /**
     * Adds a key-value pair to the Tree.
     */
    public void add(String key, Object value) {
        values.put(key, value);
    }

    /**
     * Adds a nested Tree to this Tree.
     */
    public void add(Tree tree) {
        nestedTrees.add(tree);
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        appendYml(builder, 0);
        return builder;
    }

    /**
     * Recursively appends YAML formatted data to the builder.
     */
    private void appendYml(StringBuilder builder, int indentationLevel) {
        if (!nestedTrees.isEmpty() || !values.isEmpty()) {
            String indentation = "  ".repeat(indentationLevel);
            builder.append(indentation).append(name).append(":\n");
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                builder.append(indentation).append("  ").append(entry.getKey()).append(": ");
                builder.append(entry.getValue()).append("\n");
            }
            for (Tree nestedTree : nestedTrees) {
                nestedTree.appendYml(builder, indentationLevel + 1);
            }
        }
    }

    /**
     * Retrieves the full mapping path for a specific key in the tree.
     *
     * @param key The key to search for.
     * @return The full mapping path as a String, or null if the key does not exist.
     */
    public String getMapping(String key) {
        // Search in the current tree's values
        if (values.containsKey(key)) {
            return name + ":" + key;
        }

        // Search in nested trees
        for (Tree nestedTree : nestedTrees) {
            String nestedMapping = nestedTree.getMapping(key);
            if (nestedMapping != null) {
                return name + ":" + nestedMapping;
            }
        }

        return null;
    }
}
