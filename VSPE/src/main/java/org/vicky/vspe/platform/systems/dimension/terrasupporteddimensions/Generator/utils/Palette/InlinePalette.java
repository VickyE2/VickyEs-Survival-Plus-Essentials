package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette;

import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.MetaClass;

import java.util.ArrayList;
import java.util.List;

public class InlinePalette implements BasePalette {
    public final List<String> inlinePaletteVariables = new ArrayList<>();

    public List<String> getInlinePaletteVariables() {
        return this.inlinePaletteVariables;
    }

    public void addPaletteElement(PlatformMaterial material, int layerHeight) {
        if (material.isSolid()) {
            String palette = "BLOCK:" + material.getResourceLocation() + ": " + layerHeight;
            this.inlinePaletteVariables.add(palette);
        }
    }

    public void addPaletteElement(MetaClass.MetaVariables metaVariable) {
        this.inlinePaletteVariables.add("<< meta.yml:" + metaVariable.name().toLowerCase().replaceAll("[^a-z]", "-"));
    }

    public void addPaletteElement(Palette palette, int layerHeight) {
        this.inlinePaletteVariables.add(palette.id + ": " + layerHeight);
    }
}
