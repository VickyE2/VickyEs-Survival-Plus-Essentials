package org.vicky.vspe.platform.systems.dimension;

public enum TimeCurve {
    LINEAR {
        @Override
        public float apply(double t) {
            return (float) clamp(t);
        }
    },
    QUADRATIC {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (t * t);
        }
    },
    TRUNK_TAPER {
        @Override
        public float apply(double t) {
            t = clamp(t);
            // Caller uses t measured from the top -> flip so t==0 is base.
            t = 1.0 - t;

            if (t < 0.23) {
                double nt = t / 0.23;
                return (float) (1.0 - Math.pow(nt, 1.5) * 0.5);
            } else if (t < 0.70) {
                double nt = (t - 0.23) / (0.70 - 0.23);
                return (float) (0.5 - nt * 0.2);
            } else {
                double nt = (t - 0.70) / (1.0 - 0.70);
                return (float) (0.3 - Math.pow(nt, 2) * 0.3);
            }
        }
    },
    INVERTED_QUADRATIC {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (1.0 - Math.pow(1.0 - t, 2));
        }
    },
    CUBIC {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) Math.pow(t, 3);
        }
    },
    INVERTED_CUBIC {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (1.0 - Math.pow(1.0 - t, 3));
        }
    },
    B_SPLINE {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (t * t * (3 - 2 * t));
        } // smoothstep
    },
    SINE {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (0.5 * (1 - Math.cos(2 * Math.PI * t)));
        }
    },
    COSINE {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (0.5 * (1 + Math.sin(2 * Math.PI * (t - 0.25))));
        }
    },
    EASE_IN_OUT_CUBIC {
        @Override
        public float apply(double t) {
            t = clamp(t);
            if (t < 0.5) return (float) (4.0 * t * t * t);
            double u = -2.0 * t + 2.0;
            return (float) (1.0 - (Math.pow(u, 3) / 2.0));
        }
    },
    EASE_IN_OUT_QUINT {
        @Override
        public float apply(double t) {
            t = clamp(t);
            if (t < 0.5) return (float) (16.0 * Math.pow(t, 5));
            double u = -2.0 * t + 2.0;
            return (float) (1.0 - Math.pow(u, 5) / 2.0);
        }
    },
    LOGISTIC {
        @Override
        public float apply(double t) {
            t = clamp(t);
            double k = 12.0;
            double y = 1.0 / (1.0 + Math.exp(-k * (t - 0.5)));
            return (float) y;
        }
    },
    SQRT {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) Math.sqrt(t);
        }
    },
    EXPONENTIAL {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (Math.pow(2, 10 * (t - 1))); // grows very steep
        }
    },
    EXPONENTIAL_OUT {
        @Override
        public float apply(double t) {
            t = clamp(t);
            return (float) (1 - Math.pow(2, -10 * t));
        }
    },
    GAUSSIAN {
        @Override
        public float apply(double t) {
            t = clamp(t);
            double sigma = 0.15;
            double x = (t - 0.5) / sigma;
            double y = Math.exp(-0.5 * x * x);
            return (float) y;
        }
    },
    TRIANGLE {
        @Override
        public float apply(double t) {
            t = clamp(t);
            if (t < 0.5) return (float) (2.0 * t);
            return (float) (2.0 * (1.0 - t));
        }
    },
    SAWTOOTH {
        @Override
        public float apply(double t) {
            return LINEAR.apply(t);
        }
    },
    PULSE {
        @Override
        public float apply(double t) {
            t = clamp(t);
            double duty = 0.10;
            return t < duty ? 1f : 0f;
        }
    },
    BEZIER_STANDARD {
        @Override
        public float apply(double t) {
            t = clamp(t);
            double u = 1.0 - t;
            double y1 = 0.1, y2 = 0.9;
            double y = 3.0 * u * u * t * y1 + 3.0 * u * t * t * y2 + t * t * t;
            return (float) y;
        }
    },
    CUSTOM_LAMBDA {
        @Override
        public float apply(double t) {
            return LINEAR.apply(t);
        } // placeholder
    };

    // small helper
    protected static double clamp(double v) {
        if (v <= 0.0) return 0.0;
        return Math.min(v, 1.0);
    }

    /**
     * New primary method: call this with progress in [0,1].
     */
    public abstract float apply(double progress);

    /**
     * Backwards-compatible overload for callers that still have (dayTime, dayLength).
     * By default this maps dayTime/dayLength -> [0,1] WITHOUT wrapping to zero for exact boundaries:
     * - If dayLength less than or equal to 0 returns apply(0)
     * - Otherwise computes fractional progress in the current cycle and for exact multiples
     * of dayLength behaves as 1.0 (end of cycle) instead of wrapping to 0.0.
     * Note: if you relied on circular wrapping behavior (mod), prefer converting to progress
     * yourself and call apply(double).
     */
    public float apply(long dayTime, long dayLength) {
        if (dayLength <= 0) return apply(0.0);
        // Prefer a stable mapping: map the fractional position within the current cycle
        long mod = Math.floorMod(dayTime, dayLength);
        double frac = (double) mod / (double) dayLength;

        // Special-case an exact multiple of dayLength: if dayTime != 0 and mod == 0, treat as 1.0
        // This avoids the "1000 % 1000 == 0 -> t==0" surprise at the exact end.
        if (mod == 0 && dayTime != 0L) {
            return apply(1.0);
        }
        return apply(frac);
    }
}