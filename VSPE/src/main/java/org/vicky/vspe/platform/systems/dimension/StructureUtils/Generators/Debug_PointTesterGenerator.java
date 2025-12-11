package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class Debug_PointTesterGenerator<T> extends ProceduralStructureGenerator<T> {

    private final Supplier<Set<Vec3>> points;
    private final PlatformBlockState<T> material;

    public Debug_PointTesterGenerator(Supplier<Set<Vec3>> points, PlatformBlockState<T> material) {
        this.points = points;
        this.material = material;
    }

    @Override
    public BlockVec3i getApproximateSize() {
        return new BlockVec3i(0, 0, 0);
    }

    @Override
    protected void performGeneration(RandomSource rnd, Vec3 origin, List outPlacements, Map outActions) {
        points.get().forEach( it ->
                guardAndStore(it, material, false)
        );
    }
}
