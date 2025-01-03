package org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplaceExtrusion implements Extrusion {
    private final String id;
    private final Map<Object, Integer> extrusionReplaceableBiome;
    private final List<String> extrusionReplaceableBiomeValues;
    public String from;
    public Range range;
    public NoiseSampler sampler;

    public ReplaceExtrusion(String fileName, Integer selfWeight) {
        this.from = null;
        this.id = fileName.toLowerCase().replaceAll("[^a-z]", "_");
        this.range = null;
        this.sampler = null;
        this.extrusionReplaceableBiome = new HashMap<>();
        this.extrusionReplaceableBiomeValues = new ArrayList<>();
        this.extrusionReplaceableBiomeValues.add("SELF: " + selfWeight);
    }

    public void addBiome(BaseBiome biome, Integer weight) {
        this.extrusionReplaceableBiome.put(biome, weight);
        this.extrusionReplaceableBiomeValues.add(biome.getId() + ": " + weight);
    }

    public String getId() {
        return id;
    }

    public Map<Object, Integer> getExtrusionReplaceableBiome() {
        return extrusionReplaceableBiome;
    }

    public StringBuilder generateFileContents() {
        StringBuilder builder = new StringBuilder();

        builder.append("extrusions: ").append("\n")
                .append("  - type: REPLACE").append("\n")
                .append("    from: ").append(from).append("\n")
                .append("    sampler: ").append("\n");

        builder.append("        ").append(sampler.name().toUpperCase()).append("\n");
        for (Map.Entry<String, Object> value : sampler.getAllParameters().entrySet())
            builder.append("        ").append(value.getKey()).append(": ").append(value.getValue()).append("\n");

        builder.append("    to:").append("\n");
        for (String to : extrusionReplaceableBiomeValues)
            builder.append("        - ").append(to);

        builder.append("    range:").append("\n")
                .append("        min: ").append(range.getMin()).append("\n")
                .append("        max: ").append(range.getMax()).append("\n");

        return builder;
    }
}
