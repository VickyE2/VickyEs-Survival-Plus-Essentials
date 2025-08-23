package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import gnu.trove.set.hash.TLongHashSet;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * Procedural structure generator: collects block placements (and optional actions)
 * into a GenerationResult rather than placing directly into the world.
 * <p>
 * Subclasses:
 * - call prepareFlush() when beginning generation
 * - use guardAndStore(...) and guardAndAction(...) as before
 * - call flush.run() if they don't use the helper generate(...) wrapper
 * <p>
 * Use the provided generate(...) signature which will call prepareFlush() and flush for you.
 */
public abstract class ProceduralStructureGenerator<T> {
    protected static final int DEFAULT_BATCH = 100_000;
    protected static final int BIT_SIZE = 21;
    protected static final int BIAS     = 1 << (BIT_SIZE - 1);
    protected static final int MASK     = (1 << BIT_SIZE) - 1;

    protected final long[] posKeys = new long[DEFAULT_BATCH * 3];

    @SuppressWarnings("unchecked")
    protected final PlatformBlockState<T>[] states = (PlatformBlockState<T>[]) new PlatformBlockState[DEFAULT_BATCH * 3];

    @SuppressWarnings("unchecked")
    // actions that used to be executed directly now get collected in the GenerationResult.actionMap
    protected final BiConsumer<PlatformWorld<T, ?>, Vec3>[] actions = (BiConsumer<PlatformWorld<T, ?>, Vec3>[]) new BiConsumer[DEFAULT_BATCH * 3];

    protected final TLongHashSet cache = new TLongHashSet(DEFAULT_BATCH * 3);
    protected final AtomicInteger ptr = new AtomicInteger();
    protected Runnable flush;
    protected RandomSource rnd;

    public ProceduralStructureGenerator() {
        prepareFlush(); // default flush; generate() will re-setup as needed
    }

    public abstract BlockVec3i getApproximateSize();

    public static int decodeX(long key) {
        return (int) ((key >>> (2 * BIT_SIZE)) & MASK) - BIAS;
    }

    protected static long encode(int x,int y,int z){
        long xb = (long)(x + BIAS) & MASK;
        long yb = (long)(y + BIAS) & MASK;
        long zb = (long)(z + BIAS) & MASK;
        return (xb << (2*BIT_SIZE))
                | (yb << BIT_SIZE)
                |  zb;
    }

    public static int decodeY(long key) {
        return (int) ((key >>> BIT_SIZE) & MASK) - BIAS;
    }

    public static int decodeZ(long key) {
        return (int) (key & MASK) - BIAS;
    }

    protected static int distanceSquared(int x1, int y1, int z1,
                                         int x2, int y2, int z2) {
        int dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Subclasses should implement this. Use guardAndStore / guardAndAction as before.
     * <p>
     * NOTE: this method is called inside the provided `generate(...)` wrapper; you normally
     * won't call it directly.
     */
    protected abstract void performGeneration(RandomSource rnd, Vec3 origin, List<BlockPlacement<T>> outPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> outActions);

    /**
     * Public convenience: call to generate placements (thread-safe-ish).
     * This sets up the flush handler and returns a GenerationResult containing all queued placements
     * and actions. Subclasses should implement performGeneration(...) not this method.
     */
    public GenerationResult<T> generate(RandomSource rnd, Vec3 origin) {
        this.rnd = rnd;
        // result holders
        List<BlockPlacement<T>> placements = new ArrayList<>();
        Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> actionMap = new HashMap<>();

        // prepare flush so it writes into these lists (not to the world)
        prepareFlush(placements, actionMap);

        // let subclass generate using guardAndStore / guardAndAction
        performGeneration(rnd, origin, placements, actionMap);

        // ensure any remaining buffered entries are flushed into result lists
        if (flush != null) flush.run();

        // reset to default flush which won't write to world (safe)
        prepareFlush();

        return new GenerationResult<>(placements, actionMap);
    }

    protected boolean guardAndStore(int x, int y, int z,
                                    PlatformBlockState<T> st,
                                    boolean useSphere) {
        return guardAndStore(x, y, z, st, useSphere, 1);
    }

    // — Helpers — keep same semantics, but buffering now targets the current flush target lists above

    protected boolean guardAndStore(int x, int y, int z, int r, PlatformBlockState<T> st, boolean useSphere) {
        return guardAndStore(x, y, z, st, useSphere, r);
    }

    protected PlatformBlockState<T> getQueuedState(int x, int y, int z) {
        long key = encode(x, y, z);
        // scan backwards for speed: most recent writes come last
        int p = ptr.get();
        for (int i = p - 1; i >= 0; i--) {
            if (posKeys[i] == key) {
                return states[i] != null
                        ? states[i]
                        : // maybe it was an action, fall back to air
                        (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState("minecraft:air");
            }
        }
        // if we never queued it, assume it was air
        return (PlatformBlockState<T>) PlatformPlugin.stateFactory().getBlockState("minecraft:air");
    }

    /**
     * Attempt to queue a block (or sphere) at cx,cy,cz.
     * @return true if a new block was queued (i.e. cache.add was true), false otherwise.
     */
    protected boolean guardAndStore(int cx, int cy, int cz, PlatformBlockState<T> st, boolean useSphere, int fixedR) {
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

    protected void guardAndAction(int x, int y, int z,
                                  BiConsumer<PlatformWorld<T, ?>, Vec3> act) {
        if (ptr.get() >= DEFAULT_BATCH) flush.run();
        if (ptr.get() >= DEFAULT_BATCH) return;

        long key = encode(x, y, z);
        if (!cache.add(key)) return;
        int idx = ptr.getAndIncrement();
        posKeys[idx] = key;
        states[idx] = null;
        actions[idx] = act;
    }

    /**
     * Primary prepareFlush() used by generate(...). When called without args it sets a
     * no-op/default flush that clears the buffer (safe default). When called with lists
     * it will append into them.
     */
    protected void prepareFlush() {
        // default flush that simply clears the buffer (defensive)
        this.flush = () -> {
            ptr.set(0);
            cache.clear();
        };
        ptr.set(0);
        cache.clear();
    }

    /**
     * Overloaded prepareFlush that collects into provided containers.
     */
    protected void prepareFlush(List<BlockPlacement<T>> collectPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> collectActions) {
        // set flush to append buffered items into the provided lists/maps
        this.flush = () -> {
            int total = ptr.getAndSet(0);
            for(int i=0;i<total; i++) {
                long key = posKeys[i];
                if (key == -1) continue;
                int x = decodeX(key);
                int y = decodeY(key);
                int z = decodeZ(key);
                // create BlockPlacement if we have a state
                if (states[i] != null) {
                    collectPlacements.add(new BlockPlacement<>(x, y, z, states[i], null));
                } else if (actions[i] != null) {
                    collectActions.put(key, actions[i]);
                }
                // clear slot for safety
                posKeys[i] = -1;
                states[i] = null;
                actions[i] = null;
            }
            cache.clear();
        };
        ptr.set(0);
        cache.clear();
    }

    public abstract static class BaseBuilder<T, G extends ProceduralStructureGenerator<T>> {
        public abstract void validate();

        public abstract G build();
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

    /**
     * New return type for generators — contains both placements and any special actions.
     */
    public static final class GenerationResult<T> {
        public final List<BlockPlacement<T>> placements;
        public final Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> actions; // keyed by encoded position

        public GenerationResult(List<BlockPlacement<T>> placements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> actions) {
            this.placements = placements;
            this.actions = actions;
        }
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
