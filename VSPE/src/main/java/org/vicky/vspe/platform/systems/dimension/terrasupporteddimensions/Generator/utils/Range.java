package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.misc.MetaMap;

public class Range {
    private final Object max;
    private final Object min;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Range(MetaMap min, MetaMap max) {
        this.min = min;
        this.max = max;
    }

    public Object getMin() {
        return this.min;
    }

    public Object getMax() {
        return this.max;
    }
}
