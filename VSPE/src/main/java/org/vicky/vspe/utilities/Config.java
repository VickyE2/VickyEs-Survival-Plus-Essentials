package org.vicky.vspe.utilities;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

import static org.vicky.vspe.utilities.global.GlobalResources.configManager;

public class Config {
    public static final Map<String, Object> configs = new HashMap<>();

    static {
        configs.put("Feature.AdvancementPlus.enable", true);
    }

    private final JavaPlugin plugin;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;

    }

    public void registerConfigs() {
        for (String key : configs.keySet()) {
            if (!configManager.doesPathExist(key)) {
                configManager.setBracedConfigValue(key, configs.getOrDefault(key, ""), "");
            }
            configManager.saveConfig();
        }
    }
}
