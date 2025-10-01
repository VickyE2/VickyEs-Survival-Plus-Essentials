package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

public interface PointFactory {
    PointFactory copy();

    Vec3 createFor(double progress, CorePointsFactory.Params params);
}
