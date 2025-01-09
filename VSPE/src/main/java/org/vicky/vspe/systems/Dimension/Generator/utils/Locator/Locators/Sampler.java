package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class Sampler implements Locator, Ymlable {

    private final NoiseSampler sampler;
    private final float threshold;

    public Sampler() {
        this.sampler = null;
        this.threshold = 0.0f;
    }

    public Sampler(NoiseSampler sampler) {
        this.sampler = sampler;
        this.threshold = 0.0f;
    }

    public Sampler(NoiseSampler sampler, Float threshold) {
        this.sampler = sampler;
        this.threshold = threshold;
    }

    public float getThreshold() {
        return threshold;
    }

    public NoiseSampler getSampler() {
        return sampler;
    }

    @Override
    public String getType() {
        return "SAMPLER";
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("type: SAMPLER").append("\n");
        if (sampler != null) {
            builder.append("sampler: ").append("\n");
            builder.append(Utilities.getIndentedBlock(sampler.getYml().toString(), "  ")).append("\n");
        }
        builder.append("threshold: ").append(threshold);

        return builder;
    }
}
