package org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.guiparent.BaseGui;
import org.vicky.guiparent.GuiCreator;
import org.vicky.utilities.DatabaseManager.dao_s.DatabasePlayerDAO;
import org.vicky.utilities.DatabaseManager.templates.DatabasePlayer;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullManagerTrinket;
import org.vicky.vspe.features.CharmsAndTrinkets.exceptions.NullTrinketUser;
import org.vicky.vspe.utilities.Hibernate.DBTemplates.CnTPlayer;
import org.vicky.vspe.utilities.Hibernate.dao_s.CnTPlayerDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.vicky.vspe.utilities.global.GlobalResources.trinketScreenListener;

public class PlayerEquippedTrinketScreen extends BaseGui {

    public PlayerEquippedTrinketScreen(JavaPlugin plugin) {
        super(plugin, trinketScreenListener);
    }

    @Override
    public void showGui(Player player) {
        Optional<CnTPlayer> oCnT = new CnTPlayerDAO().findById(player.getUniqueId());
        Optional<DatabasePlayer> oDP = new DatabasePlayerDAO().findById(player.getUniqueId());
        if (oCnT.isPresent() && oDP.isPresent()) {
            CnTPlayer cnTPlayer = oCnT.get();
            DatabasePlayer databasePlayer = oDP.get();
            try {
                List<EquippedRawTrinket> playerTrinkets = cnTPlayer.getRawWornTrinkets();
                List<GuiCreator.ItemConfig> itemConfigs = new ArrayList<>();
                for (EquippedRawTrinket trinket : playerTrinkets) {
                    GuiCreator.ItemConfig config = trinket.getItem();
                    config.setSlotRange(String.valueOf(trinket.getSlot() + 1));
                    itemConfigs.add(config);
                }
                this.guiManager.openGUI(player, 6, 9, "", true, "vicky_themes:trinket_gui_" + databasePlayer.getUserTheme(), -8, itemConfigs.toArray(GuiCreator.ItemConfig[]::new));
            } catch (NullManagerTrinket e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                throw new NullTrinketUser("Failed to locate the user in database", player);
            } catch (NullTrinketUser e) {
                throw new RuntimeException(e);
            }
        }
    }
}
