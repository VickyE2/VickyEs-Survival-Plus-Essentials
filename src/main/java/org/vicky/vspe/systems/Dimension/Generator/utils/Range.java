package org.vicky.vspe.systems.Dimension.Generator.utils;

import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;

// Range class to store min and max values
public class Range {
    private Object min;
    private final Object max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Range(MetaMap min, MetaMap max) {
        this.min = min;
        this.max = max;
    }

    public Object getMin() {
        return min;
    }

    public Object getMax() {
        return max;
    }
}

