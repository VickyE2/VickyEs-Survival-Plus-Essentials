package org.vicky.vspe.systems.Dimension.Generator.utils;

import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;

public class DimensionedSampler implements Ymlable {
    private final NoiseSampler sampler;
    private final int dimension;

    public DimensionedSampler(int dimension, NoiseSampler sampler) {
        this.sampler = sampler;
        this.dimension = dimension;
    }

    public NoiseSampler getSampler() {
        return this.sampler;
    }

    public int getDimension() {
        return this.dimension;
    }

    @Override
    public StringBuilder getYml() {
        return new StringBuilder().append("dimensions: ").append(this.dimension).append("\n").append(this.sampler.getYml());
    }
}
