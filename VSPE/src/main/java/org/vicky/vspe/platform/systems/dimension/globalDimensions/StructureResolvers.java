package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.StructureTag;
import org.vicky.vspe.platform.NativeTypeMapper;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ProceduralStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.VerticalPlacement;

import java.util.List;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.parts.RealisticRose.realisticRoseTipMulti;

/**
 * @param <T> the platform block "stated" data
 */
public class StructureResolvers<T> {
    public final List<Pair<PlatformStructure<T>, StructureRule>> structures = List.of(
            new Pair<>(
                    new ProceduralStructure<>(
                            new NoAIProceduralTreeGenerator.NoAIPTGBuilder<T>()
                                    .trunkWidth(10, 15)
                                    .trunkHeight(70, 120)
                                    .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                                    .branchType(NoAIProceduralTreeGenerator.BranchingType.TAPERED_SPINDLE)
                                    .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.REALISTIC)
                                    .realismLevel(0.7)
                                    .randomness(0.7)
                                    .spacing(2)
                                    .branchingPointRange(0.45, 1.0)
                                    .branchMaxDevianceAngle(7)
                                    .branchDepth(2)
                                    .leafPropagationChance(0.67)
                                    .branchPropagationChance(1.92)
                                    .branchSizeDecay(0.64)
                                    .maxBranchAmount(7)
                                    .tipDecoration(realisticRoseTipMulti(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            2
                                    ))
                                    .woodMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
                                    .leafMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")))
                    ),
                    new StructureRule(
                            ResourceLocation.from("crymorra:large_magenta_frost_tree"),
                            StructureTag.TREELIKE,
                            Rarity.LEGENDARY,
                            2,
                            0.97,
                            10,
                            0,
                            VerticalPlacement.SURFACE,
                            List.of(ResourceLocation.from("crymorra:magenta_forest"))
                    )
            ),
            new Pair<>(
                    new ProceduralStructure<>(
                            new NoAIProceduralTreeGenerator.NoAIPTGBuilder<T>()
                                    .trunkWidth(10, 15)
                                    .trunkHeight(70, 120)
                                    .trunkType(NoAIProceduralTreeGenerator.TrunkType.TAPERED_SPINDLE)
                                    .branchType(NoAIProceduralTreeGenerator.BranchingType.TAPERED_SPINDLE)
                                    .leafType(NoAIProceduralTreeGenerator.LeafPopulationType.REALISTIC)
                                    .realismLevel(0.7)
                                    .randomness(0.7)
                                    .spacing(2)
                                    .branchingPointRange(0.45, 1.0)
                                    .branchMaxDevianceAngle(7)
                                    .branchDepth(2)
                                    .leafPropagationChance(0.67)
                                    .branchPropagationChance(1.92)
                                    .branchSizeDecay(0.64)
                                    .maxBranchAmount(7)
                                    .tipDecoration(realisticRoseTipMulti(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            2
                                    ))
                                    .woodMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
                                    .leafMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")))
                    ),
                    new StructureRule(
                            ResourceLocation.from("crymorra:large_magenta_willow_tree"),
                            StructureTag.TREELIKE,
                            Rarity.MYTHIC,
                            2,
                            0.97,
                            16,
                            0,
                            VerticalPlacement.SURFACE,
                            List.of(ResourceLocation.from("crymorra:magenta_forest"))
                    )
            )
    );
}
