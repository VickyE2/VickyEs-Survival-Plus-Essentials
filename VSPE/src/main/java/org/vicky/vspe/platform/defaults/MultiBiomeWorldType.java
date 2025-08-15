package org.vicky.vspe.platform.defaults;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.platform.PlatformWorldType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.misc.Tree;

import java.util.ArrayList;
import java.util.List;

public class MultiBiomeWorldType implements PlatformWorldType {

    private final List<ResourceLocation> biomeList = new ArrayList<>();

    public MultiBiomeWorldType(String biomeList) {
        var biomes = biomeList.split(",");
        for (var biome : biomes) {
            this.biomeList.add(ResourceLocation.from(biome));
        }
    }

    public List<ResourceLocation> getBiomeList() {
        return biomeList;
    }

    @Override
    public String name() {
        return "MULTIBIOME";
    }
}
