package org.vicky.vspe.features.CharmsAndTrinkets.exceptions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Exception thrown when a database operation for a trinket fails
 * due to a missing or null database entry.
 * <p>
 * This exception can optionally carry an {@link ItemStack} representing the trinket
 * and a {@link Player} representing the involved player.
 * </p>
 */
public class NullManagerTrinket extends Exception {
    private final ItemStack itemStack;
    private final Player player;

    /**
     * Constructs a new NullDatabaseTrinket exception with no detail message,
     * no associated ItemStack, and no associated Player.
     */
    public NullManagerTrinket() {
        super();
        this.itemStack = null;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message,
     * and no associated ItemStack or Player.
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
     * and associated ItemStack.
     *
     * @param message   the detail message
     * @param itemStack the ItemStack associated with this exception; may be null
     */
    public NullManagerTrinket(String message, ItemStack itemStack) {
        super(message);
        this.itemStack = itemStack;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated ItemStack.
     *
     * @param itemStack the ItemStack associated with this exception
     */
    public NullManagerTrinket(ItemStack itemStack) {
        super();
        this.itemStack = itemStack;
        this.player = null;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message
     * and associated Player.
     *
     * @param message the detail message
     * @param player  the Player associated with this exception; may be null
     */
    public NullManagerTrinket(String message, Player player) {
        super(message);
        this.itemStack = null;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated Player.
     *
     * @param player the Player associated with this exception
     */
    public NullManagerTrinket(Player player) {
        super();
        this.itemStack = null;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified detail message,
     * associated ItemStack, and associated Player.
     *
     * @param message   the detail message
     * @param itemStack the ItemStack associated with this exception; may be null
     * @param player    the Player associated with this exception; may be null
     */
    public NullManagerTrinket(String message, ItemStack itemStack, Player player) {
        super(message);
        this.itemStack = itemStack;
        this.player = player;
    }

    /**
     * Constructs a new NullDatabaseTrinket exception with the specified associated ItemStack
     * and associated Player.
     *
     * @param itemStack the ItemStack associated with this exception
     * @param player    the Player associated with this exception
     */
    public NullManagerTrinket(ItemStack itemStack, Player player) {
        super();
        this.itemStack = itemStack;
        this.player = player;
    }

    /**
     * Retrieves the ItemStack associated with this exception.
     *
     * @return the associated ItemStack, or null if none was provided
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Retrieves the Player associated with this exception.
     *
     * @return the associated Player, or null if none was provided
     */
    public Player getPlayer() {
        return player;
    }
}