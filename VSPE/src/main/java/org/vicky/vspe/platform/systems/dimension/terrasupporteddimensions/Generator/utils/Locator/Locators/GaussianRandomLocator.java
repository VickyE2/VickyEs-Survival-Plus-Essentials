package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Range;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;

public class GaussianRandomLocator implements Locator, Ymlable {
    public Object amountRange = null;
    public Object heightRange = null;
    public Object standardDeviation = null;
    public Object salt;

    public GaussianRandomLocator(int salt) {
        this.salt = salt;
    }

    public GaussianRandomLocator(String salt) {
        this.salt = salt;
    }

    public GaussianRandomLocator() {
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
        if (this.amountRange != null) {
            builder.append("amount: ").append(this.amountRange).append("\n");
        }

        if (this.heightRange != null) {
            builder.append("height: ").append(this.heightRange).append("\n");
        }

        if (this.salt != null) {
            builder.append("salt: ").append(this.salt).append("\n");
        }

        if (this.standardDeviation != null) {
            builder.append("standard-deviation: ").append(this.standardDeviation).append("\n");
        }

        return builder;
    }

    @Override
    public String getType() {
        return "GAUSSIAN_RANDOM";
    }
}
