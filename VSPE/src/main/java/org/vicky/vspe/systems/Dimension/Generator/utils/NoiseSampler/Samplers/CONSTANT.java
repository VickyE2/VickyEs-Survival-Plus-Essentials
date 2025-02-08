package org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers;

import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.Map;

public class CONSTANT implements NoiseSampler, Ymlable {
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> globalValues = new HashMap<>();

    public CONSTANT() {
    }

    // Setter method to set parameters
    public CONSTANT setParameter(String parameter, Object value) {
        this.values.put(parameter, value);
        return this;
    }

    public CONSTANT addGlobalParameter(String parameter, Object value) {
        this.globalValues.put(parameter, value);
        return this;
    }

    @Override
    public Map<String, Object> getValues() {
        return values;
    }


    @Override
    public Object getParameter(String parameter) {
        if (values.entrySet().stream().anyMatch(k -> k.getKey().equals(parameter)))
            return values.get(parameter);
        else
            return null;
    }

    @Override
    public Map<String, Object> getGlobalValues() {
        return globalValues;
    }


    @Override
    public StringBuilder getYml() {
        return NoiseSampler.super.getYml();
    }
}
