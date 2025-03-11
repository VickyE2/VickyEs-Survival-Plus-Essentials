package org.vicky.vspe.features.AdvancementPlus;

import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay.AdvancementFrame;
import eu.endercentral.crazy_advancements.advancement.AdvancementFlag;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.utilities.UUIDGenerator;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementNotExists;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.NullAdvancementUser;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement;
import org.vicky.vspe.utilities.Hibernate.api.AdvanceablePlayerService;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvancementDAO;
import org.vicky.vspe.utilities.global.Events.PlayerReceivedAdvancement;

import java.util.*;

import static org.vicky.vspe.utilities.global.GlobalResources.advancementManager;

public abstract class BaseAdvancement implements Identifiable {
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.FEATURE, "ADVANCEMENT-BASE");
    protected String description;
    protected String title;
    protected String namespace;
    protected NamespacedKey unInstancedNamespace;
    protected UUID Id;
    protected AdvancementType advancementType;
    protected ToastType advancementTT;
    protected AdvancementFrame toastType;
    protected List<BaseDimension> eligibleDimensions;
    protected ItemStack icon;
    protected boolean hasParent;
    protected final Map<String, AllowedOverride<?>> overrides = new HashMap<>();
    protected eu.endercentral.crazy_advancements.advancement.Advancement instance;
    protected BaseAdvancement parent;
    protected AdvancementVisibility visibility;
    protected ArrangementType arrangementType;

    public BaseAdvancement(
            @NotNull ItemStack icon,
            @NotNull NamespacedKey namespace,
            @NotNull AdvancementFrame toastFrame,
            @Nullable String description,
            @NotNull String title,
            @Nullable List<BaseDimension> permittedDimensions,
            @NotNull AdvancementType advancementType,
            @NotNull AdvancementVisibility visibility,
            @NotNull ToastType aTT,
            @NotNull ArrangementType arrangementType
    ) {
        this.description = description;
        this.advancementType = advancementType;
        this.title = title;
        this.advancementTT = aTT;
        this.namespace = namespace.asString();
        this.unInstancedNamespace = namespace;
        this.toastType = toastFrame;
        this.icon = icon;
        this.arrangementType = arrangementType;
        this.hasParent = false;
        this.Id = UUIDGenerator.generateUUIDFromString(this.getFormattedTitle());
        this.parent = null;
        this.visibility = visibility;
        this.eligibleDimensions = permittedDimensions != null && !permittedDimensions.isEmpty() ? permittedDimensions : new ArrayList<>();
        advancementManager.LOADED_ADVANCEMENTS.add(this);
    }

    public BaseAdvancement(
            @NotNull ItemStack icon,
            @NotNull AdvancementFrame toastFrame,
            @Nullable String description,
            @NotNull String title,
            @NotNull NamespacedKey namespace,
            @Nullable List<BaseDimension> permittedDimensions,
            @NotNull AdvancementType advancementType,
            @NotNull AdvancementVisibility visibility,
            @NotNull ToastType aTT,
            @NotNull Class<? extends BaseAdvancement> parentClass
    ) {
        this.description = description;
        this.advancementType = advancementType;
        this.title = title;
        this.advancementTT = aTT;
        this.toastType = toastFrame;
        this.icon = icon;
        this.Id = UUIDGenerator.generateUUIDFromString(this.getFormattedTitle());
        this.hasParent = true;
        this.parent = advancementManager.getAdvancement(parentClass);
        this.namespace = namespace.asString();
        this.unInstancedNamespace = namespace;
        this.visibility = visibility;
        this.eligibleDimensions = permittedDimensions != null && !permittedDimensions.isEmpty() ? permittedDimensions : new ArrayList<>();
        advancementManager.LOADED_ADVANCEMENTS.add(this);
    }

    public eu.endercentral.crazy_advancements.advancement.Advancement getInstance() {
        eu.endercentral.crazy_advancements.advancement.Advancement instancedAdvancement = this.getInstancedAdvancement(this.visibility);

        if (this.advancementCriteria() != null) {
            instancedAdvancement.setCriteria(this.advancementCriteria());
        }

        if (this.advancementReward() != null) {
            instancedAdvancement.setReward(this.advancementReward());
        }

        this.instance = instancedAdvancement;
        return instancedAdvancement;
    }

    @NotNull
    private eu.endercentral.crazy_advancements.advancement.Advancement getInstancedAdvancement(
            AdvancementVisibility visibility) {

        AdvancementDisplay display = new AdvancementDisplay(this.icon, this.title, this.description, this.toastType, visibility);
        ArrangementType arrangementType = this.arrangementType;

        if (this.parent != null) {
            float parentX = this.parent.getInstance().getDisplay().getX();
            float parentY = this.parent.getInstance().getDisplay().getY();
            int index = this.parent.getInstance().getChildren().size(); // Child index

            if (arrangementType == ArrangementType.TREE) {
                int depth = index / 4; // Every 4 children, move further right
                int position = index % 4; // Spread within each depth level

                display.setX(parentX + 1.5F + depth * 0.5F); // Push right per depth
                display.setY(parentY + position * 1.2F - 1.8F); // Stagger children
            } else if (arrangementType == ArrangementType.CIRCULAR) {
                // Circular arrangement logic
                double angleStep = Math.PI / 5; // Adjust spacing (π/5 = 10 divisions in a semicircle)
                int radiusStep = index / 10 + 1; // Increase radius every 10 children
                double angle = (index % 10) * angleStep; // Angle in radians

                float offsetX = (float) (radiusStep * Math.cos(angle) * 2.0);
                float offsetY = (float) (radiusStep * Math.sin(angle) * 2.0);

                display.setX(parentX + offsetX);
                display.setY(parentY + offsetY);
            }
        } else {
            // Root advancement
            String defaultBackground = "textures/gui/advancements/backgrounds/" + this.getFormattedTitle() + ".png";
            String overriddenBackground = (String) overrides.getOrDefault("background_texture", new AllowedPath(defaultBackground)).getValue();
            display.setBackgroundTexture(overriddenBackground);

            display.setX(2.0F);
            display.setY(0.0F);
        }

        return this.getAdvancement(display);
    }

    @NotNull
    private eu.endercentral.crazy_advancements.advancement.Advancement getAdvancement(AdvancementDisplay display) {
        // Override advancement flag if available
        AdvancementFlag flag = (AdvancementFlag) overrides.getOrDefault("advancement_flag", new AllowedAdvancementFlag<>(AdvancementFlag.SHOW_TOAST)).getValue();

        eu.endercentral.crazy_advancements.advancement.Advancement instancedAdvancement;
        if (!this.hasParent) {
            instancedAdvancement = new eu.endercentral.crazy_advancements.advancement.Advancement(
                    new NameKey(this.unInstancedNamespace.getNamespace(), this.unInstancedNamespace.getKey()),
                    display,
                    flag);
        } else {
            instancedAdvancement = new eu.endercentral.crazy_advancements.advancement.Advancement(
                    this.parent.getInstance(),
                    new NameKey(this.unInstancedNamespace.getNamespace(), this.unInstancedNamespace.getKey()),
                    display,
                    flag);
        }

        return instancedAdvancement;
    }

    public String getFormattedTitle() {
        return this.title.toLowerCase().replace(" ", "_").replace("! @ # $ % ^ & * ( ) - + = { } [ ] : ; ' < , > . ? / ` ~ ", "_");
    }

    public void grantAdvancement(Player player) {
        this.performGrantAdvancement(player);
        PlayerReceivedAdvancement event = new PlayerReceivedAdvancement(player, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    protected boolean canGrantInDimension(Player player) {
        return this.eligibleDimensions.isEmpty() || this.eligibleDimensions.stream().anyMatch(dimension -> dimension.isPlayerInDimension(player));
    }

    protected abstract Criteria advancementCriteria();

    protected abstract AdvancementReward advancementReward();

    protected boolean isPlayerEligible(Player player) {
        try {
            AdvanceablePlayerService service = AdvanceablePlayerService.getInstance();
            Optional<AdvanceablePlayer> oAP = service.getPlayerById(player.getUniqueId());
            if (oAP.isEmpty()) {
                throw new NullAdvancementUser("Failed to get AdvancementPlayer from database", player);
            }
            AdvanceablePlayer contextPlayer = oAP.get();
            AdvancementDAO advancementService = new AdvancementDAO();
            Optional<Advancement> contextAdvancement = advancementService.findByName(this.title);
            if (contextAdvancement.isPresent()) {
                return !contextPlayer.getAccomplishedAdvancements().contains(contextAdvancement.get());
            } else {
                throw new AdvancementNotExists("Failed to check eligibility of player for advancement because the advancement does not exist", this);
            }
        } catch (Exception var5) {
            throw new RuntimeException("Error encountered with SQL manager: " + var5);
        }
    }

    protected abstract void performGrantAdvancement(OfflinePlayer var1);

    public String getDescription() {
        return this.description;
    }

    public <T> void addOverride(String key, AllowedOverride<T> override) {
        this.overrides.put(key, override);
    }

    public AdvancementType getAdvancementType() {
        return this.advancementType;
    }

    public ToastType getAdvancementTT() {
        return this.advancementTT;
    }

    public List<BaseDimension> getEligibleDimensions() {
        return this.eligibleDimensions;
    }

    public String getTitle() {
        return this.title;
    }

    public UUID getId() {
        return this.Id;
    }

    public String getIdentifier() {
        return this.namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public boolean isHasParent() {
        return this.hasParent;
    }

    public void enableAdvancement() {
        logger.printBukkit("Enable advancement " + this.getTitle());
        advancementManager.UNLOADED_ADVANCEMENTS.remove(this);
        advancementManager.LOADED_ADVANCEMENTS.add(this);
        advancementManager.ADVANCEMENT_MANAGER.addAdvancement(this.getInstance());
    }

    public void disableAdvancement() {
        logger.printBukkit("Disable advancement " + this.getTitle());
        advancementManager.UNLOADED_ADVANCEMENTS.add(this);
        advancementManager.LOADED_ADVANCEMENTS.remove(this);
        advancementManager.ADVANCEMENT_MANAGER.removeAdvancement(this.getInstance());
    }

    public void deleteAdvancement() {
        logger.printBukkit("Delete advancement " + this.getTitle());
        advancementManager.LOADED_ADVANCEMENTS.remove(this);
        advancementManager.ADVANCEMENT_MANAGER.removeAdvancement(this.getInstance());
    }

    public enum ArrangementType {
        TREE,
        CIRCULAR
    }

    public interface AllowedOverride<T> {
        T getValue();
    }
    public record AllowedAdvancementFlag<E extends AdvancementFlag>(E value)
            implements AllowedOverride<AdvancementFlag> {
        @Override
        public AdvancementFlag getValue() {
            return value;
        }
    }
    public record AllowedPath(String value)
            implements AllowedOverride<String> {
        @Override
        public String getValue() {
            return value;
        }
    }

}
