package org.vicky.vspe.features.CharmsAndTrinkets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.vicky.bukkitplatform.useables.BukkitItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Identifiable;
import org.vicky.utilities.JarClassScanner;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinket;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinketManager;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions.NullManagerTrinket;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions.TrinketProcessingFailureException;
import org.vicky.vspe.platform.utilities.Manager.EntityNotFoundException;
import org.vicky.vspe.platform.utilities.Manager.ManagerRegistry;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.CnTPlayer;
import org.vicky.vspe.utilities.Hibernate.dao_s.CnTPlayerDAO;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.vicky.vspe.utilities.global.GlobalResources.classLoader;

/**
 * CnTManager is responsible for managing all loaded trinkets.
 * <p>
 * It scans specified packages (both from the classpath and from JARs) for classes that extend {@link BaseTrinket},
 * instantiates them via reflection, and adds them to the LOADED_TRINKETS list.
 * It also provides helper methods for managing trinket listeners and implements the IdentifiableManager interface.
 * </p>
 */
public class CnTManager implements PlatformTrinketManager<BaseTrinket>, Listener {
    // A list of currently loaded trinket instances.
    public final List<PlatformTrinket> LOADED_TRINKETS = new ArrayList<>();
    // A list of trinket instances that have been unloaded (if applicable).
    public final List<PlatformTrinket> UNLOADED_TRINKETS = new ArrayList<>();

