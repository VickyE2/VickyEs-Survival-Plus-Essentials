package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LayerClass implements Ymlable {
    private final Map<ResourceLocation, Integer> layers = new HashMap<>();
    private final Integer layer;
    private NoiseSampler sampler = null;

    public LayerClass(Integer layer) {
        this.layer = layer;
    }

    public void addMaterial(PlatformMaterial material, Integer weight) {
        this.layers.put(material.getResourceLocation(), weight);
    }

    public void setSampler(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("  - material:").append("\n");

        for (Entry<ResourceLocation, Integer> keyIntegerEntry : this.layers.entrySet()) {
            builder.append("    - ").append(keyIntegerEntry.getKey().getNamespace()).append(": ").append(keyIntegerEntry.getValue()).append("\n");
        }

        builder.append("  layers: ").append(this.layer).append("\n");
        if (this.sampler != null) {
            builder.append("  sampler: ").append("\n");
            builder.append(Utilities.getIndentedBlock(this.sampler.getYml().toString(), "    "));
        }

        return builder;
    }
}
