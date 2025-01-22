package org.vicky.vspe.systems.Dimension.Generator.utils.Feature;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.utils.FeatureType;

public class Feature {
   protected final String id;
   protected final Map<Map<Material, Integer>, Integer> layers;
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

   public void addLayer(Map<Material, Integer> materials, int layerThickness) {
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

   public Map<Map<Material, Integer>, Integer> getLayers() {
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
