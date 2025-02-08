package org.vicky.vspe.systems.Dimension.Exceptions;

public class MissingConfigrationException extends Exception {
    private final String message;

    public MissingConfigrationException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
