package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class GaussianRandomLocator implements Locator, Ymlable {
    public Object amountRange;
    public Object heightRange;
    public Object standardDeviation;
    public Object salt;

    public GaussianRandomLocator(int salt) {
        this.amountRange = null;
        this.heightRange = null;
        this.standardDeviation = null;
        this.salt = salt;
    }

    public GaussianRandomLocator(String salt) {
        this.amountRange = null;
        this.heightRange = null;
        this.standardDeviation = null;
        this.salt = salt;
    }

    public GaussianRandomLocator() {
        this.amountRange = null;
        this.heightRange = null;
        this.standardDeviation = null;
        this.salt = null;
    }

    public void setAmountRange(Range amountRange) {
        this.amountRange = amountRange;
    }

    public void setAmountRange(String amountRange) {
        this.amountRange = amountRange;
    }

    public void setHeightRange(Range heightRange) {
        this.heightRange = heightRange;
    }

    public void setHeightRange(String heightRange) {
        this.heightRange = heightRange;
    }

    public void setSalt(int salt) {
        this.salt = salt;
    }

    public void setStandardDeviation(int standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public void setStandardDeviation(String standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("type: RANGE").append("\n");
        if (amountRange != null)
            builder.append("amount: ").append(amountRange).append("\n");
        if (heightRange != null)
            builder.append("height: ").append(heightRange).append("\n");
        if (salt != null)
            builder.append("salt: ").append(salt).append("\n");
        if (standardDeviation != null)
            builder.append("standard-deviation: ").append(standardDeviation).append("\n");

        return builder;
    }

    @Override
    public String getType() {
        return null;
    }
}

