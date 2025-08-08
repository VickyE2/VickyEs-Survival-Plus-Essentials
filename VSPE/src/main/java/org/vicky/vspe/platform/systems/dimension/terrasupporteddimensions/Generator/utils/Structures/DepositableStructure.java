package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Structures;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DepositableStructure {
    private final String id;
    private final PlatformMaterial material;
    private final Map<ResourceLocation, ResourceLocation> materialOverrides;
    private final int size;

    public DepositableStructure(String id, PlatformMaterial material, int size) {
        this.id = Utilities.getCleanedID(id);
        this.material = material;
        this.materialOverrides = new HashMap<>();
        this.size = size;
    }

    public String getId() {
        return this.id;
    }

    public ResourceLocation getMaterial() {
        return this.material.getResourceLocation();
    }

    public void addMaterialOverride(PlatformMaterial originalMaterial, PlatformMaterial newMaterial) {
        this.materialOverrides.put(originalMaterial.getResourceLocation(), newMaterial.getResourceLocation());
    }

    public Map<ResourceLocation, ResourceLocation> getMaterialOverrides() {
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
                .append(this.material.getResourceLocation().asString())
                .append("\n");
        if (!this.materialOverrides.isEmpty()) {
            builder.append("material-overrides").append("\n");

            for (Entry<ResourceLocation, ResourceLocation> entry : this.materialOverrides.entrySet()) {
                builder.append("  ").append(entry.getKey().asString()).append(": ").append(entry.getValue().asString()).append("\n");
            }
        }

        return builder;
    }
}
