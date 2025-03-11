package org.vicky.vspe.utilities.Manager;

import java.util.*;

public class ManagerRegistry {
    public static List<IdentifiableManager> registeredManagers = new ArrayList<>();

    public static void register(IdentifiableManager manager) {
        registeredManagers.add(manager);
    }

    public static Optional<IdentifiableManager> getManager(String id) {
        return registeredManagers.stream().filter(m -> m.getManagerId().equals(id)).findAny();
    }

    public static <T extends IdentifiableManager> Optional<T> getManager(Class<T> clazz) {
        return registeredManagers.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findAny();
    }

    public static String[] getRegisteredManagers() {
        Set<String> registeredManagersNames = new HashSet<>();
        for (IdentifiableManager manager : registeredManagers) {
            registeredManagersNames.add(manager.getManagerId());
        }
        return registeredManagersNames.toArray(new String[0]);
    }
}
