package org.vicky.vspe.systems.Dimension.Generator.utils.Feature;

import org.bukkit.Material;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.BaseStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.Map;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getCleanedID;

/**
 * Represents a feature in the dimension generation system.
 * A feature can have layers of materials, and information on how it is distributed
 * and located within the dimension. The feature also includes details about the
 * structures and their distributions.
 */
public class Feature {
    protected final String id;
    protected final Map<Map<Material, Integer>, Integer> layers;

    // Distributor settings
    protected Ymlable distributor;

    protected Ymlable locator;

    protected Ymlable structureDistribution;
    protected Map<Object, Integer> structures;

    /**
     * Creates a new feature with the specified ID.
     *
     * @param id The ID of the feature (e.g., "BAMBOO_PATCHES").
     */
    public Feature(String id) {
        this.id = getCleanedID(id);
        this.layers = new HashMap<>();
        this.structures = new HashMap<>();
    }

    /**
     * Adds a layer to the feature.
     *
     * @param materials      A map of materials and their corresponding quantities.
     * @param layerThickness The thickness of this layer.
     */
    public void addLayer(Map<Material, Integer> materials, int layerThickness) {
        layers.put(materials, layerThickness);
    }

    /**
     * Adds a layer to the feature.
     *
     * @param structure      The structure to add
     * @param layerThickness The weight of this structure.
     */
    public void addStructure(BaseStructure structure, int layerThickness) {
        structures.put(structure, layerThickness);
    }

    public void addStructure(String structure) {
        structures.put(structure, 0);
    }

    public void setStructuresDistributor(Ymlable distributionType) {
        this.structureDistribution = distributionType;
    }

    public String getId() {
        return id;
    }

    public Map<Map<Material, Integer>, Integer> getLayers() {
        return layers;
    }

    public Ymlable getDistributor() {
        return distributor;
    }

    public void setDistributor(Ymlable distributor) {
        this.distributor = distributor;
    }

    public Ymlable getStructureDistributor() {
        return structureDistribution;
    }

    public Ymlable getLocator() {
        return locator;
    }

    public void setLocator(Ymlable locator) {
        this.locator = locator;
    }

    public Map<Object, Integer> getStructures() {
        return structures;
    }


}
