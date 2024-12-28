/**
 * Represents a base advancement that can be extended to create custom advancements.
 * This class integrates with CrazyAdvancements for managing advancements and their properties.
 */
package org.vicky.vspe.features.AdvancementPlus;

import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementFlag;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementNotExists;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.utilities.DatabaseManager.templates.Advancement;
import org.vicky.vspe.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.utilities.UUIDGenerator;
import org.vicky.vspe.utilities.global.Events.PlayerReceivedAdvancement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.vicky.vspe.utilities.global.GlobalResources.advancementManager;
import static org.vicky.vspe.utilities.global.GlobalResources.databaseManager;

public abstract class BaseAdvancement {
    /**
     * The description of the advancement.
     */
    protected String description;

    /**
     * The title of the advancement.
     */
    protected String title;

    /**
     * The namespace associated with the advancement.
     */
    protected String namespace;

    /**
     * The unique identifier for the advancement.
     */
    protected UUID Id;

    /**
     * The type of the advancement.
     */
    protected AdvancementType advancementType;

    /**
     * The toast type used for the advancement.
     */
    protected ToastType advancementTT;

    /**
     * The toast frame used to display the advancement.
     */
    protected AdvancementDisplay.AdvancementFrame toastType;

    /**
     * The dimensions in which the advancement can be granted.
     */
    protected List<BaseDimension> eligibleDimensions;

    /**
     * The icon representing the advancement.
     */
    protected ItemStack icon;

    /**
     * Indicates if the advancement is a parent advancement.
     */
    protected boolean hasParent;

    /**
     * The parent advancement of this advancement.
     */
    protected BaseAdvancement parent;

    protected AdvancementVisibility visibility;

    /**
     * Constructs a new BaseAdvancement.
     *
     * @param icon                The icon representing the advancement.
     * @param namespace           The namespace of the advancement.
     * @param toastFrame          The toast frame to display the advancement.
     * @param description         The description of the advancement.
     * @param title               The title of the advancement.
     * @param permittedDimensions The dimensions in which the advancement is permitted.
     * @param advancementType     The type of the advancement.
     * @param aTT                 The toast type associated with the advancement.
     */
    public BaseAdvancement(ItemStack icon, String namespace, AdvancementDisplay.AdvancementFrame toastFrame, String description, String title, List<BaseDimension> permittedDimensions, AdvancementType advancementType, AdvancementVisibility visibility, ToastType aTT) {
        this.description = description;
        this.advancementType = advancementType;
        this.title = title;
        this.advancementTT = aTT;
        this.namespace = namespace;
        this.toastType = toastFrame;
        this.icon = icon;
        this.hasParent = false;
        this.Id = UUIDGenerator.generateUUIDFromString(getFormattedTitle());
        this.parent = null;
        this.visibility = visibility;
        this.eligibleDimensions = permittedDimensions != null && !permittedDimensions.isEmpty()
                ? permittedDimensions
                : new ArrayList<>();

        advancementManager.LOADED_ADVANCEMENTS.add(this);
    }

    /**
     * Constructs a new BaseAdvancement with a specified parent.
     * <p>namespace - The namespace of the advancement is gotten from the specified parent.</p>
     *
     * @param icon                The icon representing the advancement.
     * @param toastFrame          The toast frame to display the advancement.
     * @param description         The description of the advancement.
     * @param title               The title of the advancement.
     * @param permittedDimensions The dimensions in which the advancement is permitted.
     * @param advancementType     The type of the advancement.
     * @param aTT                 The toast type associated with the advancement.
     * @param parentClass         The parent advancement's class.
     */
    public BaseAdvancement(ItemStack icon, AdvancementDisplay.AdvancementFrame toastFrame, String description, String title, List<BaseDimension> permittedDimensions, AdvancementType advancementType, AdvancementVisibility visibility, ToastType aTT, Class<? extends BaseAdvancement> parentClass) {
        this.description = description;
        this.advancementType = advancementType;
        this.title = title;
        this.advancementTT = aTT;
        this.toastType = toastFrame;
        this.icon = icon;
        this.Id = UUIDGenerator.generateUUIDFromString(getFormattedTitle());
        this.hasParent = true;
        this.parent = advancementManager.getAdvancement(parentClass);
        this.namespace = parent.namespace;
        this.visibility = visibility;
        this.eligibleDimensions = permittedDimensions != null && !permittedDimensions.isEmpty()
                ? permittedDimensions
                : new ArrayList<>();

        advancementManager.LOADED_ADVANCEMENTS.add(this);
    }

    /**
     * Gets the CrazyAdvancements instance of the advancement.
     *
     * @return The advancement instance.
     */
    public eu.endercentral.crazy_advancements.advancement.Advancement getInstance() {
        eu.endercentral.crazy_advancements.advancement.Advancement instancedAdvancement = getInstancedAdvancement(visibility);

        if (advancementCriteria() != null)
            instancedAdvancement.setCriteria(advancementCriteria());
        if (advancementReward() != null)
            instancedAdvancement.setReward(advancementReward());

        return instancedAdvancement;
    }

