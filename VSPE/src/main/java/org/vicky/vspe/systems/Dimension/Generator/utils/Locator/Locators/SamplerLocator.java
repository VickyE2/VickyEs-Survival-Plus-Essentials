package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import java.util.ArrayList;
import java.util.List;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;

public class SamplerLocator implements Locator, Ymlable {
   public List<NoiseSampler> samplers = new ArrayList<>();

   public void addLocator(NoiseSampler sampler) {
      this.samplers.add(sampler);
   }

   @Override
   public StringBuilder getYml() {
      StringBuilder builder = new StringBuilder();
      builder.append("type: SAMPLER").append("\n");
      builder.append("samplers: ").append("\n");

      for (NoiseSampler locator : this.samplers) {
         builder.append(Utilities.getIndentedBlock(locator.getYml().toString(), "  ")).append("\n");
      }

      return builder;
   }

   @Override
   public String getType() {
      return null;
   }
}
