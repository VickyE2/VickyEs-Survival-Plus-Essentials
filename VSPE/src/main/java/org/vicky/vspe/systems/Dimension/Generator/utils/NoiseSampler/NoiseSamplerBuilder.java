package org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class NoiseSamplerBuilder {
    private final NoiseSampler sampler;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> globalValues = new HashMap<>();

    private NoiseSamplerBuilder(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    public static NoiseSamplerBuilder of(NoiseSampler sampler) {
        return new NoiseSamplerBuilder(sampler);
    }

    public NoiseSamplerBuilder setParameter(String parameter, Object value) {
        this.values.put(parameter, value);
        return this;
    }

    public NoiseSamplerBuilder addGlobalParameter(String parameter, Object value) {
        this.globalValues.put(parameter, value);
        return this;
    }

    @Override
    public String toString() {
        return this.build().getYml().toString();
    }

    public NoiseSampler build() {
        // Set the parameters for the sampler
        for (Entry<String, Object> entry : this.values.entrySet()) {
            this.sampler.setVoidParameter(entry.getKey(), entry.getValue());
        }

        // Add global parameters
        for (Entry<String, Object> entry : this.globalValues.entrySet()) {
            this.sampler.addGlobalValue(entry.getKey(), entry.getValue());
        }

        return this.sampler;
    }
}

