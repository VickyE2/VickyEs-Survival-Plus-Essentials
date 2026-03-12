package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis;

public class TraitKeeper {

    private long[] data;

    public TraitKeeper(int capacity) {
        data = new long[capacity];
    }

    private void ensure(int id) {
        if (id >= data.length) {
            data = java.util.Arrays.copyOf(data, id + 1);
        }
    }

    public void set(int id, TraitField trait, float value) {
        ensure(id);
        if (trait instanceof TraitField.FloatTrait ft) {
            data[id] = ft.set(data[id], value);
        }
    }

    public void set(int id, TraitField trait, double value) {
        ensure(id);
        if (trait instanceof TraitField.DoubleTrait ft) {
            data[id] = ft.set(data[id], value);
        }
    }

    public void set(int id, TraitField trait, boolean value) {
        ensure(id);
        if (trait instanceof TraitField.BooleanTrait bt) {
            data[id] = bt.set(data[id], value);
        }
    }

    public float getFloat(int id, TraitField.FloatTrait trait) {
        return trait.get(data[id]);
    }

    public double getDouble(int id, TraitField.DoubleTrait trait) {
        return trait.get(data[id]);
    }

    public boolean getBoolean(int id, TraitField.BooleanTrait trait) {
        return trait.get(data[id]);
    }

    public long raw(int id) {
        return data[id];
    }


    public abstract static class TraitField {
        protected final int offset;
        protected final int bits;
        protected final long mask;

        protected TraitField(int offset, int bits) {
            this.offset = offset;
            this.bits = bits;
            this.mask = ((1L << bits) - 1L) << offset;
        }

        protected long extract(long value) {
            return (value & mask) >>> offset;
        }

        protected long insert(long container, long raw) {
            container &= ~mask;
            container |= (raw << offset) & mask;
            return container;
        }

        public static class BooleanTrait extends TraitField {

            public BooleanTrait(int offset) {
                super(offset, 1);
            }

            public boolean get(long container) {
                return extract(container) != 0;
            }

            public long set(long container, boolean value) {
                return insert(container, value ? 1 : 0);
            }
        }

        public static class FloatTrait extends TraitField {

            private final float min;
            private final float max;
            private final int maxInt;

            public FloatTrait(int offset, int bits, float min, float max) {
                super(offset, bits);
                this.min = min;
                this.max = max;
                this.maxInt = (1 << bits) - 1;
            }

            public float get(long container) {
                long raw = extract(container);
                float normalized = raw / (float) maxInt;
                return min + normalized * (max - min);
            }

            public long set(long container, float value) {
                float clamped = Math.max(min, Math.min(max, value));
                float normalized = (clamped - min) / (max - min);
                long raw = Math.round(normalized * maxInt);
                return insert(container, raw);
            }
        }

        public static class DoubleTrait extends TraitField {

            private final double min;
            private final double max;
            private final int maxInt;

            public DoubleTrait(int offset, int bits, double min, double max) {
                super(offset, bits);
                this.min = min;
                this.max = max;
                this.maxInt = (1 << bits) - 1;
            }

            public double get(long container) {
                long raw = extract(container);
                double normalized = raw / (double) maxInt;
                return min + normalized * (max - min);
            }

            public long set(long container, double value) {
                double clamped = Math.max(min, Math.min(max, value));
                double normalized = (clamped - min) / (max - min);
                long raw = Math.round(normalized * maxInt);
                return insert(container, raw);
            }
        }
    }

}
