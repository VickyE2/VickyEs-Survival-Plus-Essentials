package org.vicky.vspe.platform.systems.dimension.StructureUtils.factories;

import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory;

import static org.vicky.vspe.platform.systems.dimension.StructureUtils.CorePointsFactory.signedPow;

/**
 * Generates an "S-shaped" 3D curve along the Y-axis with sinusoidal oscillations in X and Z.
 * <p>
 * The curve starts at {@code (0,0,0)} and extends upward in Y by {@code p.height}.
 * For each normalized progress {@code t ∈ [0,1]}, the curve computes:
 * <ul>
 *   <li>{@code y = t * p.height} – linear vertical growth</li>
 *   <li>{@code x = sin(2π * frequency * t + phase)^sharpness * amplitude * p.width}</li>
 *   <li>{@code z = cos(2π * frequency * t + phase)^sharpness * amplitude * p.width}</li>
 * </ul>
 * <p>
 * The result is a helical or "S-curve" style path where the parameters control amplitude,
 * frequency, sharpness of bends, and rotational offset.
 */
public class SCurveFactory implements PointFactory {

    private final double amplitude, frequency, sharpness, phase;

    /**
     * @param amplitude Scales the horizontal displacement (X and Z).
     *                  <ul>
     *                      <li>Larger values → wider curve radius.</li>
     *                      <li>Smaller values → tighter, narrower oscillations around the Y-axis.</li>
     *                  </ul>
     * @param frequency Number of oscillation cycles along the full height of the curve.
     *                  <ul>
     *                      <li>frequency = 1 → one full sine/cosine loop from bottom to top.</li>
     *                      <li>frequency = 2 → two complete twists along the height.</li>
     *                      <li>Fractional values allow partial turns (e.g., 0.5 = half-turn).</li>
     *                  </ul>
     * @param sharpness Exponent applied via {@link CorePointsFactory#signedPow(double, double)} to warp the sine/cosine shape.
     *                  <ul>
     *                      <li>sharpness equals to 1 → smooth sinusoidal curve (no distortion).</li>
     *                      <li>sharpness greater than 1 → flattens mid-section, accentuates near ±1 (more boxy / squared wave).</li>
     *                      <li>sharpness less than 1 → emphasizes mid-values, softens extremes (rounder wave).</li>
     *                  </ul>
     * @param phase     Horizontal rotation offset in radians.
     *                  <ul>
     *                      <li>phase = 0 → curve starts aligned with X-axis at bottom.</li>
     *                      <li>phase = π/2 → starts aligned with Z-axis.</li>
     *                      <li>Adjusting phase rotates the entire curve around the Y-axis.</li>
     *                  </ul>
     */
    public SCurveFactory(double amplitude, double frequency, double sharpness, double phase) {
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.sharpness = sharpness;
        this.phase = phase;
    }

    @Override
    public PointFactory copy() {
        return new SCurveFactory(amplitude, frequency, sharpness, phase);
    }

    @Override
    public Vec3 createFor(double t, CorePointsFactory.Params p) {
        double y = t * p.height;
        double sx = signedPow(Math.sin(2 * Math.PI * frequency * t + phase), sharpness);
        double sz = signedPow(Math.cos(2 * Math.PI * frequency * t + phase), sharpness);
        double x = sx * amplitude * p.width;
        double z = sz * amplitude * p.width;
        return new Vec3(x, y, z);
    }
}