package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class Sampler3DLocator implements Locator, Ymlable {
    public NoiseSampler sampler;

    public Sampler3DLocator(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("type: SAMPLER_3D").append("\n");
        builder.append("sampler: ").append("\n");
        builder.append(Utilities.getIndentedBlock(sampler.getYml().toString(),"  ")).append("\n");

        return builder;
    }

    @Override
    public String getType() {
        return null;
    }
}

