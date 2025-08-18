package org.vicky.vspe.utilities.global;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.vicky.ecosystem.plugin.PluginCommunicator;
import org.vicky.utilities.ConfigManager;
import org.vicky.utilities.DatabaseManager.HibernateDatabaseManager;
import org.vicky.utilities.DatabaseManager.SQLManager;
import org.vicky.utilities.DatabaseManager.utils.AggregatedClassLoader;
import org.vicky.vspe.features.AdvancementPlus.AdvancementManager;
import org.vicky.vspe.features.CharmsAndTrinkets.CnTManager;
import org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets.PlayerEquippedTrinketScreenListener;
import org.vicky.vspe.features.effects.potions.PotionGuiListener;
import org.vicky.vspe.structure_gen.KraterosGenerationEngine;
import org.vicky.vspe.systems.dimension.DimensionsGUIListener;
import org.vicky.vspe.systems.dimension.VSPEBukkitDimensionManager;

import java.util.ArrayList;

public class GlobalResources {
    public static PotionGuiListener potionGuiListener;
    public static ConfigManager configManager;
    public static PluginCommunicator pluginCommunicator;
    public static VSPEBukkitDimensionManager dimensionManager;
    public static AdvancementManager advancementManager;
    public static CnTManager trinketsManager;
    public static HibernateDatabaseManager databaseManager;
    public static SQLManager sqlManager;
    public static MVWorldManager worldManager;
    public static DimensionsGUIListener dimensionsGUIListener;
    public static CnTManager trinketManager;
    public static AggregatedClassLoader classLoader = new AggregatedClassLoader(new ArrayList<>());
    public static PlayerEquippedTrinketScreenListener trinketScreenListener;

    public static KraterosGenerationEngine kraterosGenerationEngine;
}