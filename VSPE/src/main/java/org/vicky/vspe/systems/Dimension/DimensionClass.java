package org.vicky.vspe.systems.Dimension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum DimensionClass {
   OVERWORLD,
   NETHER,
   END;

   private static final Map<String, String> customDimensions = new HashMap<>();

   public static void registerCustomDimension(String dimensionName) {
      if (dimensionName != null && !dimensionName.trim().isEmpty()) {
         String uppercaseName = dimensionName.toUpperCase();
         if (isDimensionRegistered(uppercaseName)) {
            throw new IllegalArgumentException("Dimension with name '" + dimensionName + "' is already registered.");
         } else {
            customDimensions.put(uppercaseName, uppercaseName);
         }
      } else {
         throw new IllegalArgumentException("Dimension name cannot be null or empty.");
      }
   }

   public static boolean isDimensionRegistered(String dimensionName) {
      if (dimensionName == null) {
         return false;
      } else {
         String uppercaseName = dimensionName.toUpperCase();

         for (DimensionClass dimension : values()) {
            if (dimension.name().equals(uppercaseName)) {
               return true;
            }
         }

         return customDimensions.containsKey(uppercaseName);
      }
   }

   public static String getCustomDimension(String dimensionName) {
      return dimensionName == null ? null : customDimensions.get(dimensionName.toUpperCase());
   }

   public static Map<String, String> getAllCustomDimensions() {
      return Collections.unmodifiableMap(customDimensions);
   }
}
