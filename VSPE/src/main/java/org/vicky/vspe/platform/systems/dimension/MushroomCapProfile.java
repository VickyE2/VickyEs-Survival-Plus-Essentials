package org.vicky.vspe.platform.systems.dimension;

public enum MushroomCapProfile {
    PARABOLA {
        @Override
        public double height(double rn, double capHeight) {
            // classic parabola: tall in middle, shallow edges
            return capHeight * (1.0 - rn * rn);
        }
    },
    GAUSSIAN {
        @Override
        public double height(double rn, double capHeight) {
            double k = 2.2; // steepness factor
            return capHeight * Math.exp(-k * rn * rn);
        }
    },
    HEMISPHERE {
        @Override
        public double height(double rn, double capHeight) {
            return capHeight * Math.sqrt(Math.max(0.0, 1.0 - rn * rn));
        }
    },
    SHARP_SNOUT {
        @Override
        public double height(double rn, double capHeight) {
            // shrinks fast after broad base, snout-like top
            // broad up to ~15% radius, then sharp taper
            if (rn < 0.15) {
                return capHeight; // flat plateau near center
            }
            // exponential decay beyond plateau
            double falloff = (rn - 0.15) / 0.85;
            return capHeight * Math.pow(1.0 - falloff, 3.0);
        }
    },
    CONE {
        @Override
        public double height(double rn, double capHeight) {
            // simple linear cone taper
            return capHeight * (1.0 - rn);
        }
    },
    BELL {
        @Override
        public double height(double rn, double capHeight) {
            // bell shape (like some real mushrooms): starts vertical, then flares
            return capHeight * Math.sin(Math.PI * (1.0 - rn) * 0.5);
        }
    },
    PLATE {
        @Override
        public double height(double rn, double capHeight) {
            // almost flat, small bump in middle
            return capHeight * (0.2 + 0.8 * (1.0 - rn) * (1.0 - rn));
        }
    };

    /**
     * Compute vertical height for a normalized radius.
     *
     * @param rn        normalized radius (0..1)
     * @param capHeight max height of the cap
     * @return height at this radius
     */
    public abstract double height(double rn, double capHeight);
}
