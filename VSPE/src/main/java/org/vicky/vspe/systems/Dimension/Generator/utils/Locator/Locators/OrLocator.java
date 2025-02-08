package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.ArrayList;
import java.util.List;

public class OrLocator implements Locator, Ymlable {
    private final List<Object> locators = new ArrayList<>();
    private final Context context;

    public OrLocator(Context context) {
        this.context = context;
    }

    public void addLocator(Ymlable locator) {
        this.locators.add(locator);
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        if (context.equals(Context.DISTRIBUTOR))
            builder.append("distributors: ").append("\n");
        if (context.equals(Context.LOCATOR))
            builder.append("locators: ").append("\n");

        for (Object locator : this.locators) {
            if (locator instanceof Locator) {
                builder.append("  - type: ").append(((Locator) locator).getType()).append("\n").append(Utilities.getIndentedBlock(((Ymlable) locator).getYml().toString(), "    ")).append("\n");
            } else if (locator instanceof NoiseSampler) {
                builder.append(Utilities.getIndentedBlock(((NoiseSampler) locator).getYml().toString(), "   ")).append("\n");
            }
        }

        return builder;
    }

    @Override
    public String getType() {
        return "OR";
    }
}
