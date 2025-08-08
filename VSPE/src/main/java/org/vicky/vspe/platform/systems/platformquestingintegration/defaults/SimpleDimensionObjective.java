package org.vicky.vspe.platform.systems.platformquestingintegration.defaults;

import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.platform.systems.platformquestingintegration.Objectives.PlatformDimensionObjective;

import java.util.Set;

public class SimpleDimensionObjective<T> /*implements PlatformDimensionObjective<T>, Identifiable*/ {
    /*
    private final String id;
    private final Set<String> targetDimension;

    public SimpleDimensionObjective(String id, Set<String> targetDimension) {
        this.id = id;
        this.targetDimension = targetDimension;
    }

    public String getIdentifier() {
        return id;
    }

    @Override
    public void startObjective(PlatformPlayer player) {
        player.listenToDimensionChange((from, to) -> {
            if (to.equalsIgnoreCase(targetDimension)) {
                player.completeObjective(id);
            }
        });
    }

    @Override
    public void stopObjective(PlatformPlayer player) {
        player.stopListeningToDimensionChange(id);
    }

    @Override
    public boolean isCompleted(PlatformPlayer player) {
        return player.getLocation().getWorld().getName().equalsIgnoreCase(targetDimension);
    }

    @Override
    public Set<String> getTargetDimensions() {
        return targetDimension;
    }
     */
}