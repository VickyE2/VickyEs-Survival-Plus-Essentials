package org.vicky.vspe.systems.Dimension.Generator.utils.Palette;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getCleanedID;

public class Palette implements BasePalette {

    public final String id;
    private final Map<Map<NamespacedKey, Integer>, Integer> layers;

    public Palette(String id) {
        this.id = getCleanedID(id);
        this.layers = new HashMap<>();
    }

    public void addLayer(Map<Material, Integer> materials, int layerThickness) {
        Map<NamespacedKey, Integer> instance = new HashMap<>();
        for (Map.Entry<Material, Integer> material : materials.entrySet()) {
            if (material.getKey().isBlock()) {
                instance.put(material.getKey().getKey(), material.getValue());
            }
        }
        layers.put(instance, layerThickness);
    }

    public String getId() {
        return id;
    }

    public Map<Map<NamespacedKey, Integer>, Integer> getLayers() {
        return layers;
    }

    public StringBuilder getLayerYml() {
        StringBuilder builder = new StringBuilder();

        if (!layers.isEmpty()) {
            for (Map.Entry<Map<NamespacedKey, Integer>, Integer> entry : layers.entrySet()) {
                builder.append("- materials: ").append("\n");
                for (Map.Entry<NamespacedKey, Integer> contextMaterial : entry.getKey().entrySet()) {
                    builder.append("    - ").append(contextMaterial.getKey().asString()).append(": ").append(contextMaterial.getValue()).append("\n");
                }
                builder.append("  layers: ").append(entry.getValue());
            }
        }

        return builder;
    }
}
