package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Extrusions;

import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Biomes.Abstract.Cave;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Structures.GlowBerriesVines;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test.TestMeta;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Feature;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Featureable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.PatternLocator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.DOMAIN_WARP;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.OPEN_SIMPLEX_2;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.WHITE_NOISE;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette.Palette;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MossCaves extends BaseBiome {
    public MossCaves() {
        super("MOSS_CAVES", 0x389e6e, BiomeCategory.LUSH_CAVES, org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums.Cave.SMALL_TEMPERATE, new Precipitation(1.0F, Precipitation.PrecipitaionType.RAIN), Rarity.RARE);
        addColor(Colorable.WATER_FOG, 0x0098bd);
        this.addExtendibles(Extendibles.BASE);
        this.addExtendibles(new Cave(new Random().nextInt(4, 10)));
        NoiseSampler OS2 = new OPEN_SIMPLEX_2().setParameter("frequency", 0.1);
        CELLULAR CLR = new CELLULAR().setParameter("frequency", 0.08).setParameter("return", "CellValue");
        NoiseSampler p2NS = new DOMAIN_WARP().setParameter("amplitude", 2).setParameter("warp", OS2).setParameter("sampler", CLR);
        Palette palette1 = new Palette("CAVE_PALETTE_ONE");
        Map<PlatformMaterial, Integer> materials1 = new HashMap<>();
        materials1.put(SimpleBlockState.Companion.from("minecraft:moss_block", (it) -> it).getMaterial(), 2);
        materials1.put(SimpleBlockState.Companion.from("minecraft:stone", (it) -> it).getMaterial(), 2);
        palette1.addLayer(materials1, 4);
        palette1.setSampler(p2NS.getYml());
        Palette palette2 = new Palette("CAVE_PALETTE_TWO");
        Map<PlatformMaterial, Integer> materials2 = new HashMap<>();
        materials2.put(SimpleBlockState.Companion.from("minecraft:moss_block", (it) -> it).getMaterial(), 2);
        materials2.put(SimpleBlockState.Companion.from("minecraft:stone", (it) -> it).getMaterial(), 1);
        materials2.put(SimpleBlockState.Companion.from("minecraft:deepslate", (it) -> it).getMaterial(), 2);
        palette2.addLayer(materials2, 4);
        palette2.setSampler(p2NS.getYml());
        this.addPalettes(palette1, new TestMeta().getMappingFor(MetaClass.MetaVariables.yLevel));
        this.addPalettes(palette2, new TestMeta().getMappingFor(MetaClass.MetaVariables.PALETTE_TOP_DEEPSLATE));
        Feature feature = new Feature("CAVE_GLOW_VINES", FeatureType.CAVE);
        NoiseSampler sampler = new WHITE_NOISE().setParameter("salt", Utilities.generateRandomNumber());
        Ymlable distributor = new Sampler(sampler, 0.17F);
        feature.setDistributor(distributor);
        Pattern pattern = new Pattern(Pattern.Type.AND);
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:air", (it) -> it).getMaterial(), 0));
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:moss_block", (it) -> it).getMaterial(), 1));
        MetaMap min = new TestMeta().getMappingFor(MetaClass.MetaVariables.PALETTE_BOTTOM_BEDROCK);
        min.performOperation(16, ArithmeticOperation.ADD);
        MetaMap max = new TestMeta().getMappingFor(MetaClass.MetaVariables.oceanLevel);
        max.performOperation(13, ArithmeticOperation.SUBTRACT);
        Ymlable locator = new PatternLocator(pattern, new Range(min, max));
        feature.setLocator(locator);
        feature.setStructuresDistributor(CLR);
        feature.addStructure(new GlowBerriesVines(14), 10);
        this.addFeaturesToParam(List.of(feature), Featureable.FLORA);
    }
}
