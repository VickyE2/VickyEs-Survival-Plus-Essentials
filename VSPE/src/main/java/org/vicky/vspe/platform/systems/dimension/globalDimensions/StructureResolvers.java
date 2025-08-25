package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.Mirror;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.utils.Rotation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.StructureTag;
import org.vicky.vspe.platform.NativeTypeMapper;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ProceduralBranchedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ProceduralStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.StructureRule;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @param <T> the platform block "stated" data
 */
public class StructureResolvers<T> {
    public final List<Pair<PlatformStructure<T>, StructureRule>> structures = List.of(
            new Pair<>(
                    new ProceduralStructure<>(
                            new ProceduralBranchedTreeGenerator.Builder<T>()
                                    .branchStart(0.5f)
                                    .trunkThickness(10)
                                    .height(20)
                                    .branchDepth(2)
                                    .branchShrinkPerLevel(0.3f)
                                    .maxBranchShrink(0.7f)
                                    .branchThickness(5)
                                    .qualityFactor(0.7f)
                                    .randomness(0.2259f)
                                    .woodMaterial(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_log"))
                                    )
                                    .addDanglingSequence(new ProceduralBranchedTreeGenerator.Builder.BlockSeqEntry<>(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            0.0,
                                            0.5
                                    ))
                                    .addDanglingSequence(new ProceduralBranchedTreeGenerator.Builder.BlockSeqEntry<>(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_vines")),
                                            0.5,
                                            1.0
                                    ))
                                    .setLeaf(
                                            (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState(NativeTypeMapper.getFor("vspe:magenta_frost_leaves")),
                                            ProceduralBranchedTreeGenerator.LeafType.DANGLING,
                                            Map.of("length", 20)
                                    )
                                    .roots()
                                    .build()
                    ),
                    new StructureRule(
                            ResourceLocation.from("crymorra:magenta_frost_tree"),
                            Set.of(StructureTag.TREELIKE),
                            Rotation.NONE,
                            Mirror.NONE,
                            1,
                            0.5,
                            20
                    )
            )
    );
}
