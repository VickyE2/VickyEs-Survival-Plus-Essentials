package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ProceduralBranchedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.PlatformStructure;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.ProceduralStructure;

import java.util.Map;

public class StructureResolvers<T extends PlatformBlockState<T>> {
    public final Map<ResourceLocation, PlatformStructure<T>> structures = Map.of(
            ResourceLocation.from("crymorra:pink_frost_tree"), new ProceduralStructure<>(
                    new ProceduralBranchedTreeGenerator.Builder<T>()
                            .branchStart(0.5f)
                            .trunkThickness(10)
                            .branchDepth(2)
                            .branchShrinkPerLevel(0.3f)
                            .maxBranchShrink(0.7f)
                            .branchThickness(5)
                            .roots()
                            .build()
            )
    );
}
