package org.vicky.vspe.platform.features.advancement;

import net.kyori.adventure.text.Component;
import org.vicky.platform.PlatformItem;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.Identifiable;

import java.util.UUID;

public interface PlatformAdvancement extends Identifiable {
    UUID getId();
    String getTitle();
    Component getDescription(); // Abstract text component

    PlatformItem getIcon();       // Abstracted from Bukkit ItemStack
    boolean isEligible(PlatformPlayer player);
    void grant(PlatformPlayer player);

    boolean isHasParent();
}
