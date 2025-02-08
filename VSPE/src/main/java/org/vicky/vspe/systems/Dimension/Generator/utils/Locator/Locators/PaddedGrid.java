package org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators;

import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class PaddedGrid implements Locator, Ymlable {
    public int width = 0;
    public int padding = 0;
    public int salt = 0;

    public PaddedGrid() {
    }

    public void setSalt(int salt) {
        this.salt = salt;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();
        if (width != 0)
            builder.append("width: ").append(width).append("\n");
        if (width != 0)
            builder.append("padding: ").append(padding).append("\n");
        if (width != 0)
            builder.append("salt: ").append(salt).append("\n");
        return builder;
    }

    @Override
    public String getType() {
        return "PADDED_GRID";
    }
}
