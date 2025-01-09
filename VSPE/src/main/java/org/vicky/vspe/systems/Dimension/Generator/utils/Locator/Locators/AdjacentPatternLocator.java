package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class AdjacentPatternLocator implements Locator, Ymlable {
    public Pattern pattern;
    public Range range;
    public boolean matchAll;

    public AdjacentPatternLocator(Pattern pattern, Range range, boolean matchAll) {
        this.pattern = pattern;
        this.range = range;
        this.matchAll = matchAll;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        return builder;
    }

    @Override
    public String getType() {
        return null;
    }
}

