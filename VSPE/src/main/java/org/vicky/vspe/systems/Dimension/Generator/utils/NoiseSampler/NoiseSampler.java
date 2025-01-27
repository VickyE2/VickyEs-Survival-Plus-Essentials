package org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;

import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.*;
import java.util.Map.Entry;

public interface NoiseSampler {
    Object getParameter(String parameter);

    default StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        // Add global values
        for (Entry<String, Object> entry : getGlobalValues().entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Add the sampler type
        builder.append("type: ").append(this.name()).append("\n");

        // Add specific values for the current sampler
        for (Entry<String, Object> value : getValues().entrySet()) {
            if (value.getValue() instanceof NoiseSampler sampler) {
                builder.append(value.getKey()).append(": ").append("\n")
                        .append(Utilities.getIndentedBlock(sampler.getYml().toString(), "  "));
            } else if (value.getValue() instanceof List<?> list) {
                if (!list.isEmpty() && list.get(0) instanceof NoiseSampler) {
                    for (Object obj : list) {
                        NoiseSampler innerSampler = (NoiseSampler) obj;
                        builder.append(value.getKey()).append(": ").append("\n")
                                .append("  ").append(innerSampler.getYml());
                    }
                }
            } else if (value.getValue() instanceof Map<?, ?> map) {
                if (!map.isEmpty()) {
                    builder.append(value.getKey()).append(": ").append("\n");

                    for (Entry<?, ?> maps : map.entrySet()) {
                        if (maps.getValue() instanceof Ymlable ymlableValue) {
                            builder.append("  ").append(maps.getKey()).append(":\n")
                                    .append(Utilities.getIndentedBlock(ymlableValue.getYml().toString(), "    "));
                        } else if (maps.getValue() instanceof NoiseSampler ymlableValue) {
                            builder.append("  ").append(maps.getKey()).append(":\n")
                                    .append(Utilities.getIndentedBlock(ymlableValue.getYml().toString(), "    "));
                        } else if (maps.getValue() instanceof StringBuilder ymlableValue) {
                            builder.append("  ").append(maps.getKey()).append(":\n")
                                    .append(Utilities.getIndentedBlock(ymlableValue.toString(), "    "));
                        }
                    }
                }
            } else if (value.getValue() != null) {
                builder.append(value.getKey()).append(": ").append(value.getValue()).append("\n");
            }
        }

        return builder;
    }

    default void setVoidParameter(String key, Object value) {
        this.getValues().put(key, value);
    }

    default void addGlobalValue(String key, Object value) {
        this.getGlobalValues().put(key, value);
    }
    default String name() {
        return this.getClass().getSimpleName().toUpperCase();
    }

    // Abstract methods to get values for each sampler
    Map<String, Object> getValues();

    Map<String, Object> getGlobalValues();
}