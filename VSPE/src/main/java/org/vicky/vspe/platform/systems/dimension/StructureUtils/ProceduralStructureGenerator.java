package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import gnu.trove.set.hash.TLongHashSet;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ProceduralBranchedTreeGenerator;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public abstract class ProceduralStructureGenerator<N> {
    protected static final int DEFAULT_BATCH = 100_000;
    protected static final int BIT_SIZE = 21;
    protected static final int BIAS     = 1 << (BIT_SIZE - 1);
    protected static final int MASK     = (1 << BIT_SIZE) - 1;

    protected final long[] posKeys = new long[DEFAULT_BATCH * 3];

    @SuppressWarnings("unchecked")
    protected final PlatformBlockState<String>[] states = (PlatformBlockState<String>[]) new PlatformBlockState[DEFAULT_BATCH * 3];

    @SuppressWarnings("unchecked")
    protected final BiConsumer<PlatformWorld<String, N>, Vec3>[] actions = (BiConsumer<PlatformWorld<String, N>, Vec3>[]) new BiConsumer[DEFAULT_BATCH * 3];

    protected final TLongHashSet cache = new TLongHashSet(DEFAULT_BATCH * 3);
    protected final AtomicInteger ptr = new AtomicInteger();
    protected Runnable flush;
    protected RandomSource rnd;
    protected final PlatformWorld<String, N> world;

    public ProceduralStructureGenerator(PlatformWorld<String, N> world) {
        this.world = world;
    }

    public abstract BlockVec3i getApproximateSize();

    public abstract static class BaseBuilder<N, G extends ProceduralStructureGenerator<N>> {
        public abstract void validate();
        public abstract G build(PlatformWorld<String, N> world);
    }

    protected static long encode(int x,int y,int z){
        long xb = (long)(x + BIAS) & MASK;
        long yb = (long)(y + BIAS) & MASK;
        long zb = (long)(z + BIAS) & MASK;
        return (xb << (2*BIT_SIZE))
                | (yb << BIT_SIZE)
                |  zb;
    }

    public abstract void generate(RandomSource rnd, Vec3 origin);

    // — Helpers —
    protected boolean guardAndStore(int x, int y, int z, int r, PlatformBlockState<String> st, boolean useSphere) {
        return guardAndStore(x, y, z, st, useSphere, r);
    }

    // Overload so we can call guardAndStore(x,y,z,st,cap,useSphere,0) for normals:
    protected boolean guardAndStore(int x, int y, int z,
                                  PlatformBlockState<String> st,
                                  boolean useSphere) {
        return guardAndStore(x, y, z, st, useSphere, 1);
    }

    /**
     * Attempt to queue a block (or sphere) at cx,cy,cz.
     * @return true if a new block was queued (i.e. cache.add was true), false otherwise.
     */
    protected boolean guardAndStore(int cx, int cy, int cz, PlatformBlockState<String> st, boolean useSphere, int fixedR) {
        if(ptr.get() >= DEFAULT_BATCH) {
            flush.run();
        }
        boolean placed = false;
        if(useSphere) {
            short[] off = StructureCacheUtils.getSphereOffsets(fixedR);
            for(int i = 0; i < off.length; i += 3) {
                if(ptr.get() >= DEFAULT_BATCH) break;
                int x = cx + off[i], y = cy + off[i+1], z = cz + off[i+2];
                long key = encode(x, y, z);
                if(!cache.add(key)) continue;
                int idx = ptr.getAndIncrement();
                posKeys[idx] = key;
                states[idx]  = st;
                actions[idx] = null;
                placed = true;
            }
        } else {
            short[] scan = StructureCacheUtils.getDiscScanlineWidths(fixedR);
            for(int dz = -fixedR; dz <= fixedR; dz++) {
                if(ptr.get() >= DEFAULT_BATCH) break;
                int half = scan[dz + fixedR];
                for(int dx = -half; dx <= half; dx++) {
                    long key = encode(cx + dx, cy, cz + dz);
                    if(!cache.add(key)) continue;
                    int idx = ptr.getAndIncrement();
                    posKeys[idx] = key;
                    states[idx]  = st;
                    actions[idx] = null;
                    placed = true;
                }
            }
        }
        return placed;
    }

    protected void removeAt(int x, int y, int z) {
        long key = encode(x, y, z);
        cache.remove(key);
        int p = ptr.get();
        for (int i = p - 1; i >= 0; i--) {
            if (posKeys[i] == key) {
                posKeys[i] = -1;
                actions[i] = null;
                states[i] = null;
            }
        }
    }

    protected PlatformBlockState<String> getQueuedState(int x, int y, int z) {
        long key = encode(x, y, z);
        // scan backwards for speed: most recent writes come last
        int p = ptr.get();
        for (int i = p - 1; i >= 0; i--) {
            if (posKeys[i] == key) {
                return states[i] != null
                        ? states[i]
                        : // maybe it was an action, fall back to air
                        world.createPlatformBlockState("minecraft:air", null);
            }
        }
        // if we never queued it, assume it was air
        return world.createPlatformBlockState("minecraft:air", null);
    }

    protected void guardAndAction(int x, int y, int z,
                                BiConsumer<PlatformWorld<String, N>, Vec3> act) {
        if (ptr.get() >= DEFAULT_BATCH) flush.run();
        if (ptr.get() >= DEFAULT_BATCH) return;

        long key = encode(x, y, z);
        if (!cache.add(key)) return;
        int idx = ptr.getAndIncrement();
        posKeys[idx] = key;
        states[idx] = null;
        actions[idx] = act;
    }


    protected void prepareFlush() {
        flush = () -> {
            int total = ptr.getAndSet(0);
            for(int i=0;i<total;i++){
                long key=posKeys[i];
                if (key == -1) continue;
                int x = (int)((key >>> (2*BIT_SIZE)) & MASK) - BIAS;
                int y = (int)((key >>> BIT_SIZE)     & MASK) - BIAS;
                int z = (int)( key                   & MASK) - BIAS;
                Vec3 pos = new Vec3(x,y,z);
                // VSPEPlatformPlugin.platformLogger().info(String.format("Placed at %s %s %s", x, y, z));
                if(states[i]!=null) world.setPlatformBlockState(pos, states[i]);
                else if(actions[i]!=null) actions[i].accept(world,pos);
            }
            cache.clear();
        };
        ptr.set(0);
        cache.clear();
    }

    /** Evenly‐spaced unit‐vectors on the upper hemisphere via Fibonacci sphere. */
    protected List<Vec3> fibonacciHemisphere(int count) {
        List<Vec3> pts = new ArrayList<>(count);
        double phi = Math.PI * (3 - Math.sqrt(5)); // golden angle
        for(int i = 0; i < count; i++) {
            double y = 1.0 - (double)i / (count - 1);     // from 1 down to 0
            double radius = Math.sqrt(1 - y*y);          // radius of circle at this y
            double theta = i * phi;
            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;
            pts.add(new Vec3(x, y, z));
        }
        return pts;
    }

    protected static int sq(int x){ return x*x; }


    protected static int distanceSquared(int x1, int y1, int z1,
                                       int x2, int y2, int z2) {
        int dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    protected static org.vicky.vspe.platform.utilities.Math.Vector3 rotateAroundY(org.vicky.vspe.platform.utilities.Math.Vector3 v, double angle) {
        double cos = cos(angle), sin = sin(angle);
        return org.vicky.vspe.platform.utilities.Math.Vector3.at(
                v.getX() * cos - v.getZ() * sin,
                v.getY(),
                v.getX() * sin + v.getZ() * cos
        );
    }

    protected static org.vicky.vspe.platform.utilities.Math.Vector3 rotateAroundAxis(org.vicky.vspe.platform.utilities.Math.Vector3 v, org.vicky.vspe.platform.utilities.Math.Vector3 axis, double angle) {
        axis = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = v.dot(axis);

        double x = v.getX() * cos +
                (axis.getY() * v.getZ() - axis.getZ() * v.getY()) * sin +
                axis.getX() * dot * (1 - cos);

        double y = v.getY() * cos +
                (axis.getZ() * v.getX() - axis.getX() * v.getZ()) * sin +
                axis.getY() * dot * (1 - cos);

        double z = v.getZ() * cos +
                (axis.getX() * v.getY() - axis.getY() * v.getX()) * sin +
                axis.getZ() * dot * (1 - cos);

        return org.vicky.vspe.platform.utilities.Math.Vector3.at(x, y, z);
    }

    public static class Triplet<U, V, W> {
        private final U first;
        private final V second;
        private final W third;

        protected Triplet(final @NotNull U first, final @NotNull V second, final @NotNull W third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public static Triplet<Integer, Integer, Integer> of(int dx, int dy, int dz) {
            return new Triplet<>(dx, dy, dz);
        }

        public final U first() {
            return this.first;
        }

        public final V second() {
            return this.second;
        }

        public final W third() {
            return this.third;
        }

        public final boolean equals(final Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                Triplet<?, ?, ?> triplet = (Triplet)o;
                return Objects.equals(this.first(), triplet.first()) && Objects.equals(this.second(), triplet.second()) && Objects.equals(this.third(), triplet.third());
            } else {
                return false;
            }
        }

        public final int hashCode() {
            return Objects.hash(this.first(), this.second(), this.third());
        }

        public final String toString() {
            return String.format("(%s, %s, %s)", this.first, this.second, this.third);
        }

        public final int size() {
            return 3;
        }

        public final @NotNull Object @NotNull [] toArray() {
            return new Object[]{this.first, this.second, this.third};
        }
    }

}
