package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators;

import org.vicky.vspe.platform.systems.dimension.Exceptions.MisconfigurationException;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Range;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;

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
        builder.append("range: ")
                .append("\n")
                .append("  min: ")
                .append(this.range.getMin())
                .append("\n")
                .append("  max: ")
                .append(this.range.getMax())
                .append("\n");
        builder.append("pattern:").append("\n");
        try {
            builder.append(Utilities.getIndentedBlock(this.pattern.getYml().toString(), "  ")).append("\n");
        }
        catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    @Override
    public String getType() {
        return "PATTERN";
    }
}
