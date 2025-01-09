package org.vicky.vspe.systems.Dimension.Generator.utils.Palette;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;

import java.util.ArrayList;
import java.util.List;

public class InlinePalette implements BasePalette {

    public final List<String> inlinePaletteVariables;

    public InlinePalette() {
        this.inlinePaletteVariables = new ArrayList<>();
    }

    public List<String> getInlinePaletteVariables() {
        return inlinePaletteVariables;
    }

    public void addPaletteElement(Material material, int layerHeight) {
        if (material.isBlock()) {
            ItemStack itemStack = new ItemStack(material);
            String palette = "BLOCK:minecraft:" + itemStack.getItemMeta().getDisplayName().toLowerCase() + ": " + layerHeight;
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
