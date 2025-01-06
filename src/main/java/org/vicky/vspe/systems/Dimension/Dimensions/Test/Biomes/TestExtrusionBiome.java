package org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.TestGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Cave;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.PatternLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.BaseStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;

import java.util.HashMap;
import java.util.Map;

public class TestExtrusionBiome extends BaseBiome {
    public TestExtrusionBiome() {
        super(
                "TEST_EXTRUSION_BIOME",
                0x433567,
                Biome.LUSH_CAVES,
                Cave.SMALL_TEMPERATE,
                new Precipitation(1.0f, Precipitation.PrecipitaionType.RAIN),
                Rarity.RARE
        );

        addExtendibles(Extendibles.CAVE, Extendibles.BASE);

        NoiseSampler OS2 = NoiseSampler.OPEN_SIMPLEX_2;
        OS2.setParameter("frequency", 0.1);
        NoiseSampler CLR = NoiseSampler.CELLULAR;
        CLR.setParameter("frequency", 0.08);
        CLR.setParameter("return", "CellValue");
        NoiseSampler p2NS = NoiseSampler.DOMAIN_WARP;
        p2NS.setParameter("amplitude", 2);
        p2NS.setParameter("warp", OS2);
        p2NS.setParameter("sampler", CLR);

        Palette palette1 = new Palette("CAVE_PALETTE_ONE");
            Map<Material, Integer> materials1 = new HashMap<>();
            materials1.put(Material.MOSS_BLOCK, 2);
            materials1.put(Material.STONE, 2);
        palette1.addLayer(materials1, 4);
        palette1.setSampler(p2NS);
        Palette palette2 = new Palette("CAVE_PALETTE_ONE");
            Map<Material, Integer> materials2 = new HashMap<>();
            materials1.put(Material.MOSS_BLOCK, 2);
            materials1.put(Material.STONE, 1);
            materials1.put(Material.DEEPSLATE, 2);
        palette2.addLayer(materials2, 4);
        palette2.setSampler(p2NS);

        addPalettes(palette1, new TestGenerator().META.getMappingFor(MetaClass.MetaVariables.yLevel));
        addPalettes(palette2, new TestGenerator().META.getMappingFor(MetaClass.MetaVariables.PALETTE_TOP_DEEPSLATE));

        Feature feature = new Feature("TEST_VINES");
            NoiseSampler sampler = NoiseSampler.WHITE_NOISE;
            sampler.setParameter("salt", Utilities.generateRandomNumber());
            Ymlable distributor = new Sampler(sampler, 0.17f);
        feature.setDistributor(distributor);
            Pattern pattern = new Pattern(Pattern.Type.AND, Pattern.Type.MATCH);
            pattern.addBlock(Material.AIR, 0);
            pattern.addBlock(Material.MOSS_BLOCK, 1);
            MetaMap min = new TestGenerator().META.getMappingFor(MetaClass.MetaVariables.PALETTE_BOTTOM_BEDROCK);
            min.performOperation(16, ArithmeticOperation.ADD);
            MetaMap max = new TestGenerator().META.getMappingFor(MetaClass.MetaVariables.oceanLevel);
            max.performOperation(13, ArithmeticOperation.SUBTRACT);
            Ymlable locator = new PatternLocator(pattern, new Range(min, max));
        feature.setLocator(locator);
        feature.setStructuresDistributor(NoiseSampler.CONSTANT);
            BaseStructure structure = new BaseStructure("VINE");
            structure.add
        feature.addStructure(structure, 10);
    }
}
