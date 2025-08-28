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
        double baseTaperPower = 1.0;
        double aspect = (double) maxTrunkThickness / Math.max(1, maxHeight);
        double taperPower = Math.max(0.5, baseTaperPower + aspect * 1.5);
        double hollowFactor = 0.45;

        // lateral drift state for trunk (coherent)
        double driftX = 0.0;
        double driftZ = 0.0;
        double perStepDriftScale = twistiness / 1.12; // tune: higher -> more lateral movement per block

        for (int y = 0; y < maxHeight; y++) {
            double yf = (double) y / Math.max(1, (maxHeight - 1));
            double curr = (double) maxTrunkThickness * Math.pow(1.0 - yf, taperPower);
            curr = Math.max(0.25, curr);
            int rIter = (int) Math.ceil(curr + 0.5);
            int worldY = origin.getY() + y;

            // update drift by a smooth random delta (coherent)
            // small gaussian-like via sum of two uniform deltas
            double dx = (rnd.nextDouble() - rnd.nextDouble()) * randomness * perStepDriftScale;
            double dz = (rnd.nextDouble() - rnd.nextDouble()) * randomness * perStepDriftScale;
            // apply twistiness as small rotation to drift
            double twist = (rnd.nextDouble() * 2.0 - 1.0) * twistiness * 0.03;
            double c = Math.cos(twist), s = Math.sin(twist);
            double newDriftX = driftX * c - driftZ * s + dx;
            double newDriftZ = driftX * s + driftZ * c + dz;
            driftX = newDriftX;
            driftZ = newDriftZ;

            // center for this slice
            int cx = origin.getX() + (int) Math.round(driftX);
            int cz = origin.getZ() + (int) Math.round(driftZ);

            double innerRadius = hollow ? Math.max(0.0, curr * hollowFactor) : -1.0;

            for (int dz2 = -rIter; dz2 <= rIter; dz2++) {
                for (int dx2 = -rIter; dx2 <= rIter; dx2++) {
                    double dist = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                    if (dist <= curr - 0.5) {
                        guardAndStore(cx + dx2, worldY, cz + dz2, 0, wood, false);
                        continue;
                    }
                    if (hollow && innerRadius >= 0 && dist < innerRadius) continue;
                    if (dist <= curr + 0.5) {
                        double p = (curr + 0.5 - dist);
                        if (rnd.nextDouble() < p) {
                            guardAndStore(cx + dx2, worldY, cz + dz2, 0, wood, false);
                        }
                    }
                }
            }
        }

        Vec3 trunkTop = origin.add(driftX, (double) ((maxHeight - 1) * branchStart), driftZ);
        generateSpaceColonizationBranches(rnd, trunkTop);
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

    // ---- Space Colonization helpers ----

    /**
     * Generate attraction points in an ellipsoid above the trunk top.
     */
    private List<Vec3> generateAttractionPoints(RandomSource rnd, Vec3 trunkTop, double radiusX, double radiusY, double radiusZ, int count) {
        List<Vec3> points = new ArrayList<>(count);
        int tries = 0;
        while (points.size() < count && tries < count * 6) { // some rejection sampling
            tries++;
            double rx = (rnd.nextDouble() * 2.0 - 1.0) * radiusX;
            double ry = rnd.nextDouble() * radiusY; // biased upward hemisphere (0..radiusY)
            double rz = (rnd.nextDouble() * 2.0 - 1.0) * radiusZ;
            // accept if inside ellipsoid (x/rx)^2 + (y/ry)^2 + (z/rz)^2 <= 1
            double nx = (radiusX == 0) ? 0 : rx / radiusX;
            double ny = (radiusY == 0) ? 0 : ry / radiusY;
            double nz = (radiusZ == 0) ? 0 : rz / radiusZ;
            if (nx * nx + ny * ny + nz * nz <= 1.0) {
                points.add(new Vec3(trunkTop.getX() + rx, trunkTop.getY() + ry, trunkTop.getZ() + rz));
            }
        }
        return points;
    }

    /**
     * Grow a skeleton (list of edges) using the Space Colonization algorithm.
     * <p>
     * Parameters tuned conservatively; adjust to taste.
     */
    private List<Edge> growSkeletonSpaceColonization(RandomSource rnd, Vec3 trunkTop, List<Vec3> attractPoints,
                                                     double influenceRadius, double killRadius, double stepSize,
                                                     int maxIterations, int maxNodes) {
        List<Edge> edges = new ArrayList<>();
        List<BranchNode> nodes = new ArrayList<>();
        BranchNode root = new BranchNode(trunkTop, null, 0);
        nodes.add(root);

        // precompute squares for distance tests
        double infR2 = influenceRadius * influenceRadius;
        double killR2 = killRadius * killRadius;

        int iter = 0;
        while (!attractPoints.isEmpty() && iter++ < maxIterations && nodes.size() < maxNodes) {
            // assignment: map node -> list of targets (we accumulate direction sums & counts instead)
            Map<BranchNode, Vec3> sumDir = new HashMap<>();
            Map<BranchNode, Integer> sumCount = new HashMap<>();

            // for each target, find nearest node within influenceRadius
            Iterator<Vec3> atkIter = attractPoints.iterator();
            while (atkIter.hasNext()) {
                Vec3 a = atkIter.next();
                BranchNode nearest = null;
                double best2 = Double.POSITIVE_INFINITY;
                for (BranchNode n : nodes) {
                    double dx = a.getX() - n.pos.getX();
                    double dy = a.getY() - n.pos.getY();
                    double dz = a.getZ() - n.pos.getZ();
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 < best2) {
                        best2 = d2;
                        nearest = n;
                    }
                }
                if (nearest == null) continue;
                if (best2 <= infR2) {
                    // add direction vector
                    Vec3 dir = new Vec3(a.getX() - nearest.pos.getX(), a.getY() - nearest.pos.getY(), a.getZ() - nearest.pos.getZ());
                    Vec3 prev = sumDir.get(nearest);
                    if (prev == null) sumDir.put(nearest, dir);
                    else sumDir.put(nearest, prev.add(dir)); // Vec3.add assumed
                    sumCount.merge(nearest, 1, Integer::sum);
                } else {
                    // If not within any influence radius, we keep the attraction point for later (could also discard after many iters)
                }
            }

            // Create new nodes from contributions
            List<BranchNode> newNodes = new ArrayList<>();
            for (Map.Entry<BranchNode, Vec3> e : sumDir.entrySet()) {
                BranchNode base = e.getKey();
                Vec3 avg = e.getValue();
                int count = sumCount.getOrDefault(base, 1);

                // average direction (toward attractors)
                Vec3 rawDir = avg.multiply(1.0 / count).normalize();

                // Smooth with parent's dir to avoid sudden swings (blend factor)
                double blendFactor = 0.6; // how much we trust parent dir vs new target (0..1)
                if (base.dir != null) {
                    rawDir = base.dir.multiply(blendFactor).add(rawDir.multiply(1.0 - blendFactor)).normalize();
                }

                // Apply randomness perturbation (coherent): create a small random unit vector
                // scale it by randomness parameter and reduce with depth a bit so near-trunk is less wiggly
                double depthFactor = Math.max(0.25, 1.0 - base.depth * 0.06); // tune: deeper nodes slightly less/more wiggly
                double perturbScale = randomness * 0.8 * depthFactor; // global scale; tweak 0.8 if too strong

                Vec3 rndVec = new Vec3(rnd.nextDouble() * 2.0 - 1.0, rnd.nextDouble() * 2.0 - 1.0, rnd.nextDouble() * 2.0 - 1.0)
                        .normalize().multiply(perturbScale);

                // apply twistiness as a mild yaw rotation around Y: small angle per step
                double twistAngle = (rnd.nextDouble() * 2.0 - 1.0) * twistiness * 0.25; // small angle (radians)
                // rotate rawDir around Y by twistAngle (simple yaw)
                double cos = Math.cos(twistAngle), sin = Math.sin(twistAngle);
                double dx = rawDir.getX(), dz = rawDir.getZ();
                double rotatedX = dx * cos - dz * sin;
                double rotatedZ = dx * sin + dz * cos;
                Vec3 twisted = new Vec3(rotatedX, rawDir.getY(), rotatedZ).normalize();

                // final direction = twisted + random perturbation; renormalize
                Vec3 dir = twisted.add(rndVec).normalize();

                // new position step
                Vec3 newPos = base.pos.add(dir.multiply(stepSize));

                // ensure not too close (same as before)
                double minSpacing = Math.max(0.5, stepSize * 0.6);
                boolean tooClose = false;
                for (BranchNode n : nodes) {
                    double dx2 = newPos.getX() - n.pos.getX();
                    double dy2 = newPos.getY() - n.pos.getY();
                    double dz2 = newPos.getZ() - n.pos.getZ();
                    if (dx2 * dx2 + dy2 * dy2 + dz2 * dz2 < minSpacing * minSpacing) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                // create new node and set its dir for coherent next steps
                BranchNode nn = new BranchNode(newPos, base, base.depth + 1);
                nn.dir = dir; // store the coherent direction
                nodes.add(nn);
                newNodes.add(nn);
                edges.add(new Edge(base, nn));
                if (nodes.size() >= maxNodes) break;
            }

            if (newNodes.isEmpty()) {
                // nothing grew this iteration -> stop early
                break;
            }

            // Remove attraction points that are within killRadius of any new node
            Iterator<Vec3> atRem = attractPoints.iterator();
            outer:
            while (atRem.hasNext()) {
                Vec3 a = atRem.next();
                for (BranchNode n : newNodes) {
                    double dx = a.getX() - n.pos.getX();
                    double dy = a.getY() - n.pos.getY();
                    double dz = a.getZ() - n.pos.getZ();
                    if (dx * dx + dy * dy + dz * dz <= killR2) {
                        atRem.remove();
                        continue outer;
                    }
                }
            }
        }

        return edges;
    }

    /**
     * Convert skeleton edges into block placements by stamping cylinders along each edge.
     * thicknessBase controls trunk->branch thickness scaling.
     */
    private void skeletonToBlocks(List<Edge> skeleton, RandomSource rnd, double thicknessBase) {
        for (Edge e : skeleton) {
            Vec3 a = e.a.pos;
            Vec3 b = e.b.pos;
            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();
            double dz = b.getZ() - a.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len <= 0.001) continue;

            int steps = Math.max(1, (int) Math.ceil(len / 0.5)); // sample every 0.5 blocks

            // choose thickness by child depth
            int depth = e.b.depth;
            double radius = Math.max(0.5, thicknessBase * Math.pow(branchShrinkPerLevel, depth));
            int rIter = (int) Math.ceil(radius + 0.5);

            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                // pos = a + (b-a)*t but also add small lateral perturbation for curvature (lerp node dirs)
                double px = a.getX() + dx * t;
                double py = a.getY() + dy * t;
                double pz = a.getZ() + dz * t;

                // place disk using fractional raster (anti-alias)
                int ix = (int) Math.round(px);
                int iy = (int) Math.round(py);
                int iz = (int) Math.round(pz);

                for (int dz2 = -rIter; dz2 <= rIter; dz2++) {
                    for (int dx2 = -rIter; dx2 <= rIter; dx2++) {
                        double dist = Math.sqrt(dx2 * dx2 + dz2 * dz2);
                        if (dist <= radius - 0.5) {
                            guardAndStore(ix + dx2, iy, iz + dz2, 0, wood, false);
                        } else if (dist <= radius + 0.5) {
                            double pEdge = (radius + 0.5 - dist);
                            if (rnd.nextDouble() < pEdge) {
                                guardAndStore(ix + dx2, iy, iz + dz2, 0, wood, false);
                            }
                        }
                    }
                }

                // ---- dangling leaves spawn along branches (sample density tuned by length)
                // probability per sample = randomness * 0.25 (tweakable)
                double leafSpawnChance = randomness * 0.25;
                if (addLeaves && type == LeafType.DANGLING && rnd.nextDouble() < leafSpawnChance) {
                    // compute a radius for this location (proportional to branch radius)
                    int spawnRadius = Math.max(0, (int) Math.round(radius * 0.6));
                    attachHangingChain(ix, iy - 1, iz, Math.max(1, spawnRadius), /*thickStart*/spawnRadius / 2, /*thickEnd*/0, rnd);
                }
            }
        }
    }

    /**
     * Public method to run SC branches. Call after trunk is built.
     */
    private void generateSpaceColonizationBranches(RandomSource rnd, Vec3 trunkTop) {
        // Tunables (try adjusting)
        int attractCount = Math.round(maxWidth * maxWidth * 6 * qualityFactor); // density
        double radiusX = Math.max(2, maxWidth * 0.9);
        double radiusY = Math.max(2, maxHeight * 0.55);
        double radiusZ = radiusX;
        double influenceRadius = Math.max(2.5, Math.min(radiusX, 4.0));
        double killRadius = 1.2;
        double stepSize = Math.max(0.8, 1.4 * (1.0 - qualityFactor)); // shorter steps = finer branches
        int maxIter = 200;
        int maxNodes = 2000;

        List<Vec3> attracts = generateAttractionPoints(rnd, trunkTop, radiusX, radiusY, radiusZ, attractCount);
        if (attracts.isEmpty()) return;

        List<Edge> skeleton = growSkeletonSpaceColonization(rnd, trunkTop, attracts, influenceRadius, killRadius, stepSize, maxIter, maxNodes);
        // convert skeleton to blocks
        double thicknessBase = Math.max(1.0, maxBranchThickness * 0.9);
        skeletonToBlocks(skeleton, rnd, thicknessBase);

        // optionally place leaves at tips (leaf clusters at nodes with no children)
        for (Edge ed : skeleton) {
            // find nodes with no children - naive: child nodes that are never a parent in any edge
        }

        // simple way: collect all child nodes and find child nodes that don't appear as parent in any edge
        Set<BranchNode> parents = new HashSet<>();
        Set<BranchNode> children = new HashSet<>();
        for (Edge ed : skeleton) {
            parents.add(ed.a);
            children.add(ed.b);
        }
        for (BranchNode nd : children) {
            if (!parents.contains(nd)) {
                // tip node -> spawn leaves
                if (addLeaves) setLeaves(nd.pos);
            }
        }
    }

    private static final class BranchNode {
        final Vec3 pos;
        final BranchNode parent;
        final int depth; // depth from trunk root
        Vec3 dir;

        BranchNode(Vec3 pos, BranchNode parent, int depth) {
            this.pos = pos;
            this.parent = parent;
            this.depth = depth;
            this.dir = parent != null ? parent.dir : new Vec3(0.0, 1.0, 0.0);
        }
    }

    private static final class Edge {
        final BranchNode a, b;

        Edge(BranchNode a, BranchNode b) {
            this.a = a;
            this.b = b;
        }
    }

    public static class Builder<T> extends
            BaseBuilder<T, ProceduralBranchedTreeGenerator<T>> {
        public List<BlockSeqEntry<T>> seq = new ArrayList<>();
        int maxTrunkThickness = 6, maxHeight = 20, maxWidth = 10, branchThreshold = 3, maxBranchThickness = 5;
        boolean roots = false, addLeaves = false, hollow = false, cheapMode = false;
        float randomness = 0.5f, twistiness = 0.2f, qualityFactor = 1.0f, branchStart = 0.4f, branchLengthRatioTOHeight = 0.3f, maxBranchShrink = 0.2f, minPitch = 0.1f, maxPitch = 0.3f, branchShrinkPerLevel = 0.7f;
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
