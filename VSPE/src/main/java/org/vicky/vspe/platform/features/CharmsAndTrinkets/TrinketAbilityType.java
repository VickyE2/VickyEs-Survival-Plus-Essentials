package org.vicky.vspe.platform.features.CharmsAndTrinkets;

enum AbilityCategory {
    OFFENSIVE, DEFENSIVE, UTILITY
}

public enum TrinketAbilityType {

    /**
     * An Offensive Buff relating to the damage increase on overall attacks.
     */
    ATTACK_POWER("attack_damage"),

    /**
     * An Offensive Buff relating to the damage increase on magical attacks.
     */
    MAGIC_POWER("magic_damage"),

    /**
     * An Offensive Buff relating to the attack rate of a user.
     */
    ATTACK_SPEED("attack_speed"),

    /**
     * An Offensive Buff relating to the chance increase of critical hits.
     */
    CRITICAL_CHANCE("crit_chance"),

    /**
     * An Offensive Buff relating to the damage increase on critical hits.
     */
    CRITICAL_DAMAGE("crit_damage"),

    /**
     * A Defensive Buff relating to all-round defensive abilities.
     */
    DEFENSE("defense"),

    /**
     * A Defensive Buff relating to the damage absorption rate of a user.
     */
    DAMAGE_RESISTANCE("damage_reduction"),

    /**
     * A Defensive Buff relating to the speed of hit-point regeneration.
     */
    HEALTH_REGEN("health_regen"),

    /**
     * A Defensive Buff relating to the speed of mana regeneration.
     */
    MANA_REGEN("mana_regen"),

    /**
     * A Defensive Buff relating to stealing or harvesting hit-points for other entities.
     */
    LIFE_STEAL("life_steal"),

    /**
     * A Utility Buff relating to the precision of shooting entities.
     */
    ACCURACY("accuracy"),

    /**
     * A Utility Buff relating to revealing of other entities in a particular radius.
     */
    EVASION("dodge"),

    /**
     * A Utility Buff relating to increase the users speed land, air or water.
     */
    MOVEMENT_SPEED("movement_speed"),

    /**
     * A Utility Buff relating to revealing of other entities in a particular radius.
     */
    SENSING("sensing"),

    /**
     * A Utility Buff relating to reduction in cooldown of specific tools.
     */
    COOLDOWN_REDUCTION("cooldown_reduction"),

    /**
     * A Utility Buff relating to how the chances of something are increased.
     */
    LUCK("luck"),

    /**
     * A Utility Buff that increases the amount of experience gained from actions.
     */
    EXPERIENCE_GAIN("exp_gain"),

    /**
     * A Utility Buff that enhances stealth capabilities by reducing enemy detection.
     */
    STEALTH("stealth");

    private final String icoName;

    TrinketAbilityType(String icoName) {
        this.icoName = icoName;
    }

    public String getIcoName() {
        return icoName;
    }
}
