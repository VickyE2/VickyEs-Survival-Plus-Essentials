package org.vicky.vspe.platform.features.advancement.Exceptions;

import org.vicky.platform.PlatformPlayer;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancement;

public class NullAdvancementUser extends Exception {
    private final PlatformAdvancement contextAdvancement;
    private final PlatformPlayer player;
    private final String message;

    public NullAdvancementUser(String message, PlatformAdvancement trinket, PlatformPlayer player) {
        this.message = message;
        this.player = player;
        this.contextAdvancement = trinket;
    }

    public NullAdvancementUser(String message, PlatformPlayer player) {
        super(message);
        this.player = player;
        this.contextAdvancement = null;
        this.message = message;
    }

    public PlatformAdvancement getContextAdvancement() {
        return contextAdvancement;
    }
    public String getTrinketIssue() {
        return message + ((contextAdvancement != null) ? ".. With trinket: " + contextAdvancement.getTitle() : "");
    }
}
