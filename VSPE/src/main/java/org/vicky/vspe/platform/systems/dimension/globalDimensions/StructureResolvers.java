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
import org.vicky.vspe.platform.systems.dimension.TimeCurve;
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
                            new ThesisTreeStructureGenerator.Builder<T>()
                                    .trunkRadius(2, 4)
                                    .trunkHeight(70, 120)
                                    .treeAge(120)
                                    .placeLeaves(true)
                                    .growthData(new ThesisBasedTreeGenerator.GrowthData.Builder(2.1f)
                                            .maxDepth(2)
                                            .maxKids(-1)
                                            .spread(1.1f)
                                            .influenceRadius(-1)
                                            .killRadius(30)
                                            .attractorClumping(1.0)
                                            .distanceBetweenChildren(10)
                                            .build())
                                    .leafDetails(ThesisTreeStructureGenerator.LeafDetails.newBuilder()
                                            .useRealisticType(true)
                                            .realismPow(0.54)
                                            .startIndex(0)
                                            .leafBreath(4.5f)
                                            .leafLength(0.7f)
                                            .leafSpawningPoint(0.3f)
                                            .leafSpawningPointEnd(1.0f)
                                            .leafThickness(0.08f)
                                            .build())
                                    .trunkMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
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
                            new ThesisTreeStructureGenerator.Builder<T>()
                                    .trunkRadius(4, 6)
                                    .trunkHeight(70, 120)
                                    .treeAge(170)
                                    .placeLeaves(true)
                                    .growthData(new ThesisBasedTreeGenerator.GrowthData.Builder(2.1f)
                                            .senescenceAffectsChildren(true)
                                            .senescenceStartPercentage(0.68)
                                            .senescenceBudPenalty(0.7)   // or 1.2–1.5 for very strong droop
                                            .senescenceDecayRate(0.03)
                                            .senescenceVigorPenalty(0.55)
                                            .senescenceGravBias(1.8)
                                            .maxDepth(1)
                                            .maxKids(-1)
                                            .spread(1.3f)
                                            .influenceRadius(-1)
                                            .killRadius(30)
                                            .trunkGrowthMaxAge(80)
                                            .attractorClumping(1.0)
                                            .distanceBetweenChildren(10)
                                            .pruningHeight(0.5f)
                                            .addOverrides(ThesisBasedTreeGenerator.Overrides.BranchOverrides.MIRROR_BRANCHES)
                                            .build())
                                    .leafDetails(ThesisTreeStructureGenerator.LeafDetails.newBuilder()
                                            .realismPow(0.54)
                                            .startIndex(0)
                                            .leafBreath(0.0f)
                                            .leafLength(0.34f)
                                            .leafSpawningPoint(0.2f)
                                            .leafSpawningPointEnd(1.0f)
                                            .leafThickness(0.0f)
                                            .leafSpacing(0.005f)
                                            .droopFactor(1.0f)
                                            .droopStart(0.0f)
                                            .droopMode(ThesisTreeStructureGenerator.LeafDroopMode.STRAIGHT_DOWN)
                                            .layerCount(3)
                                            .heightReduction(false)
                                            .heightReductionCurve(TimeCurve.EXPONENTIAL_OUT)
                                            .leafType(ThesisTreeStructureGenerator.NodeLeafingType.OPPOSITE)
                                            .shrinkFactor(TimeCurve.LINEAR)
                                            .leafDirection(t -> -10)
                                            .build())
                                    .trunkMaterial((PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log")))
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
                                    .growthData(new ThesisBasedTreeGenerator.GrowthData.Builder(2.1f)
                                            .distanceBetweenChildren(5)
                                            .maxDepth(2)
                                            .spread(1.2f)
                                            .multiTrunkismMaxAmount(8)
                                            .multiTrunkismAge(15)
                                            .minSplittingAge(11)
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
