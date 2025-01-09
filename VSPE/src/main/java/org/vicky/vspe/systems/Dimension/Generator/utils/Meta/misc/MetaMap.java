package org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc;

import org.vicky.vspe.systems.Dimension.Generator.utils.ArithmeticOperation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaMap {
    private String metaMap;
    private String tamperedMetaMap = null;
    private String finalMetaMap;

    public MetaMap(String metaMap) {
        this.metaMap = metaMap;
    }

    public void performOperation(int number, ArithmeticOperation operation) {
        if (tamperedMetaMap != null) {
            tamperedMetaMap = "{" + tamperedMetaMap + "}" + operation + number;
        }
        else {
            tamperedMetaMap = "${" + metaMap + "}" + operation + number;
        }
    }

    public void performOperation(MetaMap number, ArithmeticOperation operation) {
        if (tamperedMetaMap != null) {
            if (number.tamperedMetaMap != null) {
                tamperedMetaMap = "(" + tamperedMetaMap + ")" + operation + "(" + number + ")";
            }
            else {
                tamperedMetaMap = "(" + tamperedMetaMap + ")" + operation + number;
            }
        }
        else {
            if (number.tamperedMetaMap != null) {
                tamperedMetaMap = "${" + metaMap + "}" + operation + "(" + number + ")";
            }
            else {
                tamperedMetaMap = "${" + metaMap + "}" + operation + number;
            }
        }
    }

    @Override
    public String toString() {
        return cleanMap();
    }

    public String cleanMap() {
        if (tamperedMetaMap != null) {
            // Process the expression inside parentheses
            finalMetaMap = processParentheses(tamperedMetaMap);
            return finalMetaMap;
        }
        else {
            finalMetaMap = metaMap;
            return "$" + finalMetaMap;
        }
    }

    private String processParentheses(String expression) {
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(expression);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String simplified = simplifyExpression(matcher.group(1));
            matcher.appendReplacement(sb, "(" + simplified + ")");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String simplifyExpression(String expression) {
        expression = simplifyAdditionAndSubtraction(expression);
        return expression;
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
