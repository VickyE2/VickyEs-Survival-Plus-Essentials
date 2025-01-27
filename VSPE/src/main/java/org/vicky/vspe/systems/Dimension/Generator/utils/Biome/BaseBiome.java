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
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.BasePalette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;

import java.util.*;

public class BaseBiome {
    public final Map<Colorable, String> colors;
    public final Map<Palette, Object> palettes;
    public final Map<Featureable, List<Feature>> features;
    public final Map<Integer, Map<BasePalette, Integer>> slant;
    public final List<Extendibles> extendibles;
    public final List<Tags> tags;
    public final List<BaseExtendibles> customExtendibles;
    public final List<String> biomeExtendibles;
    private final String biomeColor;
    private final String biome;
    private final String uncleanedId;
    public BiomeVariant variantOf;
    public BiomeType biomeType;
    public Precipitation precipitaion;
    public PrecipitaionType precipitaionType;
    public Rarity rarity;
    public StringBuilder terrain;
    public boolean hasTerrain = false;
    public boolean isAbstract = false;
    private String id;
    private Ocean ocean = null;

    public BaseBiome(String id, Integer biomeColor, Biome biome, BiomeType biomeType, Precipitation precipitaion, Rarity rarity) {
        this.id = Utilities.getCleanedID(id);
        this.uncleanedId = id;
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
            if ((double) precipitaion.getPrecipitationAmount() > 0.75) {
                this.precipitaionType = PrecipitaionType.HUMID;
            }

            if ((double) precipitaion.getPrecipitationAmount() > 0.5 && (double) precipitaion.getPrecipitationAmount() <= 0.75) {
                this.precipitaionType = PrecipitaionType.SEMI_HUMID;
            }

            if ((double) precipitaion.getPrecipitationAmount() > 0.25 && (double) precipitaion.getPrecipitationAmount() <= 0.5) {
                this.precipitaionType = PrecipitaionType.SEMI_ARID;
            }

            if ((double) precipitaion.getPrecipitationAmount() <= 0.25) {
                this.precipitaionType = PrecipitaionType.ARID;
            }
        }
    }

    public void addColor(Colorable colorable, Integer color) {
        this.colors.put(colorable, "0x" + Integer.toHexString(color));
    }

    public Ocean getOcean() {
        return ocean;
    }

    public void setOcean(Ocean ocean) {
        this.ocean = ocean;
    }

    public void addTag(Tags tag) {
        this.tags.add(tag);
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
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

    public void setTerrain(StringBuilder terrain) {
        this.terrain = terrain;
        this.hasTerrain = true;
    }

    protected void addExtendibles(BaseBiome... extendibles) {
        for (BaseBiome extendible : extendibles) {
            this.biomeExtendibles.add(extendible.id.toLowerCase().replaceAll("[^a-z]", "_"));
        }
    }

    public void addExtendible(BaseExtendibles extendible) {
        this.customExtendibles.add(extendible);
    }

    public void addExtendible(BaseBiome extendible) {
        this.biomeExtendibles.add(extendible.id);
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

    public BiomeVariant getVariantOf() {
        return this.variantOf;
    }

    public void setVariantOf(BiomeVariant variantOf) {
        this.variantOf = variantOf;
    }

    public String getUncleanedId() {
        return this.uncleanedId;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getVariantName() {
        return this.id;
    }

    public static class Ocean {
        private Palette oceanMaterial;
        private int oceanLevel;

        public int getOceanLevel() {
            return oceanLevel;
        }

        public void setOceanLevel(int oceanLevel) {
            this.oceanLevel = oceanLevel;
        }

        public Palette getOceanMaterial() {
            return oceanMaterial;
        }

        public void setOceanMaterial(Palette oceanMaterial) {
            this.oceanMaterial = oceanMaterial;
        }

        public static class Builder {
            private final Ocean ocean = new Ocean();

            public Builder setOceanMaterial(Palette material) {
                ocean.setOceanMaterial(material);
                return this;
            }

            public Builder setOceanLevel(int oceanLevel) {
                ocean.setOceanLevel(oceanLevel);
                return this;
            }

            public Ocean build() {
                return ocean;
            }
        }
    }
}
