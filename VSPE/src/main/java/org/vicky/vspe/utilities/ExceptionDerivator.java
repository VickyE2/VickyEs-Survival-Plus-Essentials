package org.vicky.vspe.utilities;

import org.vicky.vspe.systems.dimension.Exceptions.MissingConfigrationException;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

public class ExceptionDerivator {
    public static String parseException(Throwable exception) {
        String message = exception.getMessage();

        if (exception instanceof NullPointerException && message.contains("because")) {
            String[] parts = message.split("because", 2);
            if (parts.length > 1) {
                String[] words = parts[1].trim().split("\\s+");
                if (words.length > 0) {
                    String field = words[0].replace("\"", "");
                    if (field.contains(".")) {
                        String[] fieldParts = field.split("\\.");
                        if (fieldParts.length >= 2) {
                            String className = capitalizeFirstLetter(fieldParts[0]);
                            String parameterName = capitalizeFirstLetter(fieldParts[1]);
                            return String.format("From %s, parameter %s is not specified.", className, parameterName);
                        }
                    }
                }
            }
        }

        // Handle NullPointerException
        if (exception instanceof NullPointerException) {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0]; // Get the first element in the stack trace
                return String.format(
                        "NullPointerException at %s.%s (line %d). Ensure that the object is initialized before use.",
                        element.getClassName(),
                        element.getMethodName(),
                        element.getLineNumber()
                );
            }
            return "A NullPointerException occurred. Ensure that no object is null before using it.";
        }

        // Handle InstantiationException
        if (exception instanceof InstantiationException) {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
                return String.format(
                        "InstantiationException at %s.%s (line %d). Check if the class is abstract or an interface.",
                        element.getClassName(),
                        element.getMethodName(),
                        element.getLineNumber()
                );
            }
        }

        // Handle IllegalAccessException
        if (exception instanceof IllegalAccessException) {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
                return String.format(
                        "IllegalAccessException at %s.%s (line %d). Check if the class or method is accessible.",
                        element.getClassName(),
                        element.getMethodName(),
                        element.getLineNumber()
                );
            }
        }

        // Handle InvocationTargetException
        if (exception instanceof InvocationTargetException) {
            Throwable cause = exception.getCause();
            if (cause != null) {
                String causeMessage = parseException(cause); // Recursively parse the underlying cause
                StackTraceElement[] stackTrace = exception.getStackTrace();
                if (stackTrace.length > 0) {
                    StackTraceElement element = stackTrace[0];
                    return String.format(
                            "InvocationTargetException at %s.%s (line %d). The invocation caused an exception:%n             -> %s",
                            element.getClassName(),
                            element.getMethodName(),
                            element.getLineNumber(),
                            causeMessage == null ? cause.getCause() : causeMessage
                    );
                }
            } else {
                StackTraceElement[] stackTrace = exception.getStackTrace();
                if (stackTrace.length > 0) {
                    StackTraceElement element = stackTrace[0];
                    return String.format(
                            "InvocationTargetException at %s.%s (line %d). Check the underlying exception caused by the invocation.",
                            element.getClassName(),
                            element.getMethodName(),
                            element.getLineNumber()
                    );
                }
            }
        }

        // Handle NoSuchMethodException
        if (exception instanceof NoSuchMethodException) {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
                return String.format(
                        "NoSuchMethodException at %s.%s (line %d). Ensure the method name and parameters are correct.",
                        element.getClassName(),
                        element.getMethodName(),
                        element.getLineNumber()
                );
            }
        }

        // Handle IllegalArgumentException
        if (exception instanceof IllegalArgumentException && message != null && message.contains("argument")) {
            String[] parts = message.split("argument");
            if (parts.length > 1) {
                String invalidArgument = parts[1].trim().split(" ")[0];
                return String.format("The argument '%s' is invalid. Please verify the method parameters.", invalidArgument);
            }
        }

        // Handle IndexOutOfBoundsException
        if (exception instanceof IndexOutOfBoundsException && message != null) {
            String[] parts = message.split("index");
            if (parts.length > 1) {
                String index = parts[1].trim().split(" ")[0];
                return String.format("Attempted to access index '%s' which is out of bounds. Check the list or array size.", index);
            }
        }

        // Handle FileNotFoundException
        if (exception instanceof FileNotFoundException && message != null) {
            String[] parts = message.split("'");
            if (parts.length > 1) {
                String filePath = parts[1].replace("'", "").trim();
                return String.format("The file at path '%s' could not be found. Please check the file path.", filePath);
            }
        }

        // Handle FileNotFoundException
        if (exception instanceof MissingConfigrationException && message != null) {
            return "The context generator failed to provide a necessary parameter: " + message;
        }

        // Handle NumberFormatException
        if (exception instanceof NumberFormatException && message != null) {
            return "The string could not be parsed into a valid number. Please check the input format.";
        }

        // Handle ClassNotFoundException
        if (exception instanceof ClassNotFoundException && message != null) {
            String[] parts = message.split("class");
            if (parts.length > 1) {
                String className = parts[1].trim();
                return String.format("The referenced class '%s' could not be found. Please verify the class name.", className);
            }
        }

        // Handle NoSuchElementException
        if (exception instanceof NoSuchElementException && message != null) {
            return "No such element was found. Ensure the collection contains the expected elements.";
        }

        return null;
    }

    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
