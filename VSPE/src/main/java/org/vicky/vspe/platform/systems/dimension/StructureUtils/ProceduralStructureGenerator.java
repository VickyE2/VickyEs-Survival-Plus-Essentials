package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import gnu.trove.set.hash.TLongHashSet;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    protected static final int DEFAULT_BATCH = 500_000;
    protected static final int BIT_SIZE = 21;
    protected static final int BIAS     = 1 << (BIT_SIZE - 1);
    protected static final int MASK     = (1 << BIT_SIZE) - 1;
    protected static final ContextLogger LOGGER = new ContextLogger(ContextLogger.ContextType.SUB_SYSTEM,
            new Object() {
            }.getClass().getEnclosingClass().getSimpleName());
    private static final ExecutorService GENERATOR_EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "proc-gen-worker-" + UUID.randomUUID());
        t.setDaemon(true);
        return t;
    });

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
    protected final List<Future<GenerationResult<T>>> subtaskFutures = Collections.synchronizedList(new ArrayList<>());
    protected final List<Future<GenerationResult<T>>> subtaskEndingFutures = Collections.synchronizedList(new ArrayList<>());
    private final List<AbstractMap.SimpleEntry<Consumer<SubGenerator>, CompletableFuture<GenerationResult<T>>>>
            pendingFinalJobs = Collections.synchronizedList(new ArrayList<>());
    protected boolean airFiller = true;

    public ProceduralStructureGenerator() {
        prepareFlush(); // default flush; generate() will re-setup as needed
    }


    public static double distance(Vec3 v1, Vec3 v2) {
        double dx = v2.x - v1.x;
        double dy = v2.y - v1.y;
        double dz = v2.z - v1.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Linearly interpolates between two Vec3 vectors based on a 't' parameter.
     *
     * @param startVec The starting vector.
     * @param endVec   The ending vector.
     * @param t        The interpolation parameter, from 0.0 to 1.0.
     * @return A new Vec3 object representing the interpolated point.
     */
    public static Vec3 lerp(Vec3 startVec, Vec3 endVec, double t) {
        // Clamp 't' to ensure the result is always between the two vectors
        t = Math.max(0.0, Math.min(1.0, t));

        // Apply the LERP formula to each component
        double lerpedX = startVec.x + t * (endVec.x - startVec.x);
        double lerpedY = startVec.y + t * (endVec.y - startVec.y);
        double lerpedZ = startVec.z + t * (endVec.z - startVec.z);

        return new Vec3(lerpedX, lerpedY, lerpedZ);
    }

    protected static org.vicky.platform.utils.Vec3 rotateAroundY(org.vicky.platform.utils.Vec3 v, double angle) {
        double cos = cos(angle), sin = sin(angle);
        return org.vicky.platform.utils.Vec3.of(
                v.getIntX() * cos - v.getIntZ() * sin,
                v.getIntY(),
                v.getIntX() * sin + v.getIntZ() * cos
        );
    }

    protected static org.vicky.platform.utils.Vec3 rotateAroundAxis(org.vicky.platform.utils.Vec3 v, org.vicky.platform.utils.Vec3 axis, double angle) {
        axis = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = v.dot(axis);

        double x = v.getIntX() * cos +
                (axis.getIntY() * v.getIntZ() - axis.getIntZ() * v.getIntY()) * sin +
                axis.getIntX() * dot * (1 - cos);

        double y = v.getIntY() * cos +
                (axis.getIntZ() * v.getIntX() - axis.getIntX() * v.getIntZ()) * sin +
                axis.getIntY() * dot * (1 - cos);

        double z = v.getIntZ() * cos +
                (axis.getIntX() * v.getIntY() - axis.getIntY() * v.getIntX()) * sin +
                axis.getIntZ() * dot * (1 - cos);

        return org.vicky.platform.utils.Vec3.of(x, y, z);
    }

    /**
     * Submit a subtask that will run off-thread. The provided Consumer receives a fresh
     * SubGenerator instance. The returned Future will produce a GenerationResult from that
     * subtask; generate(...) will wait for and merge those results automatically.
     * <p>
     * Usage from subclass:
     * submitSubtask(sub -> {
     * // heavy calculations
     * sub.guardAndStore(x,y,z,blockState,false);
     * ...
     * });
     */
    protected Future<GenerationResult<T>> submitSubtask(Consumer<SubGenerator> job) {
        CompletableFuture<GenerationResult<T>> fut = CompletableFuture.supplyAsync(() -> {
            SubGenerator sub = new SubGenerator();
            try {
                job.accept(sub);
                return sub.flushToResult();
            } catch (Throwable t) {
                // log & rethrow so future completes exceptionally
                LOGGER.print("Subtask threw: " + t, ContextLogger.LogType.ERROR);
                t.printStackTrace();
                throw t;
            }
        }, GENERATOR_EXEC);
        subtaskFutures.add(fut);
        return fut;
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
     * Submit a subtask that will run off-thread. The provided Consumer receives a fresh
     * SubGenerator instance. The returned Future will produce a GenerationResult from that
     * subtask; generate(...) will wait for and merge those results automatically.
     * <p>
     * Usage from subclass:
     * submitSubtask(sub -> {
     * // heavy calculations
     * sub.guardAndStore(x,y,z,blockState,false);
     * ...
     * });
     */
    protected Future<GenerationResult<T>> submitFinalisingSubtask(Consumer<SubGenerator> job) {
        CompletableFuture<GenerationResult<T>> placeholder = new CompletableFuture<>();
        pendingFinalJobs.add(new AbstractMap.SimpleEntry<>(job, placeholder));
        return placeholder;
    }

    protected boolean guardAndStore(int x, int y, int z,
                                    PlatformBlockState<T> st,
                                    boolean useSphere) {
        return guardAndStore(x, y, z, st, useSphere, 1);
    }

    /**
     * Wait for all previously submitted subtasks and merge their results into the provided
     * lists/maps. Clears the subtask list afterwards.
     */
    protected void waitAndMergeSubtasks(List<BlockPlacement<T>> collectPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> collectActions) {
        while (true) {
            List<Future<GenerationResult<T>>> futures;
            synchronized (subtaskFutures) {
                if (subtaskFutures.isEmpty()) return;
                futures = new ArrayList<>(subtaskFutures);
                subtaskFutures.clear();
            }

            for (Future<GenerationResult<T>> f : futures) {
                try {
                    GenerationResult<T> res = f.get(); // will block until finished OR throw
                    if (res != null) {
                        res.placements.forEach(it -> {
                            if (airFiller) {
                                collectPlacements.add(it);
                            } else {
                                if (!collectPlacements.contains(it)) collectPlacements.add(it);
                            }
                        });
                        res.actions.forEach((it, it2) -> {
                            if (airFiller) {
                                collectActions.put(it, it2);
                            } else {
                                collectActions.putIfAbsent(it, it2);
                            }
                        });
                    }
                } catch (ExecutionException ee) {
                    LOGGER.print("Subtask failed: " + ee.getCause(), ContextLogger.LogType.ERROR);
                    // you can decide whether to rethrow or continue; here we continue but log
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.print("Interrupted while waiting for subtasks", ContextLogger.LogType.WARNING);
                    throw new RuntimeException(ie);
                }
            }
        }
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
     * Wait for all previously submitted subtasks and merge their results into the provided
     * lists/maps. Clears the subtask list afterwards.
     */
    protected void waitAndMergeFinalSubtasks(List<BlockPlacement<T>> collectPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> collectActions) {
        while (true) {
            List<Future<GenerationResult<T>>> finalFutures;
            synchronized (subtaskEndingFutures) {
                if (subtaskEndingFutures.isEmpty()) return;
                finalFutures = new ArrayList<>(subtaskEndingFutures);
                subtaskEndingFutures.clear();
            }

            for (Future<GenerationResult<T>> f : finalFutures) {
                try {
                    GenerationResult<T> res = f.get(); // will block until finished OR throw
                    if (res != null) {
                        res.placements.forEach(it -> {
                            if (airFiller) {
                                collectPlacements.add(it);
                            } else {
                                if (!collectPlacements.contains(it)) collectPlacements.add(it);
                            }
                        });
                        res.actions.forEach((it, it2) -> {
                            if (airFiller) {
                                collectActions.put(it, it2);
                            } else {
                                collectActions.putIfAbsent(it, it2);
                            }
                        });
                    }
                } catch (ExecutionException ee) {
                    LOGGER.print("Finalising Subtask failed: " + ee.getCause(), ContextLogger.LogType.ERROR);
                    // you can decide whether to rethrow or continue; here we continue but log
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.print("Interrupted while waiting for finalising subtasks", ContextLogger.LogType.WARNING);
                    throw new RuntimeException(ie);
                }
            }
        }
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

    private void drainAndSubmitFinalJobs() {
        List<AbstractMap.SimpleEntry<Consumer<SubGenerator>, CompletableFuture<GenerationResult<T>>>> copy;
        synchronized (pendingFinalJobs) {
            if (pendingFinalJobs.isEmpty()) return;
            copy = new ArrayList<>(pendingFinalJobs);
            pendingFinalJobs.clear();
        }

        for (var entry : copy) {
            Consumer<SubGenerator> job = entry.getKey();
            CompletableFuture<GenerationResult<T>> placeholder = entry.getValue();

            // submit the actual work now
            CompletableFuture<GenerationResult<T>> fut = CompletableFuture.supplyAsync(() -> {
                SubGenerator sub = new SubGenerator();
                try {
                    job.accept(sub);
                    return sub.flushToResult();
                } catch (Throwable t) {
                    LOGGER.print("Finalising subtask threw: " + t, ContextLogger.LogType.ERROR);
                    t.printStackTrace();
                    throw t;
                }
            }, GENERATOR_EXEC);

            // When fut completes, complete the placeholder so original caller's Future resolves
            fut.whenComplete((res, ex) -> {
                if (ex != null) placeholder.completeExceptionally(ex);
                else placeholder.complete(res);
            });

            // Add the real future to the list that generate() will wait on
            subtaskEndingFutures.add(fut);
        }
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

    /**
     * Public convenience: call to generate placements (thread-safe-ish).
     * This sets up the flush handler and returns a GenerationResult containing all queued placements
     * and actions. Subclasses should implement performGeneration(...) not this method.
     */
    public final synchronized GenerationResult<T> generate(RandomSource rnd, Vec3 origin) {
        this.rnd = rnd;
        // result holders
        List<BlockPlacement<T>> placements = new ArrayList<>();
        Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> actionMap = new HashMap<>();
        subtaskFutures.clear();
        subtaskEndingFutures.clear();

        // prepare flush so it writes into these lists (not to the world)
        prepareFlush(placements, actionMap);

        // let subclass generate using guardAndStore / guardAndAction
        performGeneration(rnd, origin, placements, actionMap);

        // ensure any remaining buffered entries are flushed into result lists
        if (flush != null) flush.run();

        waitAndMergeSubtasks(placements, actionMap);
        drainAndSubmitFinalJobs();
        waitAndMergeFinalSubtasks(placements, actionMap);

        // reset to default flush which won't write to world (safe)
        prepareFlush();

        return new GenerationResult<>(placements, actionMap);
    }

    protected boolean guardAndStore(Vec3 vec,
                                    PlatformBlockState<T> st,
                                    boolean useSphere) {
        return guardAndStore(vec.round().getIntX(), vec.round().getIntY(), vec.round().getIntZ(), st, useSphere, 1);
    }

    /**
     * Attempt to queue a block (or sphere) at cx,cy,cz.
     *
     * @return true if a new block was queued (i.e. cache.add was true), false otherwise.
     */
    protected boolean guardAndStore(int cx, int cy, int cz, PlatformBlockState<T> st, boolean useSphere, int fixedR) {
        if (ptr.get() >= DEFAULT_BATCH) {
            flush.run();
        }
        boolean placed = false;
        if (fixedR == 1) {
            if (ptr.get() >= DEFAULT_BATCH) return false;
            long key = encode(cx, cy, cz);
            if (!cache.add(key)) return false;
            int idx = ptr.getAndIncrement();
            posKeys[idx] = key;
            states[idx] = st;
            actions[idx] = null;
            placed = true;
        } else {
            if (useSphere) {
                short[] off = StructureCacheUtils.getSphereOffsets(fixedR);
                for (int i = 0; i < off.length; i += 3) {
                    if (ptr.get() >= DEFAULT_BATCH) break;
                    int x = cx + off[i], y = cy + off[i + 1], z = cz + off[i + 2];
                    long key = encode(x, y, z);
                    if (!cache.add(key)) continue;
                    int idx = ptr.getAndIncrement();
                    posKeys[idx] = key;
                    states[idx] = st;
                    actions[idx] = null;
                    placed = true;
                }
            } else {
                short[] scan = StructureCacheUtils.getDiscScanlineWidths(fixedR);
                for (int dz = -fixedR; dz <= fixedR; dz++) {
                    if (ptr.get() >= DEFAULT_BATCH) break;
                    int half = scan[dz + fixedR];
                    for (int dx = -half; dx <= half; dx++) {
                        long key = encode(cx + dx, cy, cz + dz);
                        if (!cache.add(key)) continue;
                        int idx = ptr.getAndIncrement();
                        posKeys[idx] = key;
                        states[idx] = st;
                        actions[idx] = null;
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    public abstract static class BaseBuilder<T, G extends ProceduralStructureGenerator<T>> {

        public abstract void validate();

        protected abstract G create();

        public final G build() {
            validate();
            return create();
        }
    }

    /**
     * SubGenerator: thread-local small generator context for subtasks.
     * It re-implements only the parts we need: caching, ptr, buffers and guard/flush.
     */
    public class SubGenerator {
        private final long[] s_posKeys = new long[DEFAULT_BATCH];
        @SuppressWarnings("unchecked")
        private final PlatformBlockState<T>[] s_states = (PlatformBlockState<T>[]) new PlatformBlockState[DEFAULT_BATCH];
        @SuppressWarnings("unchecked")
        private final BiConsumer<PlatformWorld<T, ?>, Vec3>[] s_actions = (BiConsumer<PlatformWorld<T, ?>, Vec3>[]) new BiConsumer[DEFAULT_BATCH];
        private final TLongHashSet s_cache = new TLongHashSet(DEFAULT_BATCH);
        private final AtomicInteger s_ptr = new AtomicInteger(0);

        public SubGenerator() {
            // neutral init if needed
        }

        // Use same encode helper from outer class
        public boolean guardAndStore(int cx, int cy, int cz, PlatformBlockState<T> st, boolean useSphere, int fixedR) {
            if (s_ptr.get() >= DEFAULT_BATCH) {
                // If a subtask overruns the batch, flush to local placements (but we can't call outer flush).
                // To keep it simple, we just stop accepting further placements in this subtask.
                return false;
            }
            boolean placed = false;
            if (fixedR == 1) {
                long key = encode(cx, cy, cz);
                if (!s_cache.add(key)) return false;
                int idx = s_ptr.getAndIncrement();
                s_posKeys[idx] = key;
                s_states[idx] = st;
                s_actions[idx] = null;
                placed = true;
            } else {
                // sphere/disc handling: reuse your StructureCacheUtils exactly as outer does
                if (useSphere) {
                    short[] off = StructureCacheUtils.getSphereOffsets(fixedR);
                    for (int i = 0; i < off.length; i += 3) {
                        if (s_ptr.get() >= DEFAULT_BATCH) break;
                        int x = cx + off[i], y = cy + off[i + 1], z = cz + off[i + 2];
                        long key = encode(x, y, z);
                        if (!s_cache.add(key)) continue;
                        int idx = s_ptr.getAndIncrement();
                        s_posKeys[idx] = key;
                        s_states[idx] = st;
                        s_actions[idx] = null;
                        placed = true;
                    }
                } else {
                    short[] scan = StructureCacheUtils.getDiscScanlineWidths(fixedR);
                    for (int dz = -fixedR; dz <= fixedR; dz++) {
                        if (s_ptr.get() >= DEFAULT_BATCH) break;
                        int half = scan[dz + fixedR];
                        for (int dx = -half; dx <= half; dx++) {
                            long key = encode(cx + dx, cy, cz + dz);
                            if (!s_cache.add(key)) continue;
                            int idx = s_ptr.getAndIncrement();
                            s_posKeys[idx] = key;
                            s_states[idx] = st;
                            s_actions[idx] = null;
                            placed = true;
                        }
                    }
                }
            }
            return placed;
        }

        public boolean guardAndStore(Vec3 vec, PlatformBlockState<T> st, boolean useSphere) {
            return guardAndStore(vec.round().getIntX(), vec.round().getIntY(), vec.round().getIntZ(), st, useSphere, 1);
        }

        public boolean guardAndStore(Vec3 vec, PlatformBlockState<T> st, boolean useSphere, int fixedR) {
            return guardAndStore(vec.round().getIntX(), vec.round().getIntY(), vec.round().getIntZ(), st, useSphere, fixedR);
        }

        public void guardAndAction(int x, int y, int z, BiConsumer<PlatformWorld<T, ?>, Vec3> act) {
            if (s_ptr.get() >= DEFAULT_BATCH) return;
            long key = encode(x, y, z);
            if (!s_cache.add(key)) return;
            int idx = s_ptr.getAndIncrement();
            s_posKeys[idx] = key;
            s_states[idx] = null;
            s_actions[idx] = act;
        }

        /**
         * Build and return a GenerationResult for this subgenerator and clear buffers.
         */
        public GenerationResult<T> flushToResult() {
            List<BlockPlacement<T>> placements = new ArrayList<>();
            Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> actions = new HashMap<>();
            int total = s_ptr.getAndSet(0);
            for (int i = 0; i < total; i++) {
                long key = s_posKeys[i];
                if (key == -1) continue;
                int x = decodeX(key), y = decodeY(key), z = decodeZ(key);
                if (s_states[i] != null) {
                    placements.add(new BlockPlacement<>(x, y, z, s_states[i], null));
                } else if (s_actions[i] != null) {
                    actions.put(key, s_actions[i]);
                }
                s_posKeys[i] = -1;
                s_states[i] = null;
                s_actions[i] = null;
            }
            s_cache.clear();
            return new GenerationResult<>(placements, actions);
        }
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
