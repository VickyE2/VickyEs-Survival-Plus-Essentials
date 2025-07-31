package org.vicky.vspe.systems.dimension.dimensions.Test;

import org.vicky.vspe.systems.dimension.Generator.utils.Meta.MetaClass;

public class TestMeta extends MetaClass {
    public TestMeta() {
        this.oceanLevel = 80;
        this.setHeightVariance(3000.0);
        this.setTemperatureVariance(-0.3);
    }
}
