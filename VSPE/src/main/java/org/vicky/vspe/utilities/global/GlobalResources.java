package org.vicky.vspe.utilities.global;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.vicky.utilities.ConfigManager;
import org.vicky.utilities.DatabaseManager.HibernateDatabaseManager;
import org.vicky.utilities.DatabaseManager.SQLManager;
import org.vicky.utilities.Theme.ThemeStorer;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.systems.Dimension.DimensionManager;
import org.vicky.vspe.systems.Dimension.DimensionsGUIListener;

public class GlobalResources {
    public static ConfigManager configManager;
    public static DimensionManager dimensionManager;
    public static AdvancementManager advancementManager;
    public static HibernateDatabaseManager databaseManager;
    public static SQLManager sqlManager;
    public static MVWorldManager worldManager;
    public static DimensionsGUIListener dimensionsGUIListener = new DimensionsGUIListener();
}