package org.vicky.vspe.platform;

import org.vicky.platform.world.PlatformMaterial;

public interface PlatformItemFactory {
    PlatformItem fromMaterial(PlatformMaterial material);
}
