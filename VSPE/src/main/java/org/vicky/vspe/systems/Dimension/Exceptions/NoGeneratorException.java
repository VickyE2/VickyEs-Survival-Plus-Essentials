package org.vicky.vspe.systems.Dimension.Exceptions;

public class NoGeneratorException extends Exception {
    private final String message;

    public NoGeneratorException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
