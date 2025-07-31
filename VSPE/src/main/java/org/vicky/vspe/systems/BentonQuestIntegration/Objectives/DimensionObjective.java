package org.vicky.vspe.systems.BentonQuestIntegration.Objectives;

import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.Objective;
import org.betonquest.betonquest.api.profiles.OnlineProfile;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.vicky.vspe.systems.dimension.Events.DimensionWarpEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DimensionObjective is an objective that requires players to visit a set of target dimensions.
 * <p>
 * When the objective starts for a player, it initializes persistent data (an instance of DimensionsData)
 * which stores the list of dimensions the player has visited. Each time the player warps to a dimension
 * (via a DimensionWarpEvent), the objective checks whether that dimension is one of the target dimensions
 * and not already marked as visited. When all target dimensions have been visited, the objective is completed.
 * </p>
 *
 * <p>
 * To configure this objective in your quest file, include an array for "dimensions", for example:
 * <pre>
 *   objectives:
 *     visit_all_dimensions:
 *       type: dimension_objective
 *       data:
 *         dimensions: [overworld, nether, end]
 * </pre>
 * </p>
 */
public class DimensionObjective extends Objective implements Listener {

    /**
     * The set of target dimensions (lowercase) that must be visited.
     */
    private final Set<String> targetDimensions;
    private final String Id = UUID.randomUUID().toString();

    /**
     * Constructs a new DimensionObjective.
     *
     * @param instruction the Instruction object containing configuration data.
     * @throws InstructionParseException if the instruction cannot be parsed.
     */
    public DimensionObjective(Instruction instruction) throws InstructionParseException {
        super(instruction);
        // Parse the target dimensions from the instruction and convert to lowercase
        this.targetDimensions = Arrays.stream(instruction.getArray("dimensions"))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        // Set the template to our custom data class so that BetonQuest saves and loads our persistent data.
        template = DimensionsData.class;
    }

