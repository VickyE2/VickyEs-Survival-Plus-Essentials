package org.vicky.vspe.features.CharmsAndTrinkets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.utilities.SmallCapsConverter;
import org.vicky.utilities.UUIDGenerator;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullTrinketUser;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.CnTPlayer;
import org.vicky.vspe.utilities.Hibernate.dao_s.AvailableTrinketDAO;
import org.vicky.vspe.utilities.Hibernate.dao_s.CnTPlayerDAO;
import org.vicky.vspe.utilities.SymbolManager;
import org.vicky.vspe.utilities.global.Events.TrinketEquippedEvent;
import org.vicky.vspe.utilities.global.Events.TrinketGenerationEvent;
import org.vicky.vspe.utilities.global.Events.TrinketUnEquippedEvent;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import static org.vicky.vspe.utilities.global.GlobalResources.trinketManager;

public abstract class BaseTrinket implements Identifiable {
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.FEATURE, "TRINKET-BASE");
    protected final String description;
    protected final String name;
    protected final UUID Id;
    protected final TrinketSlot trinketSlot;
    protected final List<TrinketAbilityType> trinketGroups;
    protected final ItemStack icon;
    protected final GuiCreator.ItemConfig rawIcon;

    // Thread-safe set to store the UUIDs of players using this trinket.
    protected final Set<UUID> activePlayers = new ConcurrentSkipListSet<>();

    public BaseTrinket(
            String description,
            TrinketSlot trinketSlot,
            List<TrinketAbilityType> trinketGroups,
            GuiCreator.ItemConfig icon
    ) {
        TrinketGenerationEvent contextEvent = new TrinketGenerationEvent(this);
        Bukkit.getPluginManager().callEvent(contextEvent);
        if (contextEvent.isCancelled()) {
            this.description = null;
            this.trinketSlot = null;
            this.trinketGroups = null;
            this.icon = null;
            this.Id = null;
            this.name = null;
            this.rawIcon = null;
            return;
        }
        this.description = description;
        this.trinketSlot = trinketSlot;
        this.trinketGroups = trinketGroups;
        String aspects = trinketGroups.stream()
                .map(trinketGroup -> "  " + ChatColor.GOLD + trinketGroup.name().toLowerCase() + ChatColor.RESET
                        + SymbolManager.getSymbol(trinketGroup.getIcoName()))
                .collect(Collectors.joining("\n"));
        icon.getLore().add("sʟᴏᴛ: " + ChatColor.WHITE + SymbolManager.getSymbol(trinketSlot.name().toLowerCase()) + ChatColor.RESET);
        icon.getLore().add(ChatColor.GOLD + "ʙᴜғғ ᴀᴄᴘᴇᴄᴛs: \n" + ChatColor.RESET + aspects);
        icon.addNbtData("vspe_trinket_slot", new GuiCreator.AllowedEnum<>(trinketSlot));
        icon.addNbtData("Unique_Nonstack_Identifier", new GuiCreator.AllowedUUID(UUID.randomUUID()));
        this.name = icon.getName();
        this.Id = UUIDGenerator.generateUUIDFromString(getFormattedName());
        icon.addNbtData("vspe_trinket_id", new GuiCreator.AllowedUUID(this.Id));
        icon.setName(SmallCapsConverter.toSmallCaps(icon.getName()));
        this.icon = GuiCreator.createItem(icon, null, CnTManager.getPlugin());
        this.rawIcon = icon;

        AvailableTrinketDAO dao = new AvailableTrinketDAO();
        if (dao.findById(this.Id.toString()).isEmpty()) {
            dao.create(getId().toString(), this.name);
        }
    }

    public ItemStack getIcon() {
        return icon;
    }

    public GuiCreator.ItemConfig getRawIcon() {
        return rawIcon;
    }

    public String getName() {
        return name;
    }

    public List<TrinketAbilityType> getTrinketGroups() {
        return trinketGroups;
    }

    public Set<UUID> getActivePlayers() {
        return activePlayers;
    }

    public String getFormattedName() {
        return this.name.toLowerCase().replace(" ", "_")
                .replace("! @ # $ % ^ & * ( ) - + = { } [ ] : ; ' < , > . ? / ` ~ ", "_");
    }

    /**
     * Adds a player to this trinket’s active user list.
     *
     * @param player the player using the trinket.
     */
    public void addPlayer(Player player) {
        logger.printBukkit("Added player " + player.name() + " to trinket " + this.name);
        activePlayers.add(player.getUniqueId());
    }

    /**
     * Removes a player from this trinket’s active user list.
     *
     * @param player the player to remove.
     */
    public void removePlayer(Player player) {
        logger.printBukkit("Removed player " + player.name() + " to trinket " + this.name);
        activePlayers.remove(player.getUniqueId());
    }

    /**
     * Checks if a player is currently using this trinket.
     *
     * @param player the player to check.
     * @return true if the player is active.
     */
    public boolean isPlayerUsing(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }

    /**
     * When a player applies the trinket, they are added to the active players list
     * and the ability is executed.
     *
     * @param player the player using the trinket.
     */
    public void applyTrinketAbility(Player player) {
        addPlayer(player);
        performTrinketAbility(player);
    }

    /**
     * Executes the specific trinket ability logic.
     *
     * @param activeUser the player using the trinket.
     */
    protected abstract void performTrinketAbility(Player activeUser);

    /**
     * Removes the trinket ability from the player.
     *
     * @param disabler the player disabling the trinket.
     */
    protected abstract void removeTrinketAbility(Player disabler);

    public String getDescription() {
        return this.description;
    }

    public TrinketSlot getTrinketSlot() {
        return this.trinketSlot;
    }

    public void setTrinketListener(TrinketEvent listener) {
        trinketManager.addTrinketFromListener(listener, this);
    }

    public UUID getId() {
        return this.Id;
    }

    public String getIdentifier() {
        return this.Id.toString();
    }

    public final void enableTrinket() {
        logger.printBukkit("Enabling trinket " + this.name, ContextLogger.LogType.PENDING);
    }

    public final void disableTrinket() {
        logger.printBukkit("Disabling trinket " + this.name, ContextLogger.LogType.PENDING);
    }

    public final void deleteTrinket() {
        logger.printBukkit("Deleting trinket " + this.name, ContextLogger.LogType.PENDING);
    }

    /**
     * Base class for trinket event listeners that are shared for all users of this trinket.
     * The listener has a reference to its parent trinket so it can check the active players list.
     */
    public abstract static class TrinketEvent implements Listener {
        protected final BaseTrinket trinket;
        protected final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "TRINKET-LISTENER-" + this.getClass().getSimpleName().toUpperCase());

        public void unloadSelf() {
            HandlerList.unregisterAll(this);
        }

        protected TrinketEvent(BaseTrinket trinket) {
            this.trinket = trinket;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            return this.getClass().equals(obj.getClass());
        }

        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }

        @EventHandler
        public void onPlayerQuitServerTrinketCheck(PlayerQuitEvent event) {
            if (trinket.isPlayerUsing(event.getPlayer())) {
                trinket.removeTrinketAbility(event.getPlayer());
            }
        }

        @EventHandler
        public void onPlayerEquipTrinket(TrinketEquippedEvent event) {
            if (!event.isTrinket(this.trinket)) return;
            if (this.trinket.isPlayerUsing(event.getPlayer())) return;
            this.trinket.addPlayer(event.getPlayer());
        }

        @EventHandler
        public void onPlayerUnEquipTrinket(TrinketUnEquippedEvent event) {
            if (!event.isTrinket(this.trinket)) return;
            if (!this.trinket.isPlayerUsing(event.getPlayer())) return;
            this.trinket.removePlayer(event.getPlayer());
        }
    }
}
