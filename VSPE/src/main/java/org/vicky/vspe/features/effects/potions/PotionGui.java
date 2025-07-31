package org.vicky.vspe.features.effects.potions;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.guiparent.BaseGui;

import static org.vicky.vspe.utilities.global.GlobalResources.potionGuiListener;

public class PotionGui extends BaseGui {
    public PotionGui(JavaPlugin plugin) {
        super(plugin, potionGuiListener);
    }
    @Override
    public void showGui(Player player) {

    }
}
