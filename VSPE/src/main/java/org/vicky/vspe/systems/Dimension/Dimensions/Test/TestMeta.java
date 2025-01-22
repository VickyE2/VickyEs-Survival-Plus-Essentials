package org.vicky.vspe.systems.Dimension.Dimensions.Test;

import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;

public class TestMeta extends MetaClass {
   public TestMeta() {
      this.oceanLevel = 80;
      this.setHeightVariance(3000.0);
      this.setTemperatureVariance(-0.3);
   }
}
