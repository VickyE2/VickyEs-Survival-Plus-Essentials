package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.BaseExtendibles;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Colorable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Feature;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Featureable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette.BasePalette;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette.Palette;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;

import java.util.*;

public class AbstractBiome {
    private final String uncleanedId;
    public Map<Colorable, String> colors;
    public Map<Palette, Object> palettes;
    public Map<Featureable, List<Feature>> features;
    public Map<Integer, Map<BasePalette, Integer>> slant;
    public List<Extendibles> extendibles;
    public List<Tags> tags;
    public List<BaseExtendibles> customExtendibles;
    public List<String> biomeExtendibles;
    public NoiseSampler terrain;
    public boolean hasTerrain = false;
    protected String id;
    private String biomeColor;
    private String biome;

    public AbstractBiome(String id) {
        this.id = Utilities.getCleanedID(id);
        this.uncleanedId = id;
        this.colors = new HashMap<>();
        this.palettes = new HashMap<>();
        this.features = new HashMap<>();
        this.slant = new HashMap<>();
        this.tags = new ArrayList<>();
        this.extendibles = new ArrayList<>();
        this.biomeExtendibles = new ArrayList<>();
        this.customExtendibles = new ArrayList<>();
    }

    public void addColor(Colorable colorable, Integer color) {
        this.colors.put(colorable, "0x" + Integer.toHexString(color));
    }

    public void addTag(Tags tag) {
        this.tags.add(tag);
    }

    public void addSlant(int threshold, Map<BasePalette, Integer> palette) {
        this.slant.put(threshold, palette);
    }

    public void addExtendible(Extendibles extendible) {
        this.extendibles.add(extendible);
    }

    protected void addExtendibles(Extendibles... extendibles) {
        this.extendibles.addAll(Arrays.asList(extendibles));
    }

    protected void addExtendibles(BaseExtendibles... extendibles) {
        this.customExtendibles.addAll(Arrays.asList(extendibles));
    }

    public void setTerrain(NoiseSampler terrain) {
        this.terrain = terrain;
        this.hasTerrain = true;
    }

    protected void addExtendibles(BaseBiome... extendibles) {
        for (BaseBiome extendible : extendibles) {
            this.biomeExtendibles.add(extendible.getId().toLowerCase().replaceAll("[^a-z]", "_"));
        }
    }

    public void addExtendible(BaseExtendibles extendible) {
        this.customExtendibles.add(extendible);
    }

    public void addExtendible(BaseBiome extendible) {
        this.biomeExtendibles.add(extendible.getId());
    }

    public void addPalettes(Palette palette, int height) {
        this.palettes.put(palette, height);
    }

    public void addPalettes(Palette palette, MetaMap height) {
        this.palettes.put(palette, height);
    }

    public void addFeaturesToParam(List<Feature> features, Featureable featureable) {
        this.features.put(featureable, features);
    }

    public String getId() {
        return this.id;
    }

    public String getBiomeColor() {
        return this.biomeColor;
    }

    public String getBiome() {
        return this.biome;
    }

    public List<String> getBiomeExtendibles() {
        return this.biomeExtendibles;
    }

    public List<BaseExtendibles> getCustomExtendibles() {
        return this.customExtendibles;
    }

    public List<Extendibles> getExtendibles() {
        return this.extendibles;
    }

    public Map<Palette, Object> getPalettes() {
        return this.palettes;
    }

    public Map<Colorable, String> getColors() {
        return this.colors;
    }

    public Map<Featureable, List<Feature>> getFeatures() {
        return this.features;
    }

    public Map<Integer, Map<BasePalette, Integer>> getSlant() {
        return this.slant;
    }

    public List<Tags> getTags() {
        return this.tags;
    }

    public String getUncleanedId() {
        return this.uncleanedId;
    }

    public void setID(String id) {
        this.id = id;
    }
}
