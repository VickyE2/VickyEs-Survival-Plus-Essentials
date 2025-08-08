package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.EXPRESSION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class NoiseSamplerBuilder {
    private final NoiseSampler sampler;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> variables = new HashMap<>();
    private final List<Function> functions = new ArrayList<>();
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

    public NoiseSamplerBuilder addFunctions(Function... functions) {
        this.functions.addAll(List.of(functions));
        return this;
    }

    public NoiseSamplerBuilder addVariable(String varNAme, Object value) {
        this.variables.put(varNAme, value);
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

        if (this.sampler instanceof EXPRESSION exp) {
            for (Entry<String, Object> ent : variables.entrySet())
                exp.addVariable(ent.getKey(), ent.getValue());
            exp.functions.addAll(functions);
        }

        return this.sampler;
    }
}

