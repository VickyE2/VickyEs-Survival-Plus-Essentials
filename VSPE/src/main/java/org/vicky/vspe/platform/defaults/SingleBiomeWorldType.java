package org.vicky.vspe.platform.defaults;

import com.fasterxml.jackson.core.JsonToken;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformWorldType;

public class SingleBiomeWorldType implements PlatformWorldType {
    private final ResourceLocation resource;

    public SingleBiomeWorldType(String biome) {
        resource = ResourceLocation.from(biome);
    }

    public String getBiomeID() {
        return resource.asString();
    }

    @Override
    public String name() {
        return "SINGLEBIOME";
    }
}
