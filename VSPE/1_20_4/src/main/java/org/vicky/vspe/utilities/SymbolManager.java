package org.vicky.vspe.utilities;
import java.util.HashMap;
import java.util.Map;

/**
 * SymbolManager is a utility class to retrieve a rarely used symbol for a given key.
 * If the key is not found, it returns an empty string.
 */
public class SymbolManager {
    private static final Map<String, String> SYMBOL_MAP = new HashMap<>();

    static {
        // Group images
        SYMBOL_MAP.put("accuracy", "⍟");
        SYMBOL_MAP.put("attack_damage", "⚔");
        SYMBOL_MAP.put("attack_speed", "⏩");
        SYMBOL_MAP.put("cooldown_reduction", "⏳");
        SYMBOL_MAP.put("crit_chance", "✧");
        SYMBOL_MAP.put("crit_damage", "❖");
        SYMBOL_MAP.put("damage_reduction", "🛡");
        SYMBOL_MAP.put("defense", "⛨");
        SYMBOL_MAP.put("dodge", "⤴");
        SYMBOL_MAP.put("exp_gain", "📖");
        SYMBOL_MAP.put("health_regen", "❤");
        SYMBOL_MAP.put("life_steal", "🩸");
        SYMBOL_MAP.put("luck", "🍀");
        SYMBOL_MAP.put("magic_damage", "🔮");
        SYMBOL_MAP.put("mana", "🔵");
        SYMBOL_MAP.put("mana_regen", "💧");
        SYMBOL_MAP.put("movement_speed", "🏃");
        SYMBOL_MAP.put("range", "🎯");
        SYMBOL_MAP.put("sensing", "🔍");
        SYMBOL_MAP.put("stealth", "🕵");

        // Slot images
        SYMBOL_MAP.put("anklet", "✶");
        SYMBOL_MAP.put("belt", "➶");
        SYMBOL_MAP.put("bracelet", "✦");
        SYMBOL_MAP.put("charm", "❉");
        SYMBOL_MAP.put("dual_anklet", "❈");
        SYMBOL_MAP.put("dual_bracelet", "✷");
        SYMBOL_MAP.put("dual_earring", "✵");
        SYMBOL_MAP.put("dual_ring", "✪");
        SYMBOL_MAP.put("earring", "❂");
        SYMBOL_MAP.put("head", "☉");
        SYMBOL_MAP.put("neck", "♢");
        SYMBOL_MAP.put("ring", "◎");
    }

    /**
     * Retrieves the symbol for the given key.
     *
     * @param key the name for which the symbol is requested
     * @return the corresponding symbol, or an empty string if not found
     */
    public static String getSymbol(String key) {
        return SYMBOL_MAP.getOrDefault(key, "");
    }
}