package org.vicky.vspe.features.AdvancementPlus.Exceptions;

import org.vicky.utilities.ANSIColor;
import org.vicky.vspe.features.AdvancementPlus.BaseAdvancement;

public class AdvancementNotExists extends Exception {
    private volatile BaseAdvancement linkedAdvancement;

    public AdvancementNotExists(String message) {
        super(message);
    }

    public AdvancementNotExists(String message, BaseAdvancement advancement) {
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
