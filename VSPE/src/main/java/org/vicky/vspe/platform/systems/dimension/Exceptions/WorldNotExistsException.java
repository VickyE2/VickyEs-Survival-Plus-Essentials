package org.vicky.vspe.platform.systems.dimension.Exceptions;

public class WorldNotExistsException extends Exception {
    private final String message;

    public WorldNotExistsException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
