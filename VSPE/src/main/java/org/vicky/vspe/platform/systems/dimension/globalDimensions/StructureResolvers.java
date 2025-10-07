package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.StructureTag;
import org.vicky.vspe.platform.NativeTypeMapper;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ProceduralStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.VerticalPlacement;

import java.util.List;
import java.util.Set;

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
                                    .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.HANGING_MUSHROOM)
                                    .vineSequenceMaterial(List.of(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_vine"))
                                    ))
                                    .spacing(5)
                                    .vineHeight(0.45)
                                    .leafPropagationChance(0.67)
                                    .branchPropagationChance(0.78)
                                    .branchSizeDecay(0.75)
                                    .branchingPointRange(0.35, 0.80)
                                    .branchMaxDevianceAngle(5)
                                    .branchMaxHorizontalDevianceAngle(20)
                                    .branchDepth(2)
                                    .maxBranchAmount(5)
                                    .slantAngleRange(-50, 50)
                                    .mushroomCapWidth(40, 70)
                                    .branchVerticalDensity(2)
                                    .woodMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
                                    .leafMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")))
                                    .build()
                    ),
                    new StructureRule(
                            ResourceLocation.from("crymorra:magenta_frost_tree"),
                            Set.of(StructureTag.TREELIKE),
                            5,
                            0.97,
                            1,
                            0,
                            VerticalPlacement.SURFACE,
                            List.of(ResourceLocation.from("crymorra:magenta_forest"))
                    )
            )
    );
}
