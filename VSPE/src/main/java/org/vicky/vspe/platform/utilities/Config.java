package org.vicky.vspe.platform.utilities;

import org.vicky.utilities.PermittedObject;
import org.vicky.utilities.PermittedObjects.AllowedBoolean;
import org.vicky.utilities.PermittedObjects.AllowedString;
import org.vicky.vspe.platform.VSPEPlatformPlugin;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public static final Map<String, PermittedObject<?>> configs = new HashMap<>();

    static {
        configs.put("Feature.AdvancementPlus.enable", new AllowedBoolean(true));
        configs.put("Debug", new AllowedBoolean(false));
    }

    public void registerConfigs() {
        for (String key : configs.keySet()) {
            if (!VSPEPlatformPlugin.config().doesKeyExist(key)) {
                VSPEPlatformPlugin.config().setConfigValue(key, configs.getOrDefault(key, new AllowedString("")));
            }
            VSPEPlatformPlugin.config().saveConfig();
        }
    }
}
