package org.vicky.vspe.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.vicky.bukkitplatform.useables.BukkitBlockState;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.PrecipitationType;
import org.vicky.vspe.nms.BiomeCompatibilityAPI;
import org.vicky.vspe.nms.BiomeRegistry;
import org.vicky.vspe.nms.BiomeWrapper;
import org.vicky.vspe.nms.SpecialEffectsBuilder;
import org.vicky.vspe.nms.impl.BiomeWrapper_1_20_R4;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.ArrayList;
import java.util.List;

public class BukkitBiome implements PlatformBiome {
    private final Biome bukkitBiome;
    private final @NotNull String name;
    private final int biomeColor;
    private final int fogColor;
    private final int waterColor;
    private final int waterFogColor;
    private final int foliageColor;
    private final int skyColor;
    private final boolean isOcean;
    private final double temperature;
    private final double humidity;
    private final double elevation;
    private final double rainfall;
    private final @NotNull List<NoiseLayer> heightSampler;
    private final @NotNull BiomeCategory biomeCategory;
    private final @NotNull PrecipitationType precipitationType;
    private final @NotNull BiomeStructureData structureData;
    private final @NotNull List<BiomeFeature<BlockData>> features;
    private final @NotNull BiomeBlockDistributionPalette<BukkitBlockState> distributionPalette;
    private final @NotNull BiomeSpawnSettings spawnSettings;
    private final String identifier;
    private final boolean isMountainous;
    private final boolean isHumid;
    private final boolean isCold;
    private final BiomeWrapper_1_20_R4 wrapper;

    private BukkitBiome(Biome bukkitBiome, @NotNull String name, int biomeColor, int fogColor, int waterColor, int waterFogColor, int foliageColor, int skyColor, boolean isOcean, double temperature, double humidity, double elevation, double rainfall, @NotNull List<NoiseLayer> heightSampler, @NotNull BiomeCategory biomeCategory, @NotNull PrecipitationType precipitationType, @NotNull BiomeStructureData structureData, @NotNull List<BiomeFeature<BlockData>> features, @NotNull BiomeBlockDistributionPalette<BukkitBlockState> distributionPalette, @NotNull BiomeSpawnSettings spawnSettings, String identifier, boolean isMountainous, boolean isHumid, boolean isCold) {
        this.bukkitBiome = bukkitBiome;
        this.name = name;
        this.biomeColor = biomeColor;
        this.fogColor = fogColor;
        this.waterColor = waterColor;
        this.foliageColor = foliageColor;
        this.skyColor = skyColor;
        this.waterFogColor = waterFogColor;
        this.isOcean = isOcean;
        this.temperature = temperature;
        this.humidity = humidity;
        this.elevation = elevation;
        this.rainfall = rainfall;
        this.heightSampler = heightSampler;
        this.biomeCategory = biomeCategory;
        this.precipitationType = precipitationType;
        this.structureData = structureData;
        this.features = features;
        this.distributionPalette = distributionPalette;
        this.spawnSettings = spawnSettings;
        this.identifier = identifier;
        this.isMountainous = isMountainous;
        this.isHumid = isHumid;
        this.isCold = isCold;

        var splitted = getIdentifier().split(":");
        NamespacedKey key;
        if (splitted.length == 2) {
            key = new NamespacedKey(splitted[0], splitted[1]);
        } else {
            key = new NamespacedKey("vspe", getIdentifier());
        }

        BiomeWrapper baseWrapper = BiomeCompatibilityAPI.Companion.getBiomeCompatibility().createBiome(key,
                BiomeRegistry.getInstance().getBukkit(bukkitBiome));
        BiomeWrapper_1_20_R4 wrapper = (BiomeWrapper_1_20_R4) baseWrapper;

        // Apply special effects
        SpecialEffectsBuilder effects = wrapper.getSpecialEffects()
                .setFoliageColorOverride(getFoliageColor())
                .setSkyColor(getSkyColor())
                .setFogColor(getFogColor())
                .setWaterColor(getWaterColor())
                .setWaterFogColor(getWaterFogColor())
                .setGrassColorOverride(getBiomeColor());

        wrapper.setSpecialEffects(effects);
        wrapper.register(true);
        this.wrapper = wrapper;
    }
    private BukkitBiome(Builder builder) {
        this.bukkitBiome = builder.bukkitBiome;
        this.name = builder.name;
        this.biomeColor = builder.biomeColor;
        this.fogColor = builder.fogColor;
        this.waterColor = builder.waterColor;
        this.waterFogColor = builder.waterFogColor;
        this.foliageColor = builder.foliageColor;
        this.skyColor = builder.skyColor;
        this.isOcean = builder.isOcean;
        this.temperature = builder.temperature;
        this.humidity = builder.humidity;
        this.elevation = builder.elevation;
        this.rainfall = builder.rainfall;
        this.heightSampler = builder.heightSampler;
        this.biomeCategory = builder.biomeCategory;
        this.precipitationType = builder.precipitationType;
        this.structureData = builder.structureData;
        this.features = builder.features;
        this.distributionPalette = builder.distributionPalette;
        this.spawnSettings = builder.spawnSettings;
        this.identifier = builder.identifier;
        this.isMountainous = builder.isMountainous;
        this.isHumid = builder.isHumid;
        this.isCold = builder.isCold;

        var splitted = getIdentifier().split(":");
        NamespacedKey key;
        if (splitted.length == 2) {
            key = new NamespacedKey(splitted[0], splitted[1]);
        } else {
            key = new NamespacedKey("vspe", getIdentifier());
        }

        BiomeWrapper baseWrapper = BiomeCompatibilityAPI.Companion.getBiomeCompatibility().createBiome(key,
                BiomeRegistry.getInstance().getBukkit(bukkitBiome));
        BiomeWrapper_1_20_R4 wrapper = (BiomeWrapper_1_20_R4) baseWrapper;

        // Apply special effects
        SpecialEffectsBuilder effects = wrapper.getSpecialEffects()
                .setFoliageColorOverride(getFoliageColor())
                .setSkyColor(getSkyColor())
                .setFogColor(getFogColor())
                .setWaterColor(getWaterColor())
                .setWaterFogColor(getWaterFogColor())
                .setGrassColorOverride(getBiomeColor());

        wrapper.setSpecialEffects(effects);
        wrapper.register(true);
        this.wrapper = wrapper;
    }

