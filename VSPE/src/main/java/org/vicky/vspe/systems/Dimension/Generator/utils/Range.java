package org.vicky.vspe.systems.Dimension.Generator.utils;

import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;

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
      return this.min;
   }

   public Object getMax() {
      return this.max;
   }
}
