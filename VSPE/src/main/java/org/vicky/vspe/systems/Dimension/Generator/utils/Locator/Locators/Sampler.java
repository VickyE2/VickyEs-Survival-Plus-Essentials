package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class Sampler implements Locator, Ymlable {
    private final NoiseSampler sampler;
    private final float threshold;

    public Sampler() {
        this.sampler = null;
        this.threshold = 0.0F;
    }

    public Sampler(NoiseSampler sampler) {
        this.sampler = sampler;
        this.threshold = 0.0F;
    }

    public Sampler(NoiseSampler sampler, Float threshold) {
        this.sampler = sampler;
        this.threshold = threshold;
    }

    public float getThreshold() {
        return this.threshold;
    }

    public NoiseSampler getSampler() {
        return this.sampler;
    }

    @Override
    public String getType() {
        return "SAMPLER";
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        if (this.sampler != null) {
            builder.append("sampler: ").append("\n");
            builder.append(Utilities.getIndentedBlock(this.sampler.getYml().toString(), "  "));
        }

        builder.append("threshold: ").append(this.threshold);
        return builder;
    }
}
