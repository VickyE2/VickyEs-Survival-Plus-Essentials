package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators;

import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformWorld;
import org.vicky.vspe.BlockVec3i;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.StructureCacheUtils;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.BlockPlacement;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.RandomSource;

import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.Math.TAU;

/**
 * Procedural tree generator (trunk + branches + roots, with cheapMode).
 */
public class ProceduralBranchedTreeGenerator<T> extends
        ProceduralStructureGenerator<T> {
    private final int maxTrunkThickness, maxHeight, maxWidth, branchThreshold, maxBranchThickness;
    private final boolean roots, addLeaves, hollow;
    private final float randomness, twistiness, branchLengthRatioToHeight, branchStart, minPitch, maxPitch, maxBranchShrink, branchShrinkPerLevel;
    private final PlatformBlockState<T> wood, leaves;
    private final List<Builder.DecoratorEntry<T>> decorators;
    private final boolean cheapMode;
    private final float qualityFactor;
    private final LeafType type;
    private final Map<String, Object> params;
    private final List<Builder.BlockSeqEntry<T>> seq;

    public ProceduralBranchedTreeGenerator(Builder<T> b) {
        b.validate();
        this.maxTrunkThickness = b.maxTrunkThickness;
        this.maxHeight = b.maxHeight;
        this.maxWidth = b.maxWidth;
        this.branchThreshold = b.branchThreshold;
        this.roots = b.roots;
        this.addLeaves = b.addLeaves;
        this.hollow = b.hollow;
        this.randomness = b.randomness;
        this.twistiness = b.twistiness;
        this.wood = b.woodMaterial;
        this.leaves = b.leavesMaterial;
        this.decorators = b.decoratorEntries;
        this.cheapMode = b.cheapMode;
        this.qualityFactor = b.qualityFactor;
        this.branchStart = b.branchStart;
        this.branchLengthRatioToHeight = b.branchLengthRatioTOHeight;
        this.maxPitch = b.maxPitch;
        this.minPitch = b.minPitch;
        this.seq = b.seq;
        this.maxBranchThickness = b.maxBranchThickness;
        this.maxBranchShrink = b.maxBranchShrink;
        this.branchShrinkPerLevel = b.branchShrinkPerLevel;
        this.type = b.leafType;
        this.params = b.params != null ? b.params : Collections.emptyMap();
    }

    @Override
    public BlockVec3i getApproximateSize() {
        // Conservative horizontal radius:
        // trunk radius + main branch reach + branch thickness + small margin
        int horizRadius = maxTrunkThickness + maxWidth + maxBranchThickness + 3;

        // Width and depth (square footprint)
        int width = horizRadius * 2 + 1;

        // Vertical extents:
        // upward: main trunk height + branch vertical excursion + a margin
        int up = maxHeight + maxBranchThickness + 3;

        // downward: if roots enabled, roots extend up to ~maxHeight/2 below origin,
        // plus branch thickness margin; otherwise just a small margin for rooty bits
        int down = (roots ? (maxHeight / 2) : 0) + maxBranchThickness + 3;

        int height = up + down + 1; // +1 to be safe (inclusive bounds)

        return new BlockVec3i(width, height, width);
    }


    @Override
    protected void performGeneration(RandomSource rnd, Vec3 origin, List<BlockPlacement<T>> outPlacements, Map<Long, BiConsumer<PlatformWorld<T, ?>, Vec3>> outActions) {
        generateAsync(rnd, origin);
    }

    public void generateAsync(RandomSource rnd,
                              Vec3 origin) {
        calculateTrunk(origin, rnd);
        if (roots) calculateRoots(origin, rnd);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (Math.min(v, max));
    }

    // — Trunk via scanlines and ring‐skipping —
    private void calculateTrunk(Vec3 origin, RandomSource rnd) {
        final double alpha = 0.05;  // controls trunk taper speed

        // 1) Build the trunk itself
        for(int y = 0; y < maxHeight; y++) {
            double curr = (double) maxTrunkThickness / Math.sqrt(1 + alpha * y);
            int r = Math.max(1, (int)Math.round(curr));
            int worldY = origin.getY() + y;
            short[] scan = StructureCacheUtils.getDiscScanlineWidths(r);

            for(int dz = -r; dz <= r; dz++) {
                int half = scan[dz + r];
                for(int dx = -half; dx <= half; dx++) {
                    guardAndStore(origin.getX() + dx,
                            worldY,
                            origin.getZ() + dz,
                            r, wood, false);
                }
            }
        }

        // 2) Generate an even hemisphere of directions
        List<Vec3> hemisphereDirs = fibonacciHemisphere(branchThreshold);

        // 3) Spawn one primary branch for each direction
        int yTop = (int) (origin.getY() + ((maxHeight - 1) * branchStart));
        double topR = maxTrunkThickness / Math.sqrt(1 + alpha * (maxHeight - 1));

        for(Vec3 dir : hemisphereDirs) {
            // project onto trunk rim for start point:
            int startX = origin.getX() + (int)Math.round(dir.x * topR);
            int startZ = origin.getZ() + (int)Math.round(dir.z * topR);

            // branch thickness & length bounds
            recurseBranch(
                    rnd,
                    new Vec3(startX, yTop, startZ),
                    0,
                    maxBranchThickness,
                    dir
            );
        }
    }


    // new recurseBranch signature:
    /**
     * @param dir  Unit‐direction vector (x=dx/length, y=dy/length, z=dz/length)
     */
    private void recurseBranch(RandomSource rnd,
                               Vec3 origin,
                               int level,
                               double parentR,
                               Vec3 dir) {
        // 1) stop if too thin or too deep
        // safe upper bound: avoid runaway recursion; use branchThreshold as the main limit
        if (parentR < 1 || level >= Math.max(4, branchThreshold)) return;

        double depthFactor = Math.pow(1.0 - (level / (double)branchThreshold), 1.5);
        double spawnChance = randomness * depthFactor;

        // 2) shrink per-level
        double currR = Math.max(1, parentR * branchShrinkPerLevel);
        int branchRadius = (int)Math.round(currR);

        // 3) fixed straight-line length (no extra randomness here)
        int denom = Math.max(1, Math.round(branchLengthRatioToHeight * Math.max(1, level)));
        int len = (int) Math.max(1, Math.round(maxWidth / (double) denom));
        len = Math.min(len, maxWidth * 2); // arbitrary cap

        if(cheapMode) len = (int)(len * qualityFactor);

        // 4) march straight out
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int lastDx = 0, lastDy = 0, lastDz = 0;

        int maxChildren = Math.max(1, branchThreshold / (level + 2));
        int spawned      = 0;
        boolean placed   = false;
        int minStep = Math.max(1, branchRadius/2);
        int thresholdSq = minStep*minStep;

        for(int j = 0; j <= len; j++) {
            // current offset along dir
            int dx = (int)Math.round(dir.x * j);
            int dy = (int)Math.round(dir.y * j);
            int dz = (int)Math.round(dir.z * j);

            int bx = ox + dx, by = oy + dy, bz = oz + dz;

            // stamp only when we've moved ~1.2× radius or at the tip
            int dd = (dx-lastDx)*(dx-lastDx)
                    + (dy-lastDy)*(dy-lastDy)
                    + (dz-lastDz)*(dz-lastDz);

            if(j == len || dd >= thresholdSq) {
                // place the cylinder slice
                guardAndStore(bx, by, bz, branchRadius, wood, true);

                // decorators…
                for(var d : decorators) if(rnd.nextFloat() < d.chance) {
                    for(var ori : d.orientations) {
                        int tx = bx, ty = by;
                        switch(ori) {
                            case TOP    -> ty += branchRadius + 1;
                            case BOTTOM -> ty -= branchRadius + 1;
                            case LEFT   -> tx -= branchRadius + 1;
                            case RIGHT  -> tx += branchRadius + 1;
                        }
                        // capture coordinate in action wrapper so actions executed later get correct Vec3
                        int fx = tx, fy = ty, fz = bz;
                        guardAndAction(fx, fy, fz, (w, v) -> d.action.accept(w, new Vec3(fx, fy, fz)));
                    }
                }

                // Put dangling leaves at joint locations (where sub-branches may spawn) — makes dangling actually hang from joints.
                if (addLeaves && type == LeafType.DANGLING) {
                    // attach a small cluster of dangling chains around this stamp point
                    attachHangingChain(bx, by, bz, branchRadius, rnd);
                }

                // **only here** do we optionally spawn a sub-branch
                boolean inRange = j >= len * 0.6 && j <= len * 0.9;
                if(!placed
                        && inRange
                        && level + 2 < branchThreshold
                        && level+1 != branchThreshold
                        && rnd.nextFloat() < spawnChance) {
                    // 5a) find a perp vector to dir
                    // 1) Parent direction (unit vector)
                    Vec3 axis = dir.normalize();

                    // 2) Build an orthonormal basis {U,V} perpendicular to axis
                    Vec3 U = axis.crossProduct(new Vec3(0, 1, 0));
                    if(U.lengthSq() < 1e-6) {
                        // axis was (nearly) vertical—use another fallback
                        U = axis.crossProduct(new Vec3(1, 0, 0));
                    }
                    U = U.normalize();
                    Vec3 V = axis.crossProduct(U).normalize();

                    // 3) Pick a random angle around the circle
                    double step    = TAU / maxChildren;
                    double baseAz  = spawned * step;
                    double jitter  = step * 0.1 * depthFactor;  // 20%
                    double azim    = baseAz + (rnd.nextDouble()*2 - 1) * jitter;

                    // 4) Your lateral offset direction on that circle:
                    Vec3 lateral = U.multiply(Math.cos(azim))
                            .add(V.multiply(Math.sin(azim)))
                            .normalize();

                    double raw = randomness * twistiness;            // [0..1]
                    double skew = Math.pow(raw, 0.5);         // tweak exponent for stronger bias
                    double pitch = minPitch + skew*(maxPitch-minPitch); // always positive
                    if (level+2 == branchThreshold && rnd.nextFloat() < twistiness)
                        pitch = -pitch;

                    // 6) Build child direction
                    Vec3 childDir = lateral.multiply(Math.cos(pitch))
                            .add(axis.multiply(Math.sin(pitch)))
                            .normalize();

                    // 7) If it somehow went downward, flip its vertical component
                    if (childDir.y < 0) {
                        childDir = new Vec3(childDir.x,
                                -childDir.y,
                                childDir.z).normalize();
                    }

                    // 6) Recurse from this exact stamp point:
                    recurseBranch(
                            rnd,
                            new Vec3(bx, by, bz),
                            level + 1,
                            currR,
                            childDir
                    );
                    spawned++;
                }
            }
            lastDx = dx;
            lastDy = dy;
            lastDz = dz;
        }
        if (level+1 == branchThreshold && addLeaves) {
            // lastDx/lastDy/lastDz are offsets from the branch's origin (ox,oy,oz)
            setLeaves(new Vec3(ox + lastDx, oy + lastDy, oz + lastDz));
        }

        if(level + 2 == branchThreshold) {
            // 1) Build only the upper hemisphere directions
            List<Vec3> hemiAll = fibonacciHemisphere(branchThreshold * 2);
            List<Vec3> hemiUp = hemiAll.stream()
                    .filter(v -> v.y > 0)  // keep only upward‐pointing
                    .toList();

            // 2) Compute world‐space rim height
            int worldDy = oy + lastDy;
            int yTop    = (int)(worldDy + ((maxHeight - 1) * branchStart));
            double topR = maxTrunkThickness / Math.sqrt(1 + 0.04 * (maxHeight - 1));

            for(Vec3 rimDir : hemiUp) {
                // project start point around that rim vector
                int startX = ox + lastDx + (int)Math.round(rimDir.x * topR);
                int startZ = oz + lastDz + (int)Math.round(rimDir.z * topR);

                // now build a perfect U,V ⟂ parent axis = rimDir
                Vec3 N = rimDir.normalize();
                Vec3 up = new Vec3(0,1,0);
                Vec3 U = N.crossProduct(up);
                if(U.lengthSq() < 1e-6) U = N.crossProduct(new Vec3(1,0,0));
                U = U.normalize();
                Vec3 V = N.crossProduct(U).normalize();

                // random around the circle
                double w = rnd.nextDouble() * TAU;

                // tilt by exactly 90° ± pitch
                double o = minPitch + rnd.nextDouble() * (maxPitch - minPitch);
                if(rnd.nextBoolean()) o = -o;
                double t = Math.PI/2 + o;

                // assemble child direction
                Vec3 childDir = U.multiply(Math.cos(w)*Math.sin(t))
                        .add(V.multiply(Math.sin(w)*Math.sin(t)))
                        .add(N.multiply(Math.cos(t)))
                        .normalize();

                // spawn that secondary branch
                recurseBranch(
                        rnd,
                        new Vec3(startX, yTop, startZ),
                        level + 1,
                        currR,
                        childDir
                );
            }
        }

    }

    /**
     * Helper — attach several dangling leaf chains around a joint position.
     */
    // Convenience: previous call-site compatibility
    private void attachHangingChain(int bx, int by, int bz, int branchRadius, RandomSource rnd) {
        // number of strands around the rim: proportional to branch radius (clamped)
        int strands = Math.max(1, Math.min(8, branchRadius * 2));
        double angleStep = 2 * Math.PI / strands;
        int maxLen = Math.max(3, branchRadius * 3); // chain length scale
        int thickStart = Math.max(0, branchRadius / 2); // fluff at top
        int thickEnd = 0; // taper at tip

        for (int s = 0; s < strands; s++) {
            double a = s * angleStep + (rnd.nextDouble() * angleStep * 0.5 - angleStep * 0.25);
            int ox = bx + (int) Math.round(Math.cos(a) * (branchRadius + 0.5));
            int oz = bz + (int) Math.round(Math.sin(a) * (branchRadius + 0.5));
            // small random variation in length
            int len = (int) Math.max(1, Math.round(maxLen * (0.7 + rnd.nextDouble() * 0.6)));
            attachHangingChain(ox, by - 1, oz, len, thickStart, thickEnd, rnd);
        }
    }

    /**
     * Attach a hanging chain at (bx,by,bz).
     * - length: number of vertical steps (int)
     * - seq: ordered list of BlockSeqEntry describing gradient segments (0.0..1.0)
     * - thickStart/thickEnd: radii at top(0) and bottom(1) (ints, can be 0..n)
     * - tipClusterChance: chance to place final cluster (amethyst etc)
     */
    private void attachHangingChain(
            int bx, int by, int bz,
            int length,
            int thickStart,
            int thickEnd,
            RandomSource rnd
    ) {
        if (length <= 0) return;
        length = Math.max(1, length);

        for (int i = 0; i < length; i++) {
            // norm: 0 at top/joint, 1 at bottom/tip
            double norm = length == 1 ? 1.0 : (double) i / (double) (length - 1);

            // pick block by first matching range, otherwise fallback to leaves or last sequence
            PlatformBlockState<T> chosen = null;
            if (seq != null && !seq.isEmpty()) {
                for (Builder.BlockSeqEntry<T> e : seq) {
                    if (e.contains(norm)) {
                        chosen = e.state;
                        break;
                    }
                }
                if (chosen == null) chosen = seq.getLast().state;
            }
            if (chosen == null) chosen = leaves; // fallback to configured leaves block

            // thickness interpolation (top -> bottom)
            int radius = (int) Math.round(thickStart * (1.0 - norm) + thickEnd * norm);
            radius = Math.max(0, radius);

            int px = bx;
            int py = by - i; // drop downwards
            int pz = bz;

            // use sphere/disc when radius>0 for fluff, otherwise single block
            guardAndStore(px, py, pz, radius, chosen, radius > 0);
        }
    }


    public enum LeafType {
        NORMAL,
        DANGLING,
        BUSHY
    }

    /**
     * Spawn leaves according to the given type and params.
     * @param origin    Center/origin of the leaf‐cluster.
     */
    public void setLeaves(Vec3 origin) {
        int tx = origin.getX(), ty = origin.getY(), tz = origin.getZ();
        int thickness = ((Number) params.getOrDefault("thickness", 2)).intValue();
        switch(type) {
            case NORMAL -> {
                // filled sphere radius = thickness
                short[] offsets = StructureCacheUtils.getSphereOffsets(thickness);
                for(int i = 0; i < offsets.length; i += 3) {
                    int x = tx + offsets[i];
                    int y = ty + offsets[i+1];
                    int z = tz + offsets[i+2];
                    guardAndStore(x, y, z, leaves, true, 0);
                }
            }
            case DANGLING -> {
                // vertical chain downwards of length `length`
                int length = ((Number) params.getOrDefault("length", 5)).intValue();
                for(int d = 0; d < length; d++) {
                    int y = ty - d;
                    guardAndStore(tx, y, tz, leaves, false, thickness);
                }
            }
            case BUSHY -> {
                // hemisphere pointing downwards (a "mushroom" cap)
                short[] off = StructureCacheUtils.getSphereOffsets(thickness);
                for(int i = 0; i < off.length; i += 3) {
                    int dx = off[i], dy = off[i+1], dz = off[i+2];
                    // only place if below or at origin
                    if(dy <= 0) {
                        guardAndStore(tx + dx, ty + dy, tz + dz, leaves, true, 0);
                    }
                }
            }
        }
    }

    // — Roots as radial spokes that drop into the ground —
    // — Roots as radial spokes that drop into the ground (improved rooting) —
    private void calculateRoots(Vec3 o, RandomSource rnd) {
        int ox = o.getX(), oy = o.getY(), oz = o.getZ();
        int maxLen = Math.max(4, maxWidth * 3);
        int spokes = Math.max(6, maxWidth * 2);

        int maxProbeDepth = Math.max(4, maxHeight / 2); // how far down we search for ground

        for (int s = 0; s < spokes; s++) {
            double angle = rnd.nextDouble() * TAU;
            double ax = Math.cos(angle), az = Math.sin(angle);

            // choose an outward extent (not all the way to maxLen every time)
            int ext = (int) Math.round(maxLen * (0.5 + rnd.nextDouble() * 0.5));

            // compute the final projected endpoint at surface
            int rx = ox + (int) Math.round(ax * ext);
            int rz = oz + (int) Math.round(az * ext);

            // scan downwards to find first non-air (queued or previously placed)
            int groundY = Integer.MIN_VALUE;
            for (int probe = 0; probe <= maxProbeDepth; probe++) {
                int testY = oy - probe;
                PlatformBlockState<T> st = getQueuedState(rx, testY, rz);
                if (st != null && !st.getId().equals("minecraft:air")) {
                    groundY = testY;
                    break;
                }
            }
            // If we didn't find a ground block in the probe range, default to oy - maxProbeDepth
            if (groundY == Integer.MIN_VALUE) {
                groundY = oy - maxProbeDepth;
            }

            // Build the root as a tapering column from trunk base down/out to groundY
            // We'll step along the line from trunk base (ox,oy,oz) -> (rx,groundY,rz)
            int dx = rx - ox, dz = rz - oz, dy = groundY - oy;
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(Math.hypot(dx, dy), dz)));
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                int px = ox + (int) Math.round(dx * t);
                int pz = oz + (int) Math.round(dz * t);
                int py = oy + (int) Math.round(dy * t);

                // taper radius: thicker near trunk
                double fracFromBase = 1.0 - t;
                int r = Math.max(0, (int) Math.round((maxTrunkThickness / 2.0) * fracFromBase));

                guardAndStore(px, py, pz, r, wood, r > 0);
            }

            // At the ground contact, optionally place a little clamp / rootlet deeper
            if (rnd.nextDouble() < 0.4) {
                int probeDown = 1 + rnd.nextInt(0, Math.max(1, maxProbeDepth / 3));
                for (int k = 1; k <= probeDown; k++) {
                    guardAndStore(rx, groundY - k, rz, 0, wood, false);
                }
            }
        }
    }


    public static class Builder<T> extends
            BaseBuilder<T, ProceduralBranchedTreeGenerator<T>> {
        public List<BlockSeqEntry<T>> seq = new ArrayList<>();
        int maxTrunkThickness = 6, maxHeight = 20, maxWidth = 10, branchThreshold = 3, maxBranchThickness = 5;
        boolean roots = false, addLeaves = false, hollow = false, cheapMode = false;
        float randomness = 0.5f, twistiness = 0.2f, qualityFactor = 0.4f, branchStart=0.4f, branchLengthRatioTOHeight=0.3f, maxBranchShrink = 0.2f, minPitch = 0.1f, maxPitch = 0.3f, branchShrinkPerLevel = 0.7f;
        PlatformBlockState<T> woodMaterial, leavesMaterial;
        LeafType leafType;
        Map<String, Object> params;
        final List<DecoratorEntry<T>> decoratorEntries = new ArrayList<>();

        /**
         * @param leafState The {@code PlatformBlockState<V>} to use for leaves.
         * @param leafType      The type of leaf cluster (NORMAL, DANGLING, BUSHY).
         * @param params    Parameter map:
         *                  - "thickness": integer radius for NORMAL/BUSHY, thickness for DANGLING
         *                  - "length":    integer length for DANGLING only
         */
        public Builder<T> setLeaf(PlatformBlockState<T> leafState, LeafType leafType, Map<String, Object> params) {
            this.leavesMaterial = leafState;
            this.leafType = leafType;
            this.params = params;
            this.addLeaves = true;
            return this;
        }

        public Builder<T> addDanglingSequence(BlockSeqEntry<T> entry) {
            this.seq.add(entry);
            return this;
        }

        public Builder<T> addDanglingSequences(List<BlockSeqEntry<T>> entry) {
            this.seq.addAll(entry);
            return this;
        }

        public Builder<T> cheapMode(boolean c) {
            cheapMode = c;
            return this;
        }

        public Builder<T> qualityFactor(float q) {
            qualityFactor = q;
            return this;
        }

        public Builder<T> branchShrinkPerLevel(float q) {
            branchShrinkPerLevel = q;
            return this;
        }

        public Builder<T> trunkThickness(int t) {
            maxTrunkThickness = t;
            return this;
        }

        public Builder<T> height(int h) {
            maxHeight = h;
            return this;
        }

        public Builder<T> width(int w) {
            maxWidth = w;
            return this;
        }

        public Builder<T> branchDepth(int d) {
            branchThreshold = d;
            return this;
        }

        public Builder<T> roots() {
            roots = true;
            return this;
        }

        public Builder<T> noLeaves() {
            addLeaves = false;
            return this;
        }

        public Builder<T> hollow() {
            hollow = true;
            return this;
        }

        public Builder<T> randomness(float r) {
            randomness = r;
            return this;
        }

        public Builder<T> twistiness(float t) {
            twistiness = t;
            return this;
        }

        public Builder<T> woodMaterial(PlatformBlockState<T> m) {
            woodMaterial = m;
            return this;
        }
        // Percentage of trunk height for branches to start spawning
        public Builder<T> branchStart(float p) {
            branchStart = p;
            return this;
        }
        // Reduction in branch length with spawn height on tree
        public Builder<T> branchLengthReduction(float p) {
            this.branchLengthRatioTOHeight= p;
            return this;
        }

        public Builder<T> maxBranchShrink(float p) {
            this.maxBranchShrink = p;
            return this;
        }

        public Builder<T> branchPitchRange(float min, float max) {
            this.minPitch = min;
            this.maxPitch = max;
            return this;
        }

        public Builder<T> branchThickness(int i) {
            this.maxBranchThickness = i;
            return this;
        }

        public Builder<T> addDecoration(float c,
                                        BiConsumer<PlatformWorld<T, ?>, Vec3> a,
                                        Orientation... o) {
            decoratorEntries.add(new DecoratorEntry<>(a, c,
                    EnumSet.copyOf(Arrays.asList(o))));
            return this;
        }


        @Override
        public void validate() {
            // === PlatformBlockState<T> Checks ===
            Objects.requireNonNull(woodMaterial, "Wood material must be set");

            if (addLeaves) {
                Objects.requireNonNull(leavesMaterial, "Leaves material must be set when addLeaves is true");
                Objects.requireNonNull(leafType, "Leaf type must be specified when addLeaves is true");

                if (params == null)
                    throw new IllegalArgumentException("Leaf parameters map must be provided");

                if (!params.containsKey("thickness") && leafType != LeafType.DANGLING)
                    throw new IllegalArgumentException("Leaf parameter 'thickness' is required");

                if (leafType == LeafType.DANGLING && !params.containsKey("length"))
                    throw new IllegalArgumentException("Dangling leaf type requires 'length' parameter");
            }

            // === Geometric Constraints ===
            if (maxTrunkThickness < 1)
                throw new IllegalArgumentException("Max trunk thickness must be >= 1");

            if (maxHeight < 1)
                throw new IllegalArgumentException("Max tree height must be >= 1");

            if (maxWidth < 1)
                throw new IllegalArgumentException("Max tree width must be >= 1");

            if (branchThreshold < 1)
                throw new IllegalArgumentException("Branch threshold (depth) must be >= 1");

            if (maxBranchThickness < 1)
                throw new IllegalArgumentException("Max branch thickness must be >= 1");

            // === Probability & Percent Constraints ===
            if (randomness < 0f || randomness > 1f)
                throw new IllegalArgumentException("Randomness must be between 0 and 1");

            if (twistiness < 0f || twistiness > 1f)
                throw new IllegalArgumentException("Twistiness must be between 0 and 1");

            if (qualityFactor < 0f || qualityFactor > 1f)
                throw new IllegalArgumentException("Quality factor must be between 0 and 1");

            if (branchStart < 0f || branchStart > 1f)
                throw new IllegalArgumentException("Branch start percentage must be between 0 and 1");

            if (branchLengthRatioTOHeight < 0f || branchLengthRatioTOHeight > 1f)
                throw new IllegalArgumentException("Branch length ratio to height must be between 0 and 1");

            if (maxBranchShrink < 0f || maxBranchShrink > 1f)
                throw new IllegalArgumentException("Max branch shrink must be between 0 and 1");

            if (branchShrinkPerLevel <= 0f || branchShrinkPerLevel > 1f)
                throw new IllegalArgumentException("Branch shrink per level must be > 0 and <= 1");

            if (minPitch < 0f || maxPitch < 0f || maxPitch < minPitch)
                throw new IllegalArgumentException("Invalid pitch range: min=" + minPitch + ", max=" + maxPitch);

            // === Decorations Check (optional) ===
            for (DecoratorEntry<T> entry : decoratorEntries) {
                if (entry == null || entry.action == null || entry.orientations == null || entry.orientations.isEmpty()) {
                    throw new IllegalArgumentException("Invalid decorator entry found");
                }
            }
        }

        @Override
        public ProceduralBranchedTreeGenerator<T> build() {
            return new ProceduralBranchedTreeGenerator<>(this);
        }


        static class DecoratorEntry<T> {
            final BiConsumer<PlatformWorld<T, ?>, Vec3> action;
            final float chance;
            final Set<Orientation> orientations;

            DecoratorEntry(BiConsumer<PlatformWorld<T, ?>, Vec3> a,
                           float c, Set<Orientation> o) {
                action = a;
                chance = c;
                orientations = o;
            }
        }

        // small helper class (put it inside the generator class or as static nested)
        public static final class BlockSeqEntry<T> {
            public final PlatformBlockState<T> state;
            public final double startNorm; // inclusive
            public final double endNorm;   // inclusive
            public final double weight;    // optional, currently unused but handy

            public BlockSeqEntry(PlatformBlockState<T> state, double s, double e) {
                this(state, s, e, 1.0);
            }

            public BlockSeqEntry(PlatformBlockState<T> state, double s, double e, double w) {
                this.state = state;
                this.startNorm = Math.max(0.0, Math.min(1.0, s));
                this.endNorm = Math.max(0.0, Math.min(1.0, e));
                this.weight = w;
            }

            public boolean contains(double norm) {
                return norm >= startNorm && norm <= endNorm;
            }
        }

    }

    public enum Orientation {TOP, BOTTOM, LEFT, RIGHT}
}
