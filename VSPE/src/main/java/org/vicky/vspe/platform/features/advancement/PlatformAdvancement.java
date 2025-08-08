package org.vicky.vspe.platform.features.advancement;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlayer;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.platform.PlatformItem;

import java.util.*;

public interface PlatformAdvancement extends Identifiable {
    String getId();
    String getTitle();
    Component getDescription(); // Abstract text component
    List<PlatformItem> getIcon();       // Abstracted from Bukkit ItemStack
    boolean isEligible(PlatformPlayer player);
    void grant(PlatformPlayer player);

    boolean isHasParent();
}
