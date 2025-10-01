package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

public class HelixPointFactory implements PointFactory {

    private final double verticalCompression;
    private final double helixTaper;
    private final double coils;

    public HelixPointFactory(double coils, double helixTaper, double verticalCompression) {
        this.coils = coils;
        this.helixTaper = helixTaper;
        this.verticalCompression = verticalCompression;
    }

    @Override
    public PointFactory copy() {
        return new HelixPointFactory(coils, helixTaper, verticalCompression);
    }

    @Override
    public Vec3 createFor(double progress, CorePointsFactory.Params p) {
        double angle = 2.0 * Math.PI * coils * progress;

        // taper goes from spiral (1.0) → helix (0.0)
        double taperFactor = (1.0 - progress);
        double radiusFactor = (1.0 - helixTaper) + taperFactor * helixTaper;
        double baseR = p.width * radiusFactor;

        // --- axis from yaw/pitch ---
        double yaw = Math.toRadians(p.yawDegrees);
        double pitch = Math.toRadians(p.pitchDegrees);

        // Default forward axis is +Y
        Vec3 w = new Vec3(
                Math.sin(yaw) * Math.cos(pitch),  // X
                Math.cos(pitch),                  // Y (up at pitch=0)
                Math.cos(yaw) * Math.cos(pitch)   // Z
        ).normalize();

        // build orthonormal basis (u,v,w)
        Vec3 arbitrary = Math.abs(w.getIntX()) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
        Vec3 u = w.crossProduct(arbitrary).normalize();
        Vec3 v = w.crossProduct(u).normalize();

        // position = along axis + radial offset
        Vec3 axis = w.multiply(progress * p.height * verticalCompression);
        Vec3 radial = u.multiply(Math.cos(angle) * baseR).add(v.multiply(Math.sin(angle) * baseR));

        return axis.add(radial);
    }

}
