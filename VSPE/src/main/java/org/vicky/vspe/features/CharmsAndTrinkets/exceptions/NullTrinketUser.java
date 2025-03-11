package org.vicky.vspe.features.CharmsAndTrinkets.exceptions;

import org.bukkit.entity.Player;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;

public class NullTrinketUser extends Exception {
    private final BaseTrinket contextTrinket;
    private final Player player;
    private final String message;

    public NullTrinketUser(String message, BaseTrinket trinket, Player player) {
        this.message = message;
        this.player = player;
        this.contextTrinket = trinket;
    }

    public NullTrinketUser(String message, Player player) {
        super(message);
        this.player = player;
        this.contextTrinket = null;
        this.message = message;
    }

    public BaseTrinket getContextTrinket() {
        return contextTrinket;
    }
    public String getTrinketIssue() {
        return message + ((contextTrinket != null) ? ".. With trinket: " + contextTrinket.getFormattedName() : "");
    }
}
