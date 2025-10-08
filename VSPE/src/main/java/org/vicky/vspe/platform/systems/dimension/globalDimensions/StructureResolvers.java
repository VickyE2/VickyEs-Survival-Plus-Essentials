package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.StructureTag;
import org.vicky.vspe.platform.NativeTypeMapper;
import org.vicky.vspe.platform.systems.dimension.MushroomCapProfile;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ProceduralStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.VerticalPlacement;

import java.util.List;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts.RealisticRose.realisticRoseTipSingle;

/**
 * @param <T> the platform block "stated" data
 */
public class StructureResolvers<T> {
    public final List<Pair<PlatformStructure<T>, StructureRule>> structures = List.of(
            new Pair<>(
                    new ProceduralStructure<>(
                            new NoAIProceduralTreeGenerator.NoAIPTGBuilder<T>()
                                    .trunkWidth(10, 15)
                                    .trunkHeight(70, 110)
                                    .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                                    .branchType(NoAIProceduralTreeGenerator.BranchingType.TAPERED_SPINDLE)
                                    .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.ON_BRANCH_TIP)
                                    .randomness(0.8)
                                    .vineSequenceMaterial(List.of(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_vine"))
                                    ))
                                    .tipDecoration(realisticRoseTipSingle(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            2
                                    ))
                                    .spacing(5)
                                    .vineHeight(0.45)
                                    .leafPropagationChance(0.67)
                                    .branchPropagationChance(0.78)
                                    .branchSizeDecay(0.95)
                                    .maxBranchAmount(7)
                                    .branchingPointRange(0.35, 0.80)
                                    .branchMaxDevianceAngle(7)
                                    .branchMaxHorizontalDevianceAngle(20)
                                    .branchDepth(2)
                                    .slantAngleRange(-50, 50)
                                    .mushroomCapWidth(17, 20)
                                    .capProfile(MushroomCapProfile.SHARP_SNOUT)
                                    .branchVerticalDensity(2)
                                    .woodMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
                                    .leafMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")))
                                    .build()
                    ),
                    new StructureRule(
                            ResourceLocation.from("crymorra:magenta_frost_tree"),
                            StructureTag.TREELIKE,
                            Rarity.EPIC,
                            2,
                            0.97,
                            10,
                            0,
                            VerticalPlacement.SURFACE,
                            List.of(ResourceLocation.from("crymorra:magenta_forest"))
                    )
            )
    );
}
