package org.vicky.vspe.systems.dimension.StructureUtils.Generators;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.Bukkit;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.systems.dimension.StructureUtils.StructureCacheUtils;

import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.Math.TAU;

/**
 * Procedural tree generator (trunk + branches + roots, with cheapMode).
 */
public class ProceduralBranchedTreeGenerator extends ProceduralStructureGenerator {
    private final int maxTrunkThickness, maxHeight, maxWidth, branchThreshold, maxBranchThickness;
    private final boolean roots, addLeaves, hollow;
    private final float randomness, twistiness, branchLengthRatioToHeight, branchStart, minPitch, maxPitch, maxBranchShrink, branchShrinkPerLevel;
    private final BlockState wood, leaves;
    private final List<Builder.DecoratorEntry> decorators;
    private final boolean cheapMode;
    private final float qualityFactor;
    private final LeafType type;
    private final Map<String, Object> params;

    public ProceduralBranchedTreeGenerator(Builder b) {
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
        this.maxBranchThickness = b.maxBranchThickness;
        this.maxBranchShrink = b.maxBranchShrink;
        this.branchShrinkPerLevel = b.branchShrinkPerLevel;
        this.type = b.leafType;
        this.params = b.params;
    }

    /**
     * Synchronous generate (fires off async & returns immediately).
     */
    public void generate(Random rnd, WritableWorld world, Vector3Int origin, Platform platform) {
        generateAsync(rnd, world, origin, platform);
    }

