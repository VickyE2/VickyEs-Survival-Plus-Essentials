package org.vicky.vspe.systems.dimension.Generator.utils.Palette;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.dimension.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LayerClass implements Ymlable {
    private final Map<NamespacedKey, Integer> layers = new HashMap<>();
    private final Integer layer;
    private NoiseSampler sampler = null;

    public LayerClass(Integer layer) {
        this.layer = layer;
    }

    public void addMaterial(Material material, Integer weight) {
        this.layers.put(material.getKey(), weight);
    }

    public void setSampler(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("  - material:").append("\n");

        for (Entry<NamespacedKey, Integer> keyIntegerEntry : this.layers.entrySet()) {
            builder.append("    - ").append(keyIntegerEntry.getKey().namespace()).append(": ").append(keyIntegerEntry.getValue()).append("\n");
        }

        builder.append("  layers: ").append(this.layer).append("\n");
        if (this.sampler != null) {
            builder.append("  sampler: ").append("\n");
            builder.append(Utilities.getIndentedBlock(this.sampler.getYml().toString(), "    "));
        }

        return builder;
    }
}
