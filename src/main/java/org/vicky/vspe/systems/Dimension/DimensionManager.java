package org.vicky.vspe.systems.Dimension;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class DimensionManager {

    public final Map<String, BaseDimension> LOADED_DIMENSIONS = new HashMap<>();

    public BaseDimension getPlayerDimension(Player player) {
        for (BaseDimension dimension : LOADED_DIMENSIONS.values()) {
            if (dimension.isPlayerInDimension(player)) {
                return dimension;
            }
        }
        return null;
    }
}
