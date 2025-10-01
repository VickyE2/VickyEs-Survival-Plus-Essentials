package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

public class StraightPointFactory implements PointFactory {
    @Override
    public PointFactory copy() {
        return new StraightPointFactory();
    }

    @Override
    public Vec3 createFor(double progress, CorePointsFactory.Params params) {
        return new Vec3(0.0, progress * params.height, 0.0);
    }
}
