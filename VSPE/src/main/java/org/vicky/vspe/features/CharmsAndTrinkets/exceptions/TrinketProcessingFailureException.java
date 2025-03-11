package org.vicky.vspe.features.CharmsAndTrinkets.exceptions;

public class TrinketProcessingFailureException extends Exception {
    private final Exception e;

    public TrinketProcessingFailureException(String message, Exception e) {
        super(message);
        this.e = e;
    }

    @Override
    public String toString() {
        int index = 0;
        StringBuilder error = new StringBuilder();
        error.append(e.getMessage()).append(", Caused by ").append(e.getCause()).append("\n");
        for (StackTraceElement s : e.getStackTrace()) {
            if (index <= 20) {
                index++;
                error.append("Class: ").append(s.getClassName()).append(" Line: ").append(s.getLineNumber()).append(" File: ").append(s.getFileName()).append("\n");
            }
        }
        return error.toString();
    }
}
