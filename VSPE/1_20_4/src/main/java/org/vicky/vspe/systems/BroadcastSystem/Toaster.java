package org.vicky.vspe.systems.BroadcastSystem;

import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay.AdvancementFrame;
import eu.endercentral.crazy_advancements.advancement.ToastNotification;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Toaster {
    public static void sendAdvancementToast(ItemStack icon, Player player, String description, AdvancementFrame toastType) {
        ToastNotification notification = new ToastNotification(icon, description, toastType);
        notification.send(player);
    }
}
