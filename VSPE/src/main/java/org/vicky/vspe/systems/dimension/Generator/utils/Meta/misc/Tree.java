package org.vicky.vspe.systems.dimension.Generator.utils.Meta.misc;

import org.vicky.vspe.systems.dimension.Generator.utils.Ymlable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
        return this.name;
    }

    public Map<String, Object> getValues() {
        return this.values;
    }

    public List<Tree> getNestedTrees() {
        return this.nestedTrees;
    }

    public void add(String key, Object value) {
        this.values.put(key, value);
    }

    public void add(Tree tree) {
        this.nestedTrees.add(tree);
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        this.appendYml(builder, 0);
        return builder;
    }

    private void appendYml(StringBuilder builder, int indentationLevel) {
        if (!this.nestedTrees.isEmpty() || !this.values.isEmpty()) {
            String indentation = "  ".repeat(indentationLevel);
            builder.append(indentation).append(this.name).append(":\n");

            for (Entry<String, Object> entry : this.values.entrySet()) {
                builder.append(indentation).append("  ").append(entry.getKey()).append(": ");
                builder.append(entry.getValue()).append("\n");
            }

            for (Tree nestedTree : this.nestedTrees) {
                nestedTree.appendYml(builder, indentationLevel + 1);
            }
        }
    }

    public String getMapping(String key) {
        if (this.values.containsKey(key)) {
            return this.name + "." + key;
        } else {
            for (Tree nestedTree : this.nestedTrees) {
                String nestedMapping = nestedTree.getMapping(key);
                if (nestedMapping != null) {
                    return this.name + "." + nestedMapping;
                }
            }

            return null;
        }
    }
}
