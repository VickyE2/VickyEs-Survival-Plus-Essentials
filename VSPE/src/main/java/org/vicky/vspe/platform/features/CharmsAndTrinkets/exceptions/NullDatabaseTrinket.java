package org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions;


import org.vicky.platform.PlatformPlayer;
import org.vicky.vspe.platform.PlatformItem;

/**
 * Exception thrown when a database operation for a trinket fails
 * due to a missing or null database entry.
 * <p>
 * This exception can optionally carry an {@link PlatformItem} representing the trinket
 * and a {@link PlatformPlayer} representing the involved player.
 * </p>
 */
public class NullDatabaseTrinket extends Exception {
    private final PlatformItem itemStack;
    private final PlatformPlayer player;

    /**
     * Constructs a new NullDatabaseTrinket exception with no detail message,
     * no associated PlatformItem, and no associated PlatformPlayer.
     */
    public NullDatabaseTrinket() {
        super();
        this.itemStack = null;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message,
     * and no associated PlatformItem or PlatformPlayer.
     *
     * @param message the detail message
     */
    public NullDatabaseTrinket(String message) {
        super(message);
        this.itemStack = null;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message
     * and associated PlatformItem.
     *
     * @param message   the detail message
     * @param itemStack the PlatformItem associated with this exception; may be null
     */
    public NullDatabaseTrinket(String message, PlatformItem itemStack) {
        super(message);
        this.itemStack = itemStack;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated PlatformItem.
     *
     * @param itemStack the PlatformItem associated with this exception
     */
    public NullDatabaseTrinket(PlatformItem itemStack) {
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
    public NullDatabaseTrinket(String message, PlatformPlayer player) {
        super(message);
        this.itemStack = null;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated PlatformPlayer.
     *
     * @param player the PlatformPlayer associated with this exception
     */
    public NullDatabaseTrinket(PlatformPlayer player) {
        super();
        this.itemStack = null;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message,
     * associated PlatformItem, and associated PlatformPlayer.
     *
     * @param message   the detail message
     * @param itemStack the PlatformItem associated with this exception; may be null
     * @param player    the PlatformPlayer associated with this exception; may be null
     */
    public NullDatabaseTrinket(String message, PlatformItem itemStack, PlatformPlayer player) {
        super(message);
        this.itemStack = itemStack;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated PlatformItem
     * and associated PlatformPlayer.
     *
     * @param itemStack the PlatformItem associated with this exception
     * @param player    the PlatformPlayer associated with this exception
     */
    public NullDatabaseTrinket(PlatformItem itemStack, PlatformPlayer player) {
        super();
        this.itemStack = itemStack;
        this.player = player;
    }

    /**
     * Retrieves the PlatformItem associated with this exception.
     *
     * @return the associated PlatformItem, or null if none was provided
     */
    public PlatformItem getPlatformItem() {
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