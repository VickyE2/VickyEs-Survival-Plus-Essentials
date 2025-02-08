package org.vicky.vspe.features.AdvancementPlus;

import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.packet.AdvancementsPacket;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.utilities.Manager.EntityNotFoundException;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.utilities.UUIDGenerator;
import org.vicky.vspe.utilities.api.AdvancementManagerService;
import org.vicky.vspe.utilities.api.AdvancementService;
import org.vicky.vspe.utilities.global.GlobalResources;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.vicky.vspe.utilities.global.GlobalResources.configManager;

public class AdvancementManager implements IdentifiableManager {
    public static Advancement[] SORTED_ADVANCEMENTS;
    public final Set<BaseAdvancement> LOADED_ADVANCEMENTS = new TreeSet<>(Comparator.comparing(BaseAdvancement::isHasParent).reversed());
    public final Set<BaseAdvancement> UNLOADED_ADVANCEMENTS = new TreeSet<>(Comparator.comparing(BaseAdvancement::isHasParent).reversed());
    public final eu.endercentral.crazy_advancements.manager.AdvancementManager ADVANCEMENT_MANAGER = new eu.endercentral.crazy_advancements.manager.AdvancementManager(
            new NameKey("vspe", "manager")
    );
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.FEATURE, "ADVANCEMENT");

    public AdvancementManager() {
        ManagerRegistry.register(this);
    }

    public void processAdvancements() throws AdvancementProcessingFailureException {
        try {
            AdvancementManagerService managerService = AdvancementManagerService.getInstance();
            AdvancementService advancementService = AdvancementService.getInstance();

            org.vicky.vspe.utilities.DBTemplates.AdvancementManager databaseAM =
                    managerService.getAdvancementManagerById(UUIDGenerator.generateUUIDFromString("vspe_advancement_manager").toString());

            if (databaseAM == null) {
                databaseAM = new org.vicky.vspe.utilities.DBTemplates.AdvancementManager();
                databaseAM.setId(UUIDGenerator.generateUUIDFromString("vspe_advancement_manager").toString());
                databaseAM = managerService.createAdvancementManager(databaseAM);
            }

            logger.print(ANSIColor.colorize("yellow[Starting advancement Processing...]"));
            int index = 0;
            Reflections reflections = new Reflections("org.vicky.vspe.features.AdvancementPlus.Advancements");

            for (Class<? extends BaseAdvancement> clazz : reflections.getSubTypesOf(BaseAdvancement.class)) {
                try {
                    Constructor<? extends BaseAdvancement> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BaseAdvancement advancement = constructor.newInstance();
                    logger.print(ANSIColor.colorize("purple[Loaded advancement: ]") + advancement.getTitle());
                } catch (Exception var9) {
                    VSPE.getInstancedLogger().severe("Failed to load advancement: " + clazz.getName());
                    var9.printStackTrace();
                }
            }

            SORTED_ADVANCEMENTS = new Advancement[this.LOADED_ADVANCEMENTS.size()];

            for (BaseAdvancement advancement : this.LOADED_ADVANCEMENTS) {
                if (configManager.getBooleanValue("Debug")) {
                    logger.print(ANSIColor.colorize("Processing advancement: purple[" + advancement.getNamespace() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement Title: purple[" + advancement.getFormattedTitle() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement toast type: purple[" + advancement.getAdvancementTT() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement type: purple[" + advancement.getAdvancementType() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement description: purple[" + advancement.getDescription() + "]"));

                    StringBuilder dimensions = new StringBuilder();
                    if (advancement.getEligibleDimensions() != null) {
                        for (BaseDimension dimension : advancement.getEligibleDimensions()) {
                            dimensions.append("    \n").append(dimension.getName());
                        }
                    }

                    logger.print(ANSIColor.colorize("    Advancement permitted dimensions: purple[-" + (dimensions.isEmpty() ? "null [basically all]" : dimensions) + "-]"));
                }

                org.vicky.vspe.utilities.DBTemplates.Advancement databaseAdvancement =
                        advancementService.getAdvancementById(advancement.getIdentifier());

                if (databaseAdvancement == null) {
                    databaseAdvancement = new org.vicky.vspe.utilities.DBTemplates.Advancement();
                    databaseAdvancement.setId(advancement.getId());
                    databaseAdvancement.setName(advancement.getTitle());
                    if (databaseAM.getAdvancements().stream().noneMatch(a -> a.getId().equals(advancement.getId())))
                        databaseAM.addAdvancement(databaseAdvancement);

                    advancementService.createAdvancement(databaseAdvancement);
                } else {
                    databaseAdvancement.setName(advancement.getTitle());
                    advancementService.updateAdvancement(databaseAdvancement);
                }
                SORTED_ADVANCEMENTS[index] = advancement.instance;
                index++;
            }

            managerService.updateAdvancementManager(databaseAM);

            this.ADVANCEMENT_MANAGER.addAdvancement(SORTED_ADVANCEMENTS);
        } catch (Exception e) {
            throw new AdvancementProcessingFailureException("Failure on advancement Processing", e);
        }
    }

    public void saveManagerProgress() {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            this.ADVANCEMENT_MANAGER.saveProgress(player.getUniqueId());
        }
    }

    public void saveAndUnloadManagerProgress() {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            this.ADVANCEMENT_MANAGER.saveProgress(player.getUniqueId());
            this.ADVANCEMENT_MANAGER.unloadProgress(player.getUniqueId());
            this.ADVANCEMENT_MANAGER.loadProgress(player.getUniqueId());
        }
    }

    public void loadManagerProgress() {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            this.ADVANCEMENT_MANAGER.loadProgress(player.getUniqueId());
        }
    }

    public void grantAdvancemet(Class<? extends BaseAdvancement> advancementClass, Player player) {
        Optional<BaseAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getClass().equals(advancementClass))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            BaseAdvancement contextAdvancement = contextAdvancementOptional.get();
            contextAdvancement.grantAdvancement(player);
            this.ADVANCEMENT_MANAGER.grantAdvancement(player, contextAdvancement.instance);
        } else {
            logger.print("Advancement of type " + advancementClass.getSimpleName() + " not found in LOADED_ADVANCEMENTS.", true);
        }
    }

    public void grantAdvancemet(String advancementId, Player player) {
        Optional<BaseAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getFormattedTitle().equals(advancementId))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            BaseAdvancement contextAdvancement = contextAdvancementOptional.get();
            this.ADVANCEMENT_MANAGER.grantAdvancement(player, contextAdvancement.instance);
        } else {
            logger.print("Advancement of Id " + advancementId + " not found in LOADED_ADVANCEMENTS.", true);
        }
    }

    public void sendAdvancementPackets(Player player) {
        AdvancementsPacket packet = new AdvancementsPacket(player, false, Arrays.stream(SORTED_ADVANCEMENTS).toList(), new ArrayList<>());
        packet.send();
    }

    public BaseAdvancement getAdvancement(Class<? extends BaseAdvancement> advancementClass) {
        Optional<BaseAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getClass().equals(advancementClass))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            return contextAdvancementOptional.get();
        } else {
            logger.print("Advancement of type " + advancementClass.getSimpleName() + " not found in LOADED_ADVANCEMENTS.", true);
            return null;
        }
    }

    @Override
    public String getManagerId() {
        return "advancement_manager";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        Optional<BaseAdvancement> optional = LOADED_ADVANCEMENTS.stream().filter(k -> k.getNamespace().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BaseAdvancement context = optional.get();
            context.deleteAdvancement();
            saveAndUnloadManagerProgress();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<BaseAdvancement> optional = LOADED_ADVANCEMENTS.stream().filter(k -> k.getNamespace().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BaseAdvancement context = optional.get();
            context.disableAdvancement();
            saveAndUnloadManagerProgress();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<BaseAdvancement> optional = UNLOADED_ADVANCEMENTS.stream().filter(k -> k.getNamespace().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BaseAdvancement context = optional.get();
            context.enableAdvancement();
            saveAndUnloadManagerProgress();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }
    }

    @Override
    public List<Identifiable> getRegisteredEntities() {
        return new ArrayList<>(LOADED_ADVANCEMENTS);
    }

    @Override
    public List<Identifiable> getUnregisteredEntities() {
        return new ArrayList<>(UNLOADED_ADVANCEMENTS);
    }
}