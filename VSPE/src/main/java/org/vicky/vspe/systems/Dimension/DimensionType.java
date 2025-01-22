package org.vicky.vspe.systems.Dimension;

import java.util.ArrayList;
import java.util.List;

public enum DimensionType {
   DISTANT_WORLD(new ArrayList<>()),
   CLOSE_WORLD(new ArrayList<>()),
   HOLLOW_WORLD(new ArrayList<>()),
   NORMAL_WORLD(new ArrayList<>()),
   AQUATIC_WORLD(new ArrayList<>()),
   FROZEN_WORLD(new ArrayList<>()),
   NETHER_WORLD(new ArrayList<>()),
   DARK_WORLD(new ArrayList<>()),
   BRIGHT_WORLD(new ArrayList<>()),
   BARREN_WORLD(new ArrayList<>()),
   ARBOREAL_WORLD(new ArrayList<>()),
   ELEMENTAL_WORLD(new ArrayList<>()),
   DREAM_WORLD(new ArrayList<>()),
   ABSTRACT_WORLD(new ArrayList<>()),
   ALIEN_WORLD(new ArrayList<>()),
   FUTURISTIC_WORLD(new ArrayList<>()),
   ANCIENT_WORLD(new ArrayList<>()),
   AETHER_WORLD(new ArrayList<>());

   private final List<BaseDimension> dimensions;

   private DimensionType(List<BaseDimension> dimensions) {
      this.dimensions = dimensions;
   }

   public void addDimension(BaseDimension dimension) {
      this.dimensions.add(dimension);
   }

   public void removeDimension(BaseDimension dimension) {
      this.dimensions.remove(dimension);
   }

   public boolean hasDimension(BaseDimension dimension) {
      return this.dimensions.stream().anyMatch(dimension1 -> dimension1.equals(dimension));
   }
}
