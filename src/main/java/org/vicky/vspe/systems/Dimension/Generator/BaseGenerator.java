package org.vicky.vspe.systems.Dimension.Generator;

import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.BaseExtendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Land;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Temperature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.Extrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.DepositableFeature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.Base;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.BasePalette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.InlinePalette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.BaseStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.DepositableStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.ClimateVariant;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.Variant;
import org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.ProgressListener;
import org.vicky.vspe.utilities.ZipUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Meta.HeightTemperatureRarityCalculationMethod.SEPARATE;
import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.*;

public abstract class BaseGenerator {

    private final String packID;
    private final String packVersion;
    private final String packAuthors;

    private final List<BaseBiome> BIOMES;
    private final List<String> CUSTOM_STAGES;
    private final List<Extrusion> CUSTOM_EXTRUSIONS;
    private final Map<BiomeType, Integer> biomeTypeIntegerMap = new HashMap<>();
    private final Map<String, List<String>> colorMap = new HashMap<>();
    private final StringBuilder distributionYML = new StringBuilder();
    private final StringBuilder defaultDeposits = new StringBuilder();
    private final StringBuilder defaultOres = new StringBuilder();
    private final List<Feature> INCLUDED_FEATURES;
    private final List<Palette> INCLUDED_PALETTES;
    private final List<DepositableStructure> INCLUDED_DEPOSITABLES;
    private final List<DepositableStructure> INCLUDED_ORES;
    private final List<BaseStructure> INCLUDED_STRUCTURES;
    private final List<BaseExtendibles> INCLUDED_CUSTOM_BASE_EXTENDIBLES;
    private final Map<BiomeVariant, List<Variant>> variantMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> groupedBiomes = new HashMap<>();
    private final Map<BiomeVariant, Rarity> biomeRarityMap = new HashMap<>();
    private final Map<ClimateVariant, List<Variant>> climateVariantListMap = new HashMap<>();
    private final Map<ClimateVariant, BiomeVariant> climateVariantBiomeVariantMap = new HashMap<>();
    private final Map<ClimateVariant, Rarity> climateVariantRarityMap = new HashMap<>();
    private final Map<String, Set<String>> temperatureToHeightMap = new HashMap<>();
    private final Set<BaseBiome> biomeSet = new HashSet<>();
    private final Map<BaseBiome, Map<BiomeType, Rarity>> contextBiomeRarityMap = new HashMap<>();
    public MetaClass META;
    public Base BASE;
    Map<String, String> INCLUDED_COLOR_MAP;
    Map<Class<? extends BiomeType>, Map<Temperature, Integer>> biomeTypeTemperatureMap = new HashMap<>();
    private final Map<BiomeType, Integer> coastalBiomeMap = new HashMap<>();
    private final Set<BiomeType> biomeTypes = new HashSet<>();


    public BaseGenerator(@NotNull String packID, String packVersion, String packAuthors) {
        this.packID = packID.toUpperCase().replaceAll("^A-Z", "_");
        this.packVersion = packVersion;
        this.packAuthors = packAuthors;
        this.BIOMES = new ArrayList<>();
        this.CUSTOM_STAGES = new ArrayList<>();
        this.CUSTOM_EXTRUSIONS = new ArrayList<>();
        this.META = null;
        this.BASE = null;

        this.INCLUDED_CUSTOM_BASE_EXTENDIBLES = new ArrayList<>();
        this.INCLUDED_FEATURES = new ArrayList<>();
        this.INCLUDED_PALETTES = new ArrayList<>();
        this.INCLUDED_STRUCTURES = new ArrayList<>();
        this.INCLUDED_COLOR_MAP = new HashMap<>();
        this.INCLUDED_DEPOSITABLES = new ArrayList<>();
        this.INCLUDED_ORES = new ArrayList<>();

        defaultDeposits.append("""
                id: DEPOSITS_DEFAULT
                type: BIOME
                abstract: true
                """);

        defaultOres.append("""
                id: ORES_DEFAULT
                type: BIOME
                abstract: true
                """);

    }

