package org.vicky.vspe.platform.features.CharmsAndTrinkets;

import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.platform.PlatformItem;

import java.util.*;

public interface PlatformTrinket extends Identifiable {
    PlatformItem getIcon();
    String getName();
    String getDescription();
    TrinketAbility getTrinketCategory();
    PlatformTrinketSlot getTrinketSlot();
    List<TrinketAbilityType> getTrinketGroups();

    default String getFormattedName() {
        return getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    void equipPlayer(PlatformPlayer player);
    void removePlayer(PlatformPlayer player);
    boolean isPlayerUsing(PlatformPlayer player);

    void applyTrinketAbility(PlatformPlayer player);
    void performTrinketAbility(PlatformPlayer player);
    void removeTrinketAbility(PlatformPlayer player);

    // You can leave these optional
    void enableTrinket();
    void disableTrinket();

    /**
     * Base class for trinket event listeners that are shared for all users of this trinket.
     * The listener has a reference to its parent trinket so it can check the active players list.
     */
    public abstract class PlatformTrinketEventListener {
        protected final PlatformTrinket trinket;

        public PlatformTrinketEventListener(PlatformTrinket trinket) {
            this.trinket = trinket;
        }

        public void onPlayerJoin(PlatformPlayer player) {
            if (!trinket.isPlayerUsing(player)) {
                trinket.equipPlayer(player);
            }
        }

        public void onPlayerLeave(PlatformPlayer player) {
            if (trinket.isPlayerUsing(player)) {
                trinket.removeTrinketAbility(player);
            }
        }

        public void onTrinketEquip(PlatformPlayer player, PlatformTrinket trinketEquipped) {
            if (!trinket.equals(trinketEquipped)) return;
            if (!trinket.isPlayerUsing(player)) {
                trinket.equipPlayer(player);
            }
        }

        public void onTrinketUnequip(PlatformPlayer player, PlatformTrinket trinketUnequipped) {
            if (!trinket.equals(trinketUnequipped)) return;
            if (trinket.isPlayerUsing(player)) {
                trinket.removePlayer(player);
            }
        }

        public abstract void register();   // platform-specific
        public abstract void unregister(); // platform-specific
    }
}
