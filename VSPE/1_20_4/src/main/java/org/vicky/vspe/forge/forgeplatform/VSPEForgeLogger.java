package org.vicky.vspe.forge.forgeplatform;

import org.vicky.platform.PlatformLogger;
import org.vicky.vspe.VspeForge;

public class VSPEForgeLogger implements PlatformLogger {
    @Override
    public void info(String message) {
        VspeForge.LOGGER.info(message);
    }

    @Override
    public void warn(String msg) {
        VspeForge.LOGGER.warn(msg);
    }

    @Override
    public void error(String msg) {
        VspeForge.LOGGER.error(msg);
    }

    @Override
    public void debug(String msg) {
        VspeForge.LOGGER.debug(msg);
    }

    @Override
    public void error(String msg, Throwable throwable) {
        VspeForge.LOGGER.error(msg, throwable);
    }
}