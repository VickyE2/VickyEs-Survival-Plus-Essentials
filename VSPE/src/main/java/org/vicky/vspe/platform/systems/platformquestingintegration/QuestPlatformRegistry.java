package org.vicky.vspe.platform.systems.platformquestingintegration;

import java.util.ArrayList;
import java.util.List;

public class QuestPlatformRegistry {
    private static final List<QuestProductionFactory> factories = new ArrayList<>();

    public static void register(QuestProductionFactory factory) {
        factories.add(factory);
    }

    public static Quest createQuest(QuestContext context) throws Exception {
        for (var factory : factories) {
            if (factory.supports(context)) return factory.produce(context);
        }
        throw new IllegalStateException("No suitable factory for context: " + context.sourceType());
    }
}
