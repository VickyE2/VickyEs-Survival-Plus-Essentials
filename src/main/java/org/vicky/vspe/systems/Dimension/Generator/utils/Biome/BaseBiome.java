package org.vicky.vspe.systems.Dimension.Generator.utils.Biome;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.BaseExtendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.PrecipitaionType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.BasePalette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;

import java.util.*;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getCleanedID;

public class BaseBiome {

    public final Map<Colorable, String> colors;
    public final Map<Palette, Integer> palettes;
    public final Map<Featureable, List<Feature>> features;
    public final Map<Integer, Map<BasePalette, Integer>> slant;
    public final List<Extendibles> extendibles;
    public final List<Tags> tags;
    public final List<BaseExtendibles> customExtendibles;
    public final List<String> biomeExtendibles;
    private final String id;
    private final String biomeColor;
    private final String biome;
    public BiomeVariant variantOf;
    public BiomeType biomeType;
    public Precipitation precipitaion;
    public PrecipitaionType precipitaionType;
    public Rarity rarity;

    public BaseBiome(String id, Integer biomeColor, Biome biome, BiomeType biomeType, Precipitation precipitaion, Rarity rarity) {
        this.id = getCleanedID(id);
        this.biome = biome.getKey().asString();
        this.biomeColor = "0x" + Integer.toHexString(biomeColor);
        this.colors = new HashMap<>();
        this.palettes = new HashMap<>();
        this.features = new HashMap<>();
        this.slant = new HashMap<>();
        this.tags = new ArrayList<>();
        this.extendibles = new ArrayList<>();
        this.biomeExtendibles = new ArrayList<>();
        this.customExtendibles = new ArrayList<>();
        this.biomeType = biomeType;
        this.precipitaion = precipitaion;
        this.rarity = rarity;
        if (!precipitaion.getPrecipitationType().equals(Precipitation.PrecipitaionType.SNOW)) {
            if (precipitaion.getPrecipitationAmount() > 0.75)
                precipitaionType = PrecipitaionType.HUMID;
            if (precipitaion.getPrecipitationAmount() > 0.5 && precipitaion.getPrecipitationAmount() <= 0.75)
                precipitaionType = PrecipitaionType.SEMI_HUMID;
            if (precipitaion.getPrecipitationAmount() > 0.25 && precipitaion.getPrecipitationAmount() <= 0.5)
                precipitaionType = PrecipitaionType.SEMI_ARID;
            if (precipitaion.getPrecipitationAmount() <= 0.25)
                precipitaionType = PrecipitaionType.ARID;
        }
    }

    public void addColor(Colorable colorable, Integer color) {
        colors.put(colorable, "0x" + Integer.toHexString(color));
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

    protected void addExtendibles(BaseBiome... extendibles) {
        for (BaseBiome extendible : extendibles)
            this.biomeExtendibles.add(extendible.id.toLowerCase().replaceAll("[^a-z]", "_"));
    }

    public void addExtendible(BaseExtendibles extendible) {
        this.customExtendibles.add(extendible);
    }

    public void addExtendible(BaseBiome extendible) {
        this.biomeExtendibles.add(extendible.id.toLowerCase().replaceAll("[^a-z]", "_"));
    }

    public void addPalettes(Palette palette, int height) {
        palettes.put(palette, height);
    }

    public void addFeaturesToParam(List<Feature> features, Featureable featureable) {
        this.features.put(featureable, features);
    }

    public String getId() {
        return id;
    }

    public String getBiomeColor() {
        return biomeColor;
    }

    public String getBiome() {
        return biome;
    }

    public List<String> getBiomeExtendibles() {
        return biomeExtendibles;
    }

    public List<BaseExtendibles> getCustomExtendibles() {
        return customExtendibles;
    }

    public List<Extendibles> getExtendibles() {
        return extendibles;
    }

    public Map<Palette, Integer> getPalettes() {
        return palettes;
    }

    public Map<Colorable, String> getColors() {
        return colors;
    }

    public Map<Featureable, List<Feature>> getFeatures() {
        return features;
    }

    public Map<Integer, Map<BasePalette, Integer>> getSlant() {
        return slant;
    }

    public List<Tags> getTags() {
        return tags;
    }

    public BiomeVariant getVariantOf() {
        return variantOf;
    }

    public void setVariantOf(BiomeVariant variantOf) {
        this.variantOf = variantOf;
    }
}
