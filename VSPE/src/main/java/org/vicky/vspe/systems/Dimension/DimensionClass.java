package org.vicky.vspe.systems.Dimension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum DimensionClass {

    OVERWORLD,
    NETHER,
    END;

    // Registry for dynamic grantableDimensions
    private static final Map<String, String> customDimensions = new HashMap<>();

    /**
     * Registers a new custom dimension.
     *
     * @param dimensionName The name of the custom dimension (case-insensitive).
     */
    public static void registerCustomDimension(String dimensionName) {
        if (dimensionName == null || dimensionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Dimension name cannot be null or empty.");
        }

        // Convert to uppercase
        String uppercaseName = dimensionName.toUpperCase();

        // Ensure the name is not already in use
        if (isDimensionRegistered(uppercaseName)) {
            throw new IllegalArgumentException("Dimension with name '" + dimensionName + "' is already registered.");
        }

        // Add the custom dimension
        customDimensions.put(uppercaseName, uppercaseName);
    }

    /**
     * Checks if a dimension (predefined or custom) is registered.
     *
     * @param dimensionName The name of the dimension to check.
     * @return True if the dimension is registered, false otherwise.
     */
    public static boolean isDimensionRegistered(String dimensionName) {
        if (dimensionName == null) return false;

        String uppercaseName = dimensionName.toUpperCase();

        // Check predefined grantableDimensions
        for (DimensionClass dimension : values()) {
            if (dimension.name().equals(uppercaseName)) {
                return true;
            }
        }

        // Check custom grantableDimensions
        return customDimensions.containsKey(uppercaseName);
    }

    /**
     * Gets a custom dimension by name.
     *
     * @param dimensionName The name of the custom dimension.
     * @return The custom dimension as a string, or null if not found.
     */
    public static String getCustomDimension(String dimensionName) {
        if (dimensionName == null) return null;

        return customDimensions.get(dimensionName.toUpperCase());
    }

    /**
     * Gets all registered custom grantableDimensions.
     *
     * @return An unmodifiable map of custom dimension names.
     */
    public static Map<String, String> getAllCustomDimensions() {
        return Collections.unmodifiableMap(customDimensions);
    }
}
