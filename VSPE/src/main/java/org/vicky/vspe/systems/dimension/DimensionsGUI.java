package org.vicky.vspe.systems.dimension;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.bukkitplatform.useables.BukkitPlatformPlayer;
import org.vicky.guiparent.BaseGui;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.platform.features.advancement.Exceptions.NullAdvancementUser;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.AdvanceablePlayer;
import org.vicky.vspe.utilities.Hibernate.api.AdvanceablePlayerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.vicky.global.Global.storer;
import static org.vicky.vspe.utilities.global.GlobalResources.dimensionManager;
import static org.vicky.vspe.utilities.global.GlobalResources.dimensionsGUIListener;

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
        List<GuiCreator.ItemConfig> MainItems = new ArrayList<>();
        List<GuiCreator.ItemConfig> PaginatedItems = new ArrayList<>();
        AdvanceablePlayerService service = AdvanceablePlayerService.getInstance();
        Optional<AdvanceablePlayer> oAP = service.getPlayerById(player.getUniqueId());
        if (oAP.isEmpty()) {
            try {
                throw new NullAdvancementUser("Failed to get AdvancementPlayer from database", BukkitPlatformPlayer.of(player));
            } catch (NullAdvancementUser e) {
                throw new RuntimeException(e);
            }
        }
        AdvanceablePlayer settings = oAP.get();

        String theme_id = "";
        if (storer.isRegisteredTheme(settings.getDatabasePlayer().getUserTheme())) {
            theme_id = settings.getDatabasePlayer().getUserTheme();
        } else {
            logger.print(
                    "Player " +
                            player.getName() +
                            " has an enabled theme: " +
                            settings.getDatabasePlayer().getUserTheme() +
                            " which dosent exist",
                    true
            );
            theme_id = "lt";
        }
        for (BukkitBaseDimension dimension : dimensionManager.LOADED_DIMENSIONS.stream().map(BukkitBaseDimension.class::cast).toList())
            PaginatedItems.add(dimension.getItemConfig(0));

        guiManager.paginated(
                player,
                6,
                GuiCreator.ArrowGap.SMALL,
                new ArrayList<>(),
                PaginatedItems,
                "1-5,10-14,19-23,28-32,37-41,46-50",
                1,
                30,
                "",
                true,
                true,
                "vicky_themes:five_by_six_left_" + theme_id,
                -8
        );
    }
}
