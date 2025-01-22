package org.vicky.vspe.systems.Dimension.Generator.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NoiseSamplerBuilder {
   private final NoiseSampler sampler;
   private final Map<String, Object> values = new HashMap<>();
   private final Map<String, Object> globalValues = new HashMap<>();

   private NoiseSamplerBuilder(NoiseSampler sampler) {
      this.sampler = sampler;
   }

   public static NoiseSamplerBuilder of(NoiseSampler sampler) {
      return new NoiseSamplerBuilder(sampler);
   }

   public NoiseSamplerBuilder setParameter(String parameter, Object value) {
      this.values.put(parameter, value);
      return this;
   }

   public NoiseSamplerBuilder addGlobalParameter(String parameter, Object value) {
      this.globalValues.put(parameter, value);
      return this;
   }

   public NoiseSampler build() {
      for (Entry<String, Object> entry : this.values.entrySet()) {
         this.sampler.setParameter(entry.getKey(), entry.getValue());
      }

      for (Entry<String, Object> entry : this.globalValues.entrySet()) {
         this.sampler.addGlobalParameter(entry.getKey(), entry.getValue());
      }

      return this.sampler;
   }
}
