package org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc;

import org.vicky.vspe.systems.Dimension.Generator.utils.ArithmeticOperation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaMap {
    private final String metaMap;
    private String tamperedMetaMap = null;
    private String finalMetaMap;

    public MetaMap(String metaMap) {
        this.metaMap = metaMap;
    }

    public void performOperation(int number, ArithmeticOperation operation) {
        if (this.tamperedMetaMap != null) {
            this.tamperedMetaMap = "{" + this.tamperedMetaMap + "}" + operation + number;
        } else {
            this.tamperedMetaMap = "${" + this.metaMap + "}" + operation + number;
        }
    }

    public void performOperation(double number, ArithmeticOperation operation) {
        if (this.tamperedMetaMap != null) {
            this.tamperedMetaMap = "{" + this.tamperedMetaMap + "}" + operation + number;
        } else {
            this.tamperedMetaMap = "${" + this.metaMap + "}" + operation + number;
        }
    }

    public void performOperation(MetaMap number, ArithmeticOperation operation) {
        if (this.tamperedMetaMap != null) {
            if (number.tamperedMetaMap != null) {
                this.tamperedMetaMap = "(" + this.tamperedMetaMap + ")" + operation + "(" + number + ")";
            } else {
                this.tamperedMetaMap = "(" + this.tamperedMetaMap + ")" + operation + number;
            }
        } else if (number.tamperedMetaMap != null) {
            this.tamperedMetaMap = "${" + this.metaMap + "}" + operation + "(" + number + ")";
        } else {
            this.tamperedMetaMap = "${" + this.metaMap + "}" + operation + number;
        }
    }

    @Override
    public String toString() {
        return this.cleanMap();
    }

    public String cleanMap() {
        if (this.tamperedMetaMap != null) {
            this.finalMetaMap = this.processParentheses(this.tamperedMetaMap);
            return this.finalMetaMap;
        } else {
            this.finalMetaMap = this.metaMap;
            return "$" + this.finalMetaMap;
        }
    }

    private String processParentheses(String expression) {
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(expression);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String simplified = this.simplifyExpression(matcher.group(1));
            matcher.appendReplacement(sb, "(" + simplified + ")");
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String simplifyExpression(String expression) {
        return this.simplifyAdditionAndSubtraction(expression);
    }

    private String simplifyAdditionAndSubtraction(String expression) {
        Pattern additionPattern = Pattern.compile("(-?\\d+)\\+(-?\\d+)");
        Matcher additionMatcher = additionPattern.matcher(expression);

        while (additionMatcher.find()) {
            int a = Integer.parseInt(additionMatcher.group(1));
            int b = Integer.parseInt(additionMatcher.group(2));
            int result = a + b;
            expression = expression.replaceFirst(Pattern.quote(additionMatcher.group(0)), String.valueOf(result));
        }

        Pattern subtractionPattern = Pattern.compile("(-?\\d+)-(-?\\d+)");
        Matcher subtractionMatcher = subtractionPattern.matcher(expression);

        while (subtractionMatcher.find()) {
            int a = Integer.parseInt(subtractionMatcher.group(1));
            int b = Integer.parseInt(subtractionMatcher.group(2));
            int result = a - b;
            expression = expression.replaceFirst(Pattern.quote(subtractionMatcher.group(0)), String.valueOf(result));
        }

        return expression;
    }
}
