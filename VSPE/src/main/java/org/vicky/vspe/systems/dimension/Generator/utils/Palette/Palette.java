package org.vicky.vspe.systems.dimension.Generator.utils.Palette;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.vicky.vspe.systems.dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.dimension.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.vicky.vspe.systems.dimension.Generator.utils.Utilities.getIndentedBlock;

public class Palette implements BasePalette, Ymlable {
    public final String id;
    private final Map<Object, Integer> layers;
    private final Map<Map<Object, Integer>, StringBuilder> sampledLayers;
    private StringBuilder sampler;

    public Palette(String id) {
        this.id = Utilities.getCleanedID(id);
        this.layers = new HashMap<>();
        this.sampledLayers = new HashMap<>();
        this.sampler = null;
    }

    public void addLayer(Map<Material, Integer> materials, int layerThickness) {
        Map<NamespacedKey, Integer> instance = new HashMap<>();

        for (Entry<Material, Integer> material : materials.entrySet()) {
            if (material.getKey().isBlock()) {
                instance.put(material.getKey().getKey(), material.getValue());
            }
        }

        this.layers.put(instance, layerThickness);
    }

    public void addLayer(Map<Material, Integer> materials, int layerThickness, StringBuilder sampler) {
        Map<NamespacedKey, Integer> instance = new HashMap<>();

        for (Entry<Material, Integer> material : materials.entrySet()) {
            if (material.getKey().isBlock()) {
                instance.put(material.getKey().getKey(), material.getValue());
            }
        }

        this.sampledLayers.put(Map.of(instance, layerThickness), sampler);
    }

    public Map<Object, Integer> getLayers() {
        return layers;
    }

    public void addLayer(LayerClass layerClass) {
        this.layers.put(layerClass, 0);
    }

    public void setSampler(StringBuilder sampler) {
        this.sampler = sampler;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("id: ").append(this.id).append("\n");
        builder.append("type: PALETTE").append("\n");
        builder.append("layers: ").append("\n");
        if (!this.layers.isEmpty() || !this.sampledLayers.isEmpty()) {
            for (Entry<Object, Integer> entry : this.layers.entrySet()) {
                Object var6 = entry.getKey();
                if (var6 instanceof Map<?, ?> key) {
                    builder.append("  - materials: ").append("\n");

                    for (Entry<?, ?> contextMaterial : key.entrySet()) {
                        if (contextMaterial.getKey() instanceof NamespacedKey contextKey) {
                            builder.append("      - ").append(contextKey.asString()).append(": ").append(contextMaterial.getValue()).append("\n");
                        }
                    }

                    builder.append("    layers: ").append(entry.getValue()).append("\n");
                }
                /*
                else if (var6 instanceof LayerClass layerClass) {
                    builder.append(layerClass.getYml().toString());
                }
                 */
            }

            for (Entry<Map<Object, Integer>, StringBuilder> context : this.sampledLayers.entrySet()) {
                for (Entry<Object, Integer> entry : context.getKey().entrySet()) {
                    Object var6 = entry.getKey();
                    if (var6 instanceof Map<?, ?> key) {
                        builder.append("  - materials: ").append("\n");

                        for (Entry<?, ?> contextMaterial : key.entrySet()) {
                            if (contextMaterial.getKey() instanceof NamespacedKey contextKey) {
                                builder.append("      - ").append(contextKey.asString()).append(": ").append(contextMaterial.getValue()).append("\n");
                            }
                        }

                        builder.append("    layers: ").append(entry.getValue()).append("\n");
                    }
                    /*
                    else if (var6 instanceof LayerClass layerClass) {
                        builder.append(layerClass.getYml().toString());
                    }
                     */
                }
                builder.append("    sampler: \n").append(getIndentedBlock(context.getValue().toString(), "      ")).append("\n");
            }
        }

        if (this.sampler != null) {
            builder.append("sampler: ").append("\n");
            builder.append(getIndentedBlock(this.sampler.toString(), "  "));
        }

        return builder;
    }
}
