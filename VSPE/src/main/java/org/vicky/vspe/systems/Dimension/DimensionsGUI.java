package org.vicky.vspe.systems.Dimension;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.guiparent.BaseGui;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.VSPE;

import java.util.HashMap;

import static org.vicky.global.Global.storer;
import static org.vicky.vspe.utilities.global.GlobalResources.*;

public class DimensionsGUI extends BaseGui {

    private final GuiCreator guiManager;
    private final JavaPlugin plugin = VSPE.getPlugin();
    private final ContextLogger logger = new ContextLogger(ContextLogger.ContextType.SYSTEM, "DIMENSIONS-GUI");
    private final DimensionsGUIListener listener;

    public DimensionsGUI() {
        super(VSPE.getPlugin(), dimensionsGUIListener);
        this.listener = dimensionsGUIListener;
        this.guiManager = new GuiCreator(plugin, dimensionsGUIListener);
    }

    @Override
    public void showGui(Player player) {

        boolean hasMore = false;
        HashMap<String, GuiCreator.ItemConfig> Items = new HashMap<>();
        HashMap<String, GuiCreator.ItemConfig> Residual = new HashMap<>();
        DatabasePlayer settings = databaseManager.getEntityById(DatabasePlayer.class, player.getUniqueId().toString());

        String theme_id = "";
        if (storer.isRegisteredTheme(settings.getUserTheme())) {
            theme_id = storer.getThemeID(settings.getUserTheme());
        } else {
            logger.print(
                    "Player " +
                            player.getName() +
                            " has an enabled theme: " +
                            settings.getUserTheme() +
                            " which dosent exist",
                    true
            );
            theme_id = "light_theme";
        }
        int index = 0;
        int residualIndex = 0;
        for (BaseDimension dimension : dimensionManager.LOADED_DIMENSIONS) {
            if (index < 54) {
                // For the first 54 dimensions, use the current index.
                Items.put(dimension.getIdentifier(), dimension.getItemConfig(index));
                index++;
            } else {
                // Mark that we have more than 54 dimensions.
                hasMore = true;
                // Use a separate residualIndex to get the item configuration.
                Residual.put(dimension.getIdentifier(), dimension.getItemConfig(residualIndex));
                residualIndex++;
            }
        }

        guiManager.openGUI(player, 6, 9, "", true, "vicky_themes:friends_gui_main_panel_" + theme_id, -8,
                Items.values().toArray(new GuiCreator.ItemConfig[0])
        );
    }
}
