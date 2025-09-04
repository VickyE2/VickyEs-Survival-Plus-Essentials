package org.vicky.vspe.platform.systems.dimension;

public enum TimeCurve {
    LINEAR {
        @Override
        public float apply(long dayTime, long dayLength) {
            double cycle = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) cycle;
        }
    },
    QUADRATIC {
        @Override
        public float apply(long dayTime, long dayLength) {
            double cycle = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) (cycle * cycle);
        }
    },
    INVERTED_QUADRATIC {
        @Override
        public float apply(long dayTime, long dayLength) {
            double cycle = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) (1.0 - Math.pow(1.0 - cycle, 2));
        }
    },
    CUBIC {
        @Override
        public float apply(long dayTime, long dayLength) {
            double cycle = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) Math.pow(cycle, 3);
        }
    },
    INVERTED_CUBIC {
        @Override
        public float apply(long dayTime, long dayLength) {
            double cycle = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) (1.0 - Math.pow(1.0 - cycle, 3));
        }
    },
    B_SPLINE { // smoothstep / cubic Hermite-like

        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) (t * t * (3 - 2 * t)); // smoothstep
        }
    },

    /* --- New curves below --- */

    SINE {
        // Smooth cyclic wave: 0 -> 1 -> 0 over the cycle (dawn->midday->dusk if you want)
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            // 0 at t=0, 1 at t=0.5, 0 at t=1. Use cosine to get correct phase.
            return (float) (0.5 * (1 - Math.cos(2 * Math.PI * t)));
        }
    },

    COSINE {
        // Phase-shifted sine — sometimes easier for certain offsets
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) (0.5 * (1 + Math.sin(2 * Math.PI * (t - 0.25))));
        }
    },

    EASE_IN_OUT_CUBIC {
        // Slow start, fast middle, slow end
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            if (t < 0.5) {
                return (float) (4.0 * t * t * t);
            } else {
                double u = -2.0 * t + 2.0;
                return (float) (1.0 - (Math.pow(u, 3) / 2.0));
            }
        }
    },

    EASE_IN_OUT_QUINT {
        // Even steeper ease in/out for snappier mid-transition
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            if (t < 0.5) {
                return (float) (16.0 * Math.pow(t, 5));
            } else {
                double u = -2.0 * t + 2.0;
                return (float) (1.0 - Math.pow(u, 5) / 2.0);
            }
        }
    },

    LOGISTIC {
        // Sigmoid centered at 0.5: slow until mid, then quick flip, then slow
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            double k = 12.0; // steepness; tweakable
            double y = 1.0 / (1.0 + Math.exp(-k * (t - 0.5)));
            // Already in (0,1) approximately — return as float
            return (float) y;
        }
    },

    GAUSSIAN {
        // Bell curve peaked at mid-cycle (0.5). Good for midday-focused effects.
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            double sigma = 0.15; // width of the bell (adjust as needed)
            double x = (t - 0.5) / sigma;
            double y = Math.exp(-0.5 * x * x);
            return (float) y; // peak 1 at t=0.5
        }
    },

    TRIANGLE {
        // Ramps up then down linearly
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            if (t < 0.5) return (float) (2.0 * t);
            else return (float) (2.0 * (1.0 - t));
        }
    },

    SAWTOOTH {
        // Repeats 0->1 each cycle (same as LINEAR but explicit name)
        @Override
        public float apply(long dayTime, long dayLength) {
            return LINEAR.apply(dayTime, dayLength);
        }
    },

    PULSE {
        // Square-ish pulse: mostly 0, short on-time. Duty cycle fixed at 10%
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            double duty = 0.10; // 10% on-time
            return t < duty ? 1f : 0f;
        }
    },

    BEZIER_STANDARD {
        // Cubic Bezier with two interior control points (y1, y2) -> designer-tuned S-curve
        // control points chosen to give a gentle S: (0.25, 0.1) and (0.75, 0.9)
        @Override
        public float apply(long dayTime, long dayLength) {
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            double u = 1.0 - t;
            double y1 = 0.1;
            double y2 = 0.9;
            // Bezier formula: B(t) = u^3 * 0 + 3 u^2 t * y1 + 3 u t^2 * y2 + t^3 * 1
            double y = 3.0 * u * u * t * y1 + 3.0 * u * t * t * y2 + t * t * t;
            return (float) y;
        }
    },

    CUSTOM_LAMBDA {
        // A placeholder: you can set a custom Function<Double,Double> at runtime if you want.
        @Override
        public float apply(long dayTime, long dayLength) {
            // default identity (acts like LINEAR) — replace via setter if you add one
            double t = (double) (dayTime % dayLength) / (double) dayLength;
            return (float) t;
        }
    };

    public abstract float apply(long dayTime, long dayLength);
}
