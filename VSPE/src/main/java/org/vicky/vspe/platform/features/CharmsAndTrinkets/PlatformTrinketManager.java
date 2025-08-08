package org.vicky.vspe.platform.features.CharmsAndTrinkets;

import org.vicky.platform.PlatformPlayer;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions.TrinketProcessingFailureException;
import org.vicky.vspe.platform.utilities.Manager.IdentifiableManager;

import java.util.*;

/**
 * CnTManager is responsible for managing all loaded trinkets.
 * <p>
 * It scans specified packages (both from the classpath and from JARs) for classes that extend {@link PlatformTrinket},
 * instantiates them via reflection, and adds them to the LOADED_TRINKETS list.
 * It also provides helper methods for managing trinket listeners and implements the IdentifiableManager interface.
 * </p>
 */
public interface PlatformTrinketManager<E> extends IdentifiableManager {

    List<PlatformTrinket> getLoadedTrinkets();
    List<PlatformTrinket> getUnLoadedTrinkets();

    /**
     * Removes the given trinket from the specified listener.
     * If no trinkets remain for that listener, calls {@link PlatformTrinket.PlatformTrinketEventListener#unregister()}
     * and removes the listener from the map.
     *
     * @param listener the trinket event listener
     * @param trinket  the trinket to remove
     */
    public void removeTrinketFromListener(PlatformTrinket.PlatformTrinketEventListener listener, PlatformTrinket trinket);

    /**
     * Adds the given trinket to the specified listener.
     * If no trinkets remain for that listener, calls {@link PlatformTrinket.PlatformTrinketEventListener#unregister()}}
     * and removes the listener from the map.
     *
     * @param listener the trinket event listener
     * @param trinket  the trinket to remove
     */
    public void addTrinketFromListener(PlatformTrinket.PlatformTrinketEventListener listener, PlatformTrinket trinket);

    /**
     * Scans the specified packages for classes extending {@link PlatformTrinket},
     * instantiates each (using a no-argument constructor), and adds the instance
     * to {@code LOADED_TRINKETS}.
     *
     * @throws TrinketProcessingFailureException if any error occurs during processing
     */
    public void processTrinkets() throws TrinketProcessingFailureException;

    /**
     * Retrieves a trinket by its identifier.
     *
     * @param Id the identifier of the trinket to search for
     * @return an Optional containing the trinket if found, otherwise empty
     */
    public Optional<PlatformTrinket> getTrinketById(String Id);

    public void givePlayerTrinket(PlatformPlayer sender, String trinketId);

    public void onPlayerJoinServerTrinketCheck(E event);
}