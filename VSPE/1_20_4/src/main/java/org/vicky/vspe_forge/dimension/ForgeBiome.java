package org.vicky.vspe_forge.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.vicky.forge.forgeplatform.useables.ForgePlatformBlockStateAdapter;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.PrecipitationType;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ForgeBiome implements PlatformBiome {
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
    private final @NotNull List<BiomeFeature<BlockState>> features;
    private final @NotNull BiomeBlockDistributionPalette<ForgePlatformBlockStateAdapter> distributionPalette;
    private final @NotNull BiomeSpawnSettings spawnSettings;
    private final String identifier;
    private final boolean isMountainous;
    private final boolean isHumid;
    private final boolean isCold;
    private final ResourceKey<Biome> resourceKey;
    private Biome nativeBiome;
    public final Function<BootstapContext<Biome>, Biome> funner =
            stap -> {
                var biome = buildNativeBiome(stap);
                this.nativeBiome = biome;
                return biome;
            };

    private ForgeBiome(Builder builder) {
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
        this.resourceKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(identifier));
    }

    public ForgeBiome(BiomeParameters params) {
        this.name = params.getName();
        this.biomeColor = params.getBiomeColor();
        this.fogColor = params.getFogColor();
        this.waterColor = params.getWaterColor();
        this.waterFogColor = params.getWaterFogColor();
        this.foliageColor = params.getFoliageColor();
        this.skyColor = params.getSkyColor();
        this.isOcean = params.isOcean();
        this.temperature = params.getTemperature();
        this.humidity = params.getHumidity();
        this.elevation = params.getElevation();
        this.rainfall = params.getRainfall();
        this.heightSampler = params.getHeightSampler();
        this.biomeCategory = params.getCategory();
        this.precipitationType = params.getPrecipitation();
        this.structureData = params.getBiomeStructureData();
        this.features = (List<BiomeFeature<BlockState>>) (List<?>) params.getFeatures();
        this.distributionPalette = (BiomeBlockDistributionPalette<ForgePlatformBlockStateAdapter>) params.getDistributionPalette();
        this.spawnSettings = params.getSpawnSettings();
        this.identifier = params.getId();
        this.isMountainous = params.isMountainous();
        this.isHumid = params.isHumid();
        this.isCold = params.isCold();
        this.resourceKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(identifier));
    }

    // Translate your fields to a vanilla Biome
    private Biome buildNativeBiome(BootstapContext<Biome> context) {
        // 1) Special effects
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .foliageColorOverride(this.foliageColor)
                .grassColorOverride(this.biomeColor)
                .waterColor(this.waterColor)
                .waterFogColor(this.waterFogColor)
                .skyColor(this.skyColor)
                .fogColor(this.fogColor)
                .build();

        BiomeGenerationSettings.Builder biomeBuilder =
                new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));

        // TODO: translate each Platform BiomeFeature<BlockState> to a ConfiguredFeature / PlacedFeature and add:
        // Example: genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, yourPlacedFeatureHolder);
        for (BiomeFeature<BlockState> f : this.features) {
            // Adapter.translateFeature(f, genBuilder); // implement this: map your feature -> configured/placed feature and add
        }

        // 3) Mob spawn settings
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        // TODO: translate spawnSettings into spawnBuilder (your Platform spawn model -> vanilla spawn entries)

        // 4) Precipitation mapping
        Biome.TemperatureModifier precip =
                precipitationType == PrecipitationType.SNOW ? Biome.TemperatureModifier.FROZEN :
                        Biome.TemperatureModifier.NONE;

        // 5) Build the biome
        return new Biome.BiomeBuilder()
                .temperature((float) this.temperature)
                .temperatureAdjustment(precip)
                .downfall((float) this.rainfall)
                .specialEffects(effects)
                .generationSettings(biomeBuilder.build())
                .mobSpawnSettings(spawnBuilder.build())
                .hasPrecipitation(true)
                .build();
    }

    @Override
    public @NotNull Biome toNativeBiome() {
        return this.nativeBiome;
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
    public @NotNull BiomeBlockDistributionPalette<ForgePlatformBlockStateAdapter> getDistributionPalette() {
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

    public ResourceKey<Biome> getResourceKey() {
        return resourceKey;
    }

    public static class Builder {
        private final String identifier;
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
        private List<BiomeFeature<BlockState>> features = new ArrayList<>();
        private BiomeBlockDistributionPalette<ForgePlatformBlockStateAdapter> distributionPalette = new BiomeBlockDistributionPalette<>();
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

        public Builder features(List<BiomeFeature<BlockState>> features) {
            this.features = features;
            return this;
        }

        public Builder addFeature(BiomeFeature<BlockState> feature) {
            this.features.add(feature);
            return this;
        }

        public Builder distributionPalette(BiomeBlockDistributionPalette<ForgePlatformBlockStateAdapter> palette) {
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

        public ForgeBiome build() {
            return new ForgeBiome(this);
        }
    }
}
