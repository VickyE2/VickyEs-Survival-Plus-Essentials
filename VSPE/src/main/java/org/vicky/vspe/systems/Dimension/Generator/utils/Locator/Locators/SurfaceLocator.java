package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class SurfaceLocator implements Locator, Ymlable {
    public Range range;

    public SurfaceLocator(Range range) {
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
        return builder;
    }

    @Override
    public String getType() {
        return "SURFACE";
    }
}
