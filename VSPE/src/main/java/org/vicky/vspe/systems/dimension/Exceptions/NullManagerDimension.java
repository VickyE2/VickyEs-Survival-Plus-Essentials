package org.vicky.vspe.systems.dimension.Exceptions;

/**
 * Exception thrown when a manager operation for a dimension fails
 * due to a missing or null database entry.
 * <p>
 * This exception can optionally carry a {@link String} representing the id
 * </p>
 */
public class NullManagerDimension extends Exception {
    private final String identifier;

    /**
     * Constructs a new NullManagerDimension exception with no detail message,
     * no associated identifier, and no associated Player.
     */
    public NullManagerDimension() {
        super();
        this.identifier = null;
    }

    /**
     * Constructs a new NullManagerDimension exception with the specified detail message,
     * and no associated identifier.
     *
     * @param message the detail message
     */
    public NullManagerDimension(String message) {
        super(message);
        this.identifier = null;
    }

    /**
     * Constructs a new NullManagerDimension exception with the specified detail message
     * and associated identifier.
     *
     * @param message   the detail message
     * @param identifier the identifier associated with this exception; may be null
     */
    public NullManagerDimension(String message, String identifier) {
        super(message);
        this.identifier = identifier;
    }

    /**
     * Retrieves the identifier associated with this exception.
     *
     * @return the associated identifier, or null if none was provided
     */
    public String getIdentifier() {
        return identifier;
    }
}