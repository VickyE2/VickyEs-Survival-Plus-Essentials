package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.ArrayList;
import java.util.List;

public class AndLocator implements Locator, Ymlable {
    private final List<Object> locators;

    public AndLocator() {
        this.locators = new ArrayList<>();
    }

    public void addLocator(Locator locator) {
        this.locators.add(locator);
    }

    public void addLocator(NoiseSampler locator) {
        this.locators.add(locator);
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("type: AND").append("\n");
        builder.append("locators: ").append("\n");
        for (Object locator : locators)
            if (locator instanceof Locator)
                builder.append(" -").append(Utilities.getIndentedBlock(((Ymlable) locator).getYml().toString(), "   ")).append("\n");
            else if (locator instanceof NoiseSampler)
                builder.append(" -").append(Utilities.getIndentedBlock(((NoiseSampler) locator).getYml().toString(), "   ")).append("\n");

        return builder;
    }

    @Override
    public String getType() {
        return "AND";
    }
}