    /* TODO -- Make this cleaner? */
    public static BukkitBiome fromBukkit(Biome biome) {
        return new BukkitBiome(
                biome,
                biome.name(),               // name
                0x91BD59,                   // biomeColor (fallback greenish)
                0xC0D8FF,                   // fogColor
                0x3F76E4,                   // waterColor
                0x050533,                   // waterFogColor
                0x20e417,                   // waterColor
                0x6fa5b3,                   // waterFogColor
                biome.name().contains("OCEAN"),
                0.8,                        // temperature (vanilla defaultish)
                0.4,                        // humidity
                0.125,                      // elevation
                0.4,                        // rainfall
                new ArrayList<>(),  // stub height sampler
                BiomeCategory.PLAINS,       // category fallback
                PrecipitationType.RAIN,     // fallback
                BiomeStructureData.EMPTY.INSTANCE,   // empty
                List.of(),                  // no features
                new BiomeBlockDistributionPalette<>(), // empty palette
                new BiomeSpawnSettings(),   // no spawns
                biome.getKey().toString(),  // identifier from key
                biome.name().contains("MOUNTAIN"),
                biome.name().contains("HUMID"),
                biome.name().contains("COLD")
        );
    }
    public static BukkitBiome fromParams(BiomeParameters biomeParameters) {
        return new BukkitBiome(
                Biome.PLAINS,
                biomeParameters.getName(),
                biomeParameters.getBiomeColor(),
                biomeParameters.getFogColor(),
                biomeParameters.getWaterColor(),
                biomeParameters.getWaterFogColor(),
                biomeParameters.getFoliageColor(),
                biomeParameters.getSkyColor(),
                biomeParameters.isOcean(),
                biomeParameters.getTemperature(),
                biomeParameters.getHumidity(),
                biomeParameters.getElevation(),
                biomeParameters.getRainfall(),
                biomeParameters.getHeightSampler(),
                biomeParameters.getCategory(),
                biomeParameters.getPrecipitation(),
                biomeParameters.getBiomeStructureData(),
                biomeParameters.getFeatures().stream()
                        .map(f -> (BiomeFeature<BlockData>) f)
                        .toList(),
                (BiomeBlockDistributionPalette<BukkitBlockState>) biomeParameters.getDistributionPalette(),
                biomeParameters.getSpawnSettings(),
                biomeParameters.getId(),
                biomeParameters.isMountainous(),
                biomeParameters.isHumid(),
                biomeParameters.isCold()
        );
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public int getBiomeColor() {
        return biomeColor;
    }

    @Override
    public int getFogColor() {
        return fogColor;
    }

    @Override
    public int getWaterColor() {
        return waterColor;
    }

    @Override
    public int getWaterFogColor() {
        return waterFogColor;
    }

    @Override
    public boolean isOcean() {
        return isOcean;
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public double getHumidity() {
        return humidity;
    }

    @Override
    public double getElevation() {
        return elevation;
    }

    @Override
    public double getRainfall() {
        return rainfall;
    }

    @Override
    public @NotNull BiomeCategory getCategory() {
        return biomeCategory;
    }

    @Override
    public @NotNull List<NoiseLayer> getHeightSampler() {
        return heightSampler;
    }

    @Override
    public @NotNull PrecipitationType getPrecipitation() {
        return precipitationType;
    }

    @Override
    public @NotNull BiomeStructureData getBiomeStructureData() {
        return structureData;
    }

    @Override
    public @NotNull BiomeBlockDistributionPalette<BukkitBlockState> getDistributionPalette() {
        return distributionPalette;
    }

    @Override
    public boolean isCold() {
        return isCold;
    }

    @Override
    public boolean isHumid() {
        return isHumid;
    }

    @Override
    public boolean isMountainous() {
        return isMountainous;
    }

    @Override
    public @NotNull List<BiomeFeature<?>> getFeatures() {
        return (List<BiomeFeature<?>>) (List<?>) features;
    }

    @Override
    public @NotNull BiomeSpawnSettings getSpawnSettings() {
        return spawnSettings;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int getSkyColor() {
        return skyColor;
    }

    @Override
    public int getFoliageColor() {
        return foliageColor;
    }

    @Override
    public @NotNull BiomeWrapper_1_20_R4 toNativeBiome() {
        return wrapper;
    }

    public static class Builder {
        private final String identifier;
        private Biome bukkitBiome = Biome.PLAINS;
        private String name = "";
        private int biomeColor = 0xAAFF00;
        private int fogColor = 0xEEEEEE;
        private int waterColor = 0x0088AA;
        private int waterFogColor = 0x0077BB;
        private int foliageColor = 0x26aa3e;
        private int skyColor = 0x0077BB;
        private boolean isOcean = false;
        private double temperature = 0.5;
        private double humidity = 0.5;
        private double elevation = 0.5;
        private double rainfall = 0.5;
        private List<NoiseLayer> heightSampler = new ArrayList<>();
        private BiomeCategory biomeCategory = BiomeCategory.PLAINS;
        private PrecipitationType precipitationType = PrecipitationType.RAIN;
        private BiomeStructureData structureData = BiomeStructureData.EMPTY.INSTANCE;
        private List<BiomeFeature<BlockData>> features = new ArrayList<>();
        private BiomeBlockDistributionPalette<BukkitBlockState> distributionPalette = new BiomeBlockDistributionPalette<>();
        private BiomeSpawnSettings spawnSettings = new BiomeSpawnSettings();
        private boolean isMountainous = false;
        private boolean isHumid = false;
        private boolean isCold = false;

        private Builder(String identifier) {
            this.identifier = identifier;
        }

        public static Builder of(String identifier) {
            return new Builder(identifier);
        }

        public Builder bukkitBiome(Biome biome) {
            this.bukkitBiome = biome;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder biomeColor(int color) {
            this.biomeColor = color;
            return this;
        }

        public Builder fogColor(int color) {
            this.fogColor = color;
            return this;
        }

        public Builder waterColor(int color) {
            this.waterColor = color;
            return this;
        }

        public Builder waterFogColor(int color) {
            this.waterFogColor = color;
            return this;
        }

        public Builder foliageColor(int color) {
            this.foliageColor = color;
            return this;
        }

        public Builder skyColor(int color) {
            this.skyColor = color;
            return this;
        }

        public Builder isOcean(boolean ocean) {
            this.isOcean = ocean;
            return this;
        }

        public Builder temperature(double temp) {
            this.temperature = temp;
            return this;
        }

        public Builder humidity(double hum) {
            this.humidity = hum;
            return this;
        }

        public Builder elevation(double elev) {
            this.elevation = elev;
            return this;
        }

        public Builder rainfall(double rain) {
            this.rainfall = rain;
            return this;
        }

        public Builder heightSampler(List<NoiseLayer> sampler) {
            this.heightSampler = sampler;
            return this;
        }

        public Builder addHeightSampler(List<NoiseLayer> sampler) {
            this.heightSampler.addAll(sampler);
            return this;
        }

        public Builder addHeightSampler(NoiseLayer sampler) {
            this.heightSampler.add(sampler);
            return this;
        }

        public Builder biomeCategory(BiomeCategory category) {
            this.biomeCategory = category;
            return this;
        }

        public Builder precipitationType(PrecipitationType type) {
            this.precipitationType = type;
            return this;
        }

        public Builder structureData(BiomeStructureData data) {
            this.structureData = data;
            return this;
        }

        public Builder features(List<BiomeFeature<BlockData>> features) {
            this.features = features;
            return this;
        }

        public Builder addFeature(BiomeFeature<BlockData> feature) {
            this.features.add(feature);
            return this;
        }

        public Builder distributionPalette(BiomeBlockDistributionPalette<BukkitBlockState> palette) {
            this.distributionPalette = palette;
            return this;
        }

        public Builder spawnSettings(BiomeSpawnSettings settings) {
            this.spawnSettings = settings;
            return this;
        }

        public Builder mountainous(boolean val) {
            this.isMountainous = val;
            return this;
        }

        public Builder humid(boolean val) {
            this.isHumid = val;
            return this;
        }

        public Builder cold(boolean val) {
            this.isCold = val;
            return this;
        }

        public BukkitBiome build() {
            return new BukkitBiome(this);
        }
    }
}
