package org.vicky.vspe.platform.systems.platformquestingintegration;

import org.vicky.vspe.platform.utilities.OutputTarget;

import java.io.IOException;

public interface QuestProductionFactory {

    /**
     * Creates a Quest from some source (AI, file, script, etc.).
     */
    Quest produce(QuestContext context) throws Exception;

    /**
     * Optional: Save/export the generated quest to file or datapack.
     */
    default void export(Quest quest, OutputTarget target) throws IOException {
        // Optional - platform implementors can override
    }

    /**
     * Checks whether this factory supports the input format/context.
     */
    boolean supports(QuestContext context);
}