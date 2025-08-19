package org.vicky.vspe.features.AdvancementPlus;

import eu.endercentral.crazy_advancements.NameKey;
import eu.endercentral.crazy_advancements.advancement.Advancement;
import eu.endercentral.crazy_advancements.packet.AdvancementsPacket;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.vicky.bukkitplatform.useables.BukkitPlatformPlayer;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.ANSIColor;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.platform.features.advancement.AdvancementStorage;
import org.vicky.vspe.platform.features.advancement.Exceptions.AdvancementProcessingFailureException;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancementManager;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.systems.dimension.BukkitBaseDimension;
import org.vicky.vspe.utilities.Hibernate.dao_s.AdvancementDAO;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.vicky.vspe.utilities.global.GlobalResources.configManager;

public class AdvancementManager implements PlatformAdvancementManager<BukkitAdvancement> {
    public static final ArrayList<Advancement> SORTED_ADVANCEMENTS = new ArrayList<>();
    public final Set<BukkitAdvancement> LOADED_ADVANCEMENTS = new TreeSet<>(
            Comparator.comparing(BukkitAdvancement::isHasParent)
                    .reversed()
                    .thenComparing(BukkitAdvancement::getIdentifier)
    );

    public final Set<BukkitAdvancement> UNLOADED_ADVANCEMENTS = new TreeSet<>(
            Comparator.comparing(BukkitAdvancement::isHasParent)
                    .reversed()
                    .thenComparing(BukkitAdvancement::getIdentifier)
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
            for (Class<? extends BukkitAdvancement> clazz : reflections.getSubTypesOf(BukkitAdvancement.class)) {
                try {
                    Constructor<? extends BukkitAdvancement> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    BukkitAdvancement advancement = constructor.newInstance();
                    logger.print(ANSIColor.colorize("purple[Loaded advancement: ]") + advancement.getTitle());
                } catch (Exception var9) {
                    VSPE.getInstancedLogger().severe("Failed to load advancement: " + clazz.getName());
                    var9.printStackTrace();
                }
            }

            for (BukkitAdvancement advancement : this.LOADED_ADVANCEMENTS) {
                if (configManager.getBooleanValue("Debug")) {
                    logger.print(ANSIColor.colorize("Processing advancement: purple[" + advancement.getNamespace() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement Title: purple[" + advancement.getFormattedTitle() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement toast type: purple[" + advancement.getAdvancementTT() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement type: purple[" + advancement.getAdvancementType() + "]"));
                    logger.print(ANSIColor.colorize("    Advancement description: purple[" + advancement.getDescription() + "]"));

                    StringBuilder dimensions = new StringBuilder();
                    if (advancement.getEligibleDimensions() != null) {
                        for (BukkitBaseDimension dimension : advancement.getEligibleDimensions()) {
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

    @Override
    public boolean grantAdvancement(Class<? extends BukkitAdvancement> aClass, PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            return grantAdvancemet(aClass, player.getBukkitPlayer());
        throw new IllegalArgumentException("Got generic PlatformPlayer instead of BukkitPlatformPlayer");
    }

    @Override
    public void grantAdvancement(String s, PlatformPlayer platformPlayer) {
        if (platformPlayer instanceof BukkitPlatformPlayer player)
            grantAdvancemet(s, player.getBukkitPlayer());
        throw new IllegalArgumentException("Got generic PlatformPlayer instead of BukkitPlatformPlayer");
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

    public boolean grantAdvancemet(Class<? extends BukkitAdvancement> advancementClass, Player player) {
        Optional<BukkitAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getClass().equals(advancementClass))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            BukkitAdvancement contextAdvancement = contextAdvancementOptional.get();
            contextAdvancement.grantAdvancement(player);
            this.ADVANCEMENT_MANAGER.grantAdvancement(player, contextAdvancement.getInstance());
            return true;
        } else {
            logger.print("Advancement of type " + advancementClass.getSimpleName() + " not found in LOADED_ADVANCEMENTS.", true);
            return false;
        }
    }

    public void grantAdvancemet(String advancementId, Player player) {
        Optional<BukkitAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
                .stream()
                .filter(advancement -> advancement.getFormattedTitle().equals(advancementId))
                .findFirst();
        if (contextAdvancementOptional.isPresent()) {
            BukkitAdvancement contextAdvancement = contextAdvancementOptional.get();
            this.ADVANCEMENT_MANAGER.grantAdvancement(player, contextAdvancement.getInstance());
        } else {
            logger.print("Advancement of Id " + advancementId + " not found in LOADED_ADVANCEMENTS.", true);
        }
    }

    public void addAdvancement(BukkitAdvancement advancement) {
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

    public BukkitAdvancement getAdvancement(Class<? extends BukkitAdvancement> advancementClass) {
        Optional<BukkitAdvancement> contextAdvancementOptional = this.LOADED_ADVANCEMENTS
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
    public void setStorage(AdvancementStorage advancementStorage) {

    }

    @Override
    public String getManagerId() {
        return "advancement_manager";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        Optional<BukkitAdvancement> optional = LOADED_ADVANCEMENTS.stream().filter(k -> k.getNamespace().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BukkitAdvancement context = optional.get();
            context.deleteAdvancement();
            saveAndUnloadManagerProgress();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        Optional<BukkitAdvancement> optional = LOADED_ADVANCEMENTS.stream().filter(k -> k.getNamespace().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BukkitAdvancement context = optional.get();
            context.disableAdvancement();
            saveAndUnloadManagerProgress();
        } else {
            throw new EntityNotFoundException("Failed to locate entity with id: " + namespace);
        }

    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        Optional<BukkitAdvancement> optional = UNLOADED_ADVANCEMENTS.stream().filter(k -> k.getNamespace().equals(namespace)).findAny();
        if (optional.isPresent()) {
            BukkitAdvancement context = optional.get();
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