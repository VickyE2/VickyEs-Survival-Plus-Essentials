package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature;

import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.Map;

public class Feature {
    protected final String id;
    protected final Map<Map<PlatformMaterial, Integer>, Integer> layers;
    protected final FeatureType type;
    protected Ymlable distributor;
    protected Ymlable locator;
    protected Ymlable structureDistribution;
    protected Map<Object, Integer> structures;

    public Feature(String id, FeatureType type) {
        this.id = Utilities.getCleanedID(id);
        this.layers = new HashMap<>();
        this.structures = new HashMap<>();
        this.type = type;
    }

    public FeatureType getType() {
        return this.type;
    }

    public void addLayer(Map<PlatformMaterial, Integer> materials, int layerThickness) {
        this.layers.put(materials, layerThickness);
    }

    public void addStructure(BaseStructure structure, int layerThickness) {
        this.structures.put(structure, layerThickness);
    }

    public void addStructure(String structure) {
        this.structures.put(structure, 0);
    }

    public void setStructuresDistributor(Ymlable distributionType) {
        this.structureDistribution = distributionType;
    }

    public String getId() {
        return this.id;
    }

    public Map<Map<PlatformMaterial, Integer>, Integer> getLayers() {
        return this.layers;
    }

    public Ymlable getDistributor() {
        return this.distributor;
    }

    public void setDistributor(Ymlable distributor) {
        this.distributor = distributor;
    }

    public Ymlable getStructureDistributor() {
        return this.structureDistribution;
    }

    public Ymlable getLocator() {
        return this.locator;
    }

    public void setLocator(Ymlable locator) {
        this.locator = locator;
    }

    public Map<Object, Integer> getStructures() {
        return this.structures;
    }
}
