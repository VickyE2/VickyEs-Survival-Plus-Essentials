package org.vicky.vspe.utilities.Manager;

import com.google.common.base.MoreObjects;

public class EntityNotFoundException extends Exception {

    private final String message;

    public EntityNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("message", message)
                .toString();
    }
}
