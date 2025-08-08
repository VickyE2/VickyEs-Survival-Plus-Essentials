package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils;

public enum ArithmeticOperation {
    ADD("+"),
    SUBTRACT("-"),
    DIVIDE("/"),
    MULTIPLY("*");

    private final String operator;

    ArithmeticOperation(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return this.operator;
    }
}
