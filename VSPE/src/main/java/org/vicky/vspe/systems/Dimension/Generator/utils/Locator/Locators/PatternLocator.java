package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class PatternLocator implements Locator, Ymlable {
    public Pattern pattern;
    public Range range;

    public PatternLocator(Pattern pattern, Range range) {
        this.pattern = pattern;
        this.range = range;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        builder.append("type: PATTERN").append("\n");
        builder.append("range: ")
                .append("\n")
                .append("  min: ")
                .append(this.range.getMin())
                .append("\n")
                .append("  max: ")
                .append(this.range.getMax())
                .append("\n");
        builder.append("pattern:").append("\n").append(Utilities.getIndentedBlock(this.pattern.getYml().toString(), "  ")).append("\n");
        return builder;
    }

    @Override
    public String getType() {
        return null;
    }
}
