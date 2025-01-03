package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

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
        if (amountRange != null)
            builder.append("amount: ").append(amountRange.getMax() - amountRange.getMin()).append("\n");
        if (heightRange != null)
            if (heightRange instanceof Range) {
                builder.append("height: ").append("\n")
                        .append("  max: ").append(((Range) heightRange).getMax()).append("\n")
                        .append("  min: ").append(((Range) heightRange).getMin()).append("\n");
            } else
                builder.append("height: ").append(heightRange).append("\n");
        if (salt != null)
            builder.append("salt: ").append(salt).append("\n");

        return builder;
    }

    @Override
    public String getType() {
        return null;
    }
}