    @NotNull
    private eu.endercentral.crazy_advancements.advancement.Advancement getInstancedAdvancement(AdvancementVisibility visibility) {
        AdvancementDisplay display = new AdvancementDisplay(icon, title, description, toastType, visibility);

        if (parent != null) {
            display.setX(parent.getInstance().getDisplay().getX() + 1);

            if (parent.getInstance().getChildren().size() <= 4)
                display.setY(parent.getInstance().getChildren().size() - 1);
            else if (parent.getInstance().getChildren().size() <= 10)
                display.setY(parent.getInstance().getChildren().size() - 5);
            else
                display.setY(parent.getInstance().getChildren().size() - 10);

        } else {
            display.setBackgroundTexture("textures/gui/advancements/backgrounds/" + getFormattedTitle() + ".png");
            display.setX(2);
            display.setY(0);
        }

        return getAdvancement(display);
    }

    @NotNull
    private eu.endercentral.crazy_advancements.advancement.Advancement getAdvancement(AdvancementDisplay display) {
        eu.endercentral.crazy_advancements.advancement.Advancement instancedAdvancement;
        if (!hasParent) {
            instancedAdvancement = new eu.endercentral.crazy_advancements.advancement.Advancement(
                    new NameKey(namespace, getFormattedTitle()), display,
                    AdvancementFlag.SHOW_TOAST
            );

        } else {
            instancedAdvancement = new eu.endercentral.crazy_advancements.advancement.Advancement(
                    parent.getInstance(),
                    new NameKey(namespace, getFormattedTitle()), display, AdvancementFlag.SHOW_TOAST
            );

        }
        return instancedAdvancement;
    }

    /**
     * Formats the title of the advancement to a valid format.
     *
     * @return The formatted title.
     */
    public String getFormattedTitle() {
        return title.toLowerCase().replace(" ", "_").replace("! @ # $ % ^ & * ( ) - + = { } [ ] : ; ' < , > . ? / ` ~ ", "_");
    }

    /**
     * Grants the advancement to a player.
     *
     * @param player The player to grant the advancement to.
     */
    public void grantAdvancement(Player player) {
        performGrantAdvancement(player);
        PlayerReceivedAdvancement event = new PlayerReceivedAdvancement(player, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Checks if the player is in an eligible dimension to receive the advancement if any.
     *
     * @param player The player to check.
     * @return True if the player is in an eligible dimension, false otherwise.
     */
    protected boolean canGrantInDimension(Player player) {
        if (eligibleDimensions.isEmpty()) {
            return true;
        }
        return eligibleDimensions.stream().anyMatch(dimension -> dimension.isPlayerInDimension(player));
    }

    /**
     * Gets the criteria for the advancement.
     *
     * @return The advancement criteria.
     */
    protected abstract Criteria advancementCriteria();

    /**
     * Gets the reward for completing the advancement.
     *
     * @return The advancement reward.
     */
    protected abstract AdvancementReward advancementReward();


    /**
     * Checks if a player is eligible for the advancement.
     *
     * @param player The player to check.
     * @return True if the player is eligible, false otherwise.
     */
    protected boolean isPlayerEligible(Player player) {
        try {
            DatabasePlayer contextPlayer = databaseManager.getEntityById(DatabasePlayer.class, player.getUniqueId());
            org.vicky.vspe.utilities.DatabaseManager.templates.AdvancementManager advancementManager =
                    databaseManager.getEntityById(org.vicky.vspe.utilities.DatabaseManager.templates.AdvancementManager.class, 1);

            Optional<Advancement> contextAdvancement = advancementManager.getAdvancements()
                    .stream()
                    .filter(advancement -> advancement.getId().equals(getId()))
                    .findAny();

            if (contextAdvancement.isPresent()) {
                return !contextPlayer.getAccomplishedAdvancements().contains(contextAdvancement.get());
            } else {
                throw new AdvancementNotExists("Failed to check eligibility of player for advancement", this);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error encountered with SQL manager: " + e);
        }
    }

    /**
     * Performs the action to grant the advancement to the player.
     *
     * @param player The player to grant the advancement to.
     */
    protected abstract void performGrantAdvancement(OfflinePlayer player);

    public String getDescription() {
        return description;
    }

    public AdvancementType getAdvancementType() {
        return advancementType;
    }

    public ToastType getAdvancementTT() {
        return advancementTT;
    }

    public List<BaseDimension> getEligibleDimensions() {
        return eligibleDimensions;
    }

    public String getTitle() {
        return title;
    }

    public UUID getId() {
        return Id;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isHasParent() {
        return hasParent;
    }
}