    /**
     * Called when the objective starts globally.
     * <p>This implementation does not need global initialization since data is tracked per player.</p>
     */
    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, BetonQuest.getInstance());
    }

    /**
     * Called when the objective starts for a specific profile.
     * <p>
     * In this method, we initialize the persistent data (DimensionsData) for the profile if it isn’t already set.
     * A notification is also sent to the player.
     * </p>
     *
     * @param profile the Profile for which the objective is starting.
     */
    @Override
    public void start(Profile profile) {
        if (dataMap.get(profile) == null) {
            dataMap.put(profile, new DimensionsData(getDefaultDataInstruction(), profile, Id));
        }
        OfflinePlayer player = profile.getPlayer();
        if (player.isOnline()) {
            player.getPlayer().sendMessage("Quest accepted: Visit all target dimensions!");
        }
    }

    /**
     * Called when the objective stops globally.
     * <p>This implementation unregisters all listeners.</p>
     */
    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }

    /**
     * Called when the objective stops for a specific profile.
     * <p>
     * Removes the persistent data associated with the profile and notifies the player that the quest has been aborted.
     * </p>
     *
     * @param profile the Profile for which the objective is stopping.
     */
    @Override
    public void stop(Profile profile) {
        dataMap.remove(profile);
        OfflinePlayer player = profile.getPlayer();
        if (player.isOnline()) {
            player.getPlayer().sendMessage("Quest aborted: Dimension adventure cancelled!");
        }
    }

    /**
     * Returns the default data instruction for this objective.
     * <p>
     * In this case, it returns the string representation of the target dimensions.
     * </p>
     *
     * @return a string representing the target dimensions.
     */
    @Override
    public String getDefaultDataInstruction() {
        return targetDimensions.toString();
    }

    /**
     * Returns the default data instruction for a specific profile.
     * <p>
     * This method retrieves the visited dimensions from the persistent data and returns them as a string.
     * </p>
     *
     * @param profile the Profile for which to return data.
     * @return a string representing the visited dimensions.
     */
    @Override
    public String getDefaultDataInstruction(Profile profile) {
        DimensionsData data = getDimensionsData(profile);
        return String.join(",", data.getVisitedDimensions());
    }

    /**
     * Returns a property value for this objective.
     * <p>This objective does not have additional properties, so an empty string is returned.</p>
     *
     * @param key     the property key.
     * @param profile the Profile.
     * @return an empty string.
     */
    @Override
    public String getProperty(String key, Profile profile) {
        return "";
    }

    /**
     * Event handler for DimensionWarpEvent.
     * <p>
     * When a player warps to a dimension, this method checks if the dimension is one of the target dimensions.
     * If so, and if it has not been recorded yet, it adds the dimension to the persistent data.
     * Once all target dimensions have been visited, the objective is completed.
     * </p>
     *
     * @param event the DimensionWarpEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDimensionWarp(DimensionWarpEvent event) {
        // Get the player's profile from the event.
        OnlineProfile profile = PlayerConverter.getID(event.getPlayer());
        // Get the identifier of the warped dimension and convert it to lowercase.
        String dimensionId = event.getDimension().getIdentifier().toLowerCase();
        // Retrieve the persistent data (DimensionsData) for this profile.
        DimensionsData data = getDimensionsData(profile);
        if (!containsPlayer(profile)) {
            return;
        }
        // If there are no target dimensions, complete the objective immediately.
        if (targetDimensions.isEmpty()) {
            completeObjective(profile);
            return;
        }
        // If the objective is not yet complete and the dimension is a target dimension that hasn't been visited:
        if (!data.isCompleted() &&
                targetDimensions.contains(dimensionId) &&
                !data.getVisitedDimensions().contains(dimensionId)) {
            data.addVisitedDimension(dimensionId);
            // After updating, if all target dimensions have been visited and conditions are met, complete the objective.
            if (data.isCompleted() && checkConditions(profile)) {
                completeObjective(profile);
            } else {
                // Optionally send a notification to the player about their progress.
                // (sendNotify(...) is a custom method you may implement.)
            }
        } else if (data.isCompleted() && checkConditions(profile)) {
            completeObjective(profile);
        }
    }

    /**
     * Retrieves the DimensionsData for a given profile.
     *
     * @param profile the Profile.
     * @return the DimensionsData associated with the profile.
     */
    private DimensionsData getDimensionsData(Profile profile) {
        return (DimensionsData) dataMap.get(profile);
    }

    /**
     * DimensionsData is a custom ObjectiveData class used to track which target dimensions a player has visited.
     * <p>
     * It stores the array of target dimensions and a list of visited dimension IDs. Every time the data changes,
     * the update() method is called to save the data.
     * </p>
     */
    public static class DimensionsData extends ObjectiveData {
        private final Set<String> targetDimensions;
        private final Set<String> visitedDimensions = new HashSet<>();

        /**
         * Constructs a new DimensionsData object.
         *
         * @param instruction the data instruction string (should be a comma‐separated list of target dimensions).
         * @param profile     the Profile to load data for.
         * @param objID       the objective ID.
         */
        public DimensionsData(String instruction, Profile profile, String objID) {
            super(instruction, profile, objID);
            // Parse target dimensions from the instruction.
            // (For simplicity, assume the instruction is a comma-separated list of dimensions.)
            String[] targets = instruction.replaceAll("[\\[\\]\\s]", "").split(",");
            this.targetDimensions = Arrays.stream(targets)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }

        /**
         * Adds a dimension to the set of visited dimensions and updates the persistent data.
         *
         * @param dimension the dimension identifier to add.
         */
        public void addVisitedDimension(String dimension) {
            visitedDimensions.add(dimension);
            update(); // Save updated data to the database.
        }

        /**
         * Returns an unmodifiable set of visited dimensions.
         *
         * @return the set of visited dimension IDs.
         */
        public Set<String> getVisitedDimensions() {
            return java.util.Collections.unmodifiableSet(visitedDimensions);
        }

        /**
         * Checks whether all target dimensions have been visited.
         *
         * @return true if every target dimension is contained in the visited dimensions.
         */
        public boolean isCompleted() {
            return visitedDimensions.containsAll(targetDimensions);
        }

        /**
         * Returns the default data instruction for this data object.
         * <p>
         * For example, it returns a comma‐separated list of visited dimensions.
         * </p>
         *
         * @return a string representing the visited dimensions.
         */
        @Override
        public String toString() {
            return String.join(",", visitedDimensions);
        }
    }
}
