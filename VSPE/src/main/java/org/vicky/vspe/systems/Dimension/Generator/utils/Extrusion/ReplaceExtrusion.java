package org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplaceExtrusion implements Extrusion, Ymlable {
    private final String id;
    private final List<BaseBiome> extrusionReplaceableBiome;
    private final List<String> extrusionReplaceableBiomeValues;
    public String from;
    public Range range;
    public NoiseSampler sampler;

    public ReplaceExtrusion(String fileName, Integer selfWeight) {
        this.from = null;
        this.id = fileName.toLowerCase().replaceAll("[^a-z]", "_");
        this.range = null;
        this.sampler = null;
        this.extrusionReplaceableBiome = new ArrayList<>();
        this.extrusionReplaceableBiomeValues = new ArrayList<>();
        this.extrusionReplaceableBiomeValues.add("SELF: " + selfWeight);
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setFrom(Tags from) {
        this.from = from.name();
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public void setSampler(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    public void addBiome(BaseBiome biome, Integer weight) {
        this.extrusionReplaceableBiome.add(biome);
        this.extrusionReplaceableBiomeValues.add(biome.getId() + ": " + weight);
    }

    public String getId() {
        return id;
    }

    public List<BaseBiome> getExtrusionReplaceableBiome() {
        return extrusionReplaceableBiome;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("extrusions: ").append("\n")
                .append("  - type: REPLACE").append("\n")
                .append("    from: ").append(from).append("\n")
                .append("    sampler: ").append("\n");

        builder.append(Utilities.getIndentedBlock(sampler.getYml().toString(), "      ")).append("\n");

        builder.append("    to:").append("\n");
        for (String to : extrusionReplaceableBiomeValues)
            builder.append("        - ").append(to).append("\n");

        builder.append("    range:").append("\n")
                .append("        min: ").append(range.getMin()).append("\n")
                .append("        max: ").append(range.getMax()).append("\n");

        return builder;
    }
}
