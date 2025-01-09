package org.vicky.vspe.systems.Dimension.Generator.utils.Structures;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getCleanedID;

public class DepositableStructure {
    private final String id;
    private final Material material;  // The base material
    private final Map<NamespacedKey, NamespacedKey> materialOverrides;  // A map for overrides like "minecraft:deepslate" -> "minecraft:deepslate_copper_ore"
    private final int size;

    public DepositableStructure(String id, Material material, int size) {
        this.id = getCleanedID(id);
        this.material = material;
        this.materialOverrides = new HashMap<>();
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public NamespacedKey getMaterial() {
        return material.getKey();
    }

    public void addMaterialOverride(Material originalMaterial, Material newMaterial) {
        materialOverrides.put(originalMaterial.getKey(), newMaterial.getKey());
    }

    public Map<NamespacedKey, NamespacedKey> getMaterialOverrides() {
        return materialOverrides;
    }

    public int getSize() {
        return size;
    }

    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("id: ").append(id).append("\n")
                .append("type: ORE").append("\n")
                .append("extends: ABSTRACT_DEPOSIT").append("\n")
                .append("material: ").append(material.getKey().asString()).append("\n");

        if (!materialOverrides.isEmpty()) {
            builder.append("material-overrides").append("\n");
            for (Map.Entry<NamespacedKey, NamespacedKey> entry : materialOverrides.entrySet())
                builder.append("  ").append(entry.getKey().asString()).append(": ")
                        .append(entry.getValue().asString()).append("\n");
        }

        return builder;
    }
}
