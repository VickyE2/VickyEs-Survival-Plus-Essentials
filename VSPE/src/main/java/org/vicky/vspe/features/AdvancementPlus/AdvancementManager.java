package org.vicky.vspe.features.AdvancementPlus;

import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.packet.AdvancementsPacket;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.features.AdvancementPlus.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.systems.dimension.BaseDimension;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvancementDAO;
import org.vicky.vspe.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.utilities.Manager.IdentifiableManager;
import org.vicky.vspe.utilities.Manager.ManagerRegistry;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.vicky.vspe.utilities.global.GlobalResources.configManager;

public class AdvancementManager implements IdentifiableManager {
    public static final ArrayList<Advancement> SORTED_ADVANCEMENTS = new ArrayList<>();
    public final Set<BaseAdvancement> LOADED_ADVANCEMENTS = new TreeSet<>(
            Comparator.comparing(BaseAdvancement::isHasParent)
                    .reversed()
                    .thenComparing(BaseAdvancement::getIdentifier)
    );

    public final Set<BaseAdvancement> UNLOADED_ADVANCEMENTS = new TreeSet<>(
            Comparator.comparing(BaseAdvancement::isHasParent)
                    .reversed()
                    .thenComparing(BaseAdvancement::getIdentifier)
    );
    public final eu.endercentral.crazy_advancements.manager.AdvancementManager ADVANCEMENT_MANAGER = new eu.endercentral.crazy_advancements.manager.AdvancementManager(
            new NameKey("vspe", "manager")
    );
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.FEATURE, "ADVANCEMENT");
    final AdvancementDAO advancementService = new AdvancementDAO();
    final JavaPlugin plugin;

    public AdvancementManager(JavaPlugin plugin) {
        ManagerRegistry.register(this);
        this.plugin = plugin;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void processAdvancements() throws AdvancementProcessingFailureException {
        try {
            logger.print("Starting advancement Processing...", ContextLogger.LogType.PENDING);
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

                    logger.print(ANSIColor.colorize("    Advancement permitted dimensions: purple[-" + (dimensions.isEmpty() ? "Basically all" : dimensions) + "-]"));
                }

                Optional<org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement> optionalAdvancement =
                        advancementService.findByName(advancement.getTitle());
                if (optionalAdvancement.isEmpty()) {
                    org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement databaseAdvancement = new org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement();
                    databaseAdvancement.setName(advancement.getTitle());
                    advancementService.create(databaseAdvancement);
                }
                SORTED_ADVANCEMENTS.add(advancement.getInstance());
            }

            this.ADVANCEMENT_MANAGER.addAdvancement(SORTED_ADVANCEMENTS.toArray(Advancement[]::new));
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

    public boolean grantAdvancemet(Class<? extends BaseAdvancement> advancementClass, Player player) {
        Optional<BaseAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getClass().equals(advancementClass))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            BaseAdvancement contextAdvancement = contextAdvancementOptional.get();
            contextAdvancement.grantAdvancement(player);
            this.ADVANCEMENT_MANAGER.grantAdvancement(player, contextAdvancement.getInstance());
            return true;
        } else {
            logger.print("Advancement of type " + advancementClass.getSimpleName() + " not found in LOADED_ADVANCEMENTS.", true);
            return false;
        }
    }

    public void grantAdvancemet(String advancementId, Player player) {
        Optional<BaseAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getFormattedTitle().equals(advancementId))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            BaseAdvancement contextAdvancement = contextAdvancementOptional.get();
            this.ADVANCEMENT_MANAGER.grantAdvancement(player, contextAdvancement.getInstance());
        } else {
            logger.print("Advancement of Id " + advancementId + " not found in LOADED_ADVANCEMENTS.", true);
        }
    }

    public void addAdvancement(BaseAdvancement advancement) {
        LOADED_ADVANCEMENTS.add(advancement);
        SORTED_ADVANCEMENTS.add(advancement.getInstance());
        ADVANCEMENT_MANAGER.addAdvancement(advancement.getInstance());
        Optional<org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement> optionalAdvancement =
                advancementService.findByName(advancement.getTitle());
        if (optionalAdvancement.isEmpty()) {
            org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement databaseAdvancement = new org.vicky.vspe.utilities.Hibernate.DBTemplates.Advancement();
            databaseAdvancement.setName(advancement.getTitle());
            advancementService.create(databaseAdvancement);
        }
        logger.print(ANSIColor.colorize("purple[Added advancement: ]") + advancement.getTitle());
        saveAndUnloadManagerProgress();
    }

    public void sendAdvancementPackets(Player player) {
        AdvancementsPacket packet = new AdvancementsPacket(player, false, Arrays.stream(SORTED_ADVANCEMENTS.toArray(Advancement[]::new)).toList(), new ArrayList<>());
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