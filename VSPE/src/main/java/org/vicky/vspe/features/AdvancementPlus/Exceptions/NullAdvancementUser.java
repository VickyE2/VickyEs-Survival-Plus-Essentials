package org.vicky.vspe.features.AdvancementPlus.Exceptions;

import org.bukkit.entity.Player;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;
import org.vicky.vspe.features.CharmsAndTrinkets.BaseTrinket;

public class NullAdvancementUser extends Exception {
    private final BaseAdvancement contextAdvancement;
    private final Player player;
    private final String message;

    public NullAdvancementUser(String message, BaseAdvancement trinket, Player player) {
        this.message = message;
        this.player = player;
        this.contextAdvancement = trinket;
    }

    public NullAdvancementUser(String message, Player player) {
        super(message);
        this.player = player;
        this.contextAdvancement = null;
        this.message = message;
    }

    public BaseAdvancement getContextAdvancement() {
        return contextAdvancement;
    }
    public String getTrinketIssue() {
        return message + ((contextAdvancement != null) ? ".. With trinket: " + contextAdvancement.getFormattedTitle() : "");
    }
}
