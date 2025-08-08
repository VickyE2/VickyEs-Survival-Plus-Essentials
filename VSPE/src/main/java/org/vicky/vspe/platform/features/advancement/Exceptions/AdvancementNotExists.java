package org.vicky.vspe.platform.features.advancement.Exceptions;

import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.platform.features.advancement.PlatformAdvancement;

public class AdvancementNotExists extends Exception {
    private volatile PlatformAdvancement linkedAdvancement;

    public AdvancementNotExists(String message) {
        super(message);
    }

    public AdvancementNotExists(String message, PlatformAdvancement advancement) {
        super(message);
        this.linkedAdvancement = advancement;
    }

    @Override
    public String toString() {
        return this.linkedAdvancement == null
                ? super.toString()
                : super.toString()
                + ANSIColor.colorize(
                "\n - with linked advancement:\npurple[" + this.linkedAdvancement.getTitle() + "] UUID:purple[" + this.linkedAdvancement.getIdentifier() + "]"
        );
    }
}
