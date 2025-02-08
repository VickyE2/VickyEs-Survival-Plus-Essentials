package org.vicky.vspe.systems.Dimension.Generator;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.systems.Dimension.Exceptions.MissingConfigrationException;
import org.vicky.vspe.systems.Dimension.Generator.utils.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.BaseExtendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.BiomeType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Land;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Temperature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.Extrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.DepositableFeature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.Configuration;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.HeightTemperatureRarityCalculationMethod;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSamplerBuilder;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.BasePalette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.InlinePalette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.DepositableStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.ClimateVariant;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.Variant;
import org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.ProgressListener;
import org.vicky.utilities.Identifiable;
import org.vicky.vspe.utilities.ZipUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipOutputStream;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.generateRandomNumber;
import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getIndentedBlock;

public abstract class BaseGenerator implements Identifiable {
    public final String packID;
    public final List<BaseBiome> BIOMES;
    private final String packVersion;
    private final String packAuthors;
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
    private final Map<BiomeType, Integer> coastalBiomeMap = new HashMap<>();
    private final Set<BiomeType> biomeTypes = new HashSet<>();
    public MetaClass META;
    public BaseClass BASEClass;
    public Configuration configuration;
    Map<String, String> INCLUDED_COLOR_MAP;
    Map<Class<? extends BiomeType>, Map<Temperature, Integer>> biomeTypeTemperatureMap = new HashMap<>();

    public BaseGenerator(@NotNull String packID, String packVersion, String packAuthors) {
        this.packID = packID.toUpperCase().replaceAll("^A-Z", "_");
        this.packVersion = packVersion;
        this.packAuthors = packAuthors;
        this.BIOMES = new ArrayList<>();
        this.CUSTOM_STAGES = new ArrayList<>();
        this.CUSTOM_EXTRUSIONS = new ArrayList<>();
        this.META = null;
        this.BASEClass = null;
        this.INCLUDED_CUSTOM_BASE_EXTENDIBLES = new ArrayList<>();
        this.INCLUDED_FEATURES = new ArrayList<>();
        this.INCLUDED_PALETTES = new ArrayList<>();
        this.INCLUDED_STRUCTURES = new ArrayList<>();
        this.INCLUDED_COLOR_MAP = new HashMap<>();
        this.INCLUDED_DEPOSITABLES = new ArrayList<>();
        this.INCLUDED_ORES = new ArrayList<>();
        this.defaultDeposits.append("id: DEPOSITS_DEFAULT\ntype: BIOME\nabstract: true\n");
        this.defaultOres.append("id: ORES_DEFAULT\ntype: BIOME\nabstract: true\n");
    }

