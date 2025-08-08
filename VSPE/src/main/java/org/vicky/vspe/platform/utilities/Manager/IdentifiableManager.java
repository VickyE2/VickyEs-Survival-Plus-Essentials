package org.vicky.vspe.platform.utilities.Manager;

import org.vicky.utilities.Identifiable;

import java.util.List;

public interface IdentifiableManager {
    String getManagerId();

    void removeEntity(String namespace) throws EntityNotFoundException;

    void disableEntity(String namespace) throws EntityNotFoundException;

    void enableEntity(String namespace) throws EntityNotFoundException;

    List<Identifiable> getRegisteredEntities();

    List<Identifiable> getUnregisteredEntities();
}
