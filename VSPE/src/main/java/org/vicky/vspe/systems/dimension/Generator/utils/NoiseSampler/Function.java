package org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler;

import org.vicky.vspe.systems.dimension.Generator.utils.Ymlable;

import java.util.Arrays;

public class Function implements Ymlable {
    final String name;
    final String[] arguments;
    final String expression;

    public Function(String name, String expression, String... arguments) {
        this.arguments = arguments;
        this.name = name;
        this.expression = expression;
    }

    @Override
    public StringBuilder getYml() {
        return new StringBuilder(name).append(": \n")
                .append("  arguments: ").append(Arrays.toString(arguments))
                .append("  expression: ").append(expression);
    }
}
