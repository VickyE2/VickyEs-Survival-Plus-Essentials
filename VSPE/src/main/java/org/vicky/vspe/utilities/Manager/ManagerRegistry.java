package org.vicky.vspe.utilities.Manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManagerRegistry {
    public static List<IdentifiableManager> registeredManagers = new ArrayList<>();

    public static void register(IdentifiableManager manager) {
        registeredManagers.add(manager);
    }

    public static IdentifiableManager getManager(String id) {
        return registeredManagers.stream().filter(m -> m.getManagerId().equals(id)).findAny().get();
    }

    public static String[] getRegisteredManagers() {
        Set<String> registeredManagersNames = new HashSet<>();
        for (IdentifiableManager manager : registeredManagers) {
            registeredManagersNames.add(manager.getManagerId());
        }
        return registeredManagersNames.toArray(new String[0]);
    }
}
