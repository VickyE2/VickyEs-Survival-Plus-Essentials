package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.StructureTag;
import org.vicky.vspe.platform.NativeTypeMapper;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.NoAIProceduralTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ThesisTreeStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis.ThesisBasedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

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
            ),
            new Pair<>(
                    new ProceduralStructure<>(
                            new ThesisTreeStructureGenerator.Builder<T>()
                                    .trunkRadius(10, 19)
                                    .trunkHeight(7, 8)
                                    .treeAge(50)
                                    .placeLeaves(true)
                                    .growthData(new ThesisBasedTreeGenerator.GrowthData.Builder(4)
                                            .senescenceAffectsChildren(true)
                                            .distanceBetweenChildren(5)
                                            .maxDepth(2)
                                            .multiTrunkismMaxAmount(8)
                                            .multiTrunkismAge(15)
                                            .apicalControl(0.9f)
                                            .addOverrides(
                                                    ThesisBasedTreeGenerator.Overrides.TrunkOverrides.MULTI_TRUNKISM
                                            )
                                            .build())
                                    .leafDetails(ThesisTreeStructureGenerator.LeafDetails.newBuilder()
                                            .startIndex(1)
                                            // .leafBreath(1.0f)
                                            // .leafLength(1.5f)
                                            // .leafSpawningPoint(0.5f)
                                            .useRealisticType(true)
                                            .realismPow(0.77)
                                            .build())
                                    .seed(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()).getInt())
                                    .trunkMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
                                    .leafMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")))

                    ),
                    new StructureRule(
                            ResourceLocation.from("crymorra:large_magenta_multi_branched_willow_tree"),
                            StructureTag.TREELIKE,
                            Rarity.LEGENDARY,
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