    private static void addEntry(Map<String, List<String>> map, String key, String name) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(name);
    }

    public String getGeneratorName() {
        return "Terra:" + this.packID;
    }

    public void addBiome(BaseBiome... biomes) {
        this.BIOMES.addAll(Arrays.asList(biomes));
    }

    public void addExtrusion(Extrusion extrusion) {
        this.CUSTOM_EXTRUSIONS.add(extrusion);
    }

    public void addStage(String stage) {
        this.CUSTOM_STAGES.add(stage);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setMeta(MetaClass META) {
        this.META = META;
    }

    public void setBase(BaseClass BASEClass) {
        this.BASEClass = BASEClass;
    }

    public void generatePack(ProgressListener progressListener) throws IOException, MissingConfigrationException {
        if (META == null)
            throw new MissingConfigrationException("The generator " + packID + " fails to provide a META configuration");
        if (BASEClass == null)
            throw new MissingConfigrationException("The generator " + packID + " fails to provide a BASE configuration");
        if (configuration == null)
            throw new MissingConfigrationException("The generator " + packID + " fails to provide a COMMONS(Configuration) configuration");
        int progress = 0;
        progressListener.onProgressUpdate(progress, "Starting pack generation.");
        File temporaryZip = Utilities.createTemporaryZipFile();
        ZipUtils utils = new ZipUtils(new ZipOutputStream(new FileOutputStream(temporaryZip)));
        progress += 5;
        progressListener.onProgressUpdate(progress, "Temporary zip file created.");
        Map<String, String> resourceMappings = Map.of(
                "biomes/",
                "assets/systems/dimension/biomes/",
                "math/",
                "assets/systems/dimension/math/",
                "biome-distribution/",
                "assets/systems/dimension/biome-providers/",
                "palettes/",
                "assets/systems/dimension/palettes/",
                "structures/",
                "assets/systems/dimension/structures/",
                "features/",
                "assets/systems/dimension/features/"
        );
        int totalSteps = resourceMappings.size();

        for (Entry<String, String> entry : resourceMappings.entrySet()) {
            String resourcePath = entry.getValue();
            int secondLastSlashIndex = resourcePath.lastIndexOf("/", resourcePath.lastIndexOf("/") - 1);
            String lastTrailingFolder = resourcePath.substring(secondLastSlashIndex + 1, resourcePath.lastIndexOf("/"));
            utils.addResourceDirectoryToZip(entry.getValue(), entry.getKey(), lastTrailingFolder);
            progress += 10 / totalSteps;
            progressListener.onProgressUpdate(progress, "Resources written: " + entry.getKey());
        }
        Map<DepositableFeature.Type, Map<String, StringBuilder>> map = this.getDepositableFeaturesYml();

        for (Entry<DepositableFeature.Type, Map<String, StringBuilder>> entry : map.entrySet()) {
            if (entry.getKey().equals(DepositableFeature.Type.DEPOSIT)) {
                this.writeMultipleYmlFiles(utils, entry.getValue(), "features/deposits/deposits/");
            } else {
                this.writeMultipleYmlFiles(utils, entry.getValue(), "features/deposits/ores/");
            }
        }

        progress += 10;
        progressListener.onProgressUpdate(progress, "Depositable features written to features folder.");
        utils.writeStringToBaseDirectory(this.getPackYml().toString(), "pack.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "pack.yml written.");
        utils.writeStringToBaseDirectory(this.getMetaYML().toString(), "meta.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Meta class written.");
        utils.writeStringToBaseDirectory(this.configuration.getYml().toString(), "customization.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "configuration.yml written.");
        this.writeYamlToZip(utils, this.getBaseYML(), "biomes/abstract/", "base.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "BaseClass YAML written.");
        this.writeYamlToZip(utils, this.defaultDeposits, "biomes/abstract/features/deposits/deposits/", "deposits_default.yml");
        this.writeYamlToZip(utils, this.defaultOres, "biomes/abstract/features/deposits/ores/", "ores_default.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Default deposits and ores written.");
        if (progress >= 90) {
            progress = 75;
        }

        this.writeMultipleYmlFiles(utils, this.generateExtrusions(), "biome-distribution/extrusions/");

        for (Entry<String, Map<String, StringBuilder>> biomes : this.getBiomesYml().entrySet()) {
            this.writeMultipleYmlFiles(utils, biomes.getValue(), "biomes/" + biomes.getKey().toLowerCase() + "/");
        }

        this.writeYamlToZip(utils, this.generateColorsFile(), "biomes/", "colors.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Biome colors saved to colors.yml file");
        if (!this.outputBiomeVariants().isEmpty()) {
            this.writeYamlToZip(utils, this.outputBiomeVariants(), "biome-distribution/stages/", "add_variants.yml");
        }

        progress += 10;
        progressListener.onProgressUpdate(progress, "Biomes and sources written.");

        for (Entry<String, Map<String, StringBuilder>> features : this.getFeaturesYml().entrySet()) {
            this.writeMultipleYmlFiles(utils, features.getValue(), "features/" + features.getKey().toLowerCase() + "/");
        }

        if (!this.getPalettesYml().isEmpty()) {
            this.writeMultipleYmlFiles(utils, this.getPalettesYml(), "palettes/");
            progress += 10;
            progressListener.onProgressUpdate(progress, "Biome Palettes saved");
        }

        if (!this.generateCoasts().isEmpty()) {
            this.writeYamlToZip(utils, this.generateCoasts(), "biome-distribution/stages/", "coasts.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "Coasts stage produced");
            if (!this.generateFillCoasts().isEmpty()) {
                this.writeYamlToZip(utils, this.generateFillCoasts(), "biome-distribution/stages/", "fill_coasts.yml");
                progress += 10;
                progressListener.onProgressUpdate(progress, "Fill Coasts stage produced");
            }
        }

        if (progress >= 100) {
            progress -= 25;
        }

        if (!this.generateOceans().isEmpty()) {
            this.writeYamlToZip(utils, this.generateOceans(), "biome-distribution/stages/", "oceans.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "Oceans stage produced");
        }

        if (!this.generateFTZ().isEmpty()) {
            this.writeYamlToZip(utils, this.generateFTZ(), "biome-distribution/stages/", "fill_temperature_zones.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "FTZ stage produced");
        }

        if (!this.generateSTZ().isEmpty()) {
            this.writeYamlToZip(utils, this.generateSTZ(), "biome-distribution/stages/", "spread_temperature_zones.yml");
            progress += 10;
            progressListener.onProgressUpdate(progress, "STZ stage produced");
        }

        this.writeYamlToZip(utils, this.generatePreset(), "biome-distribution/presets/", "default.yml");
        progress += 5;
        progressListener.onProgressUpdate(progress, "Default biome provider written.");
        utils.close();
        this.handleFinalFileOperations(temporaryZip);
        int var21 = 100;
        progressListener.onProgressUpdate(var21, "Pack generation complete.");
    }

    public void writeMultipleYmlFiles(ZipUtils zipUtils, Map<String, StringBuilder> entries, String folder) {
        for (Entry<String, StringBuilder> file : entries.entrySet()) {
            this.writeYamlToZip(zipUtils, file.getValue(), folder, file.getKey() + ".yml");
        }
    }

    private void writeYamlToZip(ZipUtils zipUtils, StringBuilder content, String path, String fileName) {
        try {
            zipUtils.writeStringToZip(content.toString(), path, fileName);
        } catch (Exception var6) {
            Bukkit.getLogger().severe("Failed to write to zip: " + var6);
        }
    }

    private void handleFinalFileOperations(File temporaryZip) throws IOException {
        File renamedFile = new File(temporaryZip.getParent(), this.packID + ".zip");
        if (!temporaryZip.renameTo(renamedFile)) {
            Bukkit.getLogger().info("Failed to rename file");
        }

        File copiedFile = new File("./plugins/Terra/packs/" + renamedFile.getName());
        Files.copy(renamedFile.toPath(), copiedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Bukkit.getLogger().info("File moved to Terra @ " + copiedFile.getPath());
        if (renamedFile.delete()) {
            Bukkit.getLogger().info("Temporary file deleted.");
        } else {
            Bukkit.getLogger().info("Failed to delete the original file.");
        }
    }

    public String getPackID() {
        return this.packID;
    }

    public String getPackVersion() {
        return this.packVersion;
    }

    @NotNull
    private StringBuilder getPackYml() {
        StringBuilder packYml = new StringBuilder();
        packYml.append("id: ").append(this.packID).append("\n");
        packYml.append("version: ").append(this.packVersion).append("\n");
        packYml.append("author: ").append(this.packAuthors).append("\n");
        packYml.append(
                        "addons:\n  biome-provider-pipeline-v2: \"1.+\"\n  biome-provider-single: \"1.+\"\n  biome-provider-extrusion: \"1.+\"\n  chunk-generator-noise-3d: \"1.+\"\n  config-biome: \"1.+\"\n  config-flora: \"1.+\"\n  config-noise-function: \"1.+\"\n  config-ore: \"1.+\"\n  config-palette: \"1.+\"\n  config-distributors: \"1.+\"\n  config-locators: \"1.+\"\n  config-feature: \"1.+\"\n  structure-terrascript-loader: \"1.+\"\n  structure-sponge-loader: \"1.+\"\n  language-yaml: \"1.+\"\n  generation-stage-feature: \"1.+\"\n  structure-function-check-noise-3d: \"1.+\"\n  palette-block-shortcut: \"1.+\"\n  structure-block-shortcut: \"1.+\"\n  terrascript-function-sampler: \"1.+\"\n  VSPE_Structure_Loader: \"1.+\"\n\ngenerator: NOISE_3D\n\nbiomes: $biome-distribution/presets/default.yml:biomes\n\nstages:\n  - id: global-preprocessors\n    type: FEATURE\n  - id: preprocessors\n    type: FEATURE\n  - id: landforms\n    type: FEATURE\n  - id: slabs\n    type: FEATURE\n  - id: ores\n    type: FEATURE\n  - id: deposits\n    type: FEATURE\n  - id: river-decoration\n    type: FEATURE\n  - id: trees\n    type: FEATURE\n  - id: underwater-flora\n    type: FEATURE\n  - id: flora\n    type: FEATURE\n  - id: postprocessors\n    type: FEATURE\nfunctions:\n  '<<':\n    - 'math/functions/terrace.yml:functions'\n    - 'math/functions/interpolation.yml:functions'\n    - 'math/functions/maskSmooth.yml:functions'\n    - 'math/functions/clamp.yml:functions'\nsamplers:\n  '<<':\n    - 'math/samplers/terrain.yml:samplers'\n    - 'math/samplers/simplex.yml:samplers'\n    - 'math/samplers/continents.yml:samplers'\n    - 'math/samplers/precipitation.yml:samplers'\n    - 'math/samplers/temperature.yml:samplers'\n    - 'math/samplers/rivers.yml:samplers'\n    - 'math/samplers/spots.yml:samplers'\nblend:\n  palette:\n    resolution: 2\n    amplitude: 2\n    sampler:\n      type: WHITE_NOISE\nslant:\n  calculation-method: DotProduct\n\n"
                )
                .append("\n");
        return packYml;
    }

    @NotNull
    private StringBuilder getMetaYML() {
        return this.META.getYml();
    }

    @NotNull
    private StringBuilder getBaseYML() {
        StringBuilder baseYML = new StringBuilder();
        baseYML.append("id: BASE\ntype: BIOME\nabstract: true\nextends:\n  - DEPOSITS_DEFAULT\n  - ORES_DEFAULT\n")
                .append("slant-depth: ")
                .append(this.BASEClass.getSlantDepth())
                .append("\n")
                .append("ocean:\n  palette: BLOCK:minecraft:water\n")
                .append("  level: ")
                .append(this.BASEClass.getOceanLevel())
                .append("\n")
                .append("features:\n");
        if (!this.BASEClass.getEnabledPreProcessor().isEmpty()) {
            baseYML.append("  global-preprocessors: \n");

            for (GlobalPreprocessor preprocessor : this.BASEClass.getEnabledPreProcessor()) {
                baseYML.append("   - ").append(preprocessor).append("\n");
            }
        }

        return baseYML;
    }

    @NotNull
    private Map<String, Map<String, StringBuilder>> getBiomesYml() {
        Map<String, Map<String, StringBuilder>> biomes = new HashMap<>();
        Map<String, BaseBiome> biomeIds = new HashMap<>();
        List<BaseBiome> additionalBiomes = new ArrayList<>();

        for (BaseBiome biome : this.BIOMES) {
            if (biome.isAbstract)
                continue;
            addEntry(this.colorMap, biome.getBiomeColor(), biome.getId());
            if (biomeIds.containsKey(biome.getId())) {
                Bukkit.getLogger()
                        .warning(
                                "Biome "
                                        + biome.getClass()
                                        + " has identical cleaned id with biome"
                                        + biomeIds.get(biome.getId()).getClass()
                                        + ". Please resolve this issue as we will try but things might(will) be broken"
                        );
                biome.setID(Utilities.getCleanedID(biome.getUncleanedId() + "_cleaned_from_" + Utilities.generateRandomFourLetterString()));
            } else {
                biomeIds.put(biome.getId(), biome);
            }
        }
        for (BaseBiome biome : this.BIOMES) {
            if (!biome.biomeExtendibles.isEmpty()) {
                for (BaseBiome extendible : biome.biomeExtendibles) {
                    if (extendible.isAbstract) {
                        if (additionalBiomes.stream().noneMatch(k -> k.getClass().equals(extendible.getClass())))
                            additionalBiomes.add(extendible);
                    }
                }
            }
        }

        BIOMES.addAll(additionalBiomes);

        this.generateColorsFile();

        for (BaseBiome biome : this.BIOMES) {
            StringBuilder builder = new StringBuilder();
            builder.append("id: ").append(biome.getId()).append("\n");
            builder.append("type: BIOME").append("\n");
            if (!biome.customExtendibles.isEmpty() || !biome.extendibles.isEmpty() || !biome.biomeExtendibles.isEmpty()) {
                builder.append("extends: [ ");
                boolean firstElement = true;

                for (BaseExtendibles customExtendible : biome.getCustomExtendibles()) {
                    if (!firstElement) {
                        builder.append(", ");
                    }

                    builder.append(customExtendible.getId());
                    firstElement = false;
                    this.INCLUDED_CUSTOM_BASE_EXTENDIBLES.add(customExtendible);
                }

                for (BaseBiome biomeExtendible : biome.getBiomeExtendibles()) {
                    if (!firstElement) {
                        builder.append(", ");
                    }

                    builder.append(biomeExtendible.getId());
                    firstElement = false;
                }

                for (Extendibles extendible : biome.getExtendibles()) {
                    if (!firstElement) {
                        builder.append(", ");
                    }

                    builder.append(extendible);
                    firstElement = false;
                }

                builder.append(" ]").append("\n");
            }
            if (biome.isAbstract) {
                builder.append("abstract: true").append("\n");
            }
            if (biome.carving_update_palette) {
                builder.append("carving:").append("\n").append("  update-palette: true").append("\n");
            }
            if (!biome.isAbstract)
                builder.append("vanilla: ").append(biome.getBiome()).append("\n");
            if (!(biome.getBiomeColor() == null))
                builder.append("color: $biomes/colors.yml:").append(this.INCLUDED_COLOR_MAP.get(biome.getBiomeColor())).append("\n");
            if (!biome.getTags().isEmpty()) {
                builder.append("tags: ").append("\n");

                for (Tags tag : biome.getTags()) {
                    builder.append(" - ").append(tag).append("\n");
                }
            }

            if (!biome.getColors().isEmpty()) {
                builder.append("colors: ").append("\n");

                for (Entry<Colorable, String> entry : biome.colors.entrySet()) {
                    builder.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }

            if (biome.isOcean) {
                biome.setMeta(this.META);
                if (biome.getOcean() != null) {
                    builder.append("ocean: ").append("\n");
                    builder.append("  palette: ").append(biome.getOcean().getOceanMaterial().id).append("\n");
                    INCLUDED_PALETTES.add(biome.getOcean().getOceanMaterial());
                    builder.append("  level: ").append(biome.getOcean().getOceanLevel()).append("\n");
                }
            }

            if (!biome.palettes.isEmpty()) {
                builder.append("palette: ").append("\n");

                for (Entry<Palette, Object> entry : biome.getPalettes().entrySet()) {
                    builder.append(" - ").append(entry.getKey().id).append(": ").append(entry.getValue()).append("\n");
                    this.INCLUDED_PALETTES.add(entry.getKey());
                }

                builder.append(" - << meta.yml:palette-bottom").append("\n");
            }

            if (!biome.slant.isEmpty()) {
                builder.append("slant: ").append("\n");

                for (Entry<Integer, Map<BasePalette, Integer>> entry : biome.getSlant().entrySet()) {
                    builder.append(" - threshold: ").append(entry.getKey()).append("\n");
                    builder.append("   palette: ").append("\n");

                    for (Entry<BasePalette, Integer> palette : entry.getValue().entrySet()) {
                        if (palette.getKey() instanceof InlinePalette) {
                            for (String string : ((InlinePalette) palette.getKey()).inlinePaletteVariables) {
                                builder.append("   - ").append(string).append("\n");
                            }
                        } else if (palette.getKey() instanceof Palette) {
                            builder.append("   - ").append(((Palette) palette.getKey()).id).append("\n");
                            this.INCLUDED_PALETTES.add((Palette) palette.getKey());
                        }
                    }
                }
            }

            if (!biome.features.isEmpty()) {
                builder.append("features: ").append("\n");

                for (Entry<Featureable, List<Feature>> entry : biome.getFeatures().entrySet()) {
                    builder.append("  ").append(entry.getKey().toString().toLowerCase()).append(": ").append("\n");

                    for (Feature features : entry.getValue()) {
                        builder.append("   - ").append(features.getId()).append("\n");
                        if (this.INCLUDED_FEATURES.stream().noneMatch(feature -> feature.getClass() == features.getClass())) {
                            this.INCLUDED_FEATURES.add(features);
                        }
                    }
                }
            }

            if (biome.hasTerrain) {
                builder.append("terrain: ")
                        .append("\n")
                        .append("  sampler:\n")
                        .append(getIndentedBlock(
                                biome.terrain.toString(),
                                "    ")
                        )
                        .append("\n");
            }

            if (biome instanceof Variant variant) {
                if (variant.getVariantOf() != null) {
                    Optional<BiomeVariant> optionalBiomeVariant = variantMap.keySet().stream().filter(k -> k.getClass().equals(variant.getVariantOf().getClass())).findAny();
                    BiomeVariant biomeVariant;
                    biomeVariant = optionalBiomeVariant.orElseGet(variant::getVariantOf);
                    Rarity rarity = variant.getVariantRarity();
                    this.variantMap.computeIfAbsent(biomeVariant, k -> new ArrayList<>()).add(variant);
                    this.biomeRarityMap.put(biomeVariant, rarity);
                    Bukkit.getLogger().info("Added Variant: " + variant + " to BiomeVariant: " + biomeVariant);
                }

                if (variant.getClimateVariantOf() != null) {
                    ClimateVariant climateVariant = variant.getClimateVariantOf();
                    Rarity rarity = variant.getVariantRarity();
                    this.climateVariantListMap.computeIfAbsent(climateVariant, k -> new ArrayList<>()).add(variant);
                    this.climateVariantRarityMap.put(climateVariant, rarity);
                    Bukkit.getLogger().info("Added Variant: " + variant + " to ClimateVariant: " + climateVariant);
                }
            } else if (biome instanceof BiomeVariant biomeVariant) {
                String variantName = biomeVariant.getVariantName();
                Rarity selfRarity = biomeVariant.getSelfRarity();
                this.biomeRarityMap.putIfAbsent(biomeVariant, selfRarity);
                Bukkit.getLogger().info("Added BiomeVariant: " + variantName + " with rarity: " + selfRarity);
            } else if (biome instanceof ClimateVariant climateVariant) {
                String variantName = climateVariant.getVariantName();
                Rarity selfRarity = climateVariant.getSelfRarity();
                this.climateVariantRarityMap.putIfAbsent(climateVariant, selfRarity);
                Bukkit.getLogger().info("Added ClimateVariant: " + variantName + " with rarity: " + selfRarity);
            }

            if (biome instanceof BiomeVariant biomeVariant && biome instanceof Variant variant && variant.getClimateVariantOf() != null) {
                this.climateVariantBiomeVariantMap.put(variant.getClimateVariantOf(), biomeVariant);
            }

            if (!biome.isAbstract)
                if (biome.biomeType.isCoast()) {
                    this.coastalBiomeMap.put(biome.biomeType, this.biomeTypeIntegerMap.getOrDefault(biome.biomeType, 0) + 1);
                } else {
                    BiomeType type = biome.biomeType;
                    Temperature temperature = Temperature.valueOf(type.getTemperate());
                    if (type.isCoast()) {
                        this.coastalBiomeMap.put(type, this.coastalBiomeMap.getOrDefault(biome.biomeType, 0) + 1);
                    } else {
                        Map<Temperature, Integer> temperatureMap = this.biomeTypeTemperatureMap
                                .computeIfAbsent(type.getClass(), k -> new HashMap<>());
                        temperatureMap.merge(temperature, 1, Integer::sum);
                    }
                }

            if (!biome.isAbstract) {
                this.contextBiomeRarityMap.put(biome, Map.of(biome.biomeType, biome.rarity));
                this.biomeTypes.add(biome.biomeType);
                this.biomeSet.add(biome);
            }
            if (!biome.isAbstract)
                biomes.computeIfAbsent(biome.biomeType.getName(), k -> new HashMap<>()).put(biome.getId(), builder);
            else
                biomes.computeIfAbsent("custom_abstracts", k -> new HashMap<>()).put(biome.getId(), builder);
        }

        return biomes;
    }

    @NotNull
    private StringBuilder generateColorsFile() {
        StringBuilder builder = new StringBuilder();
        builder.append("DRIPSTONE_CAVES: 0x1c1a11").append("\n");

        for (Entry<String, List<String>> entry : this.colorMap.entrySet()) {
            String key = entry.getKey();
            String commonName = Utilities.generateCommonName(entry.getValue());
            this.INCLUDED_COLOR_MAP.put(key, commonName);
            builder.append(commonName).append(": ").append(key).append("\n");
        }

        return builder;
    }

    @NotNull
    private Map<String, Map<String, StringBuilder>> getFeaturesYml() {
        Map<String, Map<String, StringBuilder>> builderMap = new HashMap<>();

        for (Feature feature : this.INCLUDED_FEATURES) {
            StringBuilder builder = new StringBuilder();
            builder.append("id: ").append(feature.getId()).append("\n");
            builder.append("type: FEATURE").append("\n");
            if (feature.getDistributor() != null) {
                builder.append("distributor: ").append("\n");
                if (feature.getDistributor() instanceof Locator locator) {
                    builder.append("  type: ").append(locator.getType()).append("\n").append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");
                } else if (feature.getDistributor() instanceof NoiseSampler) {
                    builder.append("  type: ").append(((NoiseSampler) feature.getDistributor()).name()).append("\n");
                    builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");
                }
            }

            if (feature.getLocator() != null) {
                builder.append("locator: ").append("\n");
                if (feature.getLocator() instanceof Locator locator) {
                    builder.append("  type: ").append(locator.getType()).append("\n").append(getIndentedBlock(feature.getLocator().getYml().toString(), "  ")).append("\n");
                } else if (feature.getLocator() instanceof NoiseSampler sampler) {
                    builder.append("  type: ").append(sampler.name()).append("\n");
                    builder.append(getIndentedBlock(sampler.getYml().toString(), "  ")).append("\n");
                }
            }

            builder.append("structures: ").append("\n");
            if (feature.getStructureDistributor() != null) {
                builder.append("  distribution: ").append("\n");
                builder.append(getIndentedBlock(feature.getStructureDistributor().getYml().toString(), "    ")).append("\n");
            }

            if (!feature.getStructures().isEmpty()) {
                builder.append("  structures: ").append("\n");

                for (Entry<Object, Integer> structure : feature.getStructures().entrySet()) {
                    if (structure.getKey() instanceof BaseStructure) {
                        if (this.INCLUDED_STRUCTURES.stream().noneMatch(structure1 -> structure1.getClass() == structure.getKey().getClass())) {
                            this.INCLUDED_STRUCTURES.add((BaseStructure) structure.getKey());
                            builder.append("   - ").append(((BaseStructure) structure.getKey()).getId());
                        }
                    } else {
                        builder.append("   - ").append(structure.getKey());
                    }

                    builder.append(": ").append(structure.getValue()).append("\n");
                }
            }

            builderMap.computeIfAbsent(feature.getType().name(), k -> new HashMap<>()).put(feature.getId(), builder);
        }

        return builderMap;
    }

    @NotNull
    private Map<String, StringBuilder> getPalettesYml() {
        Map<String, StringBuilder> builderMap = new HashMap<>();

        for (Palette palette : this.INCLUDED_PALETTES) {
            builderMap.put(palette.getId(), palette.getYml());
        }

        return builderMap;
    }

    @NotNull
    private Map<DepositableFeature.Type, Map<String, StringBuilder>> getDepositableFeaturesYml() {
        Map<DepositableFeature.Type, Map<String, StringBuilder>> mainMap = new HashMap<>();
        Map<String, StringBuilder> builderDepositMap = new HashMap<>();
        Map<String, StringBuilder> builderOreMap = new HashMap<>();
        this.distributionYML.append("# [----------------------------- DEPOSITABLES -----------------------------]").append("\n");
        if (!this.BASEClass.getDefaultDeposits().isEmpty()) {
            this.defaultDeposits.append("features:\n  deposits:\n");
        }

        if (!this.BASEClass.getDefaultOres().isEmpty()) {
            this.defaultOres.append("features:\n  ores:\n");
        }

        for (DepositableFeature feature : this.BASEClass.getDefaultDeposits()) {
            this.distributionYML.append(feature.getId().toLowerCase().replaceAll("[^a-z]", "_")).append("\n");
            this.distributionYML.append("  averageCountPerChunk: ").append(feature.rarity.rarityValue).append("\n");
            this.distributionYML.append("  salt: ").append(feature.salt).append("\n");
            this.distributionYML.append("  range: ").append(feature.salt).append("\n");
            this.distributionYML.append("    max: ").append(feature.yLevelRange.getMax()).append("\n");
            this.distributionYML.append("    min: ").append(feature.yLevelRange.getMin()).append("\n");
            this.defaultDeposits.append("  - ").append(feature.getId()).append("\n");
            StringBuilder builder = new StringBuilder();
            builder.append("id: ").append(feature.getId()).append("\n");
            builder.append("type: FEATURE").append("\n");
            builder.append("anchors:").append("\n");
            if (!feature.anchorStructures.isEmpty()) {
                builder.append("&structure: ").append("\n");

                for (DepositableStructure structure : feature.anchorStructures) {
                    builder.append(" - ").append(structure.getId()).append("\n");
                }
            }

            builder.append(
                            "  - &densityThreshold 1/256 * ${features/deposits/distribution.yml:clay.averageCountPerChunk} # Divide by 16^2 to get % per column\n  - &salt $features/deposits/distribution.yml:clay.salt\n  - &range $features/deposits/distribution.yml:clay.range\n\ndistributor:\n  type: SAMPLER\n  sampler:\n    type: POSITIVE_WHITE_NOISE\n    salt: *salt\n  threshold: *densityThreshold\n"
                    )
                    .append("\n");
            builder.append("locator: ").append("\n");
            builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");
            builder.append("structures:\n  distribution:\n    type: CONSTANT\n  structures: *structure\n");
            builderDepositMap.put(feature.getId(), builder);
            this.INCLUDED_DEPOSITABLES.addAll(feature.anchorStructures);
        }

        this.distributionYML.append("# [----------------------------- ORES -----------------------------]").append("\n");
        mainMap.put(DepositableFeature.Type.DEPOSIT, builderDepositMap);

        for (DepositableFeature feature : this.BASEClass.getDefaultOres()) {
            this.distributionYML.append(feature.getId().toLowerCase().replaceAll("[^a-z]", "_")).append("\n");
            this.distributionYML.append("  averageCountPerChunk: ").append(feature.rarity.rarityValue).append("\n");
            this.distributionYML.append("  salt: ").append(feature.salt).append("\n");
            this.distributionYML.append("  range: ").append(feature.salt).append("\n");
            this.distributionYML.append("    max: ").append(feature.yLevelRange.getMax()).append("\n");
            this.distributionYML.append("    min: ").append(feature.yLevelRange.getMin()).append("\n");
            this.defaultOres.append("  - ").append(feature.getId()).append("\n");
            StringBuilder builder = new StringBuilder();
            builder.append("anchors:").append("\n");
            if (!feature.anchorStructures.isEmpty()) {
                builder.append("&structure: ").append("\n");

                for (DepositableStructure structure : feature.anchorStructures) {
                    builder.append(" - ").append(structure.getId()).append("\n");
                }
            }

            builder.append(
                            "  - &densityThreshold 1/256 * ${features/deposits/distribution.yml:gold.averageCountPerChunk}\n  - &salt $features/deposits/distribution.yml:gold.salt\n  - &range $features/deposits/distribution.yml:gold.range\n  - &standard-deviation (${features/deposits/distribution.yml:gold.range.max}-${features/deposits/distribution.yml:gold.range.min})/6\n\ndistributor:\n  type: SAMPLER\n  sampler:\n    type: POSITIVE_WHITE_NOISE\n    salt: *salt\n  threshold: *densityThreshold\n"
                    )
                    .append("\n");
            builder.append("locator: ").append("\n");
            builder.append(getIndentedBlock(feature.getDistributor().getYml().toString(), "  ")).append("\n");
            builder.append("structures:\n  distribution:\n    type: CONSTANT\n  structures: *structure\n");
            builderOreMap.put(feature.getId(), builder);
            this.INCLUDED_ORES.addAll(feature.anchorStructures);
        }

        mainMap.put(DepositableFeature.Type.ORE, builderOreMap);
        return mainMap;
    }

    @NotNull
    private StringBuilder outputBiomeVariants() {
        StringBuilder output = new StringBuilder();
        if (!variantMap.isEmpty()) {
            output.append("stages:").append("\n");
            output.append("  - type: REPLACE_LIST\n");
            List<Map.Entry<BiomeVariant, List<Variant>>> variantList = new ArrayList<>(variantMap.entrySet());
            Map.Entry<BiomeVariant, List<Variant>> firstVariant = variantList.get(0);
            if (!firstVariant.getValue().isEmpty()) {
                output.append("    default-from: ").append(firstVariant.getKey().getVariantName()).append("\n");
                output.append("    default-to:\n");
                output.append("      - SELF: ").append(firstVariant.getKey().getSelfRarity().rarityValue + 2).append("\n");
                for (Variant context : firstVariant.getValue()) {
                    output.append("      - ").append(context.getVariantName()).append(": ").append(context.getVariantRarity().rarityValue + 1).append("\n");
                }
            }
            if (variantList.size() > 1) {
                output.append("    to:\n");
                for (int i = 1; i < variantList.size(); i++) {
                    Map.Entry<BiomeVariant, List<Variant>> variant = variantList.get(i);
                    if (!variant.getValue().isEmpty()) {
                        output.append("      ").append(variant.getKey().getVariantName()).append(":").append("\n");
                        output.append("        - SELF: ").append(variant.getKey().getSelfRarity().rarityValue + 2).append("\n");
                        for (Variant context : variant.getValue()) {
                            output.append("        - ").append(context.getVariantName()).append(": ").append(context.getVariantRarity().rarityValue + 1).append("\n");
                        }
                    }
                }
            }
            output.append("    sampler:\n");
            NoiseSampler simplexFBM = NoiseSamplerBuilder.of(new FBM())
                    .addGlobalParameter("dimensions", 2)
                    .setParameter("octaves", 5)
                    .setParameter("gain", 0.5)
                    .setParameter("lacunarity", 0.05)
                    .setParameter("sampler",
                            NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2S())
                                    .setParameter("salt", generateRandomNumber())
                                    .setParameter("frequency", 0.002)
                                    .build()
                    )
                    .build();
            NoiseSampler domainWarp = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                    .addGlobalParameter("dimensions", 2)
                    .setParameter("sampler", NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2S())
                            .setParameter("salt", generateRandomNumber())
                            .setParameter("frequency", 0.005)
                            .build()
                    )
                    .setParameter("warp", NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2S())
                            .setParameter("salt", generateRandomNumber())
                            .setParameter("frequency", 0.002)
                            .build()
                    )
                    .setParameter("amplitude", 5)
                    .build();
            MetaMap map = META.getMappingFor(MetaClass.MetaVariables.GLOBAL_SCALE);
            map.performOperation(0.3, ArithmeticOperation.DIVIDE);
            NoiseSampler cellular = NoiseSamplerBuilder.of(new CELLULAR())
                    .addGlobalParameter("dimensions", 2)
                    .setParameter("frequency", map)
                    .setParameter("distance", "Euclidean")
                    .setParameter("return", "Distance")
                    .setParameter("salt", generateRandomNumber())
                    .build();
            output.append(getIndentedBlock(
                    NoiseSamplerBuilder.of(new EXPRESSION())
                            .addGlobalParameter("dimensions", 2)
                            .setParameter("expression", "(simplexFBM(x, z) * domainWarp(x, z)) + cellular(x, z) * 0.3")
                            .setParameter("samplers", Map.of(
                                    "simplexFBM", simplexFBM,
                                    "domainWarp", domainWarp,
                                    "cellular", cellular
                            ))
                            .build()
                            .getYml()
                            .toString(),
                    "      "));
        }

        return output;
    }

    @NotNull
    private Map<String, StringBuilder> generateExtrusions() {
        Map<String, StringBuilder> builders = new HashMap<>();

        for (Extrusion extrusion : this.CUSTOM_EXTRUSIONS) {
            if (extrusion instanceof ReplaceExtrusion) {
                builders.put(((ReplaceExtrusion) extrusion).getId(), ((ReplaceExtrusion) extrusion).getYml());
                this.BIOMES.addAll(((ReplaceExtrusion) extrusion).getExtrusionReplaceableBiome());
            }
        }

        return builders;
    }

    @NotNull
    private StringBuilder generatePreset() {
        StringBuilder builder = new StringBuilder();
        builder.append("biomes:\n  type: EXTRUSION\n  extrusions:\n");
        if (!this.CUSTOM_EXTRUSIONS.isEmpty()) {
            for (Extrusion extrusion : this.CUSTOM_EXTRUSIONS) {
                if (extrusion instanceof ReplaceExtrusion) {
                    builder.append("    - << biome-distribution/extrusions/").append(((ReplaceExtrusion) extrusion).getId()).append(".yml:extrusions").append("\n");
                }
            }
        }

        builder.append(
                "  provider:\n    type: PIPELINE\n    resolution: 4\n    blend:\n      amplitude: 6\n      sampler:\n        type: OPEN_SIMPLEX_2\n        frequency: 0.012\n    pipeline:\n      source:\n        type: SAMPLER\n        sampler:\n          type: CELLULAR\n          jitter: ${customization.yml:biomeSpread.cellJitter}\n          return: NoiseLookup\n          frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n          lookup:\n            type: EXPRESSION\n            expression: continents(x, z)\n        biomes:\n"
        );
        int oceanSize = 0;
        int deepSeaSize = 0;
        int landSize = 0;

        for (BaseBiome entry : this.biomeSet) {
            BiomeType biome = entry.biomeType;
            int count = 1;
            if (biome instanceof Ocean_Flat || biome instanceof Ocean_Hilly || biome instanceof Ocean) {
                oceanSize += count;
            } else if (biome instanceof Deep_Ocean) {
                deepSeaSize += count;
            } else if (biome instanceof Land
                    && (!(biome instanceof Hills_Small) || !biome.isCoast())
                    && (!(biome instanceof Flat) || !biome.isCoast())
                    && (!(biome instanceof Hills_Large) || !biome.isCoast())
                    && (!(biome instanceof Mountains_Large) || !biome.isCoast())
                    && (!(biome instanceof Mountains_Small) || !biome.isCoast())) {
                landSize += count;
            }
        }

        if (deepSeaSize != 0) {
            builder.append("          deep-ocean: ").append(deepSeaSize).append("\n");
        }

        if (oceanSize != 0) {
            builder.append("          ocean: ").append(oceanSize).append("\n");
        }

        if (landSize != 0) {
            builder.append("          land: ").append(landSize).append("\n");
        }

        builder.append("      stages:").append("\n");
        if (!this.generateCoasts().isEmpty()) {
            builder.append("        - << biome-distribution/stages/coasts.yml:stages\n        - << biome-distribution/stages/fill_coasts.yml:stages\n");
        }

        if (!this.generateOceans().isEmpty()) {
            builder.append("        - << biome-distribution/stages/oceans.yml:stages").append("\n");
        }

        if (!this.generateSTZ().isEmpty()) {
            builder.append(
                    "        - << biome-distribution/stages/spread_temperature_zones.yml:stages\n        - << biome-distribution/stages/fill_temperature_zones.yml:stages\n"
            );
        }

        if (!this.outputBiomeVariants().isEmpty()) {
            builder.append(
                    "        - << biome-distribution/stages/add_variants.yml:stages\n"
            );
        }

        builder.append(
                "        - type: FRACTAL_EXPAND\n          sampler:\n            type: WHITE_NOISE\n        - type: SMOOTH\n          sampler:\n            type: WHITE_NOISE\n       #- << biome-distribution/stages/add_rivers.yml:stages\n        - type: SMOOTH\n          sampler:\n            type: WHITE_NOISE\n"
        );
        return builder;
    }

    @NotNull
    private StringBuilder generateCoasts() {
        StringBuilder builder = new StringBuilder();
        new HashMap();
        if (this.META.calculationMethod.equals(HeightTemperatureRarityCalculationMethod.SEPARATE)) {
            Map<String, Integer> temperateRarity = this.generateTemperatureRarityMap();
        } else {
            Map<String, Integer> var8 = this.generateRarityMap();
        }

        Map<String, Integer> biomeAmount = this.getBiomeAmount();
        Set<String> biomesPresent = this.getBiomesPresent();
        new HashSet();
        boolean coastSmallExists = false;
        boolean coastLargeExists = false;
        if ((biomesPresent.contains("OCEAN") || biomesPresent.contains("DEEP_OCEAN"))
                && (biomesPresent.contains("COAST_SMALL") || biomesPresent.contains("COAST_LARGE"))) {
            builder.append("stages:").append("\n");
            if (biomesPresent.contains("OCEAN")) {
                builder.append(
                        "  - type: REPLACE # split oceans into small and big coast sections\n    from: ocean\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: CellValue\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n    to:\n      SELF: 1\n"
                );
                if (biomesPresent.contains("COAST_SMALL")) {
                    builder.append("      ocean_coast_small: 2").append("\n");
                    coastSmallExists = true;
                }

                if (biomesPresent.contains("COAST_LARGE")) {
                    builder.append("      ocean_coast_large: 1").append("\n");
                    coastLargeExists = true;
                }

                if (coastSmallExists) {
                    builder.append(
                            "  - type: REPLACE # add small coasts\n    from: ocean_coast_small\n    sampler:\n      type: EXPRESSION\n      expression: continentBorderCelledSmall(x, z)\n    to:\n      SELF: 1\n      coast_small: 1\n"
                    );
                    builder.append(
                            "  - type: REPLACE # make small oceans placeholders oceans again\n    from: ocean_coast_small\n    sampler:\n      type: CONSTANT\n    to:\n      ocean: 1\n"
                    );
                }

                if (coastLargeExists) {
                    builder.append(
                            "  - type: REPLACE # add small coasts\n    from: ocean_coast_wide\n    sampler:\n      type: EXPRESSION\n      expression: continentBorderCelledSmall(x, z)\n    to:\n      SELF: 1\n      coast_wide: 1\n"
                    );
                    builder.append(
                            "  - type: REPLACE # make small oceans placeholders oceans again\n    from: ocean_coast_wide\n    sampler:\n      type: CONSTANT\n    to:\n      ocean: 1\n"
                    );
                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateFillCoasts() {
        StringBuilder builder = new StringBuilder();
        new HashMap();
        Map temperateRarity;
        if (this.META.calculationMethod.equals(HeightTemperatureRarityCalculationMethod.SEPARATE)) {
            temperateRarity = this.generateTemperatureRarityMap();
        } else {
            temperateRarity = this.generateRarityMap();
        }

        Map<String, Integer> biomeAmount = this.getBiomeAmount();
        Set<String> biomesPresent = this.getBiomesPresent();
        Set<String> coastPresent = new HashSet<>();
        if (biomesPresent.contains("COAST_SMALL") || biomesPresent.contains("COAST_LARGE")) {
            builder.append("stages: ").append("\n");
            if (biomesPresent.contains("COAST_SMALL")) {
                builder.append(
                        "  - type: REPLACE\n    from: coast_small\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: NoiseLookup\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      lookup:\n        type: EXPRESSION\n        expression: 'temperature(x, z)'\n    to:\n"
                );
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
                builder.append(
                        "  - type: REPLACE\n    from: coast_wide\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: NoiseLookup\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      lookup:\n        type: EXPRESSION\n        expression: 'temperature(x, z)'\n    to:\n"
                );
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
                    builder.append(
                            "  - type: REPLACE\n    from: coast_small_boreal\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_small_boreal")) {
                                builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_small_polar")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_small_polar\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biomex : this.biomeSet) {
                        if (biomex.biomeType.getName().equalsIgnoreCase("coast_small_polar")) {
                            builder.append("      ").append(biomex.getId()).append(": ").append(biomex.rarity.rarityValue).append("\n");
                        }
                    }
                }

                if (coastPresent.contains("coast_small_subtropical")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_small_subtropical\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_small_subtropical")) {
                                builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_small_tropical")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_small_tropical\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_small_tropical")) {
                                builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_small_temperate")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_small_temperate\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_small_temperate")) {
                                builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }
            }

            if (biomesPresent.contains("COAST_LARGE")) {
                if (coastPresent.contains("coast_large_boreal")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_large_boreal\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_large_boreal")) {
                                builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_large_polar")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_large_polar\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_large_polar")) {
                                builder.append("     ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_large_subtropical")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_large_subtropical\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_large_subtropical")) {
                                builder.append("     ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_large_tropical")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_large_tropical\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_large_tropical")) {
                                builder.append("     ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }

                if (coastPresent.contains("coast_large_temperate")) {
                    builder.append(
                            "  - type: REPLACE\n    from: coast_large_temperate\n    sampler:\n      type: CELLULAR\n      jitter: '${customization.yml:biomeSpread.cellJitter}'\n      return: CellValue\n      frequency: '1 / ${customization.yml:biomeSpread.cellDistance}'\n      salt: 8726345\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
                            if (biome.biomeType.getName().equalsIgnoreCase("coast_large_temperate")) {
                                builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                            }
                    }

                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateOceans() {
        StringBuilder builder = new StringBuilder();
        Map<String, Integer> temperatureRarity = this.META.calculationMethod.equals(HeightTemperatureRarityCalculationMethod.SEPARATE)
                ? this.generateTemperatureRarityMap()
                : this.generateRarityMap();
        Map<String, Integer> oceanHeightRarityMap = this.generateOceanHeightRarityMap();
        Set<String> biomesPresent = this.getBiomesPresent();
        Map<String, Integer> biomeAmount = this.getBiomeAmountEC();
        if (biomesPresent.contains("OCEAN")) {
            builder.append(
                    "stages:\n  - type: REPLACE\n    from: ocean\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
            );
            List<String> terrainTypes = List.of("FLAT", "HILLS");

            for (String terrain : terrainTypes) {
                String oceanBiome = "OCEAN_" + terrain;
                if (biomesPresent.contains(oceanBiome) && oceanHeightRarityMap.containsKey(terrain)) {
                    builder.append("      ").append(oceanBiome).append(": ").append(oceanHeightRarityMap.get(terrain)).append("\n");
                }
            }

            for (String terrain : terrainTypes) {
                String oceanBiome = "OCEAN_" + terrain;
                if (biomesPresent.contains(oceanBiome)) {
                    builder.append("  - type: REPLACE\n");
                    builder.append("    from: ").append(oceanBiome).append("\n");
                    builder.append("    sampler:\n");
                    builder.append("      type: CELLULAR\n");
                    builder.append("      jitter: ${customization.yml:biomeSpread.cellJitter}\n");
                    builder.append("      return: CellValue\n");
                    builder.append("      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n");
                    builder.append("    to:\n");

                    for (Entry<String, Integer> entry : temperatureRarity.entrySet()) {
                        String zone = entry.getKey();
                        String biomeName = "OCEAN_" + terrain + "_" + zone;
                        biomeName = biomeName.toLowerCase();
                        if (biomeAmount.containsKey(biomeName)) {
                            builder.append("      ").append(biomeName).append(": ").append(entry.getValue()).append("\n");
                        }
                    }

                    for (Entry<String, Integer> entry : temperatureRarity.entrySet()) {
                        String zone = entry.getKey();
                        String biomeName = "OCEAN_" + terrain + "_" + zone;
                        biomeName = biomeName.toLowerCase();
                        if (biomeAmount.containsKey(biomeName)) {
                            builder.append("  - type: REPLACE\n");
                            builder.append("    from: ").append(biomeName).append("\n");
                            builder.append("    sampler:\n");
                            builder.append("      type: CELLULAR\n");
                            builder.append("      jitter: ${customization.yml:biomeSpread.cellJitter}\n");
                            builder.append("      return: CellValue\n");
                            builder.append("      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n");
                            builder.append("    to:\n");

                            for (BaseBiome biome : this.biomeSet) {
                                if (!(biome instanceof Variant) && !biome.isAbstract)
                                    if (biome.biomeType.getName().equalsIgnoreCase(biomeName)) {
                                        builder.append("      ").append(biome.getId()).append(": ").append(biome.rarity.rarityValue).append("\n");
                                    }
                            }

                        }
                    }
                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateSTZ() {
        StringBuilder builder = new StringBuilder();
        Map<String, Integer> temperateRarity;
        if (this.META.calculationMethod.equals(HeightTemperatureRarityCalculationMethod.SEPARATE)) {
            temperateRarity = this.generateTemperatureRarityMap();
        } else {
            temperateRarity = this.generateRarityMap();
        }

        Map<String, Integer> heightRarity = this.generateLandHeightRarityMap();
        Map<String, Integer> biomeAmount = this.getBiomeAmountEC();
        Set<String> biomesPresent = this.getBiomesPresent();
        Set<String> temperatesPresent = new HashSet<>();
        if (this.getBiomesPresent().contains("LAND")) {
            builder.append(
                    "stages:\n  - type: REPLACE\n    from: land\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
            );
            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("boreal"))) {
                builder.append("      boreal: ").append(temperateRarity.get("BOREAL")).append("\n");
                temperatesPresent.add("boreal");
                this.temperatureToHeightMap.put("BOREAL", new HashSet<>());
            }

            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("polar"))) {
                builder.append("      polar: ").append(temperateRarity.get("POLAR")).append("\n");
                temperatesPresent.add("polar");
                this.temperatureToHeightMap.put("POLAR", new HashSet<>());
            }

            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("subtropical"))) {
                builder.append("      subtropical: ").append(temperateRarity.get("SUBTROPICAL")).append("\n");
                temperatesPresent.add("subtropical");
                this.temperatureToHeightMap.put("SUBTROPICAL", new HashSet<>());
            }

            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("tropical"))) {
                builder.append("      tropical: ").append(temperateRarity.get("TROPICAL")).append("\n");
                temperatesPresent.add("tropical");
                this.temperatureToHeightMap.put("TROPICAL", new HashSet<>());
            }

            if (biomeAmount.keySet().stream().anyMatch(key -> (key.contains("mountains") || key.contains("hills")) && key.contains("temperate"))) {
                builder.append("      temperate: ").append(temperateRarity.get("TEMPERATE")).append("\n");
                temperatesPresent.add("temperate");
                this.temperatureToHeightMap.put("TEMPERATE", new HashSet<>());
            }

            if (temperatesPresent.contains("boreal")) {
                builder.append(
                        "  - type: REPLACE\n    from: boreal\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
                );
                if (biomesPresent.contains("HILLS_SMALL_BOREAL")) {
                    builder.append("      hills_small_boreal: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("hills_small");
                }

                if (biomesPresent.contains("HILLS_LARGE_BOREAL")) {
                    builder.append("      hills_large_boreal: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("hills_large");
                }

                if (biomesPresent.contains("MOUNTAINS_SMALL_BOREAL")) {
                    builder.append("      mountains_small_boreal: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("mountains_small");
                }

                if (biomesPresent.contains("MOUNTAINS_LARGE_BOREAL")) {
                    builder.append("      mountains_large_boreal: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("mountains_large");
                }

                if (biomesPresent.contains("FLAT_BOREAL")) {
                    builder.append("      flat_boreal: ").append(heightRarity.get("FLAT")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("BOREAL", k -> new HashSet<>()).add("flat");
                }
            }

            if (temperatesPresent.contains("polar")) {
                builder.append(
                        "  - type: REPLACE\n    from: polar\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
                );
                if (biomesPresent.contains("HILLS_SMALL_POLAR")) {
                    builder.append("      hills_small_polar: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("hills_small");
                }

                if (biomesPresent.contains("HILLS_LARGE_POLAR")) {
                    builder.append("      hills_large_polar: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("hills_large");
                }

                if (biomesPresent.contains("MOUNTAINS_SMALL_POLAR")) {
                    builder.append("      mountains_small_polar: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("mountains_small");
                }

                if (biomesPresent.contains("MOUNTAINS_LARGE_POLAR")) {
                    builder.append("      mountains_large_polar: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("mountains_large");
                }

                if (biomesPresent.contains("FLAT_POLAR")) {
                    builder.append("      flat_polar: ").append(heightRarity.get("FLAT")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("POLAR", k -> new HashSet<>()).add("flat");
                }
            }

            if (temperatesPresent.contains("subtropical")) {
                builder.append(
                        "  - type: REPLACE\n    from: subtropical\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
                );
                if (biomesPresent.contains("HILLS_SMALL_SUBTROPICAL")) {
                    builder.append("      hills_small_subtropical: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("hills_small");
                }

                if (biomesPresent.contains("HILLS_LARGE_SUBTROPICAL")) {
                    builder.append("      hills_large_subtropical: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("hills_large");
                }

                if (biomesPresent.contains("MOUNTAINS_SMALL_SUBTROPICAL")) {
                    builder.append("      mountains_small_subtropical: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("mountains_small");
                }

                if (biomesPresent.contains("MOUNTAINS_LARGE_SUBTROPICAL")) {
                    builder.append("      mountains_large_subtropical: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("mountains_large");
                }

                if (biomesPresent.contains("FLAT_SUBTROPICAL")) {
                    builder.append("      flat_subtropical: ").append(heightRarity.get("FLAT")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("SUBTROPICAL", k -> new HashSet<>()).add("flat");
                }
            }

            if (temperatesPresent.contains("tropical")) {
                builder.append(
                        "  - type: REPLACE\n    from: tropical\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
                );
                if (biomesPresent.contains("HILLS_SMALL_TROPICAL")) {
                    builder.append("      hills_small_tropical: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("hills_small");
                }

                if (biomesPresent.contains("HILLS_LARGE_TROPICAL")) {
                    builder.append("      hills_large_tropical: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("hills_large");
                }

                if (biomesPresent.contains("MOUNTAINS_SMALL_TROPICAL")) {
                    builder.append("      mountains_small_tropical: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("mountains_small");
                }

                if (biomesPresent.contains("MOUNTAINS_LARGE_TROPICAL")) {
                    builder.append("      mountains_large_tropical: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("mountains_large");
                }

                if (biomesPresent.contains("FLAT_TROPICAL")) {
                    builder.append("      flat_tropical: ").append(heightRarity.get("FLAT")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TROPICAL", k -> new HashSet<>()).add("flat");
                }
            }

            if (temperatesPresent.contains("temperate")) {
                builder.append(
                        "  - type: REPLACE\n    from: temperate\n    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: NoiseLookup\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n      lookup:\n        type: EXPRESSION\n        expression: temperature(x, z)\n    to:\n"
                );
                if (biomesPresent.contains("HILLS_SMALL_TEMPERATE")) {
                    builder.append("      hills_small_temperate: ").append(heightRarity.get("HILLS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("hills_small");
                }

                if (biomesPresent.contains("HILLS_LARGE_TEMPERATE")) {
                    builder.append("      hills_large_temperate: ").append(heightRarity.get("HILLS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("hills_large");
                }

                if (biomesPresent.contains("MOUNTAINS_SMALL_TEMPERATE")) {
                    builder.append("      mountains_small_temperate: ").append(heightRarity.get("MOUNTAINS_SMALL")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("mountains_small");
                }

                if (biomesPresent.contains("MOUNTAINS_LARGE_TEMPERATE")) {
                    builder.append("      mountains_large_temperate: ").append(heightRarity.get("MOUNTAINS_LARGE")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("mountains_large");
                }

                if (biomesPresent.contains("FLAT_TEMPERATE")) {
                    builder.append("      flat_temperate: ").append(heightRarity.get("FLAT")).append("\n");
                    this.temperatureToHeightMap.computeIfAbsent("TEMPERATE", k -> new HashSet<>()).add("flat");
                }
            }
        }

        return builder;
    }

    @NotNull
    private StringBuilder generateFTZ() {
        StringBuilder builder = new StringBuilder();
        if (!this.generateSTZ().isEmpty()) {
            builder.append("stages:").append("\n");

            for (Entry<String, Set<String>> currentTH : this.temperatureToHeightMap.entrySet()) {
                for (String key : currentTH.getValue()) {
                    builder.append("  - type: REPLACE\n");
                    String current = key + "_" + currentTH.getKey().toLowerCase();
                    builder.append("    from: ").append(current).append("\n");
                    builder.append(
                            "    sampler:\n      type: CELLULAR\n      jitter: ${customization.yml:biomeSpread.cellJitter}\n      return: CellValue\n      frequency: 1 / ${customization.yml:biomeSpread.cellDistance}\n    to:\n"
                    );

                    for (BaseBiome biome : this.biomeSet) {
                        if (!(biome instanceof Variant) && !biome.isAbstract)
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
        this.biomeSet.forEach(biome -> {
            String simpleName = biome.biomeType.getName();
            simpleName = simpleName.toLowerCase();
            biomeMap.put(simpleName, biomeMap.getOrDefault(simpleName, 0) + 1);
        });
        return biomeMap;
    }

    private Map<String, Integer> getBiomeAmountEC() {
        Map<String, Integer> biomeMap = new HashMap<>();
        this.biomeSet.forEach(biome -> {
            if (!biome.biomeType.isCoast() && !(biome instanceof Variant)) {
                String simpleName = biome.biomeType.getName();
                simpleName = simpleName.toLowerCase();
                biomeMap.put(simpleName, biomeMap.getOrDefault(simpleName, 0) + 1);
            }
        });
        return biomeMap;
    }

    private Set<String> getBiomesPresent() {
        Set<String> biomeMap = new HashSet<>();
        this.biomeSet.forEach(biome -> {
            if (!(biome instanceof Variant)) {
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
                } else if (simpleName.contains("hills_small")) {
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
                } else if (simpleName.contains("hills_large")) {
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
                } else if (simpleName.contains("mountains_small")) {
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
                } else if (simpleName.contains("mountains_large")) {
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
                } else if (simpleName.contains("ocean")) {
                    if (simpleName.contains("deep")) {
                        biomeMap.add("DEEP_OCEAN");
                    } else if (simpleName.contains("ocean_flat")) {
                        biomeMap.add("OCEAN_FLAT");
                        biomeMap.add("OCEAN");
                    } else if (simpleName.contains("ocean_hills")) {
                        biomeMap.add("OCEAN_HILLS");
                        biomeMap.add("OCEAN");
                    } else {
                        biomeMap.add("OCEAN");
                    }
                } else if (simpleName.contains("river")) {
                    biomeMap.add("RIVER");
                } else if (simpleName.contains("flat")) {
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
            }
        });
        return biomeMap;
    }

    private Map<String, Integer> generateHeightRarityMap() {
        Map<String, Integer> heightRarityMap = new LinkedHashMap<>();
        double elevation = this.META.heightVariance;
        Map<String, double[]> heightZones = Map.of(
                "TROPICAL",
                new double[]{5000.0, 7000.0},
                "TEMPERATE",
                new double[]{1000.0, 3000.0},
                "POLAR",
                new double[]{0.0, 500.0},
                "BOREAL",
                new double[]{500.0, 1000.0},
                "SUBTROPICAL",
                new double[]{3000.0, 5000.0}
        );

        for (Entry<String, double[]> entry : heightZones.entrySet()) {
            String zone = entry.getKey();
            double[] heightRange = entry.getValue();
            double heightCenter = (heightRange[0] + heightRange[1]) / 2.0;
            double heightSuitability = this.calculateHeightSuitability(elevation, heightRange);
            int rarity = (int) (heightSuitability * 10.0);
            heightRarityMap.put(zone, rarity);
        }

        return heightRarityMap;
    }

    private Map<String, Integer> generateOceanHeightRarityMap() {
        Map<String, Integer> heightRarityMap = new LinkedHashMap<>();
        double oceanHeightVariance = this.META.oceanHeightVariance;
        Map<String, double[]> heightZones = Map.of("FLAT", new double[]{-1.0, -0.2}, "HILLS", new double[]{-0.2, 1.0});

        for (Entry<String, double[]> entry : heightZones.entrySet()) {
            String zone = entry.getKey();
            double[] heightRange = entry.getValue();
            double heightCenter = (heightRange[0] + heightRange[1]) / 2.0;
            double heightSuitability = this.calculateHeightSuitability(oceanHeightVariance, heightRange);
            int rarity = Math.max(2, Math.min(8, (int) (2.0 + heightSuitability * 6.0)));
            heightRarityMap.put(zone, rarity);
        }

        return heightRarityMap;
    }

    private Map<String, Integer> generateTemperatureRarityMap() {
        Map<String, Integer> rarityMap = new LinkedHashMap<>();
        double coldness = this.META.temperatureVariance;
        coldness *= -1.0;
        Map<String, double[]> temperatureZones = Map.of(
                "TROPICAL",
                new double[]{20.0, 30.0},
                "TEMPERATE",
                new double[]{5.0, 20.0},
                "POLAR",
                new double[]{-50.0, 0.0},
                "BOREAL",
                new double[]{-10.0, 10.0},
                "SUBTROPICAL",
                new double[]{10.0, 25.0}
        );
        Map<String, Double> normalizedCenters = new HashMap<>();

        for (Entry<String, double[]> entry : temperatureZones.entrySet()) {
            double tempCenter = (entry.getValue()[0] + entry.getValue()[1]) / 2.0;
            normalizedCenters.put(entry.getKey(), this.normalizeTemperature(tempCenter));
        }

        for (Entry<String, double[]> entry : temperatureZones.entrySet()) {
            String zone = entry.getKey();
            double[] tempRange = entry.getValue();
            double tempCenter = (tempRange[0] + tempRange[1]) / 2.0;
            double suitability = Math.max(0.0, 1.0 - Math.abs(coldness - this.normalizeTemperature(tempCenter)));
            double clusterFactor = 0.0;

            for (Entry<String, Double> neighbor : normalizedCenters.entrySet()) {
                if (!neighbor.getKey().equals(zone)) {
                    double proximity = 1.0 - Math.abs(neighbor.getValue() - this.normalizeTemperature(tempCenter));
                    clusterFactor += Math.max(0.0, proximity);
                }
            }

            double adjustedSuitability = (suitability + clusterFactor / (double) temperatureZones.size()) / 2.0;
            int rarity = (int) ((1.0 - adjustedSuitability) * 10.0);
            rarityMap.put(zone, rarity);
        }

        return rarityMap;
    }

    private Map<String, Integer> generateRarityMap() {
        Map<String, Integer> rarityMap = new LinkedHashMap<>();
        double coldness = this.META.temperatureVariance;
        double elevation = this.META.heightVariance;
        Map<String, double[][]> biomeZones = Map.of(
                "TROPICAL",
                new double[][]{{20.0, 30.0}, {5000.0, 7000.0}},
                "TEMPERATE",
                new double[][]{{5.0, 20.0}, {1000.0, 3000.0}},
                "POLAR",
                new double[][]{{-50.0, 0.0}, {0.0, 500.0}},
                "BOREAL",
                new double[][]{{-10.0, 10.0}, {500.0, 1000.0}},
                "SUBTROPICAL",
                new double[][]{{10.0, 25.0}, {3000.0, 5000.0}}
        );

        for (Entry<String, double[][]> entry : biomeZones.entrySet()) {
            String zone = entry.getKey();
            double[] tempRange = entry.getValue()[0];
            double[] elevRange = entry.getValue()[1];
            double tempCenter = (tempRange[0] + tempRange[1]) / 2.0;
            double normalizedTemp = this.normalizeTemperature(tempCenter);
            double tempSuitability = Math.max(0.0, 1.0 - Math.abs(coldness - normalizedTemp));
            double elevSuitability = this.calculateHeightSuitability(elevation, elevRange);
            double suitability = (tempSuitability + elevSuitability) / 2.0;
            int rarity = (int) (suitability * 10.0);
            rarityMap.put(zone, rarity);
        }

        return rarityMap;
    }

    private Map<String, Integer> generateLandHeightRarityMap() {
        Map<String, Integer> heightRarityMap = new LinkedHashMap<>();
        double elevation = this.META.heightVariance;
        Map<String, double[]> elevationZones = Map.of(
                "MOUNTAINS_SMALL",
                new double[]{1000.0, 3700.0},
                "MOUNTAINS_LARGE",
                new double[]{3000.0, 7000.0},
                "HILLS_SMALL",
                new double[]{300.0, 1000.0},
                "HILLS_LARGE",
                new double[]{800.0, 2500.0},
                "FLAT",
                new double[]{0.0, 300.0}
        );

        for (Entry<String, double[]> entry : elevationZones.entrySet()) {
            String zone = entry.getKey();
            int rarity = this.getLHRarity(entry, elevation);
            heightRarityMap.put(zone, rarity);
        }

        return heightRarityMap;
    }

    private int getLHRarity(Entry<String, double[]> entry, double elevation) {
        double[] elevationRange = entry.getValue();
        double elevationCenter = (elevationRange[0] + elevationRange[1]) / 2.0;
        double suitability = Math.max(0.0, 1.0 - Math.abs(elevation - this.normalizeHeight(elevationCenter)));
        return (int) ((1.0 - suitability) * 10.0);
    }

    private double normalizeHeight(double height) {
        double minHeight = 0.0;
        double maxHeight = 8000.0;
        return (height - minHeight) / (maxHeight - minHeight) * 2.0 - 1.0;
    }

    private double normalizeTemperature(double temperature) {
        double minTemp = -50.0;
        double maxTemp = 30.0;
        return (temperature - minTemp) / (maxTemp - minTemp) * 2.0 - 1.0;
    }

    public String getPackName() {
        return this.packID + ".zip";
    }

    private double calculateHeightSuitability(double elevation, double[] elevRange) {
        double minElev = elevRange[0];
        double maxElev = elevRange[1];
        double elevCenter = (minElev + maxElev) / 2.0;
        return Math.max(0.0, 1.0 - Math.abs(elevation - elevCenter) / (maxElev - minElev));
    }

    @Override
    public String getIdentifier() {
        return this.packID;
    }
}
