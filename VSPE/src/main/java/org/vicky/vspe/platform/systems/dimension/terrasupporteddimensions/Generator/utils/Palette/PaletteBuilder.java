package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;

import java.util.HashMap;
import java.util.Map;

public class PaletteBuilder {
    private final String id;
    private final Map<Object, Integer> layers;
    private NoiseSampler sampler;

    public PaletteBuilder(String id) {
        this.id = id;
        this.layers = new HashMap<>();
        this.sampler = null;
    }

    /**
     * Adds a layer with a map of materials and their respective weights.
     *
     * @param materials      A map of materials and weights.
     * @param layerThickness The thickness of the layer.
     * @return The builder instance for chaining.
     */
    public PaletteBuilder addLayer(Map<PlatformMaterial, Integer> materials, int layerThickness) {
        Map<ResourceLocation, Integer> instance = new HashMap<>();
        for (Map.Entry<PlatformMaterial, Integer> material : materials.entrySet()) {
            if (material.getKey().isSolid()) {
                instance.put(material.getKey().getResourceLocation(), material.getValue());
            }
        }
        this.layers.put(instance, layerThickness);
        return this;
    }

    /**
     * Adds a layer using a predefined `LayerClass`.
     *
     * @param layerClass The `LayerClass` instance.
     * @return The builder instance for chaining.
     */
    public PaletteBuilder addLayer(LayerClass layerClass) {
        this.layers.put(layerClass, 0);
        return this;
    }

    /**
     * Sets the noise sampler for the palette.
     *
     * @param sampler The `NoiseSampler` instance.
     * @return The builder instance for chaining.
     */
    public PaletteBuilder setSampler(NoiseSampler sampler) {
        this.sampler = sampler;
        return this;
    }

    /**
     * Builds the `Palette` instance with the specified configuration.
     *
     * @return A new `Palette` instance.
     */
    public Palette build() {
        Palette palette = new Palette(this.id);
        palette.getLayers().putAll(layers);
        if (this.sampler != null) {
            palette.setSampler(this.sampler.getYml());
        }
        return palette;
    }
}

