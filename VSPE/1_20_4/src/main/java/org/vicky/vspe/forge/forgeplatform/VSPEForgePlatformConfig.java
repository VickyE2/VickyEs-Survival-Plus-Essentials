package org.vicky.vspe.forge.forgeplatform;

import org.vicky.platform.PlatformConfig;
import org.vicky.utilities.JsonConfigManager;
import org.vicky.utilities.PermittedObject;
import org.vicky.vspe.platform.VSPEPlatformPlugin;

public class VSPEForgePlatformConfig implements PlatformConfig {

    private static VSPEForgePlatformConfig INSTANCE;
    private final JsonConfigManager manager;

    private VSPEForgePlatformConfig() {
        manager = new JsonConfigManager();
        manager.createPathedConfig(VSPEPlatformPlugin.dataFolder().getPath() + "\\configs\\config.json");
        manager.loadConfigValues();
    }

    public static VSPEForgePlatformConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VSPEForgePlatformConfig();
        }
        return INSTANCE;
    }

    @Override
    public boolean getBooleanValue(String debug) {
        return manager.getBooleanValue(debug);
    }

    @Override
    public String getStringValue(String debug) {
        return manager.getStringValue(debug);
    }

    @Override
    public Integer getIntegerValue(String debug) {
        return manager.getIntegerValue(debug);
    }

    @Override
    public Float getFloatValue(String debug) {
        return (float) manager.getIntegerValue(debug);
    }

    @Override
    public Double getDoubleValue(String debug) {
        return manager.getDoubleValue(debug);
    }

    @Override
    public void setConfigValue(String key, PermittedObject<?> value) {
        manager.setConfigValue(key, value.getValue());
    }

    @Override
    public boolean doesKeyExist(String key) {
        return manager.doesPathExist(key);
    }

    @Override
    public void saveConfig() {
        manager.saveConfig();
    }

    public void setConfigValue(String key, Object value) {
        this.manager.setConfigValue(key, value);
    }

    public void syncFromForgeConfig() {

    }
}