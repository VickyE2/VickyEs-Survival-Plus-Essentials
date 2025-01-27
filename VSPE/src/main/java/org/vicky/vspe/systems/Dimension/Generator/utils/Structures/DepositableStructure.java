package org.vicky.vspe.systems.Dimension.Generator.utils.Structures;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DepositableStructure {
    private final String id;
    private final Material material;
    private final Map<NamespacedKey, NamespacedKey> materialOverrides;
    private final int size;

    public DepositableStructure(String id, Material material, int size) {
        this.id = Utilities.getCleanedID(id);
        this.material = material;
        this.materialOverrides = new HashMap<>();
        this.size = size;
    }

    public String getId() {
        return this.id;
    }

    public NamespacedKey getMaterial() {
        return this.material.getKey();
    }

    public void addMaterialOverride(Material originalMaterial, Material newMaterial) {
        this.materialOverrides.put(originalMaterial.getKey(), newMaterial.getKey());
    }

    public Map<NamespacedKey, NamespacedKey> getMaterialOverrides() {
        return this.materialOverrides;
    }

    public int getSize() {
        return this.size;
    }

    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("id: ")
                .append(this.id)
                .append("\n")
                .append("type: ORE")
                .append("\n")
                .append("extends: ABSTRACT_DEPOSIT")
                .append("\n")
                .append("material: ")
                .append(this.material.getKey().asString())
                .append("\n");
        if (!this.materialOverrides.isEmpty()) {
            builder.append("material-overrides").append("\n");

            for (Entry<NamespacedKey, NamespacedKey> entry : this.materialOverrides.entrySet()) {
                builder.append("  ").append(entry.getKey().asString()).append(": ").append(entry.getValue().asString()).append("\n");
            }
        }

        return builder;
    }
}
