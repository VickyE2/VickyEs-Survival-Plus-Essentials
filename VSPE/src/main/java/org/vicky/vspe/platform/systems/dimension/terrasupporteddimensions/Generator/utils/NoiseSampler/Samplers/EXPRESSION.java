package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Function;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EXPRESSION implements NoiseSampler, Ymlable {
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, Object> globalValues = new HashMap<>();
    public List<Function> functions = new ArrayList<>();

    public EXPRESSION() {
    }

    // Setter method to set parameters
    public EXPRESSION setParameter(String parameter, Object value) {
        this.values.put(parameter, value);
        return this;
    }

    public EXPRESSION addGlobalParameter(String parameter, Object value) {
        this.globalValues.put(parameter, value);
        return this;
    }

    public EXPRESSION addVariable(String variable, Object value) {
        this.variables.put(variable, value);
        return this;
    }

    public EXPRESSION addFunctions(Function... functions) {
        this.functions.addAll(List.of(functions));
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
        StringBuilder builder = NoiseSampler.super.getYml();
        if (!variables.isEmpty()) {
            builder.append("variables: \n");
            for (Map.Entry<String, Object> variable : variables.entrySet()) {
                builder.append("  ").append(variable.getKey()).append(": ").append(variable.getValue()).append("\n");
            }
        }
        if (!functions.isEmpty()) {
            builder.append("functions: \n");
            for (Function function : functions) {
                builder.append(function.getYml());
            }
        }
        return builder;
    }
}
