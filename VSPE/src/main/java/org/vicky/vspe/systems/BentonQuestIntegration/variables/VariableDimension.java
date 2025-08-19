package org.vicky.vspe.systems.BentonQuestIntegration.variables;

import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.instruction.variable.Variable;
import org.betonquest.betonquest.quest.registry.processor.VariableProcessor;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.vicky.vspe.platform.systems.dimension.PlatformBaseDimension;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.systems.dimension.BukkitBaseDimension;
import org.vicky.vspe.systems.dimension.VSPEBukkitDimensionManager;

import java.util.Optional;

public class VariableDimension extends Variable<BukkitBaseDimension> {
    /**
     * Resolves a string that may contain variables to a variable of the given type.
     *
     * @param variableProcessor the processor to create the variables
     * @param pack              the package in which the variable is used in
     * @param input             the string that may contain variables
     * @throws InstructionParseException if the variables could not be created or resolved to the given type
     */
    public VariableDimension(VariableProcessor variableProcessor, QuestPackage pack, String input) throws InstructionParseException {
        super(variableProcessor, pack, input, VariableDimension::parse);
    }

    public static BukkitBaseDimension parse(final String value) throws QuestRuntimeException {
        final Optional<PlatformBaseDimension<BlockData, World>> world = ManagerRegistry.getManager(VSPEBukkitDimensionManager.class).get().getDimension(value);
        if (world.isEmpty()) {
            throw new QuestRuntimeException("Dimension " + value + " does not exists.");
        }
        return (BukkitBaseDimension) world.get();
    }
}
