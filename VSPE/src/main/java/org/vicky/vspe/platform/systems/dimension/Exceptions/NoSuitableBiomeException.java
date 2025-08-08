package org.vicky.vspe.platform.systems.dimension.Exceptions;

public class NoSuitableBiomeException extends Exception {
    private final String message;

    public NoSuitableBiomeException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
