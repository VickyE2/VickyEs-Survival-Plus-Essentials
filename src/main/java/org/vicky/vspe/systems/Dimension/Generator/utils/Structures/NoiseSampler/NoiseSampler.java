package org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler;

import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum NoiseSampler implements Ymlable {
    CHANNEL("color-sampler", "normalize", "premultiply"),
    DISTANCE_TRANSFORM("image", "channel", "clamp-to-max-edge", "cost-function", "invert-threshold", "normalization", "threshold"),
    DISTANCE("distance-function", "normalize", "point.x", "point.y", "point.z", "radius"),
    WHITE_NOISE("frequency", "salt"),
    POSITIVE_WHITE_NOISE("frequency", "salt"),
    GAUSSIAN("frequency", "salt"),
    PERLIN("frequency", "salt"),
    SIMPLEX("frequency", "salt"),
    OPEN_SIMPLEX_2("frequency", "salt"),
    OPEN_SIMPLEX_2S("frequency", "salt"),
    VALUE("frequency", "salt"),
    VALUE_CUBIC("frequency", "salt"),
    GABOR("deviation", "frequency", "frequency_0", "impulses", "isotropic", "rotation", "salt"),
    CELLULAR("distance", "frequency", "jitter", "lookup", "return", "salt"),
    IMAGE("frequency", "image"),
    CONSTANT("value"),
    DOMAIN_WARP("sampler", "warp", "amplitude"),
    KERNEL("kernel", "sampler", "factor", "frequency"),
    LINEAR_HEIGHTMAP("base", "sampler", "scale"),
    FBM("sampler", "gain", "lacunarity", "octaves", "weighted-strength"),
    PING_PONG("sampler", "gain", "lacunarity", "octaves", "ping-pong", "weighted-strength"),
    RIDGED("sampler", "gain", "lacunarity", "octaves", "weighted-strength"),
    LINEAR("max", "min", "sampler"),
    CUBIC_SPLINE("points", "sampler"),
    EXPRESSION_NORMALIZER("expression", "sampler", "functions", "samplers", "variables"),
    CLAMP("max", "min", "sampler"),
    NORMAL("mean", "sampler", "standard-deviation", "groups"),
    PROBABILITY("sampler"),
    SCALE("amplitude", "sampler"),
    POSTERIZATION("sampler", "steps"),
    ADD("left", "right"),
    SUB("left", "right"),
    MUL("left", "right"),
    DIV("left", "right"),
    MAX("left", "right"),
    MIN("left", "right"),
    EXPRESSION("expression", "functions", "samplers", "variables");

    private final String[] parameters;
    private final Map<String, Object> values;

    NoiseSampler(String... parameters) {
        this.parameters = parameters;
        this.values = new HashMap<>();
    }

    public void setParameter(String parameter, Object value) {
        for (String param : parameters) {
            if (param.equals(parameter)) {
                values.put(parameter, value);
                return;
            }
        }
        throw new IllegalArgumentException("Invalid parameter: " + parameter + " for " + this.name());
    }

    public Object getParameter(String parameter) {
        return values.get(parameter);
    }

    public Map<String, Object> getAllParameters() {
        return new HashMap<>(values);
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("type: ").append(this).append("\n");
        for (Map.Entry<String, Object> value : values.entrySet()) {
            if (value.getValue() instanceof NoiseSampler sampler) {
                builder.append(value.getKey()).append(": ").append("\n")
                        .append(sampler.getYml());
            } else if (value.getValue() instanceof List list) {
                if (!list.isEmpty() && list.get(0) instanceof NoiseSampler) {
                    for (Object obj : list) {
                        NoiseSampler innerSampler = (NoiseSampler) obj;
                        builder.append(value.getKey()).append(": ").append("\n")
                                .append("  ").append(innerSampler.getYml());
                    }
                }
            } else if (value.getValue() != null) {
                builder.append(value.getKey()).append(": ").append(value.getValue()).append("\n");
            }
        }

        return builder;
    }
}

