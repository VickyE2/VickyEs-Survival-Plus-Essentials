package org.vicky.vspe.platform.features.CharmsAndTrinkets.exceptions;

import org.vicky.platform.PlatformPlayer;
import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinket;

public class NullTrinketUser extends Exception {
    private final PlatformTrinket contextTrinket;
    private final PlatformPlayer player;
    private final String message;

    public NullTrinketUser(String message, PlatformTrinket trinket, PlatformPlayer player) {
        this.message = message;
        this.player = player;
        this.contextTrinket = trinket;
    }

    public NullTrinketUser(String message, PlatformPlayer player) {
        super(message);
        this.player = player;
        this.contextTrinket = null;
        this.message = message;
    }

    public PlatformTrinket getContextTrinket() {
        return contextTrinket;
    }
    public String getTrinketIssue() {
        return message + ((contextTrinket != null) ? ".. With trinket: " + contextTrinket.getFormattedName() : "");
    }
}