    // Map of package names to a list of package strings (for scanning trinket classes from jars)
    public final static Map<String, List<String>> TRINKET_PACKAGES = new HashMap<>();
    // List of package names on the classpath to scan for trinket classes.
    private final List<String> MY_TRINKET_PACKAGES = new ArrayList<>();
    // A map for holding listener instances and the trinkets registered to them.
    public static final Map<BaseTrinket.TrinketEvent, List<BaseTrinket>> LOADED_TRINKET_LISTENERS = new HashMap<>();
    public static JavaPlugin plugin;
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.FEATURE, "TRINKETS");

    /**
     * Constructs the CnTManager and registers it with the ManagerRegistry.
     * It also initializes the MY_TRINKET_PACKAGES list.
     */
    public CnTManager(JavaPlugin plugin) {
        ManagerRegistry.register(this);
        CnTManager.plugin =  plugin;
        MY_TRINKET_PACKAGES.add("org.vicky.vspe.features.CharmsAndTrinkets.Trinkets");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Removes the given trinket from the specified listener.
     * If no trinkets remain for that listener, calls {@link BaseTrinket.TrinketEvent#unloadSelf()}
     * and removes the listener from the map.
     *
     * @param listener the trinket event listener
     * @param trinket  the trinket to remove
     */
    public void removeTrinketFromListener(BaseTrinket.TrinketEvent listener, BaseTrinket trinket) {
        List<BaseTrinket> trinkets = LOADED_TRINKET_LISTENERS.get(listener);
        if (trinkets != null) {
            trinkets.remove(trinket);
            if (trinkets.isEmpty()) {
                listener.unloadSelf();
                LOADED_TRINKET_LISTENERS.remove(listener);
            }
        }
    }

    /**
     * Adds the given trinket to the specified listener.
     * If no trinkets remain for that listener, calls {@link BaseTrinket.TrinketEvent#unloadSelf()}
     * and removes the listener from the map.
     *
     * @param listener the trinket event listener
     * @param trinket  the trinket to remove
     */
    public void addTrinketFromListener(BaseTrinket.TrinketEvent listener, BaseTrinket trinket) {
        if (!LOADED_TRINKET_LISTENERS.containsKey(listener)) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        LOADED_TRINKET_LISTENERS.computeIfAbsent(listener, k -> new ArrayList<>());
        List<BaseTrinket> trinkets = LOADED_TRINKET_LISTENERS.get(listener);
        if (trinkets != null) {
            trinkets.add(trinket);
        }
    }

    @Override
    public List<PlatformTrinket> getLoadedTrinkets() {
        return LOADED_TRINKETS;
    }

    @Override
    public List<PlatformTrinket> getUnLoadedTrinkets() {
        return UNLOADED_TRINKETS;
    }

    @Override
    public void removeTrinketFromListener(PlatformTrinket.PlatformTrinketEventListener platformTrinketEventListener, PlatformTrinket platformTrinket) {

    }

    @Override
    public void addTrinketFromListener(PlatformTrinket.PlatformTrinketEventListener platformTrinketEventListener, PlatformTrinket platformTrinket) {

    }

    /**
     * Scans the specified packages for classes extending {@link BaseTrinket},
     * instantiates each (using a no-argument constructor), and adds the instance
     * to {@code LOADED_TRINKETS}.
     *
     * @throws TrinketProcessingFailureException if any error occurs during processing
     */
    public void processTrinkets() throws TrinketProcessingFailureException {
        logger.print("Processing Trinket Jars and Classes...", ContextLogger.LogType.PENDING);
        for (String pkg : MY_TRINKET_PACKAGES) {
            Reflections reflections = new Reflections(pkg, new SubTypesScanner(false));
            Set<Class<? extends BaseTrinket>> trinketClasses = reflections.getSubTypesOf(BaseTrinket.class);
            for (Class<? extends BaseTrinket> clazz : trinketClasses) {
                try {
                    BaseTrinket trinket = clazz.getDeclaredConstructor().newInstance();
                    LOADED_TRINKETS.add(trinket);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }

        // Use JarClassScanner to scan additional packages specified in TRINKET_PACKAGES.
        JarClassScanner scanner = new JarClassScanner();
        final List<Class<?>> trinkets = new ArrayList<>();
        for (Map.Entry<String, List<String>> packag3 : TRINKET_PACKAGES.entrySet()) {
            for (String packageName : packag3.getValue()) {
                try {
                    trinkets.addAll(scanner.getClassesFromJar(packag3.getKey(), packageName, BaseTrinket.class));
                } catch (Exception e) {
                    throw new TrinketProcessingFailureException("Failure on trinket Processing", e);
                }
            }
        }
        classLoader.getLoaders().addAll(scanner.getClassLoaders());

        if (trinkets.isEmpty()) {
            logger.print("No custom trinkets were found D:", ContextLogger.LogType.AMBIENCE);
        }
        for (Class<?> clazz : trinkets) {
            BaseTrinket trinket;
            try {
                trinket = (BaseTrinket) clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                logger.print("An error occurred when creating trinket instance: ", ContextLogger.LogType.ERROR);
                throw new RuntimeException(e);
            }
            LOADED_TRINKETS.add(trinket);
        }
    }

    /**
     * Retrieves a trinket by its identifier.
     *
     * @param Id the identifier of the trinket to search for
     * @return an Optional containing the trinket if found, otherwise empty
     */
    public Optional<PlatformTrinket> getTrinketById(String Id) {
        return LOADED_TRINKETS.stream().filter(t -> t.getIdentifier().equals(Id)).findAny();
    }

    @Override
    public void givePlayerTrinket(PlatformPlayer platformPlayer, String s) {

    }

    @Override
    public void onPlayerJoinServerTrinketCheck(BaseTrinket baseTrinket) {

    }

    @Override
    public String getManagerId() {
        return "CnTManager";
    }

    @Override
    public void removeEntity(String namespace) throws EntityNotFoundException {
        // Implement removal logic if needed.
    }

    @Override
    public void disableEntity(String namespace) throws EntityNotFoundException {
        // Implement disable logic if needed.
    }

    @Override
    public void enableEntity(String namespace) throws EntityNotFoundException {
        // Implement enable logic if needed.
    }

    @Override
    public List<Identifiable> getRegisteredEntities() {
        return new ArrayList<>(LOADED_TRINKETS);
    }

    @Override
    public List<Identifiable> getUnregisteredEntities() {
        return new ArrayList<>(UNLOADED_TRINKETS);
    }

    public void givePlayerTrinket(Player sender, String trinket) {

        Optional<PlatformTrinket> oBT = LOADED_TRINKETS.stream().filter(t -> t.getFormattedName().equals(trinket)).findFirst();
        if (oBT.isEmpty()) {
            sender.sendMessage(Component.text("That trinket does not exist :0", TextColor.color(0.6f, 0f, 0f), TextDecoration.ITALIC, TextDecoration.BOLD));
            return;
        }
        BaseTrinket baseTrinket = (BaseTrinket) oBT.get();
        sender.getInventory().addItem(((BukkitItem) baseTrinket.getIcon()).getStack());
    }

    @EventHandler
    public void onPlayerJoinServerTrinketCheck(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(CnTManager.getPlugin(), () -> {
            logger.print("Checking equipped trinkets for " + player.getName());

            Optional<CnTPlayer> optionalCnTPlayer = new CnTPlayerDAO().findById(playerUUID);
            if (optionalCnTPlayer.isEmpty()) {
                logger.print("Database entry missing for player " + player.getName(), ContextLogger.LogType.ERROR);
                return;
            }

            CnTPlayer cnTPlayer = optionalCnTPlayer.get();
            List<UUID> trinketUUID;
            try {
                trinketUUID = cnTPlayer.getTrinketUUIDs();
            } catch (NullManagerTrinket e) {
                throw new RuntimeException(e);
            }

            for (UUID uuid : trinketUUID) {
                Optional<PlatformTrinket> oT = this.getTrinketById(uuid.toString());
                if (oT.isEmpty()) {
                    try {
                        throw new NullManagerTrinket("Trinket was found on player but could not be found on manager");
                    } catch (NullManagerTrinket e) {
                        throw new RuntimeException(e);
                    }
                }
                BaseTrinket trinket = (BaseTrinket) oT.get();
                trinket.addPlayer(player);
            }

        }, 40L);
    }

}