    public void generateAsync(Random rnd,
                              WritableWorld world,
                              Vector3Int origin,
                              Platform platform) {

        prepareFlush(world, platform);

        // 3) geometry
        calculateTrunk(origin, rnd);
        if (roots) calculateRoots(origin);

        // 4) flush once
        flush.run();
        Bukkit.getScheduler().scheduleSyncDelayedTask(VSPE.getPlugin(), () -> VSPE.getInstancedLogger().info("[DEBUG] Generated Tree completed"));
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    // — Trunk via scanlines and ring‐skipping —
    private void calculateTrunk(Vector3Int origin, Random rnd) {
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
        List<Vector3> hemisphereDirs = fibonacciHemisphere(branchThreshold);

        // 3) Spawn one primary branch for each direction
        int yTop = (int) (origin.getY() + ((maxHeight - 1) * branchStart));
        double topR = maxTrunkThickness / Math.sqrt(1 + alpha * (maxHeight - 1));

        for(Vector3 dir : hemisphereDirs) {
            // project onto trunk rim for start point:
            int startX = origin.getX() + (int)Math.round(dir.x() * topR);
            int startZ = origin.getZ() + (int)Math.round(dir.z() * topR);

            // branch thickness & length bounds
            recurseBranch(
                    rnd,
                    Vector3Int.of(startX, yTop, startZ),
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
    private void recurseBranch(Random rnd,
                               Vector3Int origin,
                               int level,
                               double parentR,
                               Vector3 dir) {
        // 1) stop if too thin or too deep
        if(parentR < 1 || level >= 4) return;

        double depthFactor = Math.pow(1.0 - (level / (double)branchThreshold), 1.5);
        double spawnChance = randomness * depthFactor;

        // 2) shrink per-level
        double currR = Math.max(1, parentR * branchShrinkPerLevel);
        int branchRadius = (int)Math.round(currR);

        // 3) fixed straight-line length (no extra randomness here)
        int len = (int) (maxWidth / Math.max(1, branchLengthRatioToHeight * level));
        if(cheapMode) len = (int)(len * qualityFactor);

        // 4) march straight out
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int lastDx = 0, lastDy = 0, lastDz = 0;

        int maxChildren = branchThreshold / (level + 2);
        int spawned      = 0;
        boolean placed   = false;
        int minStep = Math.max(1, branchRadius/2);
        int thresholdSq = minStep*minStep;

        for(int j = 0; j <= len; j++) {
            // current offset along dir
            int dx = (int)Math.round(dir.x() * j);
            int dy = (int)Math.round(dir.y() * j);
            int dz = (int)Math.round(dir.z() * j);

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
                        guardAndAction(tx, ty, bz, d.action);
                    }
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
                    Vector3 axis = dir.normalize();

                    // 2) Build an orthonormal basis {U,V} perpendicular to axis
                    Vector3 U = axis.cross(Vector3.at(0, 1, 0));
                    if(U.lengthSq() < 1e-6) {
                        // axis was (nearly) vertical—use another fallback
                        U = axis.cross(Vector3.at(1, 0, 0));
                    }
                    U = U.normalize();
                    Vector3 V = axis.cross(U).normalize();

                    // 3) Pick a random angle around the circle
                    double step    = TAU / maxChildren;
                    double baseAz  = spawned * step;
                    double jitter  = step * 0.1 * depthFactor;  // 20%
                    double azim    = baseAz + (rnd.nextDouble()*2 - 1) * jitter;

                    // 4) Your lateral offset direction on that circle:
                    Vector3 lateral = U.multiply(Math.cos(azim))
                            .add(V.multiply(Math.sin(azim)))
                            .normalize();

                    double raw = randomness * twistiness;            // [0..1]
                    double skew = Math.pow(raw, 0.5);         // tweak exponent for stronger bias
                    double pitch = minPitch + skew*(maxPitch-minPitch); // always positive
                    if (level+2 == branchThreshold && rnd.nextFloat() < twistiness)
                        pitch = -pitch;

                    // 6) Build child direction
                    Vector3 childDir = lateral.multiply(Math.cos(pitch))
                            .add(axis.multiply(Math.sin(pitch)))
                            .normalize();

                    // 7) If it somehow went downward, flip its vertical component
                    if (childDir.y() < 0) {
                        childDir = Vector3.at(childDir.x(),
                                -childDir.y(),
                                childDir.z()).normalize();
                    }

                    // 6) Recurse from this exact stamp point:
                    recurseBranch(
                            rnd,
                            Vector3Int.of(bx, by, bz),
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
            setLeaves(Vector3Int.of(lastDx, lastDy, lastDz));
        }
        if(level + 2 == branchThreshold) {
            // 1) Build only the upper hemisphere directions
            List<Vector3> hemiAll = fibonacciHemisphere(branchThreshold * 2);
            List<Vector3> hemiUp = hemiAll.stream()
                    .filter(v -> v.y() > 0)  // keep only upward‐pointing
                    .toList();

            // 2) Compute world‐space rim height
            int worldDy = oy + lastDy;
            int yTop    = (int)(worldDy + ((maxHeight - 1) * branchStart));
            double topR = maxTrunkThickness / Math.sqrt(1 + 0.04 * (maxHeight - 1));

            for(Vector3 rimDir : hemiUp) {
                // project start point around that rim vector
                int startX = ox + lastDx + (int)Math.round(rimDir.x() * topR);
                int startZ = oz + lastDz + (int)Math.round(rimDir.z() * topR);

                // now build a perfect U,V ⟂ parent axis = rimDir
                Vector3 N = rimDir.normalize();
                Vector3 up = Vector3.at(0,1,0);
                Vector3 U = N.cross(up);
                if(U.lengthSq() < 1e-6) U = N.cross(Vector3.at(1,0,0));
                U = U.normalize();
                Vector3 V = N.cross(U).normalize();

                // random around the circle
                double w = rnd.nextDouble() * TAU;

                // tilt by exactly 90° ± pitch
                double o = minPitch + rnd.nextDouble() * (maxPitch - minPitch);
                if(rnd.nextBoolean()) o = -o;
                double t = Math.PI/2 + o;

                // assemble child direction
                Vector3 childDir = U.multiply(Math.cos(w)*Math.sin(t))
                        .add(V.multiply(Math.sin(w)*Math.sin(t)))
                        .add(N.multiply(Math.cos(t)))
                        .normalize();

                // spawn that secondary branch
                recurseBranch(
                        rnd,
                        Vector3Int.of(startX, yTop, startZ),
                        level + 1,
                        currR,
                        childDir
                );
            }
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
    public void setLeaves(Vector3Int origin) {
        int tx = origin.getX(), ty = origin.getY(), tz = origin.getZ();
        int thickness = (int) params.getOrDefault("thickness", 2);
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
                int length = (int) params.getOrDefault("length", 5);
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

    // — Roots as large flat discs —
    private void calculateRoots(Vector3Int o) {
        int ox = o.getX(), oy = o.getY(), oz = o.getZ();
        int h = maxHeight / 2, w3 = maxWidth * 3;
        for (int dy = 0; dy < h; dy++) {
            int r = (int) (w3 * (1 - dy / (double) h));
            guardAndStore(ox, oy - dy, oz, wood, false, r);
        }
    }

    public static class Builder {
        int maxTrunkThickness = 6, maxHeight = 20, maxWidth = 10, branchThreshold = 3, maxBranchThickness = 5;
        boolean roots = false, addLeaves = false, hollow = false, cheapMode = false;
        float randomness = 0.5f, twistiness = 0.2f, qualityFactor = 0.4f, branchStart=0.4f, branchLengthRatioTOHeight=0.3f, maxBranchShrink = 0.2f, minPitch = 0.1f, maxPitch = 0.3f, branchShrinkPerLevel = 0.7f;
        BlockState woodMaterial, leavesMaterial;
        LeafType leafType;
        Map<String, Object> params;
        final List<DecoratorEntry> decoratorEntries = new ArrayList<>();

        /**
         * @param leafState The BlockState to use for leaves.
         * @param leafType      The type of leaf cluster (NORMAL, DANGLING, BUSHY).
         * @param params    Parameter map:
         *                  - "thickness": integer radius for NORMAL/BUSHY, thickness for DANGLING
         *                  - "length":    integer length for DANGLING only
         */
        public Builder setLeaf(BlockState leafState, LeafType leafType, Map<String, Object> params) {
            this.leavesMaterial = leafState;
            this.leafType = leafType;
            this.params = params;
            this.addLeaves = true;
            return this;
        }

        public Builder cheapMode(boolean c) {
            cheapMode = c;
            return this;
        }

        public Builder qualityFactor(float q) {
            qualityFactor = q;
            return this;
        }

        public Builder branchShrinkPerLevel(float q) {
            branchShrinkPerLevel = q;
            return this;
        }

        public Builder trunkThickness(int t) {
            maxTrunkThickness = t;
            return this;
        }

        public Builder height(int h) {
            maxHeight = h;
            return this;
        }

        public Builder width(int w) {
            maxWidth = w;
            return this;
        }

        public Builder branchDepth(int d) {
            branchThreshold = d;
            return this;
        }

        public Builder roots() {
            roots = true;
            return this;
        }

        public Builder noLeaves() {
            addLeaves = false;
            return this;
        }

        public Builder hollow() {
            hollow = true;
            return this;
        }

        public Builder randomness(float r) {
            randomness = r;
            return this;
        }

        public Builder twistiness(float t) {
            twistiness = t;
            return this;
        }

        public Builder woodMaterial(BlockState m) {
            woodMaterial = m;
            return this;
        }
        // Percentage of trunk height for branches to start spawning
        public Builder branchStart(float p) {
            branchStart = p;
            return this;
        }
        // Reduction in branch length with spawn height on tree
        public Builder branchLengthReduction(float p) {
            this.branchLengthRatioTOHeight= p;
            return this;
        }
        public Builder maxBranchShrink(float p) {
            this.maxBranchShrink = p;
            return this;
        }
        public Builder branchPitchRange(float min, float max) {
            this.minPitch = min;
            this.maxPitch = max;
            return this;
        }

        public Builder branchThickness(int i) {
            this.maxBranchThickness = i;
            return this;
        }
        /*
        public Builder branchLengthReduction(float p) {
            branchLengthRatioTOHeight = p;
            return this;
        }
         */

        public Builder addDecoration(float c,
                                     BiConsumer<WritableWorld, Vector3Int> a,
                                     Orientation... o) {
            decoratorEntries.add(new DecoratorEntry(a, c,
                    EnumSet.copyOf(Arrays.asList(o))));
            return this;
        }

        public ProceduralBranchedTreeGenerator build() {
            return new ProceduralBranchedTreeGenerator(this);
        }

        static class DecoratorEntry {
            final BiConsumer<WritableWorld, Vector3Int> action;
            final float chance;
            final Set<Orientation> orientations;

            DecoratorEntry(BiConsumer<WritableWorld, Vector3Int> a,
                           float c, Set<Orientation> o) {
                action = a;
                chance = c;
                orientations = o;
            }
        }
    }

    public enum Orientation {TOP, BOTTOM, LEFT, RIGHT}
}
