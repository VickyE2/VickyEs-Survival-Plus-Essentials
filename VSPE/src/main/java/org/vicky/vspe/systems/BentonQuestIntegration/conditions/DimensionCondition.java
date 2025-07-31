package org.vicky.vspe.systems.BentonQuestIntegration.conditions;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.Condition;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;

public class DimensionCondition extends Condition {
    private final String[] dimensions;
    private final boolean doNotBeIn;

    public DimensionCondition(Instruction instruction, boolean forceSync) {
        super(instruction, forceSync);
        dimensions = instruction.getArray("dimensions");
        doNotBeIn = instruction.hasArgument("doNotBeIn");
    }

    /**
     * This method should contain all logic for the condition and use data
     * parsed by the constructor. Don't worry about inverting the condition,
     * it's done by the rest of BetonQuest's logic. When this method is called
     * all the required data must be present and parsed correctly.
     *
     * @param profile the {@link Profile} for which the condition will be checked
     * @return the result of the check
     * @throws QuestRuntimeException when an error happens at runtime (for example a numeric
     *                               variable resolves to a string)
     */
    @Override
    protected Boolean execute(Profile profile) throws QuestRuntimeException {
        return null;
    }
}
