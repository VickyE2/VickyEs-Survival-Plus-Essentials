package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns.Pattern;

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
      return new StringBuilder();
   }

   @Override
   public String getType() {
      return null;
   }
}