    private static void addEntry(Map<String, List<String>> map, String key, String name) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(name);
    }

    public String getGeneratorName() {
        return "Terra:" + packID;
    }

    public void addBiome(BaseBiome biome) {
        BIOMES.add(biome);
    }

    public void addExtrusion(Extrusion extrusion) {
        CUSTOM_EXTRUSIONS.add(extrusion);
    }

    public void addStage(String stage) {
        CUSTOM_STAGES.add(stage);
    }

    public void setMeta(MetaClass META) {
        this.META = META;
    }

    public void setBase(Base BASE) {
        this.BASE = BASE;
    }

    public void generatePack(ProgressListener progressListener) throws IOException {
        int progress = 0;

        progressListener.onProgressUpdate(progress, "Starting pack generation.");

        File temporaryZip = createTemporaryZipFile();
        ZipUtils utils = new ZipUtils(new ZipOutputStream(new FileOutputStream(temporaryZip)));

        progress += 5;
        progressListener.onProgressUpdate(progress, "Temporary zip file created.");

        Map<String, String> resourceMappings = Map.of(
                "biomes/", ("assets/systems/dimension/biomes/"),
                "math/", ("assets/systems/dimension/math/"),
                "biome-distribution/", ("assets/systems/dimension/biome-providers/"),
                "palettes/", ("assets/systems/dimension/palettes/"),
                "structures/", ("assets/systems/dimension/structures/"),
                "features/", ("assets/systems/dimension/features/")
        );
        int totalSteps = resourceMappings.size();
        for (Map.Entry<String, String> entry : resourceMappings.entrySet()) {
            String resourcePath = entry.getValue();
            int secondLastSlashIndex = resourcePath.lastIndexOf("/", resourcePath.lastIndexOf("/") - 1);
            String lastTrailingFolder = resourcePath.substring(secondLastSlashIndex + 1, resourcePath.lastIndexOf("/"));

            utils.addResourceDirectoryToZip(entry.getValue(), entry.getKey(), lastTrailingFolder);
            progress += 10 / totalSteps;
            progressListener.onProgressUpdate(progress, "Resources written: " + entry.getKey());
        }

        utils.writeResourceToZip("assets/systems/dimension/customization.yml", "customization.yml");

        // Depositable Features
        Map<DepositableFeature.Type, Map<String, StringBuilder>> map = getDepositableFeaturesYml();
        for (Map.Entry<DepositableFeature.Type, Map<String, StringBuilder>> entry : map.entrySet()) {
            if (entry.getKey().equals(DepositableFeature.Type.DEPOSIT)) {
                writeMultipleYmlFiles(utils, entry.getValue(), "features/deposits/deposits/");
            } else {
                writeMultipleYmlFiles(utils, entry.getValue(), "features/deposits/ores/");
            }
        }
        progress += 10;
        progressListener.onProgressUpdate(progress, "Depositable features written to features folder.");

        utils.writeStringToBaseDirectory(getPackYml().toString(), "pack.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "pack.yml written.");

        utils.writeStringToBaseDirectory(getMetaYML().toString(), "meta.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Meta class written.");

        writeYamlToZip(utils, getBaseYML(), "biomes/abstract/", "base.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Base YAML written.");

        writeYamlToZip(utils, defaultDeposits, "biomes/abstract/features/deposits/deposits/", "deposits_default.yml");
        writeYamlToZip(utils, defaultOres, "biomes/abstract/features/deposits/ores/", "ores_default.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Default deposits and ores written.");

        writeMultipleYmlFiles(utils, getBiomesYml(), "biomes/");
        writeYamlToZip(utils, generateColorsFile(), "biomes/", "colors.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Biome colors saved to colors.yml file");

        if (!outputBiomeVariants().isEmpty())
            writeYamlToZip(utils, outputBiomeVariants(), "biome-distribution/stages/", "add_variants.yml");
        progress += 10;
        progressListener.onProgressUpdate(progress, "Biomes and sources written.");

        if (!getPalettesYml().isEmpty()) {
            writeMultipleYmlFiles(utils, getPalettesYml(), "palettes/");
            progress += 10;
            progressListener.onProgressUpdate(progress, "Biome Palettes saved");
        }

        if (!generateCoasts().isEmpty()) {
            writeYamlToZip(utils, generateCoasts(), "biome-distribution/stages/", "coasts.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "Coasts stage produced");

            if (!generateFillCoasts().isEmpty()) {
                writeYamlToZip(utils, generateFillCoasts(), "biome-distribution/stages/", "fill_coasts.yml");
                progress += 10;
                progressListener.onProgressUpdate(progress, "Fill Coasts stage produced");
            }
        }

        if (progress >= 100)
            progress -= 25;

        if (!generateOceans().isEmpty()) {
            writeYamlToZip(utils, generateOceans(), "biome-distribution/stages/", "oceans.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "Oceans stage produced");
        }

        if (!generateFTZ().isEmpty()) {
            writeYamlToZip(utils, generateFTZ(), "biome-distribution/stages/", "fill_temperature_zones.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "FTZ stage produced");
        }

        if (!generateSTZ().isEmpty()) {
            writeYamlToZip(utils, generateSTZ(), "biome-distribution/stages/", "spread_temperature_zones.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "STZ stage produced");
        }

        writeYamlToZip(utils, generatePreset(), "biome-distribution/presets/", "default.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Default biome provider written.");

        if (progress >= 90)
            progress = 75;
        writeMultipleYmlFiles(utils, generateExtrusions(), "biome-distribution/extrusions/");
        writeMultipleYmlFiles(utils, getFeaturesYml(), "features/");

        for (Map.Entry<String, Object> structure : getStructuresYml().entrySet()) {
            String directory = "structures/";
            if (structure.getValue() instanceof String) {
                writeYamlToZip(utils, new StringBuilder(structure.getValue().toString()), directory, structure.getKey() + ".tesf");
            } else if (structure.getValue() instanceof File) {
                utils.writeFileToZip((File) structure.getValue(), directory);
            }
        }
        progress += 20;
        progressListener.onProgressUpdate(progress, "Structures and final files written.");
        utils.close();

        handleFinalFileOperations(temporaryZip);
        progress = 100;
        progressListener.onProgressUpdate(progress, "Pack generation complete.");
    }

    public void writeMultipleYmlFiles(ZipUtils zipUtils, Map<String, StringBuilder> entries, String folder) {
        for (Map.Entry<String, StringBuilder> file : entries.entrySet()) {
            writeYamlToZip(zipUtils, file.getValue(), folder, file.getKey() + ".yml");
        }
    }

    private void writeYamlToZip(ZipUtils zipUtils, StringBuilder content, String path, String fileName) {
        try {
            zipUtils.writeStringToZip(content.toString(), path, fileName);
        } catch (Exception e) {
            VSPE.getInstancedLogger().severe("Failed to write to zip: " + e);
        }
    }

    private void handleFinalFileOperations(File temporaryZip) throws IOException {
        File renamedFile = new File(temporaryZip.getParent(), packID + "-" + packVersion + ".zip");
        if (!temporaryZip.renameTo(renamedFile)) {
            VSPE.getInstancedLogger().info("Failed to rename file");
        }

        File copiedFile = new File(VSPE.getPlugin().getDataFolder().getParent(), "Terra/packs/" + renamedFile.getName());
        Files.copy(renamedFile.toPath(), copiedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        VSPE.getInstancedLogger().info("File moved to Terra @ " + copiedFile.getPath());

        if (renamedFile.delete()) {
            VSPE.getInstancedLogger().info("Temporary file deleted.");
        } else {
            VSPE.getInstancedLogger().info("Failed to delete the original file.");
        }
    }

    @NotNull
    private StringBuilder getPackYml() {
        StringBuilder packYml = new StringBuilder();
        packYml.append("id: ").append(packID).append("\n");
        packYml.append("version: ").append(packVersion).append("\n");
        packYml.append("author: ").append(packAuthors).append("\n");
        packYml.append(
                """
                        addons:
                          biome-provider-pipeline-v2: "1.+"
                          biome-provider-single: "1.+"
                          biome-provider-extrusion: "1.+"
                          chunk-generator-noise-3d: "1.+"
                          config-biome: "1.+"
                          config-flora: "1.+"
                          config-noise-function: "1.+"
                          config-ore: "1.+"
                          config-palette: "1.+"
                          config-distributors: "1.+"
                          config-locators: "1.+"
                          config-feature: "1.+"
                          structure-terrascript-loader: "1.+"
                          structure-sponge-loader: "1.+"
                          language-yaml: "1.+"
                          generation-stage-feature: "1.+"
                          structure-function-check-noise-3d: "1.+"
                          palette-block-shortcut: "1.+"
                          structure-block-shortcut: "1.+"
                          terrascript-function-sampler: "1.+"
                            
                        generator: NOISE_3D
                                        
                        biomes: $biome-distribution/presets/default.yml:biomes
                            
                        stages:
                          - id: global-preprocessors
                            type: FEATURE
                          - id: preprocessors
                            type: FEATURE
                          - id: landforms
                            type: FEATURE
                          - id: slabs
                            type: FEATURE
                          - id: ores
                            type: FEATURE
                          - id: deposits
                            type: FEATURE
                          - id: river-decoration
                            type: FEATURE
                          - id: trees
                            type: FEATURE
                          - id: underwater-flora
                            type: FEATURE
                          - id: flora
                            type: FEATURE
                          - id: postprocessors
                            type: FEATURE
                        functions:
                          '<<':
                            - 'math/functions/terrace.yml:functions'
                            - 'math/functions/interpolation.yml:functions'
                            - 'math/functions/maskSmooth.yml:functions'
                            - 'math/functions/clamp.yml:functions'
                        samplers:
                          '<<':
                            - 'math/samplers/terrain.yml:samplers'
                            - 'math/samplers/simplex.yml:samplers'
                            - 'math/samplers/continents.yml:samplers'
                            - 'math/samplers/precipitation.yml:samplers'
                            - 'math/samplers/temperature.yml:samplers'
                            - 'math/samplers/rivers.yml:samplers'
                            - 'math/samplers/spots.yml:samplers'
                        blend:
                          palette:
                            resolution: 2
                            amplitude: 2
                            sampler:
                              type: WHITE_NOISE
                        slant:
                          calculation-method: DotProduct
                                        
                        """
        ).append("\n");
        return packYml;
    }

    @NotNull
    private StringBuilder getMetaYML() {
        return META.getYml();
    }

    @NotNull
    private StringBuilder getBaseYML() {
        StringBuilder baseYML = new StringBuilder();
        baseYML.append("""
                        id: BASE
                        type: BIOME
                        abstract: true
                        extends:
                          - DEPOSITS_DEFAULT
                          - ORES_DEFAULT
                        """)
                .append("slant-depth: ").append(BASE.getSlantDepth()).append("\n")
                .append("""
                        ocean:
                          palette: BLOCK:minecraft:water
                          """)
                .append("  level: ").append(BASE.getOceanLevel()).append("\n")
                .append("""
                        features:
                        """);
        if (!BASE.getEnabledPreProcessor().isEmpty()) {
            baseYML.append("  global-preprocessors: \n");
            for (GlobalPreprocessor preprocessor : BASE.getEnabledPreProcessor()) {
                baseYML.append("   - ").append(preprocessor).append("\n");
            }
        }

        return baseYML;
    }

    @NotNull
    private Map<String, StringBuilder> getBiomesYml() {
        Map<String, StringBuilder> biomes = new HashMap<>();
        Map<String, BaseBiome> biomeIds = new HashMap<>();

        for (BaseBiome biome : BIOMES) {
            addEntry(colorMap, biome.getBiomeColor(), biome.getId());
            if (biomeIds.containsKey(biome.getId())) {
                VSPE.getInstancedLogger().warning("Biome " + biome.getClass() + " has identical cleaned id with biome" + biomeIds.get(biome.getId()).getClass() + ". Please resolve this issue as we will try but things might(will) be broken");
                biome.setID(getCleanedID(biome.getUncleanedId() + "_cleaned_from_" + Utilities.generateRandomFourLetterString()));
            }
            else {
                biomeIds.put(biome.getId(), biome);
            }
        }

        generateColorsFile();
        for (BaseBiome biome : BIOMES) {
            StringBuilder builder = new StringBuilder();
            builder.append("id: ").append(biome.getId()).append("\n");
            builder.append("type: BIOME").append("\n");
            if (!biome.customExtendibles.isEmpty() || !biome.extendibles.isEmpty() || !biome.biomeExtendibles.isEmpty()) {
                builder.append("extends: [ ");
                boolean firstElement = true;

                List<BaseExtendibles> customExtendibles = biome.getCustomExtendibles();
                for (BaseExtendibles customExtendible : customExtendibles) {
                    if (!firstElement) {
                        builder.append(", ");
                    }
                    builder.append(customExtendible.getId());
                    firstElement = false;
                    INCLUDED_CUSTOM_BASE_EXTENDIBLES.add(customExtendible);
                }

                List<String> biomeExtendibles = biome.getBiomeExtendibles();
                for (String biomeExtendible : biomeExtendibles) {
                    if (!firstElement) {
                        builder.append(", ");
                    }
                    builder.append(biomeExtendible);
                    firstElement = false;
                }

                List<Extendibles> extendibles = biome.getExtendibles();
                for (Extendibles extendible : extendibles) {
                    if (!firstElement) {
                        builder.append(", ");
                    }
                    builder.append(extendible);
                    firstElement = false;
                }

                builder.append(" ]").append("\n");
            }
            builder.append("vanilla: ").append(biome.getBiome()).append("\n");
            builder.append("color: $biomes/colors.yml:").append(INCLUDED_COLOR_MAP.get(biome.getBiomeColor())).append("\n");
            if (!biome.getTags().isEmpty()) {
                builder.append("tags: ").append("\n");
                for (Tags tag : biome.getTags()) {
                    builder.append(" - ").append(tag).append("\n");
                }
            }
            if (!biome.getColors().isEmpty()) {
                builder.append("colors: ").append("\n");
                for (Map.Entry<Colorable, String> entry : biome.colors.entrySet())
                    builder.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            if (!biome.palettes.isEmpty()) {
                builder.append("palette: ").append("\n");
                for (Map.Entry<Palette, Object> entry : biome.getPalettes().entrySet()) {
                    builder.append(" - ").append(entry.getKey().id).append(": ").append(entry.getValue()).append("\n");
                    INCLUDED_PALETTES.add(entry.getKey());
                }
                builder.append(" - << meta.yml:palette-bottom").append("\n");
            }
            if (!biome.slant.isEmpty()) {
                builder.append("slant: ").append("\n");
                for (Map.Entry<Integer, Map<BasePalette, Integer>> entry : biome.getSlant().entrySet()) {
                    builder.append(" - threshold: ").append(entry.getKey()).append("\n");
                    builder.append("   palette: ").append("\n");
                    for (Map.Entry<BasePalette, Integer> palette : entry.getValue().entrySet()) {
                        if (palette.getKey() instanceof InlinePalette) {
                            for (String string : ((InlinePalette) palette.getKey()).inlinePaletteVariables)
                                builder.append("   - ").append(string).append("\n");
                        } else if (palette.getKey() instanceof Palette) {
                            builder.append("   - ").append(((Palette) palette.getKey()).id).append("\n");
                            INCLUDED_PALETTES.add((Palette) palette.getKey());
                        }
                    }
                }
            }
            if (!biome.features.isEmpty()) {
                builder.append("features: ").append("\n");
                for (Map.Entry<Featureable, List<Feature>> entry : biome.getFeatures().entrySet()) {
                    builder.append("  ").append(entry.getKey().toString().toLowerCase()).append(": ").append("\n");
                    for (Feature features : entry.getValue()) {
                        builder.append("   - ").append(features.getId()).append("\n");
                        if (INCLUDED_FEATURES.stream().noneMatch(feature -> feature.getClass() == features.getClass())) {
                            INCLUDED_FEATURES.add(features);
                        }
                    }
                }
            }
            if (biome instanceof Variant variant) {
                if (variant.getVariantOf() != null) {
                    BiomeVariant biomeVariant = variant.getVariantOf();
                    Rarity rarity = variant.getVariantRarity();

                    variantMap.computeIfAbsent(biomeVariant, k -> new ArrayList<>()).add(variant);
                    biomeRarityMap.put(biomeVariant, rarity);
                    VSPE.getInstancedLogger().info("Added Variant: " + variant + " to BiomeVariant: " + biomeVariant);
                }
                if (variant.getClimateVariantOf() != null) {
                    ClimateVariant climateVariant = variant.getClimateVariantOf();
                    Rarity rarity = variant.getVariantRarity();

                    climateVariantListMap.computeIfAbsent(climateVariant, k -> new ArrayList<>()).add(variant);
                    climateVariantRarityMap.put(climateVariant, rarity);
                    VSPE.getInstancedLogger().info("Added Variant: " + variant + " to ClimateVariant: " + climateVariant);
                }
            } else if (biome instanceof BiomeVariant biomeVariant) {
                String variantName = biomeVariant.getVariantName();
                Rarity selfRarity = biomeVariant.getSelfRarity();
                biomeRarityMap.putIfAbsent(biomeVariant, selfRarity);

                VSPE.getInstancedLogger().info("Added BiomeVariant: " + variantName + " with rarity: " + selfRarity);
            } else if (biome instanceof ClimateVariant climateVariant) {
                String variantName = climateVariant.getVariantName();
                Rarity selfRarity = climateVariant.getSelfRarity();
                climateVariantRarityMap.putIfAbsent(climateVariant, selfRarity);

                VSPE.getInstancedLogger().info("Added ClimateVariant: " + variantName + " with rarity: " + selfRarity);
            }
            if (biome instanceof BiomeVariant biomeVariant && biome instanceof Variant variant) {
                if (variant.getClimateVariantOf() != null) {
                    climateVariantBiomeVariantMap.put(variant.getClimateVariantOf(), biomeVariant);
                }
            }

            if (biome.biomeType.isCoast()) {
                coastalBiomeMap.put(biome.biomeType, biomeTypeIntegerMap.getOrDefault(biome.biomeType, 0) + 1);
            } else {
                BiomeType type = biome.biomeType;
                Temperature temperature = Temperature.valueOf(type.getTemperate());
                if (type.isCoast()) {
                    coastalBiomeMap.put(type, coastalBiomeMap.getOrDefault(biome.biomeType, 0) + 1);
                } else {
                    Map<Temperature, Integer> temperatureMap = biomeTypeTemperatureMap
                            .computeIfAbsent(type.getClass(), k -> new HashMap<>());
                    temperatureMap.merge(temperature, 1, Integer::sum);
                }
            }

            contextBiomeRarityMap.put(biome, Map.of(biome.biomeType, biome.rarity));
            biomeTypes.add(biome.biomeType);
            biomeSet.add(biome);

            biomes.put(biome.getId(), builder);
        }
        return biomes;
    }

    @NotNull
    private StringBuilder generateColorsFile() {
        StringBuilder builder = new StringBuilder();

        builder.append("DRIPSTONE_CAVES: 0x1c1a11").append("\n");
        for (Map.Entry<String, List<String>> entry : colorMap.entrySet()) {
            String key = entry.getKey();
            String commonName = generateCommonName(entry.getValue());

            INCLUDED_COLOR_MAP.put(key, commonName);
            builder.append(commonName).append(": ").append(key).append("\n");
        }

        return builder;
    }

    @NotNull
    private Map<String, StringBuilder> getFeaturesYml() {
        Map<String, StringBuilder> builderMap = new HashMap<>();

        for (Feature feature : INCLUDED_FEATURES) {
            StringBuilder builder = new StringBuilder();

            builder.append("id: ").append(feature.getId()).append("\n");
            builder.append("type: FEATURE").append("\n");
            if (feature.getDistributor() != null) {
                builder.append("distributor: ").append("\n");
                if (feature.getDistributor() instanceof Locator) {
                    builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");
                } else if (feature.getDistributor() instanceof NoiseSampler) {
                    builder.append("type: ").append(((NoiseSampler) feature.getDistributor()).name()).append("\n");
                    builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");
                }
            }
            if (feature.getLocator() != null) {
                builder.append("locator: ").append("\n");
                builder.append(getIndentedBlock(feature.getLocator().getYml().toString(), "  ")).append("\n");
            }
            builder.append("structures: ").append("\n");
            if (feature.getStructureDistributor() != null) {
                builder.append("  distribution: ").append("\n");
                builder.append(getIndentedBlock(feature.getStructureDistributor().getYml().toString(), "    ")).append("\n");
            }
            if (!feature.getStructures().isEmpty()) {
                builder.append("  structures: ").append("\n");
                for (Map.Entry<Object, Integer> structure : feature.getStructures().entrySet()) {
                    if (structure.getKey() instanceof BaseStructure) {
                        if (INCLUDED_STRUCTURES.stream().noneMatch(structure1 -> structure1.getClass() == structure.getKey().getClass())) {
                            INCLUDED_STRUCTURES.add((BaseStructure) structure.getKey());
                            builder.append("   - ")
                                    .append((((BaseStructure) structure.getKey()).schemFileName != null) ? ((BaseStructure) structure.getKey()).schemFileName : ((BaseStructure) structure.getKey()).id);
                        }
                    } else
                        builder.append("   - ").append(structure.getKey());
                    builder.append(": ").append(structure.getValue()).append("\n");
                }
            }

            builderMap.put(feature.getId(), builder);
        }

        return builderMap;
    }

    @NotNull
    private Map<String, StringBuilder> getPalettesYml() {
        Map<String, StringBuilder> builderMap = new HashMap<>();

        for (Palette palette : INCLUDED_PALETTES) {
            builderMap.put(palette.getId(), palette.getYml());
        }

        return builderMap;
    }

    @NotNull
    private Map<String, Object> getStructuresYml() {
        Map<String, Object> builderMap = new HashMap<>();

        for (BaseStructure structure : INCLUDED_STRUCTURES) {
            if (structure.schemFileName == null)
                builderMap.put(structure.id, structure.generateStructureScript());
            else
                builderMap.put(structure.id, structure.schemFileName);
        }

        return builderMap;
    }

    @NotNull
    private Map<DepositableFeature.Type, Map<String, StringBuilder>> getDepositableFeaturesYml() {
        Map<DepositableFeature.Type, Map<String, StringBuilder>> mainMap = new HashMap<>();
        Map<String, StringBuilder> builderDepositMap = new HashMap<>();
        Map<String, StringBuilder> builderOreMap = new HashMap<>();

        distributionYML.append("# [----------------------------- DEPOSITABLES -----------------------------]").append("\n");

        if (!BASE.getDefaultDeposits().isEmpty())
            defaultDeposits.append("""     
                    features:
                      deposits:
                    """);
        if (!BASE.getDefaultOres().isEmpty())
            defaultOres.append("""     
                    features:
                      ores:
                    """);

        for (DepositableFeature feature : BASE.getDefaultDeposits()) {

            distributionYML.append(feature.getId().toLowerCase().replaceAll("[^a-z]", "_")).append("\n");
            distributionYML.append("  averageCountPerChunk: ").append(feature.rarity.rarityValue).append("\n");
            distributionYML.append("  salt: ").append(feature.salt).append("\n");
            distributionYML.append("  range: ").append(feature.salt).append("\n");
            distributionYML.append("    max: ").append(feature.yLevelRange.getMax()).append("\n");
            distributionYML.append("    min: ").append(feature.yLevelRange.getMin()).append("\n");

            defaultDeposits.append("  - ").append(feature.getId()).append("\n");

            StringBuilder builder = new StringBuilder();

            builder.append("id: ").append(feature.getId()).append("\n");
            builder.append("type: FEATURE").append("\n");
            builder.append("anchors:").append("\n");
            if (!feature.anchorStructures.isEmpty()) {
                builder.append("&structure: ").append("\n");
                for (DepositableStructure structure : feature.anchorStructures)
                    builder.append(" - ").append(structure.getId()).append("\n");
            }
            builder.append("""
                      - &densityThreshold 1/256 * ${features/deposits/distribution.yml:clay.averageCountPerChunk} # Divide by 16^2 to get % per column
                      - &salt $features/deposits/distribution.yml:clay.salt
                      - &range $features/deposits/distribution.yml:clay.range
                      
                    distributor:
                      type: SAMPLER
                      sampler:
                        type: POSITIVE_WHITE_NOISE
                        salt: *salt
                      threshold: *densityThreshold
                    """).append("\n");

            builder.append("locator: ").append("\n");
            builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");

            builder.append("""
                    structures:
                      distribution:
                        type: CONSTANT
                      structures: *structure
                    """);

            builderDepositMap.put(feature.getId(), builder);
            INCLUDED_DEPOSITABLES.addAll(feature.anchorStructures);
        }

        distributionYML.append("# [----------------------------- ORES -----------------------------]").append("\n");
        mainMap.put(DepositableFeature.Type.DEPOSIT, builderDepositMap);

        for (DepositableFeature feature : BASE.getDefaultOres()) {

            distributionYML.append(feature.getId().toLowerCase().replaceAll("[^a-z]", "_")).append("\n");
            distributionYML.append("  averageCountPerChunk: ").append(feature.rarity.rarityValue).append("\n");
            distributionYML.append("  salt: ").append(feature.salt).append("\n");
            distributionYML.append("  range: ").append(feature.salt).append("\n");
            distributionYML.append("    max: ").append(feature.yLevelRange.getMax()).append("\n");
            distributionYML.append("    min: ").append(feature.yLevelRange.getMin()).append("\n");

            defaultOres.append("  - ").append(feature.getId()).append("\n");

            StringBuilder builder = new StringBuilder();

            builder.append("anchors:").append("\n");
            if (!feature.anchorStructures.isEmpty()) {
                builder.append("&structure: ").append("\n");
                for (DepositableStructure structure : feature.anchorStructures)
                    builder.append(" - ").append(structure.getId()).append("\n");
            }
            builder.append("""
                      - &densityThreshold 1/256 * ${features/deposits/distribution.yml:gold.averageCountPerChunk}
                      - &salt $features/deposits/distribution.yml:gold.salt
                      - &range $features/deposits/distribution.yml:gold.range
                      - &standard-deviation (${features/deposits/distribution.yml:gold.range.max}-${features/deposits/distribution.yml:gold.range.min})/6
                      
                    distributor:
                      type: SAMPLER
                      sampler:
                        type: POSITIVE_WHITE_NOISE
                        salt: *salt
                      threshold: *densityThreshold
                    """).append("\n");

            builder.append("locator: ").append("\n");
            builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");

            builder.append("""
                    structures:
                      distribution:
                        type: CONSTANT
                      structures: *structure
                    """);

            builderOreMap.put(feature.getId(), builder);
            INCLUDED_ORES.addAll(feature.anchorStructures);
        }

        mainMap.put(DepositableFeature.Type.ORE, builderOreMap);

        return mainMap;
    }

    @NotNull
    private StringBuilder outputBiomeVariants() {
        StringBuilder output = new StringBuilder();
        Set<BiomeVariant> biomeVariantsInClimateVariants = new HashSet<>();
        Set<ClimateVariant> climateVariantsInBiomeVariants = new HashSet<>();

        if (!climateVariantListMap.isEmpty()) {
            output.append(""" 
                    stages:
                      - type: REPLACE_LIST
                        to:
                    """);

            for (Map.Entry<ClimateVariant, List<Variant>> entry : climateVariantListMap.entrySet()) {
                for (Map.Entry<ClimateVariant, BiomeVariant> variant : climateVariantBiomeVariantMap.entrySet()) {
                    if (Objects.equals(variant.getKey().getVariantName(), entry.getKey().getVariantName())) {
                        output.append("     ").append(variant.getValue().getVariantName().toUpperCase())
                                .append(": ").append(variant.getValue().getSelfRarity()).append("\n");
                        biomeVariantsInClimateVariants.add(variant.getValue());
                        climateVariantsInBiomeVariants.add(variant.getKey());
                    }
                }

                if (climateVariantsInBiomeVariants.stream().noneMatch(climateVariant -> climateVariant == entry.getKey())) {
                    output.append("     ").append(entry.getKey().getVariantName().toLowerCase().replaceAll("[^a-z]", "-")).append("variants:\n");
                    for (Variant variant : entry.getValue()) {
                        output.append("     - ").append(variant.getVariantName().toLowerCase().replaceAll("[^a-z]", "-")).append("-microvariants: ")
                                .append(variant.getVariantRarity().rarityValue).append("\n");
                    }
                }
            }

            output.append("""
                        sampler:
                          dimensions: 2
                          type: CELLULAR
                          return: CellValue
                          salt: 42342
                          frequency: 0.06 / ${meta.yml:biome-distribution.global-scale}
                    """);
        }
        if (!variantMap.isEmpty()) {
            output.append("""
                    - type: REPLACE_LIST
                      to:
                      """);
            for (Map.Entry<BiomeVariant, List<Variant>> entry : variantMap.entrySet()) {
                if (biomeVariantsInClimateVariants.contains(entry.getKey())) {
                    output.append("     - ").append(entry.getKey().getVariantName().toUpperCase()).append("\n");
                    output.append("      - SELF: ").append(biomeRarityMap.get(entry.getKey()).rarityValue).append("\n");
                    for (Variant variant : entry.getValue()) {
                        output.append("      - ").append(variant.getVariantName().toUpperCase()).append(": ")
                                .append(variant.getVariantRarity().rarityValue).append("\n");
                    }
                } else {
                    // If biome variant is not associated with climate, treat it as a microvariant
                    output.append("     ").append(entry.getKey().getVariantName().toLowerCase().replaceAll("[^a-z]", "-")).append("-microvariants:\n");
                    output.append("      - SELF: ").append(biomeRarityMap.get(entry.getKey()).rarityValue).append("\n");
                    for (Variant variant : entry.getValue()) {
                        output.append("      - ").append(variant.getVariantName().toUpperCase()).append(": ")
                                .append(variant.getVariantRarity().rarityValue).append("\n");
                    }
                }
            }

            output.append("""
                        sampler:
                          dimensions: 2
                          type: CELLULAR
                          return: CellValue
                          frequency: 0.3 / ${meta.yml:biome-distribution.global-scale}
                          salt: 34534
                    """);
        }

        return output;
    }

    @NotNull
    private Map<String, StringBuilder> generateExtrusions() {
        Map<String, StringBuilder> builders = new HashMap<>();
        for (Extrusion extrusion : CUSTOM_EXTRUSIONS) {
            if (extrusion instanceof ReplaceExtrusion) {
                builders.put(((ReplaceExtrusion) extrusion).getId(), ((ReplaceExtrusion) extrusion).getYml());
            }
        }
        return builders;
    }

    @NotNull
    private StringBuilder generatePreset() {
        StringBuilder builder = new StringBuilder();

        builder.append("""
                biomes:
                  type: EXTRUSION
                  extrusions:
                """);
        if (!CUSTOM_EXTRUSIONS.isEmpty()) {
            builder.append("  extrusions:").append("\n");
            for (Extrusion extrusion : CUSTOM_EXTRUSIONS) {
                if (extrusion instanceof ReplaceExtrusion) {
                    builder.append("   - << biome-distribution/extrusions/").append(((ReplaceExtrusion) extrusion).getId()).append(".yml:extrusions").append("\n");
                }
            }
        }
        builder.append("""
                  provider:
                    type: PIPELINE
                    resolution: 4
                    blend:
                      amplitude: 6
                      sampler:
                        type: OPEN_SIMPLEX_2
                        frequency: 0.012
                    pipeline:
                      source:
                        type: SAMPLER
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: NoiseLookup
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                          lookup:
                            type: EXPRESSION
                            expression: continents(x, z)
                        biomes:
                """);
        int oceanSize = 0;
        int deepSeaSize = 0;
        int landSize = 0;

        for (BaseBiome entry : biomeSet) {
            BiomeType biome = entry.biomeType;
            int count = 1;

            // Check the parent type and conditions
            if (biome instanceof Ocean) {
                oceanSize += count;
            } else if (biome instanceof Deep_Ocean) {
                deepSeaSize += count;
            } else if (biome instanceof Land) {
                if (biome instanceof Hills_Small && biome.isCoast()) {
                    continue;
                }
                if (biome instanceof Flat && biome.isCoast()) {
                    continue;
                }
                if (biome instanceof Hills_Large && biome.isCoast()) {
                    continue;
                }
                if (biome instanceof Mountains_Large && biome.isCoast()) {
                    continue;
                }
                if (biome instanceof Mountains_Small && biome.isCoast()) {
                    continue;
                }
                landSize += count;
            }
        }
        if (deepSeaSize != 0)
            builder.append("          deep-ocean: ").append(deepSeaSize).append("\n");
        if (oceanSize != 0)
            builder.append("          ocean: ").append(oceanSize).append("\n");
        if (landSize != 0)
            builder.append("          land: ").append(landSize).append("\n");
        builder.append("      stages:").append("\n");
        if (!generateCoasts().isEmpty()) {
            builder.append("""
                        - << biome-distribution/stages/coasts.yml:stages
                        - << biome-distribution/stages/fill_coasts.yml:stages
                """);
        }
        if (!generateOceans().isEmpty())
            builder.append("        - << biome-distribution/stages/oceans.yml:stages").append("\n");
        if (!generateSTZ().isEmpty()) {
            builder.append("""
                        - << biome-distribution/stages/spread_temperature_zones.yml:stages
                        - << biome-distribution/stages/fill_temperature_zones.yml:stages
                """);
        }
        builder.append("""
                        - type: FRACTAL_EXPAND
                          sampler:
                            type: WHITE_NOISE
                        - type: SMOOTH
                          sampler:
                            type: WHITE_NOISE
                       #- << biome-distribution/stages/add_rivers.yml:stages
                        - type: SMOOTH
                          sampler:
                            type: WHITE_NOISE
                """);

        return builder;
    }

    @NotNull
    private StringBuilder generateCoasts() {
        StringBuilder builder = new StringBuilder();
        Map<String, Integer> temperateRarity = new HashMap<>();
        if (META.calculationMethod.equals(SEPARATE)) {
            temperateRarity = generateTemperatureRarityMap();
        }else{
            temperateRarity = generateRarityMap();
        }
        Map<String, Integer> biomeAmount = getBiomeAmount();
        Set<String> biomesPresent = getBiomesPresent();
        Set<String> coastPresent = new HashSet<>();
        boolean coastSmallExists = false;
        boolean coastLargeExists = false;


        if (biomesPresent.contains("OCEAN") || biomesPresent.contains("DEEP_OCEAN")) {
            if (biomesPresent.contains("COAST_SMALL") || biomesPresent.contains("COAST_LARGE")) {
                builder.append("stages:").append("\n");
                if (biomesPresent.contains("OCEAN")) {
                    builder.append("""
                              - type: REPLACE # split oceans into small and big coast sections
                                from: ocean
                                sampler:
                                  type: CELLULAR
                                  jitter: ${customization.yml:biomeSpread.cellJitter}
                                  return: CellValue
                                  frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                                to:
                                  SELF: 1
                            """);
                    if (biomesPresent.contains("COAST_SMALL")) {
                        builder.append("      ocean_coast_small: 2").append("\n");
                        coastSmallExists = true;
                    }
                    if (biomesPresent.contains("COAST_LARGE")) {
                        builder.append("      ocean_coast_large: 1").append("\n");
                        coastLargeExists = true;
                    }

                    if (coastSmallExists) {
                        builder.append("""
                                  - type: REPLACE # add small coasts
                                    from: ocean_coast_small
                                    sampler:
                                      type: EXPRESSION
                                      expression: continentBorderCelledSmall(x, z)
                                    to:
                                      SELF: 1
                                      coast_small: 1
                                """);
                        builder.append("""
                                  - type: REPLACE # make small oceans placeholders oceans again
                                    from: ocean_coast_small
                                    sampler:
                                      type: CONSTANT
                                    to:
                                      ocean: 1
                                """);
                    }

                    if (coastLargeExists) {
                        builder.append("""
                                  - type: REPLACE # add small coasts
                                    from: ocean_coast_wide
                                    sampler:
                                      type: EXPRESSION
                                      expression: continentBorderCelledSmall(x, z)
                                    to:
                                      SELF: 1
                                      coast_wide: 1
                                """);
                        builder.append("""
                                  - type: REPLACE # make small oceans placeholders oceans again
                                    from: ocean_coast_wide
                                    sampler:
                                      type: CONSTANT
                                    to:
                                      ocean: 1
                                """);
                    }
                }
            }

        }

        return builder;
    }

    @NotNull
    private StringBuilder generateFillCoasts() {
        StringBuilder builder = new StringBuilder();
        Map<String, Integer> temperateRarity = new HashMap<>();
        if (META.calculationMethod.equals(SEPARATE)) {
            temperateRarity = generateTemperatureRarityMap();
        }else{
            temperateRarity = generateRarityMap();
        }
        Map<String, Integer> biomeAmount = getBiomeAmount();
        Set<String> biomesPresent = getBiomesPresent();
        Set<String> coastPresent = new HashSet<>();

        if (biomesPresent.contains("COAST_SMALL") || biomesPresent.contains("COAST_LARGE")) {
            builder.append("stages: ").append("\n");
            if (biomesPresent.contains("COAST_SMALL")) {
                builder.append("""
                          - type: REPLACE
                            from: coast_small
                            sampler:
                              type: CELLULAR
                              jitter: '${customization.yml:biomeSpread.cellJitter}'
                              return: NoiseLookup
                              frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                              lookup:
                                type: EXPRESSION
                                expression: 'temperature(x, z)'
                            to:
                        """);
                if (biomeAmount.containsKey("coast_small_boreal")) {
                    builder.append("      coast_small_boreal: ").append(temperateRarity.get("BOREAL")).append("\n");
                    coastPresent.add("coast_small_boreal");
                }
                if (biomeAmount.containsKey("coast_small_polar")) {
                    builder.append("      coast_small_polar: ").append(temperateRarity.get("POLAR")).append("\n");
                    coastPresent.add("coast_small_polar");
                }
                if (biomeAmount.containsKey("coast_small_subtropical")) {
                    builder.append("      coast_small_subtropical: ").append(temperateRarity.get("SUBTROPICAL")).append("\n");
                    coastPresent.add("coast_small_subtropical");
                }
                if (biomeAmount.containsKey("coast_small_tropical")) {
                    builder.append("      coast_small_tropical: ").append(temperateRarity.get("TROPICAL")).append("\n");
                    coastPresent.add("coast_small_tropical");
                }
                if (biomeAmount.containsKey("coast_small_temperate")) {
                    builder.append("      coast_small_temperate: ").append(temperateRarity.get("TEMPERATE")).append("\n");
                    coastPresent.add("coast_small_temperate");
                }
            }
            if (biomesPresent.contains("COAST_LARGE")) {
                builder.append("""
                          - type: REPLACE
                            from: coast_wide
                            sampler:
                              type: CELLULAR
                              jitter: '${customization.yml:biomeSpread.cellJitter}'
                              return: NoiseLookup
                              frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                              lookup:
                                type: EXPRESSION
                                expression: 'temperature(x, z)'
                            to:
                        """);
                if (biomeAmount.containsKey("coast_large_boreal")) {
                    builder.append("      coast_large_boreal: ").append(temperateRarity.get("BOREAL")).append("\n");
                    coastPresent.add("coast_large_boreal");
                }
                if (biomeAmount.containsKey("coast_large_polar")) {
                    builder.append("      coast_large_polar: ").append(temperateRarity.get("POLAR")).append("\n");
                    coastPresent.add("coast_large_polar");
                }
                if (biomeAmount.containsKey("coast_large_subtropical")) {
                    builder.append("      coast_large_subtropical: ").append(temperateRarity.get("SUBTROPICAL")).append("\n");
                    coastPresent.add("coast_large_subtropical");
                }
                if (biomeAmount.containsKey("coast_large_tropical")) {
                    builder.append("      coast_large_tropical: ").append(temperateRarity.get("TROPICAL")).append("\n");
                    coastPresent.add("coast_large_tropical");
                }
                if (biomeAmount.containsKey("coast_large_temperate")) {
                    builder.append("      coast_large_temperate: ").append(temperateRarity.get("TEMPERATE")).append("\n");
                    coastPresent.add("coast_large_temperate");
                }
            }

            if (biomesPresent.contains("COAST_SMALL")) {
                if (coastPresent.contains("coast_small_boreal")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_small_boreal
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_small_boreal"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_small_polar")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_small_polar
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_small_polar"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_small_subtropical")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_small_subtropical
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_small_subtropical"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_small_tropical")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_small_tropical
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_small_tropical"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_small_temperate")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_small_temperate
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_small_temperate"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }
            }

            if (biomesPresent.contains("COAST_LARGE")) {
                if (coastPresent.contains("coast_large_boreal")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_large_boreal
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_large_boreal"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_large_polar")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_large_polar
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_large_polar"))
                            builder.append("     ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_large_subtropical")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_large_subtropical
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_large_subtropical"))
                            builder.append("     ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_large_tropical")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_large_tropical
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_large_tropical"))
                            builder.append("     ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }

                if (coastPresent.contains("coast_large_temperate")) {
                    builder.append("""
                              - type: REPLACE
                                from: coast_large_temperate
                                sampler:
                                  type: CELLULAR
                                  jitter: '${customization.yml:biomeSpread.cellJitter}'
                                  return: CellValue
                                  frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'
                                  salt: 8726345
                                to:
                            """);
                    for (BaseBiome biome : biomeSet) {
                        if (biome.biomeType.getName().equalsIgnoreCase("coast_large_temperate"))
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                    }
                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateOceans() {
        StringBuilder builder = new StringBuilder();

        Map<String, Integer> temperateRarity;
        if (META.calculationMethod.equals(SEPARATE)) {
            temperateRarity = generateTemperatureRarityMap();
        }else{
            temperateRarity = generateRarityMap();
        }
        Map<String, Integer> biomeAmount = getBiomeAmountEC();
        Set<String> biomesPresent = getBiomesPresent();
        Set<String> oceansPresent = new HashSet<>();

        if (biomesPresent.contains("OCEAN")) {
            builder.append("""
                    stages:
                      - type: REPLACE
                        from: ocean
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: NoiseLookup
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                          lookup:
                            type: EXPRESSION\s
                            expression: temperature(x, z)
                        to:
                    """);
            if (biomeAmount.containsKey("ocean_boreal")) {
                builder.append("      ocean_boreal: ").append(temperateRarity.get("BOREAL")).append("\n");
                oceansPresent.add("ocean_boreal");
            }
            if (biomeAmount.containsKey("ocean_polar")) {
                builder.append("      ocean_polar: ").append(temperateRarity.get("POLAR")).append("\n");
                oceansPresent.add("ocean_polar");
            }
            if (biomeAmount.containsKey("ocean_subtropical")) {
                builder.append("      ocean_subtropical: ").append(temperateRarity.get("SUBTROPICAL")).append("\n");
                oceansPresent.add("ocean_subtropical");
            }
            if (biomeAmount.containsKey("ocean_tropical")) {
                builder.append("      ocean_tropical: ").append(temperateRarity.get("TROPICAL")).append("\n");
                oceansPresent.add("ocean_tropical");
            }
            if (biomeAmount.containsKey("ocean_temperate")) {
                builder.append("      ocean_temperate: ").append(temperateRarity.get("TEMPERATE")).append("\n");
                oceansPresent.add("ocean_temperate");
            }

            if (oceansPresent.contains("ocean_boreal")) {
                builder.append("""
                      - type: REPLACE
                        from: ocean_boreal
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: CellValue
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                        to:
                    """);
                for (BaseBiome biome : biomeSet) {
                    if (biome.biomeType.getName().equalsIgnoreCase("ocean_boreal"))
                        builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                }
            }

            if (oceansPresent.contains("ocean_polar")) {
                builder.append("""
                      - type: REPLACE
                        from: ocean_polar
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: CellValue
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                        to:
                    """);
                for (BaseBiome biome : biomeSet) {
                    if (biome.biomeType.getName().equalsIgnoreCase("ocean_polar"))
                        builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                }
            }

            if (oceansPresent.contains("ocean_subtropical")) {
                builder.append("""
                      - type: REPLACE
                        from: ocean_subtropical
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: CellValue
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                        to:
                    """);
                for (BaseBiome biome : biomeSet) {
                    if (biome.biomeType.getName().equalsIgnoreCase("ocean_subtropical"))
                        builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                }
            }

            if (oceansPresent.contains("ocean_tropical")) {
                builder.append("""
                      - type: REPLACE
                        from: ocean_tropical
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: CellValue
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                        to:
                    """);
                for (BaseBiome biome : biomeSet) {
                    if (biome.biomeType.getName().equalsIgnoreCase("ocean_tropical"))
                        builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                }
            }

            if (oceansPresent.contains("ocean_temperate")) {
                builder.append("""
                      - type: REPLACE
                        from: ocean_temperate
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: CellValue
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                        to:
                    """);
                for (BaseBiome biome : biomeSet) {
                    if (biome.biomeType.getName().equalsIgnoreCase("ocean_temperate"))
                        builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateSTZ() {
        StringBuilder builder = new StringBuilder();
        Map<String, Integer> temperateRarity;
        if (META.calculationMethod.equals(SEPARATE)) {
            temperateRarity = generateTemperatureRarityMap();
        }else{
            temperateRarity = generateRarityMap();
        }
        Map<String, Integer> heightRarity = generateLandHeightRarityMap();
        Map<String, Integer> biomeAmount = getBiomeAmountEC();
        Set<String> biomesPresent = getBiomesPresent();
        Set<String> temperatesPresent = new HashSet<>();

        if (getBiomesPresent().contains("LAND")) {
            builder.append("""
                    stages:
                      - type: REPLACE
                        from: land
                        sampler:
                          type: CELLULAR
                          jitter: ${customization.yml:biomeSpread.cellJitter}
                          return: NoiseLookup
                          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                          lookup:
                            type: EXPRESSION
                            expression: temperature(x, z)
                        to:
                    """);
            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("boreal"))) {
                builder.append("      boreal: ").append(temperateRarity.get("BOREAL")).append("\n");
                temperatesPresent.add("boreal");
                temperatureToHeightMap.put("BOREAL", new HashSet<>());
            }
            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("polar"))) {
                builder.append("      polar: ").append(temperateRarity.get("POLAR")).append("\n");
                temperatesPresent.add("polar");
                temperatureToHeightMap.put("POLAR", new HashSet<>());
            }
            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("subtropical"))) {
                builder.append("      subtropical: ").append(temperateRarity.get("SUBTROPICAL")).append("\n");
                temperatesPresent.add("subtropical");
                temperatureToHeightMap.put("SUBTROPICAL", new HashSet<>());
            }
            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("tropical"))) {
                builder.append("      tropical: ").append(temperateRarity.get("TROPICAL")).append("\n");
                temperatesPresent.add("tropical");
                temperatureToHeightMap.put("TROPICAL", new HashSet<>());
            }
            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("temperate"))) {
                builder.append("      temperate: ").append(temperateRarity.get("TEMPERATE")).append("\n");
                temperatesPresent.add("temperate");
                temperatureToHeightMap.put("TEMPERATE", new HashSet<>());
            }

            if (temperatesPresent.contains("boreal")) {
                builder.append("""
                          - type: REPLACE
                            from: boreal
                            sampler:
                              type: CELLULAR
                              jitter: ${customization.yml:biomeSpread.cellJitter}
                              return: NoiseLookup
                              frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                              lookup:
                                type: EXPRESSION
                                expression: temperature(x, z)
                            to:
                        """);
                if (biomesPresent.contains("HILLS_SMALL_BOREAL")) {
                    builder.append("      hills_small_boreal: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("hills_small");
                }
                if (biomesPresent.contains("HILLS_LARGE_BOREAL")) {
                    builder.append("      hills_large_boreal: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("hills_large");
                }
                if (biomesPresent.contains("MOUNTAINS_SMALL_BOREAL")) {
                    builder.append("      mountains_small_boreal: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("mountains_small");
                }
                if (biomesPresent.contains("MOUNTAINS_LARGE_BOREAL")) {
                    builder.append("      mountains_large_boreal: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("mountains_large");
                }
                if (biomesPresent.contains("FLAT_BOREAL")) {
                    builder.append("      flat_boreal: ").append(heightRarity.get("FLAT")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("flat");
                }
            }
            if (temperatesPresent.contains("polar")) {
                builder.append("""
                          - type: REPLACE
                            from: polar
                            sampler:
                              type: CELLULAR
                              jitter: ${customization.yml:biomeSpread.cellJitter}
                              return: NoiseLookup
                              frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                              lookup:
                                type: EXPRESSION
                                expression: temperature(x, z)
                            to:
                        """);
                if (biomesPresent.contains("HILLS_SMALL_POLAR")) {
                    builder.append("      hills_small_polar: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("hills_small");
                }
                if (biomesPresent.contains("HILLS_LARGE_POLAR")) {
                    builder.append("      hills_large_polar: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("hills_large");
                }
                if (biomesPresent.contains("MOUNTAINS_SMALL_POLAR")) {
                    builder.append("      mountains_small_polar: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("mountains_small");
                }
                if (biomesPresent.contains("MOUNTAINS_LARGE_POLAR")) {
                    builder.append("      mountains_large_polar: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("mountains_large");
                }
                if (biomesPresent.contains("FLAT_POLAR")) {
                    builder.append("      flat_polar: ").append(heightRarity.get("FLAT")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("flat");
                }
            }
            if (temperatesPresent.contains("subtropical")) {
                builder.append("""
                          - type: REPLACE
                            from: subtropical
                            sampler:
                              type: CELLULAR
                              jitter: ${customization.yml:biomeSpread.cellJitter}
                              return: NoiseLookup
                              frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                              lookup:
                                type: EXPRESSION
                                expression: temperature(x, z)
                            to:
                        """);
                if (biomesPresent.contains("HILLS_SMALL_SUBTROPICAL")) {
                    builder.append("      hills_small_subtropical: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("hills_small");
                }
                if (biomesPresent.contains("HILLS_LARGE_SUBTROPICAL")) {
                    builder.append("      hills_large_subtropical: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("hills_large");
                }
                if (biomesPresent.contains("MOUNTAINS_SMALL_SUBTROPICAL")) {
                    builder.append("      mountains_small_subtropical: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("mountains_small");
                }
                if (biomesPresent.contains("MOUNTAINS_LARGE_SUBTROPICAL")) {
                    builder.append("      mountains_large_subtropical: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("mountains_large");
                }
                if (biomesPresent.contains("FLAT_SUBTROPICAL")) {
                    builder.append("      flat_subtropical: ").append(heightRarity.get("FLAT")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("flat");
                }
            }
            if (temperatesPresent.contains("tropical")) {
                builder.append("""
                          - type: REPLACE
                            from: tropical
                            sampler:
                              type: CELLULAR
                              jitter: ${customization.yml:biomeSpread.cellJitter}
                              return: NoiseLookup
                              frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                              lookup:
                                type: EXPRESSION
                                expression: temperature(x, z)
                            to:
                        """);
                if (biomesPresent.contains("HILLS_SMALL_TROPICAL")) {
                    builder.append("      hills_small_tropical: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("hills_small");
                }
                if (biomesPresent.contains("HILLS_LARGE_TROPICAL")) {
                    builder.append("      hills_large_tropical: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("hills_large");
                }
                if (biomesPresent.contains("MOUNTAINS_SMALL_TROPICAL")) {
                    builder.append("      mountains_small_tropical: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("mountains_small");
                }
                if (biomesPresent.contains("MOUNTAINS_LARGE_TROPICAL")) {
                    builder.append("      mountains_large_tropical: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("mountains_large");
                }
                if (biomesPresent.contains("FLAT_TROPICAL")) {
                    builder.append("      flat_tropical: ").append(heightRarity.get("FLAT")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("flat");
                }
            }
            if (temperatesPresent.contains("temperate")) {
                builder.append("""
                          - type: REPLACE
                            from: temperate
                            sampler:
                              type: CELLULAR
                              jitter: ${customization.yml:biomeSpread.cellJitter}
                              return: NoiseLookup
                              frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                              lookup:
                                type: EXPRESSION
                                expression: temperature(x, z)
                            to:
                        """);
                if (biomesPresent.contains("HILLS_SMALL_TEMPERATE")) {
                    builder.append("      hills_small_temperate: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("hills_small");
                }
                if (biomesPresent.contains("HILLS_LARGE_TEMPERATE")) {
                    builder.append("      hills_large_temperate: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("hills_large");
                }
                if (biomesPresent.contains("MOUNTAINS_SMALL_TEMPERATE")) {
                    builder.append("      mountains_small_temperate: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("mountains_small");
                }
                if (biomesPresent.contains("MOUNTAINS_LARGE_TEMPERATE")) {
                    builder.append("      mountains_large_temperate: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("mountains_large");
                }
                if (biomesPresent.contains("FLAT_TEMPERATE")) {
                    builder.append("      flat_temperate: ").append(heightRarity.get("FLAT")).append("\n");
                    temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("flat");
                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateFTZ() {
        StringBuilder builder = new StringBuilder();

        if (!generateSTZ().isEmpty()) {
            builder.append("stages:").append("\n");
            for (Map.Entry<String, Set<String>> currentTH : temperatureToHeightMap.entrySet()) {
                for (String key : currentTH.getValue()) {
                    builder.append("""
                              - type: REPLACE
                            """);
                    String current = key + "_" + currentTH.getKey().toLowerCase();
                    builder.append("    from: ").append(current).append("\n");
                    builder.append("""
                                sampler:
                                  type: CELLULAR
                                  jitter: ${customization.yml:biomeSpread.cellJitter}
                                  return: CellValue
                                  frequency: 1 / ${customization.yml:biomeSpread.cellDistance}
                                to:
                            """);

                    for (BaseBiome biome : biomeSet) {
                        if (!biome.biomeType.isCoast() && biome.biomeType.getName().toLowerCase().equals(current)) {
                            builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                        }
                    }
                }
            }
        }

        return builder;
    }

    private Map<String, Integer> getBiomeAmount() {
        Map<String, Integer> biomeMap = new HashMap<>();

        biomeSet.forEach(biome -> {
                String simpleName = biome.biomeType.getName();

                simpleName = simpleName.toLowerCase();
                biomeMap.put(simpleName, biomeMap.getOrDefault(simpleName, 0) + 1);

        });

        return biomeMap;
    }

    private Map<String, Integer> getBiomeAmountEC() {
        Map<String, Integer> biomeMap = new HashMap<>();

        biomeSet.forEach(biome -> {
            if (!biome.biomeType.isCoast()) {
                String simpleName = biome.biomeType.getName();

                simpleName = simpleName.toLowerCase();
                biomeMap.put(simpleName, biomeMap.getOrDefault(simpleName, 0) + 1);
            }

        });

        return biomeMap;
    }

    private Set<String> getBiomesPresent() {
        Set<String> biomeMap = new HashSet<>();

        biomeSet.forEach(biome -> {
            String simpleName = biome.biomeType.getName().toLowerCase();

            if (simpleName.contains("coast")) {
                if (simpleName.contains("small")) {
                    biomeMap.add("COAST_SMALL");
                    if (simpleName.contains("boreal")) {
                        biomeMap.add("COAST_SMALL_BOREAL");
                    } else if (simpleName.contains("temperate")) {
                        biomeMap.add("COAST_SMALL_TEMPERATE");
                    } else if (simpleName.contains("polar")) {
                        biomeMap.add("COAST_SMALL_POLAR");
                    } else if (simpleName.contains("subtropical")) {
                        biomeMap.add("COAST_SMALL_SUBTROPICAL");
                    } else if (simpleName.contains("tropical")) {
                        biomeMap.add("COAST_SMALL_TROPICAL");
                    }
                    biomeMap.add("LAND");
                }
                if (simpleName.contains("large")) {
                    biomeMap.add("COAST_LARGE");
                    if (simpleName.contains("boreal")) {
                        biomeMap.add("COAST_LARGE_BOREAL");
                    } else if (simpleName.contains("temperate")) {
                        biomeMap.add("COAST_LARGE_TEMPERATE");
                    } else if (simpleName.contains("polar")) {
                        biomeMap.add("COAST_LARGE_POLAR");
                    } else if (simpleName.contains("subtropical")) {
                        biomeMap.add("COAST_LARGE_SUBTROPICAL");
                    } else if (simpleName.contains("tropical")) {
                        biomeMap.add("COAST_LARGE_TROPICAL");
                    }
                    biomeMap.add("LAND");
                }
            }
            else if (simpleName.contains("hills_small")) {
                biomeMap.add("HILLS_SMALL");
                if (simpleName.contains("boreal")) {
                    biomeMap.add("HILLS_SMALL_BOREAL");
                } else if (simpleName.contains("temperate")) {
                    biomeMap.add("HILLS_SMALL_TEMPERATE");
                } else if (simpleName.contains("polar")) {
                    biomeMap.add("HILLS_SMALL_POLAR");
                } else if (simpleName.contains("subtropical")) {
                    biomeMap.add("HILLS_SMALL_SUBTROPICAL");
                } else if (simpleName.contains("tropical")) {
                    biomeMap.add("HILLS_SMALL_TROPICAL");
                }
                biomeMap.add("LAND");
            }
            else if (simpleName.contains("hills_large")) {
                biomeMap.add("HILLS_LARGE");
                if (simpleName.contains("boreal")) {
                    biomeMap.add("HILLS_LARGE_BOREAL");
                } else if (simpleName.contains("temperate")) {
                    biomeMap.add("HILLS_LARGE_TEMPERATE");
                } else if (simpleName.contains("polar")) {
                    biomeMap.add("HILLS_LARGE_POLAR");
                } else if (simpleName.contains("subtropical")) {
                    biomeMap.add("HILLS_LARGE_SUBTROPICAL");
                } else if (simpleName.contains("tropical")) {
                    biomeMap.add("HILLS_LARGE_TROPICAL");
                }
                biomeMap.add("LAND");
            }
            else if (simpleName.contains("mountains_small")) {
                biomeMap.add("MOUNTAINS_SMALL");
                if (simpleName.contains("boreal")) {
                    biomeMap.add("MOUNTAINS_SMALL_BOREAL");
                } else if (simpleName.contains("temperate")) {
                    biomeMap.add("MOUNTAINS_SMALL_TEMPERATE");
                } else if (simpleName.contains("polar")) {
                    biomeMap.add("MOUNTAINS_SMALL_POLAR");
                } else if (simpleName.contains("subtropical")) {
                    biomeMap.add("MOUNTAINS_SMALL_SUBTROPICAL");
                } else if (simpleName.contains("tropical")) {
                    biomeMap.add("MOUNTAINS_SMALL_TROPICAL");
                }
                biomeMap.add("LAND");
            }
            else if (simpleName.contains("mountains_large")) {
                biomeMap.add("MOUNTAINS_LARGE");
                if (simpleName.contains("boreal")) {
                    biomeMap.add("MOUNTAINS_LARGE_BOREAL");
                } else if (simpleName.contains("temperate")) {
                    biomeMap.add("MOUNTAINS_LARGE_TEMPERATE");
                } else if (simpleName.contains("polar")) {
                    biomeMap.add("MOUNTAINS_LARGE_POLAR");
                } else if (simpleName.contains("subtropical")) {
                    biomeMap.add("MOUNTAINS_LARGE_SUBTROPICAL");
                } else if (simpleName.contains("tropical")) {
                    biomeMap.add("MOUNTAINS_LARGE_TROPICAL");
                }
                biomeMap.add("LAND");
            }
            else if (simpleName.contains("ocean")) {
                if (simpleName.contains("deep")) {
                    biomeMap.add("DEEP_OCEAN");
                } else {
                    biomeMap.add("OCEAN");
                }
            }
            else if (simpleName.contains("river")) {
                biomeMap.add("RIVER");
            }
            else if (simpleName.contains("flat")) {
                biomeMap.add("FLAT");
                if (simpleName.contains("boreal")) {
                    biomeMap.add("FLAT_BOREAL");
                } else if (simpleName.contains("temperate")) {
                    biomeMap.add("FLAT_TEMPERATE");
                } else if (simpleName.contains("polar")) {
                    biomeMap.add("FLAT_POLAR");
                } else if (simpleName.contains("subtropical")) {
                    biomeMap.add("FLAT_SUBTROPICAL");
                } else if (simpleName.contains("tropical")) {
                    biomeMap.add("FLAT_TROPICAL");
                }
                biomeMap.add("LAND");
            }
        });

        return biomeMap;
    }

    private Map<String, Integer> generateTemperatureRarityMap() {
        Map<String, Integer> rarityMap = new LinkedHashMap<>();
        double coldness = META.temperatureVariance;
        coldness = (coldness * -1);

        Map<String, double[]> temperatureZones = Map.of(
                "TROPICAL", new double[]{20, 30},
                "TEMPERATE", new double[]{5, 20},
                "POLAR", new double[]{-50, 0},
                "BOREAL", new double[]{-10, 10},
                "SUBTROPICAL", new double[]{10, 25}
        );

        for (Map.Entry<String, double[]> entry : temperatureZones.entrySet()) {
            String zone = entry.getKey();
            double[] tempRange = entry.getValue();

            double tempCenter = (tempRange[0] + tempRange[1]) / 2;

            double suitability = Math.max(0, 1 - Math.abs(coldness - normalizeTemperature(tempCenter)));

            // Convert suitability to rarity (higher suitability = lower rarity)
            int rarity = (int) ((1 - suitability) * 10); // Scale rarity to 0-10
            rarityMap.put(zone, rarity);
        }

        return rarityMap;
    }

    private Map<String, Integer> generateHeightRarityMap() {
        Map<String, Integer> heightRarityMap = new LinkedHashMap<>();
        double elevation = META.heightVariance;

        // Define height zones
        Map<String, double[]> heightZones = Map.of(
                "TROPICAL", new double[]{5000, 7000},        // Elevated tropical
                "TEMPERATE", new double[]{1000, 3000},       // Typical temperate
                "POLAR", new double[]{0, 500},               // Low polar
                "BOREAL", new double[]{500, 1000},           // Boreal with moderate height
                "SUBTROPICAL", new double[]{3000, 5000}      // Subtropical ranges
        );

        // Calculate rarity based on height suitability
        for (Map.Entry<String, double[]> entry : heightZones.entrySet()) {
            String zone = entry.getKey();
            double[] heightRange = entry.getValue();

            double heightCenter = (heightRange[0] + heightRange[1]) / 2;

            double heightSuitability = calculateHeightSuitability(elevation, heightRange);

            int rarity = (int) ((heightSuitability) * 10);
            heightRarityMap.put(zone, rarity);
        }

        return heightRarityMap;
    }

    private Map<String, Integer> generateRarityMap() {
        Map<String, Integer> rarityMap = new LinkedHashMap<>();
        double coldness = META.temperatureVariance;
        double elevation = META.heightVariance;

        Map<String, double[][]> biomeZones = Map.of(
                "TROPICAL", new double[][]{{20, 30}, {5000, 7000}},
                "TEMPERATE", new double[][]{{5, 20}, {1000, 3000}},
                "POLAR", new double[][]{{-50, 0}, {0, 500}},
                "BOREAL", new double[][]{{-10, 10}, {500, 1000}},
                "SUBTROPICAL", new double[][]{{10, 25}, {3000, 5000}}
        );

        for (Map.Entry<String, double[][]> entry : biomeZones.entrySet()) {
            String zone = entry.getKey();
            double[] tempRange = entry.getValue()[0];
            double[] elevRange = entry.getValue()[1];

            double tempCenter = (tempRange[0] + tempRange[1]) / 2;

            double normalizedTemp = normalizeTemperature(tempCenter);
            double tempSuitability = Math.max(0, 1 - Math.abs(coldness - normalizedTemp));

            double elevSuitability = calculateHeightSuitability(elevation, elevRange);

            double suitability = (tempSuitability + elevSuitability) / 2;

            int rarity = (int) ((suitability) * 10);
            rarityMap.put(zone, rarity);
        }

        return rarityMap;
    }

    private Map<String, Integer> generateLandHeightRarityMap() {
        Map<String, Integer> heightRarityMap = new LinkedHashMap<>();
        double elevation = META.heightVariance;

        // Define elevation ranges for each biome type
        Map<String, double[]> elevationZones = Map.of(
                "MOUNTAINS_SMALL", new double[]{1000, 3700},      // Ideal range for small mountains
                "MOUNTAINS_LARGE", new double[]{3000, 7000},      // Ideal range for large mountains
                "HILLS_SMALL", new double[]{300, 1000},           // Ideal range for small hills
                "HILLS_LARGE", new double[]{800, 2500},          // Ideal range for large hills
                "FLAT", new double[]{0, 300}                      // Ideal range for flat lands
        );

        for (Map.Entry<String, double[]> entry : elevationZones.entrySet()) {
            String zone = entry.getKey();
            int rarity = getLHRarity(entry, elevation);
            heightRarityMap.put(zone, rarity);
        }

        return heightRarityMap;
    }

    private int getLHRarity(Map.Entry<String, double[]> entry, double elevation) {
        double[] elevationRange = entry.getValue();

        // Calculate the center of the elevation range
        double elevationCenter = (elevationRange[0] + elevationRange[1]) / 2;

        // Calculate suitability based on the current elevation
        double suitability = Math.max(0, 1 - Math.abs(elevation - normalizeHeight(elevationCenter)));

        // Convert suitability to rarity (higher suitability = lower rarity)
        return (int) ((1 - suitability) * 10);
    }

    private double normalizeHeight(double height) {
        // Assuming the highest possible elevation is 8000m and lowest is 0m (sea level)
        double minHeight = 0;
        double maxHeight = 8000;
        return (height - minHeight) / (maxHeight - minHeight) * 2 - 1;
    }

    private double normalizeTemperature(double temperature) {
        double minTemp = -50;
        double maxTemp = 30;
        return (temperature - minTemp) / (maxTemp - minTemp) * 2 - 1;
    }

    private double calculateHeightSuitability(double elevation, double[] elevRange) {
        double minElev = elevRange[0];
        double maxElev = elevRange[1];
        double elevCenter = (minElev + maxElev) / 2;
        return Math.max(0, 1 - Math.abs(elevation - elevCenter) / (maxElev - minElev));
    }


}
