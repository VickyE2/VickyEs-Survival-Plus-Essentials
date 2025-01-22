package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.ArithmeticOperation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;

public class RandomLocator implements Locator, Ymlable {
   public Range amountRange;
   public Object heightRange;
   public Object salt;

   public RandomLocator() {
      this.amountRange = null;
      this.heightRange = null;
      this.salt = null;
   }

   public RandomLocator(Range range, Range height, int salt) {
      this.amountRange = range;
      this.heightRange = height;
      this.salt = salt;
   }

   public void setSalt(Object salt) {
      this.salt = salt;
   }

   public void setAmountRange(Range amountRange) {
      this.amountRange = amountRange;
   }

   public void setHeightRange(Range heightRange) {
      this.heightRange = heightRange;
   }

   public void setHeightRange(String heightRange) {
      this.heightRange = heightRange;
   }

   public void setSalt(Range salt) {
      this.salt = salt;
   }

   public void setSalt(String salt) {
      this.salt = salt;
   }

   @Override
   public StringBuilder getYml() {
      StringBuilder builder = new StringBuilder();
      builder.append("type: RANDOM").append("\n");
      if (this.amountRange != null) {
         builder.append("amount: ");
         if (this.amountRange.getMax() instanceof Integer && this.amountRange.getMin() instanceof Integer) {
            builder.append((Integer)this.amountRange.getMax() - (Integer)this.amountRange.getMin()).append("\n");
         } else if (this.amountRange.getMax() instanceof MetaMap && this.amountRange.getMin() instanceof MetaMap) {
            ((MetaMap)this.amountRange.getMax()).performOperation((MetaMap)this.amountRange.getMin(), ArithmeticOperation.SUBTRACT);
            builder.append(this.amountRange.getMax().toString()).append("\n");
         }
      }

      if (this.heightRange != null) {
         if (this.heightRange instanceof Range) {
            builder.append("height: ")
               .append("\n")
               .append("  max: ")
               .append(((Range)this.heightRange).getMax())
               .append("\n")
               .append("  min: ")
               .append(((Range)this.heightRange).getMin())
               .append("\n");
         } else {
            builder.append("height: ").append(this.heightRange).append("\n");
         }
      }

      if (this.salt != null) {
         builder.append("salt: ").append(this.salt).append("\n");
      }

      return builder;
   }

   @Override
   public String getType() {
      return null;
   }
}
