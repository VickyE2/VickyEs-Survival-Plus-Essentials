package org.vicky.vspe.platform.features.CharmsAndTrinkets;

import org.vicky.utilities.ContextLogger.ContextLogger;

import java.util.HashMap;
import java.util.Map;

public class TrinketRegistry {
    private final static Map<String, TrinketAbility> registeredAbilities =
            new HashMap<>();
    private final static ContextLogger logger =
            new ContextLogger(ContextLogger.ContextType.FEATURE, "TRINKET-ABILITY-REGISTRY");

    public static Map<String, TrinketAbility> getRegisteredAbilities() {
        return new HashMap<>(registeredAbilities);
    }

    public static void addTrinketAbility(TrinketAbility trinketAbility) {
        if (registeredAbilities.containsKey(trinketAbility.id())) {
            logger.print("Ability with id " + trinketAbility.id() + " already registered.", true);
        }
        registeredAbilities.put(trinketAbility.id(), trinketAbility);
    }

    public static void removeTrinketAbility(String id) {
        registeredAbilities.remove(id);
    }

    public static void registerAllFromEnum() {
        for (TrinketAbilityType type : TrinketAbilityType.values()) {
            TrinketAbility ability = new TrinketAbility(
                    type.name(), // Use enum name as ID
                    type.getIcoName(), // Optional display/icon name
                    resolveCategory(type)
            );
            addTrinketAbility(ability);
        }
    }

    public static void unregisterAllFromEnum() {
        for (TrinketAbilityType type : TrinketAbilityType.values()) {
            removeTrinketAbility(type.name());
        }
    }

    private static AbilityCategory resolveCategory(TrinketAbilityType type) {
        return switch (type) {
            case ATTACK_POWER, MAGIC_POWER, ATTACK_SPEED,
                 CRITICAL_CHANCE, CRITICAL_DAMAGE -> AbilityCategory.OFFENSIVE;

            case DEFENSE, DAMAGE_RESISTANCE, HEALTH_REGEN,
                 MANA_REGEN, LIFE_STEAL -> AbilityCategory.DEFENSIVE;

            case ACCURACY, EVASION, MOVEMENT_SPEED,
                 SENSING, COOLDOWN_REDUCTION, LUCK,
                 EXPERIENCE_GAIN, STEALTH -> AbilityCategory.UTILITY;
        };
    }
}
