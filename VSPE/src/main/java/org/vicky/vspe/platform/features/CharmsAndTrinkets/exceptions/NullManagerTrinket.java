package org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions;

import org.vicky.platform.PlatformItemStack;
import org.vicky.platform.PlatformPlayer;

/**
 * Exception thrown when a database operation for a trinket fails
 * due to a missing or null database entry.
 * <p>
 * This exception can optionally carry an {@link PlatformItemStack} representing the trinket
 * and a {@link PlatformPlayer} representing the involved player.
 * </p>
 */
public class NullManagerTrinket extends Exception {
    private final PlatformItemStack itemStack;
    private final PlatformPlayer player;

    /**
     * Constructs a new NullDatabaseTrinket exception with no detail message,
     * no associated PlatformItemStack, and no associated PlatformPlayer.
     */
    public NullManagerTrinket() {
        super();
        this.itemStack = null;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message,
     * and no associated PlatformItemStack or PlatformPlayer.
     *
     * @param message the detail message
     */
    public NullManagerTrinket(String message) {
        super(message);
        this.itemStack = null;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message
     * and associated PlatformItemStack.
     *
     * @param message   the detail message
     * @param itemStack the PlatformItemStack associated with this exception; may be null
     */
    public NullManagerTrinket(String message, PlatformItemStack itemStack) {
        super(message);
        this.itemStack = itemStack;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated PlatformItemStack.
     *
     * @param itemStack the PlatformItemStack associated with this exception
     */
    public NullManagerTrinket(PlatformItemStack itemStack) {
        super();
        this.itemStack = itemStack;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message
     * and associated PlatformPlayer.
     *
     * @param message the detail message
     * @param player  the PlatformPlayer associated with this exception; may be null
     */
    public NullManagerTrinket(String message, PlatformPlayer player) {
        super(message);
        this.itemStack = null;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated PlatformPlayer.
     *
     * @param player the PlatformPlayer associated with this exception
     */
    public NullManagerTrinket(PlatformPlayer player) {
        super();
        this.itemStack = null;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message,
     * associated PlatformItemStack, and associated PlatformPlayer.
     *
     * @param message   the detail message
     * @param itemStack the PlatformItemStack associated with this exception; may be null
     * @param player    the PlatformPlayer associated with this exception; may be null
     */
    public NullManagerTrinket(String message, PlatformItemStack itemStack, PlatformPlayer player) {
        super(message);
        this.itemStack = itemStack;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated PlatformItemStack
     * and associated PlatformPlayer.
     *
     * @param itemStack the PlatformItemStack associated with this exception
     * @param player    the PlatformPlayer associated with this exception
     */
    public NullManagerTrinket(PlatformItemStack itemStack, PlatformPlayer player) {
        super();
        this.itemStack = itemStack;
        this.player = player;
    }

    /**
     * Retrieves the PlatformItemStack associated with this exception.
     *
     * @return the associated PlatformItemStack, or null if none was provided
     */
    public PlatformItemStack getPlatformItemStack() {
        return itemStack;
    }

    /**
     * Retrieves the PlatformPlayer associated with this exception.
     *
     * @return the associated PlatformPlayer, or null if none was provided
     */
    public PlatformPlayer getPlayer() {
        return player;
    }
}