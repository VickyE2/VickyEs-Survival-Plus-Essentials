package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.MetaClass;

public class TestMeta extends MetaClass {
    public TestMeta() {
        this.oceanLevel = 80;
        this.setHeightVariance(3000.0);
        this.setTemperatureVariance(-0.3);
    }
}
