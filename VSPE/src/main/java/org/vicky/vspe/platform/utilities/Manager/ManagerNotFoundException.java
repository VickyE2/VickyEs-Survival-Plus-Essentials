package org.vicky.vspe.platform.utilities.Manager;

import com.google.common.base.MoreObjects;

public class ManagerNotFoundException extends Exception {

    private final String message;

    public ManagerNotFoundException(String message) {
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
