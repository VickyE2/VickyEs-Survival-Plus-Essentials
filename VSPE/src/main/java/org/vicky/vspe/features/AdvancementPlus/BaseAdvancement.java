package org.vicky.vspe.features.AdvancementPlus;

import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementFlag;
import eu.endercentral.crazy_advancements.advancement.AdvancementReward;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay.AdvancementFrame;
import eu.endercentral.crazy_advancements.advancement.criteria.Criteria;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementNotExists;
import org.vicky.vspe.systems.BroadcastSystem.ToastType;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.utilities.UUIDGenerator;
import org.vicky.vspe.utilities.DatabaseManager.templates.Advancement;
import org.vicky.vspe.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.utilities.global.GlobalResources;
import org.vicky.vspe.utilities.global.Events.PlayerReceivedAdvancement;

public abstract class BaseAdvancement {
   protected String description;
   protected String title;
   protected String namespace;
   protected UUID Id;
   protected AdvancementType advancementType;
   protected ToastType advancementTT;
   protected AdvancementFrame toastType;
   protected List<BaseDimension> eligibleDimensions;
   protected ItemStack icon;
   protected boolean hasParent;
   protected BaseAdvancement parent;
   protected AdvancementVisibility visibility;

   public BaseAdvancement(
      ItemStack icon,
      String namespace,
      AdvancementFrame toastFrame,
      String description,
      String title,
      List<BaseDimension> permittedDimensions,
      AdvancementType advancementType,
      AdvancementVisibility visibility,
      ToastType aTT
   ) {
      this.description = description;
      this.advancementType = advancementType;
      this.title = title;
      this.advancementTT = aTT;
      this.namespace = namespace;
      this.toastType = toastFrame;
      this.icon = icon;
      this.hasParent = false;
      this.Id = UUIDGenerator.generateUUIDFromString(this.getFormattedTitle());
      this.parent = null;
      this.visibility = visibility;
      this.eligibleDimensions = (List<BaseDimension>)(permittedDimensions != null && !permittedDimensions.isEmpty() ? permittedDimensions : new ArrayList<>());
      GlobalResources.advancementManager.LOADED_ADVANCEMENTS.add(this);
   }

   public BaseAdvancement(
      ItemStack icon,
      AdvancementFrame toastFrame,
      String description,
      String title,
      List<BaseDimension> permittedDimensions,
      AdvancementType advancementType,
      AdvancementVisibility visibility,
      ToastType aTT,
      Class<? extends BaseAdvancement> parentClass
   ) {
      this.description = description;
      this.advancementType = advancementType;
      this.title = title;
      this.advancementTT = aTT;
      this.toastType = toastFrame;
      this.icon = icon;
      this.Id = UUIDGenerator.generateUUIDFromString(this.getFormattedTitle());
      this.hasParent = true;
      this.parent = GlobalResources.advancementManager.getAdvancement(parentClass);
      this.namespace = this.parent.namespace;
      this.visibility = visibility;
      this.eligibleDimensions = (List<BaseDimension>)(permittedDimensions != null && !permittedDimensions.isEmpty() ? permittedDimensions : new ArrayList<>());
      GlobalResources.advancementManager.LOADED_ADVANCEMENTS.add(this);
   }

   public eu.endercentral.crazy_advancements.advancement.Advancement getInstance() {
      eu.endercentral.crazy_advancements.advancement.Advancement instancedAdvancement = this.getInstancedAdvancement(this.visibility);
      if (this.advancementCriteria() != null) {
         instancedAdvancement.setCriteria(this.advancementCriteria());
      }

      if (this.advancementReward() != null) {
         instancedAdvancement.setReward(this.advancementReward());
      }

      return instancedAdvancement;
   }

   @NotNull
   private eu.endercentral.crazy_advancements.advancement.Advancement getInstancedAdvancement(AdvancementVisibility visibility) {
      AdvancementDisplay display = new AdvancementDisplay(this.icon, this.title, this.description, this.toastType, visibility);
      if (this.parent != null) {
         display.setX(this.parent.getInstance().getDisplay().getX() + 1.0F);
         if (this.parent.getInstance().getChildren().size() <= 4) {
            display.setY((float)(this.parent.getInstance().getChildren().size() - 1));
         } else if (this.parent.getInstance().getChildren().size() <= 10) {
            display.setY((float)(this.parent.getInstance().getChildren().size() - 5));
         } else {
            display.setY((float)(this.parent.getInstance().getChildren().size() - 10));
         }
      } else {
         display.setBackgroundTexture("textures/gui/advancements/backgrounds/" + this.getFormattedTitle() + ".png");
         display.setX(2.0F);
         display.setY(0.0F);
      }

      return this.getAdvancement(display);
   }

   @NotNull
   private eu.endercentral.crazy_advancements.advancement.Advancement getAdvancement(AdvancementDisplay display) {
      eu.endercentral.crazy_advancements.advancement.Advancement instancedAdvancement;
      if (!this.hasParent) {
         instancedAdvancement = new eu.endercentral.crazy_advancements.advancement.Advancement(
            new NameKey(this.namespace, this.getFormattedTitle()), display, new AdvancementFlag[]{AdvancementFlag.SHOW_TOAST}
         );
      } else {
         instancedAdvancement = new eu.endercentral.crazy_advancements.advancement.Advancement(
            this.parent.getInstance(), new NameKey(this.namespace, this.getFormattedTitle()), display, new AdvancementFlag[]{AdvancementFlag.SHOW_TOAST}
         );
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
      return this.eligibleDimensions.isEmpty() ? true : this.eligibleDimensions.stream().anyMatch(dimension -> dimension.isPlayerInDimension(player));
   }

   protected abstract Criteria advancementCriteria();

   protected abstract AdvancementReward advancementReward();

   protected boolean isPlayerEligible(Player player) {
      try {
         DatabasePlayer contextPlayer = GlobalResources.databaseManager.getEntityById(DatabasePlayer.class, player.getUniqueId());
         org.vicky.vspe.utilities.DatabaseManager.templates.AdvancementManager advancementManager = GlobalResources.databaseManager
            .getEntityById(org.vicky.vspe.utilities.DatabaseManager.templates.AdvancementManager.class, 1);
         Optional<Advancement> contextAdvancement = advancementManager.getAdvancements()
            .stream()
            .filter(advancement -> advancement.getId().equals(this.getId()))
            .findAny();
         if (contextAdvancement.isPresent()) {
            return !contextPlayer.getAccomplishedAdvancements().contains(contextAdvancement.get());
         } else {
            throw new AdvancementNotExists("Failed to check eligibility of player for advancement", this);
         }
      } catch (Exception var5) {
         throw new RuntimeException("Error encountered with SQL manager: " + var5);
      }
   }

   protected abstract void performGrantAdvancement(OfflinePlayer var1);

   public String getDescription() {
      return this.description;
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

   public String getNamespace() {
      return this.namespace;
   }

   public boolean isHasParent() {
      return this.hasParent;
   }
}
