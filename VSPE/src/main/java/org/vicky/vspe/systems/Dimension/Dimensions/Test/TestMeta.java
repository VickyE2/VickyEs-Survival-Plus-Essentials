package org.vicky.vspe.systems.Dimension.Dimensions.Test;

import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;

public class TestMeta extends MetaClass {
    public TestMeta() {
        oceanLevel = 80;
        setHeightVariance(3000);
        setTemperatureVariance(-0.3);
    }
}
