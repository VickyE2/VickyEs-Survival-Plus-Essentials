package org.vicky.vspe.systems.Dimension.Generator.utils;

public enum GlobalPreprocessor {
   LAVA_FLOOR,
   UNDERGROUND_LAVA_COLUMNS,
   CONTAIN_FLOATING_WATER,
   CAVE_GLOW_LICHEN,
   TEXTURED_STONE_SLANT;

   public static boolean isValidPreprocessor(String name) {
      try {
         valueOf(name);
         return true;
      } catch (IllegalArgumentException var2) {
         return false;
      }
   }
}
