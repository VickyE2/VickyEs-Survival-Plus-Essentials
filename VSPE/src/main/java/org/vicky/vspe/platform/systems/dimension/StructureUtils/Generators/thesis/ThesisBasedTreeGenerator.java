package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis;

import org.jetbrains.annotations.NotNull;
import org.vicky.platform.utils.Vec3;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.utilities.Pair;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;
import org.vicky.vspe.platform.utilities.ProgressTracker;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.clamp;
import static java.lang.Math.exp;

/**
 * World-agnostic, logic-focused tree generator.
 * Replace DefaultEnvironment with your own GrowthEnvironment for custom bounds/directions/seed.
 */
public class ThesisBasedTreeGenerator {
    private static final int MAX_TOTAL_NODES = 2000;
    private static final double OWNERSHIP_CAPACITY_BASE = 10.0;   // base capacity multiplier per (1.0 vigor)
    private static final double STEAL_THRESHOLD = 0.95;          // challenger must have >= 1.25 * minScore to steal
    private static final double DISTANCE_DECAY = 2.1;            // distance decay exponent (higher -> favors closer tips)
    @NotNull
    protected final GrowthData growthData;
    private final Map<Long, TreeNode> cachedBranches = new ConcurrentHashMap<>();
    private final Vec3 global_gravity;
    private final boolean debug = false;
    private final ContextLogger logger;
    private final boolean inProduction;
    private long lastNodeId = 0;
    private int age = 0;
    private long seed;
    private List<Attractor> cachedAttractors = null;
    private int attractorLayers = 3; // default layers (tweak per species)
    private TreeNode root = null;
    private float maxRadius;
    private double influenceRadiusAdaptive;
    private double vigorRadiusAdaptive;
    private double killRadiusAdaptive;
    private double averageConsumption = 1.0; // Initialize with 1.0 (assuming high efficiency start)
    private ExecutorService executor = null;
    private Map<TreeNode, List<Vec3>> attractors;
    private ProgressTracker tracker;

    // === Public API ===
    private double targetAge;

    private TraitKeeper traits =
            new TraitKeeper(MAX_TOTAL_NODES);

    public class BranchTraitDefs {
        public static final TraitKeeper.TraitField.FloatTrait COIL_RANGE =
                new TraitKeeper.TraitField.FloatTrait(0, 8, 1f, 5f);
        public static final TraitKeeper.TraitField.FloatTrait COIL_RADIUS_RATIO =
                new TraitKeeper.TraitField.FloatTrait(8, 8, 0f, 1f);
        public static final TraitKeeper.TraitField.DoubleTrait CONED_CONIFERISM_ANGLE =
                new TraitKeeper.TraitField.DoubleTrait(16, 8, 1.0, 80.0);
    }

    public List<Attractor> getAttractorPool() {
        return cachedAttractors;
    }

    private List<Attractor> getActiveAttractors() {
        // only attractors with layer <= unlockedLayerForAge(treeAge) are visible
        int unlocked = unlockedLayerForAge(age); // e.g. floor(treeAge * someFactor)
        return cachedAttractors.stream()
                .filter(a -> !a.reached /*&& a.layer <= unlocked*/)
                .collect(Collectors.toList());
    }
    private int unlockedLayerForAge(int age) {
        // simple linear: early ages reveal layer 0; later expose next layers.
        // tweak constants to taste:
        int agesPerLayer = Math.max(1, 6); // e.g., every 6 growth ticks unlocks next layer
        return Math.min(attractorLayers - 1, age / agesPerLayer);
    }

    public ThesisBasedTreeGenerator(@NotNull GrowthData growthData, long seed, ContextLogger logger, boolean inProduction) {
        this.inProduction = inProduction;
        this.growthData = growthData;
        this.seed = seed;
        this.logger = logger;
        if (growthData.overrides.contains(Overrides.GlobalOverrides.OVERRIDE_GRAVITY)) {
            this.global_gravity = growthData.overridenGravity;
        } else {
            this.global_gravity = Vec3.of(0.0, -1.0, 0.0);
        }
        this.executor = Executors.newWorkStealingPool();
    }

    public Map<Long, TreeNode> getCachedBranches() {
        return cachedBranches;
    }

    public void initRoot(Vec3 startPos, Vec3 direction, float maxRadius) {
        lastNodeId = 0;
        this.maxRadius = maxRadius;
        root = new TreeNode(null, lastNodeId++, startPos, direction.normalize(), 2);
        root.nodeStatus = NodeStatus.BUD;
        root.createdAt = age;
        root.vigor = growthData.initialVigor;
        cachedBranches.put(root.id, root);
        if (debug) {
            log("initRoot -> " + nodeShort(root) + " startPos=" + startPos);
        }
        log("Tree exhibits: " + String.join(", ",
                growthData.overrides.stream().map(Objects::toString).toArray(String[]::new)));
    }

    public TreeNode getRoot() {
        return root;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int newAge) {
        age = newAge;
    }

    /**
     * Simulate from current age up to targetAge (inclusive of each tick).
     * Each tick runs: accumulateLight, distributeVigor, addShoots, recalc child counts, shed.
     */
    public void simulateToAge(int targetAge) {
        if (root == null) throw new IllegalStateException("Root not initialized");
        if (targetAge <= age) return;
        this.targetAge = targetAge;

        tracker = new ProgressTracker(targetAge, !inProduction);

        for (int t = age + 1; t <= targetAge; t++) {
            tracker.startPhase("Attractor Genertion");
            if (age % 3 == 0 || cachedAttractors == null)
                produceDynamicAttractors((float) (t / (targetAge + 1.0)), seed);
            tracker.endPhase("Attractor Genertion");
            age = t;
            simulateTick();
        }

        tracker.finish();
    }

    /**
     * Simulate from current age adding up to the specified addition.
     * Each tick runs: accumulateLight, distributeVigor, addShoots, recalc child counts, shed.
     */
    public void additiveSimulateToAge(int addition) {
        int targetAge = age + addition;
        this.targetAge = targetAge;
        if (root == null) throw new IllegalStateException("Root not initialized");
        if (targetAge <= age) return;

        tracker = new ProgressTracker(targetAge, !inProduction);

        for (int t = age + 1; t <= targetAge; t++) {
            tracker.startPhase("Attractor Genertion");
            if (age % 3 == 0 || cachedAttractors == null)
                produceDynamicAttractors((float) (t / (targetAge + 1.0)), seed);
            tracker.endPhase("Attractor Genertion");
            age = t;
            simulateTick();
        }
        tracker.finish(); // Done
    }

    private float accumulateSubtreeVigor(TreeNode node) {
        float sum = node.vigor;
        for (TreeNode child : node.children) {
            sum += accumulateSubtreeVigor(child);
        }
        return sum;
    }

    /**
     * Single simulation tick
     */
    public void simulateTick() {
        tracker.startTick(age);
        if (root == null) return;

        tracker.startPhase("Initialization");

        float totalVigor = accumulateSubtreeVigor(root);
        root.baseRadius = Math.min(
                maxRadius,
                growthData.minRadius + (float) Math.log1p(totalVigor) * 0.5f
        );

        double currentHeight = Math.max(8.0, root.tip().distance(root.firstPoint()) * 0.5);

        influenceRadiusAdaptive = growthData.influenceRadius + (currentHeight * 0.25) * (growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM) ? 3 : 1.5);
        vigorRadiusAdaptive = growthData.vigorRadius + currentHeight * 0.16;
        killRadiusAdaptive = growthData.killRadius + currentHeight * 0.10;

        for (TreeNode n : cachedBranches.values()) {
            n.vigor *= growthData.vigorDecay; // 2% decay per tick
        }

        tracker.endPhase("Initialization");

        if (root.vigor <= 0.4f) {
            root.vigor = growthData.initialVigor * 0.43f;
            // log("⚠️ Root had zero vigor; assigning initialVigor=" + growthData.initialVigor);
        }
        else {
            log("🌱 Root vigor=" + root.vigor);
        }

        tracker.startPhase("Attractors");

        attractors = assignAttractorsToTips();
        int totalGenerated = cachedAttractors != null ? (int) cachedAttractors.stream()
                .filter(it -> !it.reached).count() : 0;
        int totalConsumed = attractors.values().stream().mapToInt(List::size).sum();
        double currentRatio = totalGenerated > 0 ? (double) totalConsumed / totalGenerated : 1.0;
        averageConsumption = averageConsumption * 0.6 + currentRatio * 0.4;

        tracker.endPhase("Attractors");

        tracker.startPhase("Growth (Parallel)");
        List<TreeNode> allNodes = new ArrayList<>(cachedBranches.values());
        double treeHeightNominal = growthData.trunkGrowthMaxAge == -1 ?
                targetAge * growthData.baseLength :
                growthData.trunkGrowthMaxAge * 2.5 * growthData.baseLength;

        tracker.startSubPhase("PG INIT");
        CompletableFuture<?>[] futures = allNodes.stream()
                .map(node -> CompletableFuture.runAsync(
                        () -> processNodeGrowth(node, attractors, treeHeightNominal),
                        executor
                ))
                .toArray(CompletableFuture[]::new);
        tracker.endSubPhase("PG INIT");

        // Wait for ALL to complete (still parallel!)
        tracker.startSubPhase("PG EXEC");
        try {
            CompletableFuture.allOf(futures)
                    .exceptionally(e -> {
                        logger.severe("Error during parallel branch growth of single node", e);
                        return null;
                    }).join();
        }
        catch (Exception e) {
            logger.severe("Error during parallel branch growth", e);
        }
        tracker.endSubPhase("PG EXEC");

        tracker.endPhase("Growth (Parallel)");

        // --- STAGE: SHEDDING ---
        tracker.startPhase("Shedding");
        if (age % 5 == 0) {
            shedBranches(root);
        }
        tracker.endPhase("Shedding");

        log("=== simulateTick END ===\n");
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void shedBranches(TreeNode root) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            var current = queue.poll();
            // log("Shreading -> " + current.id);

            if (current.nodeStatus == NodeStatus.DEAD) {
                log("Shreaded -> " + current.id);
                current.parent.children.remove(current);
                current.children.forEach(it -> it.nodeStatus = NodeStatus.DEAD);
            }

            queue.addAll(current.children);
        }
    }

    private void processNodeGrowth(TreeNode current, Map<TreeNode, List<Vec3>> owned, double treeHeightNominal) {
        if (current == null) return;
        if (current.vigor < 0.01 || current.nodeStatus == NodeStatus.DEAD) {
            return; // Early exit
        }

        if (current.length() >= current.maxLength * 0.99) {
            current.canGrowTaller = false;
            return;
        }

        boolean isMulti = growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM);

        current.vigor = (float) Math.min(1.0f * Math.pow(0.9f, current.order), current.vigor);
        current.baseRadius = Math.min(maxRadius, current.baseRadius);

        log("Polling -> " + current.id);
        logTreeStructure(current, ">> ");
        Random rnd = new Random(seed ^ (current.id * 31L) ^ (this.age * 7919L));
        cachedBranches.putIfAbsent(current.id, current);

        if (current.order == 0) {
            Vec3 last = current.tip();
            current.nodeStatus = NodeStatus.ALIVE;
            Vec3 lightBias = growthData.lightDirection.normalize();

            if (age - current.createdAt > growthData.trunkGrowthMaxAge) {
                current.canGrowTaller = false;
            }

            if (age == growthData.multiTrunkismAge
                    && growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM)
                    && current == root) {
                log("MultiTrunkism Enabled for Root Node");
                current.canGrowTaller = false;
                Vec3 tip = current.getControlPoints().getLast();

                // choose number of trunks (tweak: use config or baseRadius)
                Random mRnd = new Random(seed ^ (current.id * 97L) ^ (this.age * 7919L));
                int defaultN = 3; // fallback
                int n = growthData.multiTrunkismMaxAmount > 1 ?
                        mRnd.nextInt(growthData.multiTrunkismMinAmount, growthData.multiTrunkismMaxAmount + 1)
                        : Math.max(defaultN, Math.min(6, Math.round(current.baseRadius))); // clamp 2..6

                // area-conserve thickness: sum(pi r_i^2) = pi R^2  -> r_i = R / sqrt(n) for equal children
                double parentRadius = current.baseRadius;
                double parentArea = Math.PI * parentRadius * parentRadius;
                double childArea = Math.pow(parentArea, 1.0 - (n / 10.0));
                float childRadius = (float) Math.max(growthData.minRadius, Math.sqrt(childArea / Math.PI));

                Vec3 up = new Vec3(0, 1, 0);
                // create trunks arranged around a horizontal circle with an upward bias
                for (int i = 0; i < n; i++) {
                    double theta = (2.0 * Math.PI * i / n) + (mRnd.nextDouble() - 0.5) * 0.2; // slight jitter
                    Vec3 horiz = new Vec3(Math.cos(theta), 0, Math.sin(theta));
                    // bias upward so they don't go perfectly horizontal
                    Vec3 dir = horiz.lerp(up, 0.35 + (float) (mRnd.nextDouble() * 0.15)).normalize();

                    // small per-trunk noise
                    Vec3 noise = Vec3.randomUnit(mRnd).multiply(0.03);
                    dir = dir.add(noise).normalize().lerp(current.direction, 0.6);

                    // make the new trunk start at the current tip

                    TreeNode trunk = new TreeNode(current, lastNodeId++, tip, dir, childRadius);
                    trunk.vigor = current.vigor * (0.9f * (float) Math.sqrt(growthData.vigorDecay)); // still healthy but a little less
                    trunk.createdAt = age;
                    trunk.isChild = true;
                    trunk.childOrder = ++current.currentParentOrder;

                    current.children.add(trunk);
                    if (growthData.overrides.contains(Overrides.BranchOverrides.CURLY_TIPS)) {
                        traits.set((int) trunk.id, BranchTraitDefs.COIL_RANGE,
                                rnd.nextFloat(growthData.coilRange.key(), growthData.coilRange.value()));
                        traits.set((int) trunk.id, BranchTraitDefs.COIL_RADIUS_RATIO,
                                rnd.nextFloat(growthData.coilRadiusRatio.key(), growthData.coilRadiusRatio.value()));
                    }
                    cachedBranches.putIfAbsent(trunk.id, trunk);
                }

                // Optionally reduce root vigor and radius so mass is conserved and root doesn't keep outcompeting
                current.vigor *= 0.5f;
                current.baseRadius *= 0.6f; // root becomes a stubbier base after splitting

                current.nodeStatus = NodeStatus.ALIVE;

                current.children.forEach(it -> cachedBranches.putIfAbsent(it.id, it));
            }
            var localPool = owned.getOrDefault(current, new ArrayList<>());

            if (current.canGrowTaller) {
                // 1. Calculate the standard Tropism (Light + Gravity)
                Vec3 tropismTarget = lightBias.multiply(growthData.phototropism)
                        .add(global_gravity.multiply(growthData.gravitropism))
                        .normalize();

                // 2. NEW: Calculate the Attraction Vector from Owned Attractors
                Vec3 attractionDir = current.direction; // Default to current if no attractors

                if (!localPool.isEmpty()) {
                    Vec3 sum = new Vec3(0, 0, 0);
                    double totalWeight = 0.0;
                    double totalDist = 0.0; // Track cumulative distance

                    for (Vec3 a : localPool) {
                        Vec3 toA = a.subtract(last);
                        double dist = Math.max(1e-4, toA.length());

                        // Weight for direction (Inverse Square: closer points pull harder)
                        double weight = 1.0 / (dist * dist);
                        sum = sum.add(toA.normalize().multiply(weight));
                        totalWeight += weight;

                        // Cumulative distance for vigor
                        totalDist += dist;
                    }

                    attractionDir = sum.divide(totalWeight).normalize();

                    // --- Vigor Stimulation ---
                    double avgDist = totalDist / localPool.size();

                    // Normalize the distance against your influence radius.
                    // If avgDist is 5.0 and influenceRadius is 5.0, stimulation is 1.0.
                    double stimulation = avgDist / growthData.influenceRadius;
                }

                // 3. Blend Tropism and Attraction
                // For trunks, we usually favor tropism (verticality) over specific attractors
                // to keep the tree straight, but attractors provide the "pull."
                double attractionInfluence = localPool.isEmpty() ? 0.0 :
                        growthData.overrides.contains(Overrides.TrunkOverrides.CONIFERISM)
                        ? 0.003 : 0.35; // 35% pull toward attractors
                Vec3 targetDir = tropismTarget.lerp(attractionDir, attractionInfluence).normalize();

                // --- [NEW] Subtle Spiral Logic for non-conifers ---
                // This rotates the current heading slightly around the Y-axis before calculating the bend.
                // The result is a trunk that tries to spiral but is constantly pulled back up by light.
                if (!growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM)
                        && !growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM)) {

                    // Strength dependent on straightness (lower straightness = more twist)
                    double twistStrength = 0.22 * (1.0 - growthData.straightness);

                    // Deterministic direction (CW vs CCW) based on seed/ID
                    if ((seed & 1) == 0) twistStrength *= -1;

                    double cos = Math.cos(twistStrength);
                    double sin = Math.sin(twistStrength);

                    double x = current.direction.x;
                    double z = current.direction.z;

                    // Standard 2D rotation matrix around Y axis
                    double nx = x * cos - z * sin;
                    double nz = x * sin + z * cos;

                    // Update current direction with the twist
                    current.direction = new Vec3(nx, current.direction.y, nz).normalize();
                }

                // 4. Calculate Bending
                double angle = current.direction.angleTo(targetDir);
                double bendStrength = Math.pow(angle / Math.PI, 1.5) * 0.25;
                bendStrength *= growthData.flexibility;

                // 5. Apply the rotation and Multi-Trunk Override
                Vec3 newDir = current.direction.lerp(targetDir, bendStrength).normalize();

                if (growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM)) {
                    // Multi-trunkism forces upward growth but allows slight outward spreading
                    newDir = new Vec3(newDir.x * 0.2, 1.0, newDir.z * 0.2).normalize();
                }

                // 6. Natural Jitter
                Vec3 noise = Vec3.randomUnit(rnd).multiply(0.05 * (1.0 - growthData.straightness));
                newDir = newDir.add(noise).normalize();

                // 7. Extend the trunk
                Vec3 nextPoint = last.add(newDir.multiply(growthData.baseLength * Math.max(0.02, current.vigor)));

                current.controlPoints.add(nextPoint);
                current.direction = newDir;
            }

            long nearbyAttractorPool = localPool.size();
            boolean rndChance = rnd.nextDouble() < growthData.budProbability;
            log(Arrays.toString(current.children.toArray()));
            boolean childrenNotClose = current.children.isEmpty() || current.children.getLast().firstPoint()
                    .distance(current.getControlPoints().getLast()) > growthData.distanceBetweenChildren;
            logf("attractorPool: %s %s, ChildSize: %s, RndChance: %s, Distance: %s%n", nearbyAttractorPool, nearbyAttractorPool > 3, isBelowMaxKids(current), rndChance, childrenNotClose);
            if ((nearbyAttractorPool > 3 || growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM))
                    && rndChance && childrenNotClose && age > (
                    growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM) ?
                            growthData.multiTrunkismAge + growthData.minSplittingAge : growthData.minSplittingAge)
                    && isBelowMaxKids(current) && current.canGrowTaller) {
                Vec3 budOrigin = current.getControlPoints().getLast();
                Vec3 attractionDir = new Vec3(0, 0, 0);
                int count = 0;
                for (Vec3 attractor : localPool) {
                    double dist = attractor.distance(budOrigin);
                    if (growthData.influenceRadius == -1 || dist < influenceRadiusAdaptive * 1.5) {
                        // weight closer attractorPool more strongly
                        double weight = 1.0 / (dist + 0.001);
                        attractionDir = attractionDir.add(attractor.subtract(budOrigin).normalize().multiply(weight));
                        count++;
                    }
                }

                if (count > 0) {
                    attractionDir = attractionDir.normalize();
                } else {
                    // fallback to random if no attractorPool nearby
                    attractionDir = Vec3.randomUnit(rnd);
                }

                // mix in a little horizontal + upward bias to look natural
                attractionDir = attractionDir.lerp(new Vec3(0, 1, 0), 0.2).normalize();

                // create the branch
                log("Adding branch: dir -> " + attractionDir);

                if (growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM)) {
                    int minBranches = (growthData.coniferousBranchMin > 0) ? growthData.coniferousBranchMin : 4;
                    int maxBranches = (growthData.coniferousBranchMax > 0) ? growthData.coniferousBranchMax : 10;
                    double budRadius = (growthData.coniferousBudRadius > 0) ? growthData.coniferousBudRadius : 0.6;
                    float vigorFactor = current.vigor * (growthData.vigorDecay * 0.8f);

                    int branchCount = minBranches + rnd.nextInt(Math.max(1, maxBranches - minBranches + 1));
                    double baseAngleOffset = rnd.nextDouble() * 2.0 * Math.PI;

                    // tuning knobs: make these small for outward-first branches
                    double upTilt = 0.0;            // small upward tilt (0.02..0.08)

                    Vec3 axis = current.direction.normalize();

                    Vec3 tangent = axis.crossProduct(new Vec3(0, 1, 0));
                    if (tangent.length() < 1e-4)
                        tangent = axis.crossProduct(new Vec3(1, 0, 0));
                    tangent = tangent.normalize();

                    Vec3 bitangent = axis.crossProduct(tangent).normalize();
                    current.currentParentOrder++;
                    var conedConifer =
                            rnd.nextDouble(growthData.conedConiferismAngle.key(), growthData.conedConiferismAngle.value());

                    for (int i = 0; i < branchCount; i++) {
                        double angle = baseAngleOffset + (2.0 * Math.PI * i) / branchCount;

                        Vec3 radialDir =
                                tangent.multiply(Math.cos(angle))
                                        .add(bitangent.multiply(Math.sin(angle)));

                        Vec3 finalDir =
                                radialDir.multiply(1.0 - upTilt)
                                        .add(axis.multiply(upTilt))
                                        .normalize();

                        if (growthData.overrides.contains(Overrides.BranchOverrides.CONED_CONIFERISM)) {
                            // read per-branch angle (in degrees), fallback to some default if not set
                            double coneDeg = conedConifer;
                            if (coneDeg <= 0f) coneDeg = 35f; // sensible default if trait missing

                            double coneRad = Math.toRadians(coneDeg);

                            // ensure axis is normalized
                            Vec3 axisN = axis.normalize();

                            // horizontal outward direction around trunk (pure XZ radial)
                            Vec3 radialHoriz = new Vec3(radialDir.x, 0.0, radialDir.z);
                            if (radialHoriz.lengthSq() < 1e-6) {
                                // fallback if radialDir is almost vertical for some reason
                                radialHoriz = new Vec3(1.0, 0.0, 0.0);
                            }
                            radialHoriz = radialHoriz.normalize();

                            // build direction lying on the cone:
                            // angle between finalDir and axis == coneDeg
                            finalDir = radialHoriz.multiply(Math.sin(coneRad))
                                    .add(axisN.multiply(Math.cos(coneRad)))
                                    .normalize();
                        }

                        // Now use the (potentially nudged) angle for startOffset
                        Vec3 startOffset = new Vec3(
                                Math.cos(angle) * budRadius * 0.9,
                                (rnd.nextDouble() - 0.5) * 0.02,
                                Math.sin(angle) * budRadius * 0.9
                        );

                        Vec3 branchOrigin = budOrigin.add(startOffset);

                        var dividor = CurveFunctions.radius(current.baseRadius, 0.0, 0.0, 1.0, growthData.forTrunk);
                        TreeNode branch = new TreeNode(current, lastNodeId++, branchOrigin, finalDir, dividor.apply(0.90).floatValue());
                        branch.vigor = vigorFactor;
                        branch.createdAt = age;
                        branch.childOrder = current.currentParentOrder;

                        branch.maxLength = getMaxLength(branch, treeHeightNominal);
                        current.children.add(branch);
                        if (growthData.overrides.contains(Overrides.BranchOverrides.CURLY_TIPS)) {
                            traits.set((int) branch.id, BranchTraitDefs.COIL_RANGE,
                                    rnd.nextFloat(growthData.coilRange.key(), growthData.coilRange.value()));
                            traits.set((int) branch.id, BranchTraitDefs.COIL_RADIUS_RATIO,
                                    rnd.nextFloat(growthData.coilRadiusRatio.key(), growthData.coilRadiusRatio.value()));
                            if (growthData.overrides.contains(Overrides.BranchOverrides.CONED_CONIFERISM)) {
                                traits.set((int) branch.id, BranchTraitDefs.CONED_CONIFERISM_ANGLE, conedConifer);
                            }
                        }
                    }
                    current.vigor *= (growthData.vigorDecay * 0.8f);
                }
                else {
                    {
                        long childId = lastNodeId + 1L;        // id that will be assigned next
                        int slot = (int) (childId % 5L);      // 0..4
                        double fraction = ((double) slot / 4.0)
                                + rnd.nextDouble(-0.05, 0.05); // 0, 0.25, 0.5, 0.75, 1.0
                        double angle = fraction * Math.PI * 2.0; // 0..2π (0..360°)

                        // rotate only XZ components, keep Y as is
                        double x = attractionDir.x;
                        double z = attractionDir.z;
                        double lenXZ = Math.sqrt(x * x + z * z);
                        if (lenXZ > 1e-6) {
                            double nx = x / lenXZ;
                            double nz = z / lenXZ;
                            double c = Math.cos(angle);
                            double s = Math.sin(angle);
                            double rx = nx * c - nz * s;
                            double rz = nx * s + nz * c;
                            attractionDir = new Vec3(rx, attractionDir.y, rz).normalize();
                        }
                    }
                    var dividor = CurveFunctions.radius(current.baseRadius, 0.0, 0.0, 1.0, growthData.forTrunk);
                    TreeNode branch = new TreeNode(current, lastNodeId++, budOrigin, attractionDir, dividor.apply(0.90).floatValue());
                    branch.vigor = current.vigor * (growthData.vigorDecay * 0.8f);
                    branch.createdAt = age;
                    branch.childOrder = ++current.currentParentOrder;

                    branch.maxLength = getMaxLength(branch, treeHeightNominal);;
                    current.children.add(branch);
                    if (growthData.overrides.contains(Overrides.BranchOverrides.CURLY_TIPS)) {
                        traits.set((int) branch.id, BranchTraitDefs.COIL_RANGE,
                                rnd.nextFloat(growthData.coilRange.key(), growthData.coilRange.value()));
                        traits.set((int) branch.id, BranchTraitDefs.COIL_RADIUS_RATIO,
                                rnd.nextFloat(growthData.coilRadiusRatio.key(), growthData.coilRadiusRatio.value()));
                    }
                    if (growthData.overrides.contains(Overrides.BranchOverrides.MIRROR_BRANCHES)) {
                        TreeNode mirrored_branch = new TreeNode(current, lastNodeId++, budOrigin, attractionDir.multiply(-1), dividor.apply(0.90).floatValue());
                        mirrored_branch.vigor = current.vigor * (growthData.vigorDecay * 0.8f);
                        mirrored_branch.createdAt = age;
                        mirrored_branch.childOrder = current.currentParentOrder;
                        mirrored_branch.maxLength = branch.maxLength;
                        current.children.add(mirrored_branch);
                        if (growthData.overrides.contains(Overrides.BranchOverrides.CURLY_TIPS)) {
                            traits.set((int) branch.id, BranchTraitDefs.COIL_RANGE,
                                    rnd.nextFloat(growthData.coilRange.key(), growthData.coilRange.value()));
                            traits.set((int) branch.id, BranchTraitDefs.COIL_RADIUS_RATIO,
                                    rnd.nextFloat(growthData.coilRadiusRatio.key(), growthData.coilRadiusRatio.value()));
                        }
                    }
                }
                current.vigor *= (growthData.vigorDecay * 0.87f);
            }

            double trunkHeight = root.tip().distance(root.firstPoint());
            double ageExp = Math.expm1(growthData.rootAgeK * (double) age);
            double heightFactor = Math.log1p(Math.max(1.0, trunkHeight)) * growthData.rootHeightScale;

            float rootTarget = (float) (
                    growthData.minRadius
                            + growthData.rootBaseMultiplier * ageExp * (1.0 + heightFactor)
            );

            float rootSmoothing = 0.25f; // 0.10–0.35 are reasonable; increase for faster trunk growth

            current.baseRadius += (rootTarget - current.baseRadius) * rootSmoothing;

            // allow temporary visual boost controlled by rootMaxMultiplier
            double maxAllowedRoot = Math.min(maxRadius * growthData.rootMaxMultiplier, (double) maxRadius * 4.0);
            current.baseRadius = Math.min(current.baseRadius, (float) maxAllowedRoot);
            current.baseRadius = Math.max(growthData.minRadius, current.baseRadius);

            current.children.forEach(it -> {
                cachedBranches.putIfAbsent(it.id, it);
            });
        }
        else {
            double parentLength = current.parent.firstPoint().distance(current.parent.tip());

            if (parentLength > 1e-6) {
                double distFromBase = current.parent.firstPoint().distance(current.firstPoint());
                double progress = distFromBase / parentLength;

                if (progress < growthData.pruningHeight && current.parent.children.size() > 3) {
                    current.nodeStatus = NodeStatus.DEAD;
                }
            }

            if (current.nodeStatus == NodeStatus.DEAD) return;

            logf("Branch id: %s, Dir: %s, Vigor: %s, First point: %s, Length: %s",
                    current.id, current.direction, current.vigor, current.startPos, current.length());

            Vec3 tip = current.getControlPoints().getLast();
            current.nodeStatus = NodeStatus.ALIVE;

            // --- Get attractorPool affecting this node ---
            List<Vec3> localattractorPool = owned.getOrDefault(current, Collections.emptyList());

            if (localattractorPool.isEmpty()) {
                logf("Local Attractor for bud %s is empty", current.id);
            }

            // use localAttractors for vigor updates, direction, and bud logic
            // also compute close attractors from localAttractors:
            List<Vec3> closeattractorPool = localattractorPool.stream()
                    .filter(a -> a.distance(current.tip()) < vigorRadiusAdaptive)
                    .toList();

            // ---------------------- REPLACEMENT: attraction -> finalDir ----------------------

            double sen;
            if (current.order > 1) sen = senescenceFactor(current.parent);
            else sen = senescenceFactor(current);
            sen = Math.min(1.0, sen);

            if (current.canGrowTaller) {

                Vec3 attractionTarget;

                if (!localattractorPool.isEmpty()) {
                    Vec3 sum = new Vec3(0, 0, 0);
                    double totalWeight = 0.0;
                    for (Vec3 a : localattractorPool) {
                        Vec3 toA = a.subtract(tip);
                        double dist = Math.max(1e-4, toA.length());
                        double weight = 1.0 / (dist * dist);
                        sum = sum.add(toA.multiply(weight));
                        totalWeight += weight;
                    }
                    Vec3 weightedAvg = totalWeight > 0 ? sum.divide(totalWeight).normalize() : current.direction.normalize();

                    double attractorStrength = Math.max(0.0, 1.0 - Math.pow(sen, 2));

                    double baseAttractorBlend = 0.15 + 0.25 * attractorStrength; // 0.15..0.4
                    if (growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM)) {
                        baseAttractorBlend *= 0.65; // conifers react slower (prefer outward/horizontal)
                    }
                    double attractorBlend = clamp(baseAttractorBlend, 0.05, 0.6);

                    Vec3 weightedAvgHoriz = new Vec3(weightedAvg.x, 0.0, weightedAvg.z);
                    if (weightedAvgHoriz.length() < 1e-6) {
                        weightedAvgHoriz = new Vec3(current.direction.x, 0.0, current.direction.z);
                    }
                    weightedAvgHoriz = weightedAvgHoriz.normalize();

                    Vec3 currentDirHoriz = new Vec3(current.direction.x, 0.0, current.direction.z);
                    if (currentDirHoriz.length() < 1e-6) currentDirHoriz = new Vec3(1.0, 0.0, 0.0);
                    currentDirHoriz = currentDirHoriz.normalize();

                    // Blend horizontally (azimuthal steering)
                    Vec3 horizBlend = currentDirHoriz.lerp(weightedAvgHoriz, attractorBlend).normalize();

                    // small explicit up-tilt: choose per-species & per-order
                    // trunk and very young tips can have larger up-tilt; lateral conifer tips should be tiny
                    double upTiltBase = 0.06; // generic small tilt
                    if (growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM)) {
                        upTiltBase = 0.02; // keep conifer laterals very flat
                    } else if (current.order <= 1) {
                        upTiltBase = 0.2; // trunk tips / leader get stronger upward bias
                    }

                    // reduce up-tilt if senescent (droopy)
                    double upTilt = upTiltBase * Math.sqrt(1.0 - sen);
                    upTilt = clamp(upTilt, 0.0, 0.5);

                    // deterministic small node bias, but clamp vertical component to avoid upward explosion
                    Random perNode = new Random(current.id * 31L + 17L);
                    Vec3 nodeBiasRaw = Vec3.randomUnit(perNode).multiply(0.03 * (1.0 - growthData.straightness));
                    // clamp Y bias tiny:
                    double maxBiasY = 0.01;
                    nodeBiasRaw = new Vec3(
                            nodeBiasRaw.x,
                            Math.max(-maxBiasY, Math.min(maxBiasY, nodeBiasRaw.y)),
                            nodeBiasRaw.z
                    );

                    double verticalFromAttractors = weightedAvg.y * (1.0 - sen);
                    double verticalFromUpTilt     = upTilt * (1.0 - sen);
                    double verticalPull = 0.5 * verticalFromAttractors + 0.5 * verticalFromUpTilt;

                    attractionTarget = new Vec3(
                            horizBlend.x,
                            verticalPull + nodeBiasRaw.y,
                            horizBlend.z
                    ).normalize();

                    // Vigor update as you had it (unchanged)
                    double baseGain = 0.01 * localattractorPool.size();
                    double effectiveGain = baseGain * (1.0 - sen * growthData.senescenceVigorPenalty);
                    effectiveGain = Math.max(0.0, effectiveGain);
                    double closeFactor = 1.0 + 0.5 * closeattractorPool.size();
                    closeFactor = Math.min(closeFactor, 3.0);
                    current.vigor += (float) (effectiveGain * closeFactor);
                    current.vigor = Math.min(current.vigor, 1.0f);
                }
                else {
                    // no attractors: inertia + droop as before
                    attractionTarget = current.direction.normalize();

                    if (sen < 0.32) {
                        double decayMultiplier = 1.0 + sen * growthData.senescenceDecayRate;
                        current.vigor *= (float) (growthData.vigorDecay * decayMultiplier / 2.0f);
                    } else {
                        float decayRate = isMulti ? 0.99f * growthData.vigorDecay :
                                growthData.vigorDecay;
                        float deltaTime = 0.076f;
                        current.vigor *= (float) Math.exp(-decayRate * deltaTime);
                    }
                }

                double distFromTop = Math.max(0.0, root.tip().y - current.firstPoint().y);

                // --- new conifer apical logic ---
                double apicalNormalized;
                double apicalUpBias = 0.0;
                double apicalGrowthMultiplier = 1.0;

                if (growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM)
                        && growthData.overrides.contains(Overrides.BranchOverrides.TAPERISM)) {
                    // avoid divide-by-zero; ensure positive range
                    double apicalRange = Math.max(1e-6, growthData.coniferApicalRange);
                    apicalNormalized = clamp(distFromTop / apicalRange, 0.0, 1.0);

                    // smoothstep-like curve (3t^2 - 2t^3) then optionally sharpen with coniferApicalCurve
                    double w = apicalNormalized;
                    double smooth = w * w * (3.0 - 2.0 * w);           // smoothstep
                    // further shape control:
                    if (growthData.coniferApicalCurve != 1.0) {
                        smooth = Math.pow(smooth, growthData.coniferApicalCurve);
                    }

                    apicalUpBias = clamp(smooth * growthData.coniferMaxUpBias, 0.0, 1.0);

                    // growth fade-out: start reducing baseLength at stopStart, reach zero at stopEnd
                    double s0 = clamp(growthData.coniferApicalStopStart, 0.0, 1.0);
                    double s1 = clamp(growthData.coniferApicalStopEnd, s0 + 1e-6, 1.0);
                    if (apicalNormalized >= s0) {
                        // linear fade to zero; you can replace with smoother curve if desired
                        apicalGrowthMultiplier = Math.max(0.0, 1.0 - (apicalNormalized - s0) / (s1 - s0));
                    }
                }

                if (apicalUpBias > 1e-9) {
                    // bias toward world-up (0,1,0). Lerp keeps vector magnitude reasonable before normalize.
                    attractionTarget = attractionTarget.lerp(new Vec3(0.0, 1.0, 0.0), apicalUpBias).normalize();
                }

                double inertia = Math.pow(1.0 - Math.min(1.0, sen), 2.0);
                double smoothLerp = clamp(0.10 + 0.35 * (1.0 - inertia), 0.04, 0.6); // slightly lower baseline
                Vec3 desiredDir = current.direction.normalize().lerp(attractionTarget, smoothLerp).normalize();

                double gravLerp = clamp(sen * growthData.senescenceGravBias, 0.0, 1.0);
                Vec3 gravityAdjusted = desiredDir.lerp(global_gravity, gravLerp).normalize();

                double maxTurnDeg = growthData.overrides.contains(Overrides.BranchOverrides.DECIDUOUS) ? 35.0 : 10.0;
                double maxTurn = Math.toRadians(maxTurnDeg);
                double angle = Math.abs(current.direction.angleTo(gravityAdjusted));
                Vec3 limitedDir;
                if (angle > 1e-6 && angle > maxTurn) {
                    double w = maxTurn / angle;
                    w = clamp(w, 0.01, 1.0);
                    limitedDir = current.direction.lerp(gravityAdjusted, w).normalize();
                }
                else {
                    limitedDir = gravityAdjusted;
                }

                Vec3 randA = Vec3.randomUnit(rnd);
                double scalar = Vec3.randomUnit(rnd).dot(limitedDir);
                Vec3 orthNoise = randA.subtract(limitedDir.multiply(scalar))
                        .normalize()
                        .multiply(0.08 * (1.0 - growthData.straightness)); // smaller than before

                Vec3 finalDir = limitedDir.add(orthNoise)
                        .normalize();

                double ageFactor = Math.max(0.1, Math.exp(-(age - current.createdAt) * growthData.ageFactorExp));
                logf("TipNumber: %s, age: %s, age-factor: %s, Dir: %s, FormerDir: %s", current.id, (age - current.createdAt), ageFactor, finalDir, current.direction);

                double persistence = growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM) ?
                        1.0 : rnd.nextDouble(0.62, 0.87);

                // 2. Blend the PREVIOUS direction with the NEW attraction
                Vec3 blendedDir = current.direction.multiply(persistence)
                        .add(finalDir.multiply(1.0 - persistence))
                        .normalize();

                // 3. Apply the Bending logic to this blended direction
                double angle2 = current.direction.angleTo(blendedDir);
                double bendStrength = growthData.flexibility;

                // -- later, when you compute the step length to nextPoint, multiply baseLength by growth multiplier --
                double effectiveBaseLength = growthData.baseLength * apicalGrowthMultiplier;

                Vec3 newDir = current.direction.lerp(blendedDir, (float) bendStrength)
                        .lerp(Vec3.randomUnit(rnd).normalize()
                                        .multiply(current.isChild ? growthData.flexibility / 2 : growthData.flexibility),
                                1.0 - Math.min(1.0, persistence * 1.3)
                        )
                        .normalize();

                // only apply to non-root branches
                if (growthData.overrides.contains(Overrides.BranchOverrides.CONED_CONIFERISM) && current.order > 0) {

                    // read desired cone angle for this branch (degrees) from traits, or fallback
                    double coneDeg = traits.getDouble((int) current.id, BranchTraitDefs.CONED_CONIFERISM_ANGLE);
                    if (coneDeg <= 0.0) coneDeg = 35.0; // default if trait not set

                    double coneRad = Math.toRadians(coneDeg);

                    // trunk/parent axis the cone is built around
                    Vec3 axis = current.parent.direction != null
                            ? current.parent.direction.normalize()
                            : current.direction.normalize();

                    // project current newDir into plane perpendicular to axis -> radial component
                    double dot = newDir.dot(axis);
                    Vec3 radial = newDir.subtract(axis.multiply(dot)); // remove axial component

                    if (radial.lengthSq() < 1e-8) {
                        // if newDir was nearly aligned with axis, pick any stable perpendicular
                        // (so we still get a meaningful cone direction)
                        radial = axis.getOrtho(); // or new Vec3(1,0,0) cross axis, etc.
                    }
                    radial = radial.normalize();

                    // build direction that lies exactly on coneDeg from axis
                    // angle between newDirConed and axis is coneDeg
                    newDir = radial.multiply(Math.sin(coneRad))
                            .add(axis.multiply(Math.cos(coneRad)))
                            .normalize();
                    
                    persistence = Math.max(persistence, 0.9);
                }

                // --- [NEW] Young Branch Cone Constraint (Growth) ---
                // For the first 5-11 years, constrain direction to max 50 degrees from parent
                if (current.order > 0 &&
                        !growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM) &&
                        !growthData.overrides.contains(Overrides.BranchOverrides.PALMISM)) {
                    int branchAge = age - current.createdAt;
                    int limitAge = (int) Math.pow(5, 1 + ((double) current.order / 10)) + (int) (current.id % 7); // Deterministic range [5, 11] based on ID

                    if (branchAge <= limitAge) {
                        Vec3 parentDir = current.parent.direction.normalize();
                        double angleFromParent = newDir.angleTo(parentDir);
                        double maxAngle = traits.getDouble((int) current.id, BranchTraitDefs.CONED_CONIFERISM_ANGLE);
                        if (maxAngle <= 0.0) maxAngle = 35.0;

                        if (angleFromParent > maxAngle) {
                            // Clamp direction back to the edge of the cone
                            double t = maxAngle / Math.max(1e-4, angleFromParent);
                            newDir = parentDir.lerp(newDir, t).normalize();
                        }
                    }
                }

                double currentLen = current.length();

                double stepLen = effectiveBaseLength * current.vigor * ageFactor
                        * (growthData.overrides.contains(Overrides.BranchOverrides.REGRESSION)
                        ? growthData.yReduction : 1.0);

                if (!growthData.lengthCapEnabled || !Double.isFinite(current.maxLength)) {
                    Vec3 nextPoint = tip.add(newDir.multiply(stepLen));
                    current.controlPoints.add(nextPoint);
                    current.direction = newDir;
                }
                else {
                    double remaining = current.maxLength - currentLen;
                    if (remaining <= 0.0) {
                        current.canGrowTaller = false;  // this branch reached its designed length
                    } else {
                        double used = Math.min(remaining, stepLen);
                        Vec3 nextPoint = tip.add(newDir.multiply(used));
                        current.controlPoints.add(nextPoint);
                        current.direction = newDir;
                        if (used >= remaining - 1e-6) {
                            current.canGrowTaller = false; // hit the cap this tick
                        }
                    }
                }

                if (current.canGrowTaller) {
                    int count = closeattractorPool.size();
                    float bonus = 0f;
                    if (count > 4) {
                        bonus = (float) Math.sqrt(count - 4) * 0.1f;
                    }

                    float growthBoost = bonus * (1f - current.vigor);
                    current.vigor += growthBoost;
                    // current.vigor -= decay;
                    current.vigor = Math.max(0f, Math.min(1f, current.vigor));

                    current.baseLengthInPoints++;

                    if (growthData.overrides.contains(Overrides.BranchOverrides.CURLY_TIPS)
                            && current.order <= growthData.coilDepth) {
                        applyCurlyness(current);
                    }
                }
            }

            // --- Branch death logic ---
            if (current.vigor < 0.05f && current.children.size() < 2) {
                current.nodeStatus = NodeStatus.DEAD;
                return;
            }

            // --- Bud creation (branching) ---
            boolean childrenNotClose = current.children.isEmpty() ||
                    current.children.getLast().firstPoint()
                            .distance(current.getControlPoints().getLast()) > (growthData.distanceBetweenChildren * (current.order + 1));

            long nearbyattractorPool = localattractorPool.size();
            double effectiveBudProb = growthData.budProbability;
            effectiveBudProb = Math.max(0.01, effectiveBudProb); // never zero if you want some chance
            boolean rndChance = rnd.nextDouble() < (effectiveBudProb * current.vigor);
            boolean age = this.age - current.createdAt > growthData.minSplittingAge;
            log(String.format(
                    "Child info >> attractorPool: %s %s, Age: %s ChildSize: %s, RndChance: %s, Distance: %s, Order: %s, Vigor: %s, %s",
                    nearbyattractorPool, nearbyattractorPool > 8, age, isBelowMaxKids(current), rndChance, childrenNotClose,
                    current.order < growthData.maxDepth, current.vigor > 0.75, current.vigor));
            if (nearbyattractorPool > 8 && childrenNotClose
                    && rndChance && age
                    && isBelowMaxKids(current)
                    && current.order < growthData.maxDepth
                    && current.vigor > 0.75
                    && sen < 0.2) {

                Vec3 attractionDir = new Vec3(0, 0, 0);
                int count = 0;
                for (Vec3 a : localattractorPool) {
                    double dist = a.distance(current.tip());
                    if (growthData.influenceRadius == -1 || dist < influenceRadiusAdaptive * 1.5) {
                        double weight = 1.0 / (dist + 0.001);
                        attractionDir = attractionDir.add(a.subtract(current.tip()).normalize().multiply(weight));
                        count++;
                    }
                }

                if (count > 0)
                    attractionDir = attractionDir.divide(count).normalize();
                else
                    attractionDir = current.direction.randomPerturbated(rnd, growthData.lateralAngleDegrees);

                if (!growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM) &&
                        !growthData.overrides.contains(Overrides.BranchOverrides.PALMISM)) {
                    Vec3 parentDir = current.direction.normalize();
                    double angleFromParent = attractionDir.angleTo(parentDir);
                    double maxAngle = Math.toRadians(50.0);

                    if (angleFromParent > maxAngle) {
                        // Clamp spawn direction to within 50 degrees of parent
                        double t = maxAngle / Math.max(1e-4, angleFromParent);
                        attractionDir = parentDir.lerp(attractionDir, t).normalize();
                    }
                }

                TreeNode bud = new TreeNode(current, lastNodeId++, current.tip(), attractionDir, current.baseRadius * 0.75f);
                bud.vigor = Math.min(1.0f, (float) (current.vigor * Math.sqrt(growthData.vigorDecay)));
                bud.createdAt = this.age;
                bud.childOrder = ++current.currentParentOrder;
                bud.maxLength = getMaxLength(bud, treeHeightNominal);;

                log("Adding new branch toward attractorPool: dir -> " + attractionDir);

                current.children.add(bud);
                if (growthData.overrides.contains(Overrides.BranchOverrides.CURLY_TIPS)) {
                    traits.set((int) bud.id, BranchTraitDefs.COIL_RANGE,
                            rnd.nextFloat(growthData.coilRange.key(), growthData.coilRange.value()));
                    traits.set((int) bud.id, BranchTraitDefs.COIL_RADIUS_RATIO,
                            rnd.nextFloat(growthData.coilRadiusRatio.key(), growthData.coilRadiusRatio.value()));
                }
                current.children.forEach(it -> {
                    cachedBranches.putIfAbsent(it.id, it);
                });
            }
            else {
                current.children.forEach(it -> {
                    cachedBranches.putIfAbsent(it.id, it);
                });
            }
        }

        float totalChildVigor = 0f;
        boolean distributeRadius = growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM) && current.order == 0;
        if (distributeRadius) {
            for (TreeNode child : current.children) totalChildVigor += child.vigor;
        }

        // child cannot exceed 0.7 parents length
        for (TreeNode child : current.children) {
            if (current.vigor < 0.87 || child.vigor > 1.0f * Math.pow(0.9f, child.order)) {
                current.vigor += (child.vigor * 0.08f);
                child.vigor -= (child.vigor * (0.08f * growthData.vigorDecay));
            }

            if (child.length() / current.length() < 0.35) {
                child.vigor += (float) Math.sqrt(current.vigor) * 0.54f;
            }

            if (child.length() / current.length() > 0.65) {
                child.vigor *= (float) Math.max(0.6, Math.pow(growthData.vigorDecay, (child.length() / current.length()) * 10.0));
            }

            double parentLength = current.firstPoint().distance(current.tip());
            double relativeHeight = parentLength > 0
                    ? child.firstPoint().distance(current.firstPoint()) / parentLength
                    : 0.0;

            relativeHeight = Math.max(0.0, Math.min(relativeHeight, 1.0));

            double heightFactor = Math.pow(1.0 - relativeHeight, 0.9); // smoother

            float targetChildRadius;

            if (distributeRadius) {
                // Da Vinci's Rule: r_child = R_parent * sqrt(vigor_fraction)
                float vigorFraction = (totalChildVigor > 0.0001f) ? (child.vigor / totalChildVigor) : (1.0f / Math.max(1, current.children.size()));
                targetChildRadius = current.baseRadius * (float) Math.sqrt(vigorFraction);
            } else {
                if (growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM) &&
                        (current.order == 0 || current == root)) heightFactor = 1.0;
                double vigorBoost = 0.8 + Math.pow(child.vigor, 0.85); // more generous
                targetChildRadius = (float) (current.baseRadius * heightFactor * vigorBoost * 0.3);
            }

            // Smooth growth instead of clamp
            float lerpRate = (isMulti ? 0.71f : 0.57f) + 0.1f * (float) Math.sqrt(child.vigor);
            child.baseRadius += (targetChildRadius - child.baseRadius) * lerpRate;

            // Keep within reasonable bounds
            float maxPossible = current.baseRadius * 0.9f;
            child.baseRadius = Math.max(growthData.minRadius,
                    Math.min(child.baseRadius, maxPossible));

            log(String.format("id=%s, hf=%.3f, vb=%.3f, target=%.3f, cbr=%.3f",
                    child.id, heightFactor, 0.0, targetChildRadius, child.baseRadius));
        }
    }

    private boolean isBelowMaxKids(TreeNode current) {
        if (current.order > 0 && growthData.maxKids == -1) {
            return current.children.size() < 10 * (1.0 + 0.5 * Math.log1p(1.0 / current.order));
        }
        return growthData.maxKids == -1 || current.children.size() < growthData.maxKids * (1.0 + 0.5 * Math.log1p(1.0 / current.order));
    }

    private void pruneConsumedAttractorPool(TreeNode root) {
        collectConsumedAttractorPool(root, cachedAttractors);
    }
    private void collectConsumedAttractorPool(TreeNode node, List<Attractor> attractorPool) {
        if (node.nodeStatus == NodeStatus.DEAD) return;

        Vec3 tip = node.getControlPoints().getLast();

        for (Attractor a : attractorPool) {
            if (a.pos.distance(tip) < killRadiusAdaptive) {
                a.reached = true;
            }
        }

        // recurse into children
        for (TreeNode child : node.children) {
            collectConsumedAttractorPool(child, attractorPool);
        }
    }

    public static class Attractor {
        public final Vec3 pos;
        public final int layer;      // activation layer (0 trunk, 1 scaffolds, 2 outer twigs, ...)
        public double strength = 1;  // optional: used for weighted removal or decay
        public boolean reached = false;

        public Attractor(Vec3 p, int layer) {
            this.pos = p;
            this.layer = layer;
        }
    }

    private void produceDynamicAttractors(float currentProgress, long seed) {
        Random rand = new Random(seed); // <-- IMPORTANT: do NOT use age here
        List<Attractor> attractors = new ArrayList<>();

        // convenience values
        Vec3 trunkTop = root.startPos.add(0.0, targetAge * growthData.baseLength * 0.8, 0.0);
        boolean isConifer = growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM);
        boolean isPalm = growthData.overrides.contains(Overrides.BranchOverrides.PALMISM); // add a flag for palms
        boolean multiTrunk = growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM);
        boolean isDeciduous = growthData.overrides.contains(Overrides.BranchOverrides.DECIDUOUS);
        double currentHeight = (targetAge * growthData.baseLength) * currentProgress;

        // species-specific base sizes
        double canopyRadius = Math.max(6.0, currentHeight * (isConifer ? 1.2 : 0.9)) * growthData.spread;
        if (multiTrunk) canopyRadius *= Math.max(1.4, Math.pow(growthData.multiTrunkismMinAmount, 0.8));

        logf("Canopy Height: %s, Canopy Radius: %s, True Height: %s", currentHeight, canopyRadius, currentHeight);

        // PALM: special case = no attractors for trunk; produce a ring at apex for fronds
        if (isPalm) {
            int frondRingCount = Math.max(6, (int)(6 + 6 * growthData.spread));
            double ringRadius = Math.max(1.5, canopyRadius * 0.7);
            Vec3 apex = trunkTop.add(0.0, currentHeight * 0.05, 0.0); // slightly above top
            for (int i = 0; i < frondRingCount; i++) {
                double angle = 2*Math.PI * (i + rand.nextDouble()) / frondRingCount;
                Vec3 p = new Vec3(
                        apex.getX() + Math.cos(angle) * ringRadius * (0.8 + 0.4 * rand.nextDouble()),
                        apex.getY() + (rand.nextDouble() - 0.5) * 0.2 * currentHeight,
                        apex.getZ() + Math.sin(angle) * ringRadius * (0.8 + 0.4 * rand.nextDouble())
                );
                // layer high so fronds spawn only after trunk matured
                attractors.add(new Attractor(p, /*layer=*/2));
            }
            cachedAttractors = attractors;
            return;
        }

        // NON-PALM: create vertical slices and assign layer by slice index
        int slices = Math.max(4, (int)Math.round(currentHeight * 1.5)); // vertical sampling resolution
        double densityFactor = Math.max(0.4, Math.min(1.6, 0.5 + averageConsumption));
        int pointsPerSlice = Math.max(12, Math.min(80, (int)((canopyRadius / 1.5) * densityFactor)));

        double rootY = root.startPos.getY();
        if (isDeciduous) {
            currentHeight = currentHeight * 0.7;
            rootY = rootY + (currentHeight * 0.3);
        }
        else if (!isConifer) {
            rootY = rootY + (currentHeight * 0.37);
        }

        for (int s = 0; s < slices; s++) {
            int layer = (int) Math.floor((double)s * attractorLayers / (double)Math.max(1, slices - 1));
            double v = (double) s / (slices - 1);
            double sliceY = rootY + (currentHeight * v);

            // --- RADIUS LOGIC ---
            double ringRadius;
            if (isDeciduous) {
                ringRadius = canopyRadius * Math.sqrt(1.0 - Math.pow(v, 2));
            } else if (isConifer) {
                ringRadius = canopyRadius * (0.2 + 0.8 * (1.0 - v));
            } else {
                ringRadius = canopyRadius * Math.sqrt(1.0 - Math.pow(v, 3.7));
            }

            // --- CLUMPING: compute cluster centers then scatter around them ---
            // number of clusters for this slice
            int clusterCount = Math.max(2, (int)(pointsPerSlice * (1.0 - growthData.attractorClumping * 0.8)));
            int pointsPerCluster = Math.max(1, pointsPerSlice / clusterCount);

            // how tight are clusters (0..1). 1 = very tight, 0 = loose
            double clumpStrength = Math.max(0.0, Math.min(1.0, growthData.attractorClumping));
            // clusterRadiusFactor: fraction of ringRadius used for cluster-center offset
            double clusterCenterJitter = 0.10 + (1.0 - clumpStrength) * 0.35; // small jitter so centers aren't perfectly on the ring
            // point scatter radius around cluster center: smaller if high clumping
            double clusterScatterScale = 0.08 + (1.0 - clumpStrength) * 0.55; // fraction of ringRadius

            // create cluster centers (one per cluster) for this slice
            List<double[]> centers = new ArrayList<>(clusterCount);
            for (int c = 0; c < clusterCount; c++) {
                double baseAngle = 2.0 * Math.PI * (c + rand.nextDouble()) / clusterCount;
                // place center near the nominal ring position but with a little radial jitter
                double centerRadius = ringRadius * (1.0 + (rand.nextDouble() - 0.5) * clusterCenterJitter);
                double cx = root.startPos.getX() + Math.cos(baseAngle) * centerRadius;
                double cz = root.startPos.getZ() + Math.sin(baseAngle) * centerRadius;
                centers.add(new double[]{cx, cz, baseAngle});
            }

            for (int c = 0; c < clusterCount; c++) {
                double[] center = centers.get(c);
                double cx = center[0], cz = center[1];

                for (int p = 0; p < pointsPerCluster; p++) {
                    // radial + angular scatter around the cluster center
                    // gaussian-style distance for natural-looking blobs, clamp to [0,2]
                    double g = Math.abs(rand.nextGaussian());
                    if (g > 2.0) g = 2.0;
                    double scatterRadius = ringRadius * clusterScatterScale * g;

                    double scatterAngle = 2.0 * Math.PI * rand.nextDouble();
                    double x = cx + Math.cos(scatterAngle) * scatterRadius;
                    double z = cz + Math.sin(scatterAngle) * scatterRadius;

                    // phototropism: compute horizontal direction from trunk center to this point
                    Vec3 dirVec = new Vec3(x - root.startPos.getX(), 0, z - root.startPos.getZ());
                    if (dirVec.lengthSq() > 0.00001) dirVec = dirVec.normalize();
                    Vec3 hLight = growthData.lightDirection.mutable().withY(0).immutable().normalize();
                    double phototropicStretch = 1.0 + (dirVec.dot(hLight) * growthData.phototropism);

                    // push the point slightly outward if phototropism demands it
                    double u = rand.nextDouble();
                    double shaped = 0.05 + 0.95 * u; // keep shell bias
                    double finalRadial = shaped * phototropicStretch;

                    // re-apply final radial factor by moving the point slightly along radially from trunk
                    double trunkDirX = x - root.startPos.getX();
                    double trunkDirZ = z - root.startPos.getZ();
                    double trunkDist = Math.sqrt(trunkDirX*trunkDirX + trunkDirZ*trunkDirZ);
                    if (trunkDist > 0.0001) {
                        double wantDist = ringRadius * finalRadial;
                        double scale = wantDist / trunkDist;
                        x = root.startPos.getX() + trunkDirX * scale;
                        z = root.startPos.getZ() + trunkDirZ * scale;
                    }

                    Vec3 pt = new Vec3(x, sliceY, z);
                    // small jitter to avoid perfect grids
                    pt = pt.add(Vec3.randomUnit(rand).multiply(0.05 * ringRadius));

                    attractors.add(new Attractor(pt, layer));
                }
            }
        }

        // Optionally add some inner canopy / trunk-adjacent attractors for big broadleaf crowns
        if (!isConifer) {
            int innerCount = Math.max(10, pointsPerSlice/2);
            for (int i = 0; i < innerCount; i++) {
                double r = canopyRadius * 0.45 * Math.pow(rand.nextDouble(), 0.8);
                double theta = 2*Math.PI*rand.nextDouble();
                double y = trunkTop.getY() - rand.nextDouble() * (currentHeight * 0.6);
                Vec3 p = new Vec3(
                        trunkTop.getX() + Math.cos(theta)*r,
                        y,
                        trunkTop.getZ() + Math.sin(theta)*r
                );
                attractors.add(new Attractor(p, /*layer=*/1)); // scaffold layer
            }
        }

        cachedAttractors = attractors;
    }

    private Map<TreeNode, List<Vec3>> assignAttractorsToTips() {
        FailureTracker tracker = new FailureTracker();
        Map<TreeNode, List<AttractorScore>> scoredPerTip = new HashMap<>();
        List<Attractor> pool = cachedAttractors;
        if (pool == null || pool.isEmpty()) return Collections.emptyMap();

        List<Attractor> activePool = getActiveAttractors();
        if (activePool.isEmpty()) {
            log("Attractors poll was empty");
            return Collections.emptyMap();
        }

        // candidate tips (skip DEAD) - cache positions and directions
        List<TreeNode> tips = cachedBranches.values().stream()
                .filter(n -> n.nodeStatus != NodeStatus.DEAD)
                .toList();
        if (tips.isEmpty()) {
            log("Tips was empty");
            return Collections.emptyMap();
        }

        // Precompute tip data to avoid repeated method calls
        class TipData { TreeNode tip; Vec3 pos; Vec3 dir; double vigor; }
        List<TipData> tipDatas = new ArrayList<>(tips.size());
        for (TreeNode tip : tips) {
            TipData td = new TipData();
            td.tip = tip;
            td.pos = tip.tip();
            td.dir = tip.direction.normalize(); // ensure normalized
            td.vigor = Math.max(0.0, tip.vigor); // clamp
            tipDatas.add(td);
        }

        boolean isConifer = growthData.overrides.contains(Overrides.BranchOverrides.CONIFERISM);
        boolean isMulti = growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM);
        boolean isPalm = growthData.overrides.contains(Overrides.BranchOverrides.PALMISM);
        boolean isDeciduous = growthData.overrides.contains(Overrides.BranchOverrides.DECIDUOUS);

        // 1. Build Spatial Index of Attractors
        // Use a grid cell size slightly larger than influence radius to minimize neighbor checks
        double cellSize = Math.max(5.0, influenceRadiusAdaptive);
        Map<Long, List<Attractor>> grid = new HashMap<>();
        Vec3 origin = root.startPos; // Use relative coords to keep hash keys consistent/small

        for (Attractor a : activePool) {
            long key = getGridKey(a.pos, origin, cellSize);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }

        // 2. Iterate Tips -> Find Best Attractors (Optimized "What can I see?" approach)
        // We find the best Tip for each Attractor by searching from the Tips outward.
        Map<Attractor, Candidate> bestForAttractor = new HashMap<>();

        for (TipData td : tipDatas) {
            // Determine range of cells to check
            int minX = (int) Math.floor((td.pos.x - origin.x - influenceRadiusAdaptive) / cellSize);
            int maxX = (int) Math.floor((td.pos.x - origin.x + influenceRadiusAdaptive) / cellSize);
            int minY = (int) Math.floor((td.pos.y - origin.y - influenceRadiusAdaptive) / cellSize);
            int maxY = (int) Math.floor((td.pos.y - origin.y + influenceRadiusAdaptive) / cellSize);
            int minZ = (int) Math.floor((td.pos.z - origin.z - influenceRadiusAdaptive) / cellSize);
            int maxZ = (int) Math.floor((td.pos.z - origin.z + influenceRadiusAdaptive) / cellSize);

            double radiusSq = (growthData.influenceRadius == -1 ? Double.MAX_VALUE : influenceRadiusAdaptive * influenceRadiusAdaptive);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        long key = spatialHash(x, y, z);
                        List<Attractor> cell = grid.get(key);
                        if (cell == null) continue;

                        for (Attractor attractor : cell) {
                            double distSq = attractor.pos.distanceSq(td.pos);
                            if (distSq > radiusSq) {
                                // tracker.distSkip++; // Valid skip
                                continue;
                            }

                            double dist = Math.sqrt(distSq);
                            Vec3 to = attractor.pos.subtract(td.pos);
                            Vec3 toN = to.divide(dist); // Optimization: reuse dist for normalize

                            double rawDot = Math.max(-1.0, Math.min(1.0, td.dir.dot(toN)));
                            double angleRad = Math.acos(rawDot);

                            double score = computeAttractorScore(td.tip, attractor.pos, attractor, dist, angleRad, toN, isConifer, isMulti, isPalm, isDeciduous, tracker);

                            if (score > 0) {
                                // If this tip is better for this attractor than any previous tip found, update it
                                // This effectively performs the "Attractor picks Best Tip" logic but driven by Tips
                                Candidate newCand = new Candidate(td.tip, score);
                                bestForAttractor.merge(attractor, newCand, (oldC, newC) -> newC.score > oldC.score ? newC : oldC);
                            }
                        }
                    }
                }
            }
        }

        // 3. Assign to Tips with Capacity Logic
        // Iterate activePool to maintain deterministic order
        for (Attractor attractor : activePool) {
            Candidate best = bestForAttractor.get(attractor);
            if (best == null) continue;

            TreeNode bestTip = best.tip;
            double bestScore = best.score;

            // capacity: base scaled by tip vigor and species-specific multiplier
            double speciesCapMul = isPalm ? 0.6 : (isConifer ? 1.2 : 1.0);
            int capacity = Math.max(1, (int) Math.ceil((1.0 + bestTip.vigor) * OWNERSHIP_CAPACITY_BASE * speciesCapMul));

            scoredPerTip.putIfAbsent(bestTip, new ArrayList<>());
            List<AttractorScore> owned = scoredPerTip.get(bestTip);

            if (owned.size() < capacity) {
                owned.add(new AttractorScore(attractor.pos, bestScore, attractor));
            } else {
                // already full — find weakest and replace if we are significantly better
                int minIndex = -1;
                double minScore = Double.POSITIVE_INFINITY;
                for (int i = 0; i < owned.size(); i++) {
                    if (owned.get(i).score < minScore) {
                        minScore = owned.get(i).score;
                        minIndex = i;
                    }
                }

                if (minIndex != -1 && bestScore > minScore * STEAL_THRESHOLD) {
                    owned.set(minIndex, new AttractorScore(attractor.pos, bestScore, attractor));
                }
            }
        }

        // convert to Map<TreeNode, List<Vec3>>
        Map<TreeNode, List<Vec3>> perTip = new HashMap<>();
        for (Map.Entry<TreeNode, List<AttractorScore>> e : scoredPerTip.entrySet()) {
            List<Vec3> list = e.getValue().stream().map(as -> as.attractorPos).collect(Collectors.toList());
            perTip.put(e.getKey(), list);
        }

        log(String.format("DIAG T%d: Rejections -> Dist: %d, Vert: %d, Angle: %d, TrunkDown: %d, ZeroVigor: %d, Low score: %d",
                age, tracker.distSkip, tracker.verticalSkip, tracker.angleSkip, tracker.trunkDownSkip, tracker.zeroVigor, tracker.lowScore));

        return perTip;
    }

    private static long getGridKey(Vec3 pos, Vec3 origin, double cellSize) {
        int x = (int) Math.floor((pos.x - origin.x) / cellSize);
        int y = (int) Math.floor((pos.y - origin.y) / cellSize);
        int z = (int) Math.floor((pos.z - origin.z) / cellSize);
        return spatialHash(x, y, z);
    }

    private static long spatialHash(int x, int y, int z) {
        // Simple XOR hash for 3D coordinates
        return (long)x * 3129871L ^ (long)y * 116129791L ^ (long)z;
    }

    private static class FailureTracker {
        int distSkip = 0, verticalSkip = 0, angleSkip = 0, trunkDownSkip = 0, zeroVigor = 0, lowScore = 0;
    }

    private double computeAttractorScore(TreeNode tip, Vec3 attractorPos, Attractor attractor,
                                         double dist, double angleRad, Vec3 to /* normalized */,
                                         boolean isConifer, boolean isMulti,
                                         boolean isPalm, boolean isDeciduous, FailureTracker tracker) {

        if (tip.order == 0) {
            if (attractorPos.y < tip.tip().y) {
                tracker.trunkDownSkip++;
                return 0.0;
            }

            double upwardDot = to.y;
            double threshold = 0.3045; // Generic
            if (isDeciduous) threshold = 0.1736; // Wide 70° cone for decurrent splitting
            else if (isConifer) threshold = 0.9030; // Narrow 25° cone for excurrent height

            if (upwardDot < threshold) {
                tracker.angleSkip++;
                return 0.0;
            }
        }

        // --- 2. Tunables for Deciduous ---
        // Deciduous trees need to be "bendier" than conifers but more structured than palms
        double maxTurnDeg = isPalm ? 70.0 : isConifer ? 20.5 : (isDeciduous ? 60.0 : isMulti ? 55.0 : 45.0);
        double maxTurnRad = Math.toRadians(maxTurnDeg);

        // Deciduous turn falloff should be lower (2.0) to allow curving toward the dome
        double turnFalloff = isConifer ? 5.0 : (isDeciduous ? 2.0 : isMulti ? 1.2 : 3.0);

        // Deciduous trees like to fill the "dome", so outward weight is moderate
        double outwardWeight = isConifer ? 0.92 : (isDeciduous ? 0.45 : 0.6);

        // Deciduous trees grow UP and OUT. A pure downBias can kill top-level growth.
        double verticalBiasWeight = isConifer ? 0.43 : (isDeciduous ? 0.15 : isMulti ? 0.35 : 0.25);
        double vigorFloor = 0.05; // Slightly higher floor to prevent early starvation

        // --- 3. Base Score Calculation ---
        double vigorFactor = vigorFloor + Math.max(0.0, tip.vigor);
        double dNorm = Math.max(1e-4, dist / Math.max(1.0, influenceRadiusAdaptive));

        double distanceFactor;
        if (isDeciduous) {
            // Linear decay instead of Power decay.
            // This keeps distant points "valid" enough to be claimed.
            distanceFactor = 1.0 - (dNorm * 0.5);
        } else {
            // Standard steep decay for other types
            distanceFactor = Math.pow(1.0 - dNorm, DISTANCE_DECAY);
        }

        // Turn penalty (Softer for deciduous)
        double turnPenalty = 1.0;
        if (angleRad > maxTurnRad) {
            double excess = angleRad - maxTurnRad;
            turnPenalty = Math.exp(-excess * turnFalloff);
        }

        // Angular factor: Deciduous branches see "wider"
        double baseSpread = isDeciduous ? 0.3 : isMulti ? 0.22 : 0.5;

        int order = tip.order;

        double rawDot = Math.max(0.0,
                tip.direction.normalize().dot(to)
        );

        double exponent = 1.0 + order * 0.4;

        double narrowed = Math.pow(rawDot, exponent);

        double angularFactor =
                baseSpread + (1.0 - baseSpread) * narrowed;

        // --- 4. Final Score Assembly ---
        double baseScore = vigorFactor * ( 0.5 * angularFactor);

        if (isDeciduous) {
            // Instead of penalizing distance, give a tiny "scout" bonus for far points
            // to ensure they are high enough to be claimed.
            baseScore *= (1.0 + dNorm * 0.15);
        } else {
            baseScore *= (1.0 + (1.0 - dNorm) * 0.25);
        }

        // Apply Multipliers
        baseScore *= (1.0 + (1.0 - dNorm) * 0.25);
        baseScore *= turnPenalty;

        // Outward Dot
        Vec3 trunkCenter = root.firstPoint();
        double outwardDot = getOutwardDot(attractorPos, to, trunkCenter);
        baseScore *= (1.0 + outwardWeight * outwardDot);

        // Vertical Bias Logic
        if (isDeciduous) {
            // Deciduous: slightly prefer attractors that are ABOVE and OUTSIDE
            double upDot = to.y;
            baseScore *= (1.0 + upDot * verticalBiasWeight);
        } else if (isMulti) {
            // Deciduous: slightly prefer attractors that are ABOVE and OUTSIDE
            double upDot = to.y;
            baseScore *= (0.65 + upDot * verticalBiasWeight);
        } else if (!isPalm) {
            // Conifer/Broadleaf: keep the downward bias for drooping/shading
            double downBias = 1.0 + (-to.y) * verticalBiasWeight;
            baseScore *= Math.max(0.6, Math.min(1.6, downBias));
        }

        // Deciduous Canopy Boost: High boost for the "dome" area
        if (isDeciduous && attractorPos.y > root.startPos.getY() + (root.length() * 0.7)) {
            baseScore *= 1.3;
        }

        if (Double.isNaN(baseScore) || baseScore <= 0.0) return 0.0;
        return baseScore;
    }

    private static double getOutwardDot(Vec3 attractor, Vec3 to, Vec3 trunkCenter) {
        Vec3 horiz = new Vec3(attractor.x - trunkCenter.x, 0.0, attractor.z - trunkCenter.z);
        double horizLen = Math.sqrt(horiz.x*horiz.x + horiz.z*horiz.z);
        double outwardDot = 0.0;
        if (horizLen > 1e-6) {
            Vec3 horizNorm = new Vec3(horiz.x / horizLen, 0.0, horiz.z / horizLen);
            // project 'to' onto horizontal plane:
            Vec3 toHoriz = new Vec3(to.x, 0.0, to.z);
            double toHorizLen = Math.sqrt(toHoriz.x*toHoriz.x + toHoriz.z*toHoriz.z);
            if (toHorizLen > 1e-6) {
                Vec3 toHorizNorm = new Vec3(toHoriz.x / toHorizLen, 0.0, toHoriz.z / toHorizLen);
                outwardDot = Math.max(0.0, horizNorm.dot(toHorizNorm)); // 0..1
            }
        }
        return outwardDot;
    }

    // small helper classes used above
    private static class Candidate { final TreeNode tip; final double score; Candidate(TreeNode t, double s) { tip=t; score=s; } }
    private static class AttractorScore {
        final Vec3 attractorPos;
        final double score;
        final Attractor attractorRef; // optional link back
        AttractorScore(Vec3 p, double s, Attractor ref) { attractorPos=p; score=s; attractorRef=ref; }
    }

    public void applyCurlyness(TreeNode branch) {
        if (branch.controlPoints.size() < 2) return;

        // 1. "Surgery": Clear the extra points
        branch.extraPoints.clear();

        List<Vec3> pts = branch.controlPoints;
        int n = pts.size();
        Vec3 p1 = pts.get(n - 2);
        Vec3 p2 = pts.get(n - 1);

        // --- STEP 1: Create a perfectly Orthonormal Basis ---
        // Tangent is the growth direction
        Vec3 T = p2.subtract(p1).normalize();

        // We need a helper vector to find a "side" (Normal)
        // Using the branch direction as a hint for which way it's already bending
        Vec3 hint = branch.direction.normalize();
        if (Math.abs(hint.dot(T)) > 0.99) {
            hint = T.getOrtho(); // Safety if growing perfectly straight
        }

        // Binormal (B) is perpendicular to both
        Vec3 B = T.crossProduct(hint).normalize();
        // Re-calculate Normal (N) to ensure perfect 90-degree basis (T, N, B)
        Vec3 N = B.crossProduct(T).normalize();

        // --- STEP 2: Spiral Parameters ---
        double coilRange = traits.getFloat((int) branch.id, BranchTraitDefs.COIL_RANGE);
        double coilRadiusRatio = traits.getFloat((int) branch.id, BranchTraitDefs.COIL_RADIUS_RATIO);
        double totalRotation = 2.0 * Math.PI * coilRange;

        // Use the actual distance between points for scale consistency
        double segmentLen = p2.distance(p1);
        double startRadius = branch.length() * coilRadiusRatio;

        // The center is offset along the Normal
        Vec3 spiralCenter = p2.add(N.multiply(startRadius));

        // --- STEP 3: Generate the Curve ---
        int resolution = growthData.coilResolution; // Increased from 8 to fix the "oval" look
        for (int i = 0; i <= resolution; i++) {
            double t = (double) i / (double) resolution;
            double theta = t * totalRotation;

            // Radius tapers to zero
            double currentRadius = startRadius * (1.0 - t);

            // Circular math on the T/N plane
            // We start at p2 (where cos(0)=1, sin(0)=0)
            // Offset = -N * cos(theta) + -T * sin(theta)
            Vec3 offset = N.multiply(-Math.cos(theta) * currentRadius)
                    .add(T.multiply(-Math.sin(theta) * currentRadius));

            branch.extraPoints.add(spiralCenter.add(offset));
        }
    }

    public double getMaxLength(TreeNode branch, double treeHeightNominal) {
        double relHeight =
                (branch.firstPoint().y - branch.parent.firstPoint().y) / treeHeightNominal;
        relHeight = Math.max(0.0, Math.min(1.0, relHeight));
        double heightFactor = growthData.branchLengthHeightCurveF != null ?
                growthData.branchLengthHeightCurveF.apply(relHeight)
                : growthData.branchLengthHeightCurve.apply(1.0 - relHeight);
        double orderFactor = Math.pow(growthData.branchLengthOrderFalloff, branch.order);

        logf("ya: %s, yb: %s, treeHeightNomial: %s, heightFac: %s, orderFac: %s, relHeight: %s, final: %s",
                branch.firstPoint().y, branch.parent.firstPoint().y,
                treeHeightNominal, heightFactor, orderFactor, relHeight, branch.maxLength);

        return treeHeightNominal *
                growthData.branchLengthPercentage *
                heightFactor *
                orderFactor;
    }

    private double senescenceFactor(TreeNode node) {
        if (node == null) return 0.0;
        if (!growthData.senescenceAffectsChildren && node.order > 0) return 0.0;

        double lengthSince = node.length() / node.maxLength;
        if (lengthSince <= growthData.senescenceStartPercentage) return 0.0;

        // time since senescence actually started
        double senAge = lengthSince - growthData.senescenceStartPercentage;

        // ramp duration (how long until fully senescent)
        double rampDur = Math.max(0.0001, 1.0 - growthData.senescenceStartPercentage);

        // normalize into [0..1] range of total senescence timeline
        double t = Math.min(1.0, senAge / rampDur);

        // Exponential ease-in (slow start, rapid late)
        // The 3.5 constant controls curve steepness: 3.5 = quite sharp, 2.0 = smoother
        double shaped = 1.0 - exp(-t * 3.5);

        // optional: combine with linear to give controllable growth
        double factor = (t * 0.4) + (shaped * 0.6);

        // clamp
        factor = Math.max(0.0, Math.min(1.0, factor));

        // reduce for child branches if needed
        if (node.order > 0 && growthData.senescenceChildAttenuation > 0.0) {
            factor *= growthData.senescenceChildAttenuation;
        }

        // System.out.println("Vigor: " + node.vigor + " Factor: " + factor + " senAge: " + senAge + " rampDur: " + rampDur);
        return factor;
    }

    public enum NodeStatus {BUD, ALIVE, DEAD}

    public interface Overrides {
        enum BranchOverrides implements Overrides {
            MIRROR_BRANCHES, CONIFERISM, CONED_CONIFERISM, REGRESSION, PALMISM, DECIDUOUS, CURLY_TIPS, TAPERISM;

            @Override
            public String toString() {
                return "B_" + super.toString();
            }
        }
        enum TrunkOverrides implements Overrides {
            MULTI_TRUNKISM, CONIFERISM;

            @Override
            public String toString() {
                return "T_" + super.toString();
            }
        }
        enum GlobalOverrides implements Overrides {
            OVERRIDE_GRAVITY;

            @Override
            public String toString() {
                return "G_" + super.toString();
            }
        }
    }
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }


    private void logTreeStructure(TreeNode node, String prefix) {
        if (node == null) return;
        log(prefix + nodeShort(node));
        new ArrayList<>(node.children).forEach(it -> logTreeStructure(it, prefix + ">> child "));
    }

    private void logStage(String stageName) {
        log("------ [" + stageName.toUpperCase() + "] ------");
    }

    private void logTickSummary() {
        int alive = 0, buds = 0, dead = 0;
        for (TreeNode n : cachedBranches.values()) {
            switch (n.nodeStatus) {
                case ALIVE -> alive++;
                case BUD -> buds++;
                case DEAD -> dead++;
            }
        }
        log(String.format("TICK %d SUMMARY: nodes=%d alive=%d buds=%d dead=%d maxOrder=%d",
                age, cachedBranches.size(), alive, buds, dead, growthData.maxDepth));
    }

    private void logf(String message, Object...objects) {
        log(String.format(message, objects));
    }

    private void logft(String message, Object...objects) {
        log(String.format(message, objects));
    }

    private void log(String s) {
        if (debug) logger.ambient("ThesisTree@" + age + " " + s);
    }

    private String nodeShort(TreeNode n) {
        if (n == null) return "null";
        return String.format("id=%d st=%s order=%d vig=%.3f direction=%s parent=%s",
                n.id, n.nodeStatus, n.order, n.vigor, n.direction, n.parent == null ? "ROOT" : Long.toString(n.parent.id));
    }

    private void dumpSummary() {
        if (!debug) return;
        log("SUMMARY: cachedBranches.size=" + cachedBranches.size() + " lastNodeId=" + lastNodeId + " maxOrder=" + growthData.maxDepth);
    }

    /**
     * Calculates trunk tropism: the gradual directional change caused by light and gravity.
     *
     * <p>Biological interpretation:
     * <ul>
     *     <li><b>Phototropism</b> – growth bias toward the light source.</li>
     *     <li><b>Gravitropism</b> – bias to maintain upward stability (opposes drooping).</li>
     *     <li><b>Flexibility</b> – defines how strongly the trunk bends toward its bias each tick.</li>
     *     <li><b>Straightness</b> – controls randomness; lower values yield more organic, uneven curvature.</li>
     * </ul>
     *
     * <p>Over multiple simulation ticks, the direction vector is linearly interpolated toward a
     * combined tropism target. Because this interpolation is incremental, curvature accumulates over time,
     * forming natural arcs rather than abrupt kinks. The resulting "arch" is an emergent equilibrium
     * between gravitational pull and phototropic attraction toward the light direction.</p>
     *
     * <p>In short:
     * <pre>
     * - Low flexibility → rigid, vertical trunk.
     * - High flexibility → soft, bending trunk.
     * - Low straightness → irregular, wavy arch.
     * - Strong phototropism → pronounced leaning toward light.
     * </pre>
     * <p>
     * Typical behavior:
     * <ul>
     *     <li>Young trees (age lessthan 10): small curvature near the base as light bias dominates.</li>
     *     <li>Mature trees (age greaterthan 30): vertical base stabilized by gravity, curved crown following light vector.</li>
     * </ul>
     */
    public static class GrowthData {
        public final float baseLength;
        public Set<Overrides> overrides = new HashSet<>();
        public Vec3 lightDirection = new Vec3(0f, 1.0f, 0f);
        public Vec3 overridenGravity = new Vec3(0f, -1.0f, 0f);
        public float gravitropism = -0.2f;
        public float phototropism = 1.0f;
        public float lateralAngleDegrees = 20.0f;
        public float initialVigor = 1.0f;
        public float baseBudLight = 0.24f;
        public float straightness = 0.37f;
        public float flexibility = 0.95f;
        public int minAgeBeforeShed = 10;
        public double influenceRadius = 25.0;   // how far a branch can “see” light
        public double vigorRadius = 10.5;   // how far a branch can “see” light
        public double killRadius = 15.5;        // distance at which attractor is considered reached
        public double budProbability = 0.86;    // how likely a bud forms when vigor is high
        public float vigorDecay = 0.95f;
        public int maxKids = 15;
        public int trunkGrowthMaxAge = -1;
        public int maxDepth = 2;
        public float pruningHeight = 0.75f;
        public TimeCurve forTrunk = TimeCurve.LINEAR;
        public float minRadius = 0.05f;
        public float rootAgeK = 0.057f;        // exponent rate for age — small so growth is gradual
        public float rootBaseMultiplier = 0.74f; // scales the exponential term
        public float rootHeightScale = 0.75f;  // how much height multiplies growth
        public float rootMaxMultiplier = 3.50f;
        public float distanceBetweenChildren = 1.0f;
        public int minSplittingAge = 10;
        public double senescenceStartPercentage = 0.4;
        public double senescenceDecayRate = 0.03;
        public double senescenceVigorPenalty = 0.55;
        public double senescenceGravBias = 0.4;
        public double senescenceBudPenalty = 0.6;
        public double senescenceChildAttenuation = 1.0;

        public double canopyCylinderHeightPct = 1.0;
        public double canopyCylinderRadiusFactor = 1.0;
        public double canopyCylinderTaper = 0.0;

        public boolean senescenceAffectsChildren = true;
        public int multiTrunkismAge = 8;
        public int multiTrunkismMaxAmount = 4;
        public int multiTrunkismMinAmount = 2;
        public double ageFactorExp = 0.023f;
        public double yReduction = 1.0;

        public boolean lengthCapEnabled = true;
        public double branchLengthPercentage = 1.0;
        public double branchLengthOrderFalloff = 0.91;
        public TimeCurve branchLengthHeightCurve = TimeCurve.SQRT;
        public Function<Double, Double> branchLengthHeightCurveF = null;
        public double branchLengthGlobalScale = 1.0;

        public double coniferousMaxVerticalTolerance = 0.8;
        public int coniferousBranchMin = 4;
        public int coniferousBranchMax = 6;
        public float coniferousBudRadius = 0.4f;
        public double coniferApicalRange = 20.0;
        public double coniferMaxUpBias = 5.0;
        public double coniferApicalCurve = 2.0;
        public double coniferApicalStopStart = 0.58;
        public double coniferApicalStopEnd = 1.00;

        public double attractorClumping = 0.0;
        private float spread = 1;

        public Pair<Float, Float> coilRange =
                new Pair<>(1.0f, 2.1f);
        public Pair<Float, Float> coilRadiusRatio =
                new Pair<>(0.3f, 0.34f);
        public Pair<Double, Double> conedConiferismAngle =
                new Pair<>(20.0, 35.7);
        private int coilResolution = 15;
        private int coilDepth = 1;


        // constructor for quick creation
        public GrowthData() {
            baseLength = 1.0f;
        }

        public GrowthData(float baseLength) {
            this.baseLength = baseLength;
        }

        private GrowthData(Builder builder) {
            this.lengthCapEnabled = builder.lengthCapEnabled;
            this.branchLengthGlobalScale = builder.branchLengthGlobalScale;
            this.branchLengthPercentage = builder.branchLengthPercentage;
            this.branchLengthHeightCurve = builder.branchLengthHeightCurve;
            this.branchLengthHeightCurveF = builder.branchLengthHeightCurveF;
            this.branchLengthOrderFalloff = builder.branchLengthOrderFalloff;
            this.baseLength = builder.baseLength;
            this.lightDirection = builder.lightDirection;
            this.overridenGravity = builder.overridenGravity;
            this.gravitropism = builder.gravitropism;
            this.phototropism = builder.phototropism;
            this.coilRange = builder.coilRange;
            this.coilRadiusRatio = builder.coilRadiusRatio;
            this.conedConiferismAngle = builder.conedConiferismAngle;
            this.coilResolution = builder.coilResolution;
            this.coilDepth = builder.coilDepth;
            this.lateralAngleDegrees = builder.lateralAngleDegrees;
            this.initialVigor = builder.initialVigor;
            this.baseBudLight = builder.baseBudLight;
            this.straightness = builder.straightness;
            this.attractorClumping = builder.attractorClumping;
            this.flexibility = builder.flexibility;
            this.minAgeBeforeShed = builder.minAgeBeforeShed;
            this.influenceRadius = builder.influenceRadius;
            this.vigorRadius = builder.vigorRadius;
            this.killRadius = builder.killRadius;
            this.budProbability = builder.budProbability;
            this.vigorDecay = builder.vigorDecay;
            this.maxKids = builder.maxKids;
            this.trunkGrowthMaxAge = builder.trunkGrowthMaxAge;
            this.maxDepth =
                    builder.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM) ?
                            builder.maxDepth + 1 : builder.maxDepth;
            this.pruningHeight = builder.pruningHeight;
            this.forTrunk = builder.forTrunk;
            this.minRadius = builder.minRadius;
            this.rootAgeK = builder.rootAgeK;
            this.rootBaseMultiplier = builder.rootBaseMultiplier;
            this.rootHeightScale = builder.rootHeightScale;
            this.rootMaxMultiplier = builder.rootMaxMultiplier;
            this.distanceBetweenChildren = builder.distanceBetweenChildren;
            this.minSplittingAge = builder.minSplittingAge;
            this.senescenceStartPercentage = builder.senescenceStartPercentage;
            this.senescenceDecayRate = builder.senescenceDecayRate;
            this.senescenceVigorPenalty = builder.senescenceVigorPenalty;
            this.senescenceGravBias = builder.senescenceGravBias;
            this.senescenceBudPenalty = builder.senescenceBudPenalty;
            this.senescenceChildAttenuation = builder.senescenceChildAttenuation;
            this.senescenceAffectsChildren = builder.senescenceAffectsChildren;
            this.overrides = builder.overrides;
            this.multiTrunkismMaxAmount = builder.multiTrunkismMaxAmount;
            this.multiTrunkismMinAmount = builder.multiTrunkismMinAmount;
            this.multiTrunkismAge = builder.multiTrunkismAge;
            this.spread = builder.spread;
            this.coniferousBranchMin = builder.coniferousBranchMin;
            this.coniferousBranchMax = builder.coniferousBranchMax;
            this.coniferousBudRadius = builder.coniferousBudRadius;
            this.coniferousMaxVerticalTolerance = builder.coniferousMaxVerticalTolerance;
            this.canopyCylinderHeightPct = builder.canopyCylinderHeightPct;
            this.canopyCylinderRadiusFactor = builder.canopyCylinderRadiusFactor;
            this.canopyCylinderTaper = builder.canopyCylinderTaper;
            this.ageFactorExp = builder.ageFactorExp;
            this.yReduction = builder.yReduction;
        }

        public static final class Builder {
            private final float baseLength;
            public double attractorClumping = 0.0;
            private Set<Overrides> overrides = new HashSet<>();
            private Vec3 lightDirection = new Vec3(0f, 1.0f, 0f);
            private Vec3 overridenGravity = new Vec3(0f, -1.0f, 0f);
            private float gravitropism = -0.2f;
            private float phototropism = 1.0f;
            private float lateralAngleDegrees = 20.0f;
            private float initialVigor = 1.0f;
            private float baseBudLight = 0.24f;
            private float straightness = 0.37f;
            private float flexibility = 0.95f;
            private int minAgeBeforeShed = 10;
            private double influenceRadius = 25.0;   // how far a branch can “see” light
            private double vigorRadius = 10.5;   // how far a branch can “see” light
            private double killRadius = 15.5;        // distance at which attractor is considered reached
            private double budProbability = 0.86;    // how likely a bud forms when vigor is high
            private float vigorDecay = 0.95f;
            private int maxKids = 15;
            private int trunkGrowthMaxAge = -1;
            private int maxDepth = 2;
            private float pruningHeight = 0.75f;
            private TimeCurve forTrunk = TimeCurve.LINEAR;
            private float minRadius = 0.05f;
            private float rootAgeK = 0.057f;        // exponent rate for age — small so growth is gradual
            private float rootBaseMultiplier = 0.74f; // scales the exponential term
            private float rootHeightScale = 0.75f;  // how much height multiplies growth
            private float rootMaxMultiplier = 3.50f;
            private float distanceBetweenChildren = 1.0f;
            private int minSplittingAge = 10;
            private double senescenceStartPercentage = 0.4;
            private double senescenceDecayRate = 0.03;
            private double senescenceVigorPenalty = 0.55;
            private double senescenceGravBias = 0.4;
            private double senescenceBudPenalty = 0.6;
            private double senescenceChildAttenuation = 1.0;
            private boolean senescenceAffectsChildren = true;
            private int multiTrunkismAge = 8;
            private int multiTrunkismMaxAmount = 4;
            private int multiTrunkismMinAmount = 2;
            private float spread = 1;
            public int coniferousBranchMin = 4;
            public int coniferousBranchMax = 6;
            public float coniferousBudRadius = 0.4f;
            public double coniferousMaxVerticalTolerance = 0.8;

            public double ageFactorExp = 0.00023f;
            public double canopyCylinderHeightPct = 1.0;
            public double canopyCylinderRadiusFactor = 1.0;
            public double canopyCylinderTaper = 0.0;
            public double yReduction = 1.0;

            public boolean lengthCapEnabled = true;
            public double branchLengthPercentage = 1.0;
            public double branchLengthOrderFalloff = 0.91;
            public TimeCurve branchLengthHeightCurve = TimeCurve.SQRT;
            public Function<Double, Double> branchLengthHeightCurveF = null;
            public double branchLengthGlobalScale = 1.0;

            public Pair<Float, Float> coilRange =
                    new Pair<>(1.0f, 2.1f);
            public Pair<Float, Float> coilRadiusRatio =
                    new Pair<>(0.3f, 0.34f);
            public Pair<Double, Double> conedConiferismAngle =
                    new Pair<>(20.0, 35.7);
            private int coilResolution = 15;
            private int coilDepth = 1;


            public Builder(float baseLength) {
                this.baseLength = baseLength;
            }

            @Nonnull
            public Builder branchLengthPercentage(double v) { branchLengthPercentage = v; return this; }
            @Nonnull
            public Builder branchLengthOrderFalloff(double v) { branchLengthOrderFalloff = v; return this; }
            @Nonnull
            public Builder branchLengthHeightCurve(TimeCurve c) { branchLengthHeightCurve = c; return this; }
            @Nonnull
            public Builder branchLengthHeightCurve(Function<Double, Double> f) { branchLengthHeightCurveF = f; return this; }
            @Nonnull
            public Builder branchLengthGlobalScale(double v) { branchLengthGlobalScale = v; return this; }
            @Nonnull
            public Builder lengthCapEnabled(boolean v) { lengthCapEnabled = v; return this; }
            
            @Nonnull
            public Builder multiTrunkismAge(int val) {
                multiTrunkismAge = val;
                return this;
            }

            @Nonnull
            public Builder coilRange(float min, float max) {
                if (min < max) {
                    float minimum = Math.min(4.9f, Math.max(1.0f, min));
                    float maximum = Math.max(1.1f, Math.min(5.0f, max));

                    coilRange = new Pair<>(minimum, maximum);
                }
                return this;
            }

            @Nonnull
            public Builder coilRadiusRatio(float min, float max) {
                if (min < max) {
                    float minimum = Math.min(0.9f, Math.max(0.0f, min));
                    float maximum = Math.max(0.1f, Math.min(1.0f, max));

                    coilRadiusRatio = new Pair<>(minimum, maximum);
                }
                return this;
            }

            @Nonnull
            public Builder conedConiferismAngle(double min, double max) {
                if (min < max) {
                    double minimum = Math.min(1, Math.max(0.0, min));
                    double maximum = Math.max(minimum + 1, Math.min(80, max));

                    conedConiferismAngle = new Pair<>(minimum, maximum);
                }
                return this;
            }

            @Nonnull
            public Builder multiTrunkismMaxAmount(int val) {
                multiTrunkismMaxAmount = val;
                return this;
            }

            @Nonnull
            public Builder multiTrunkismMinAmount(int val) {
                multiTrunkismMinAmount = val;
                return this;
            }

            @Nonnull
            public Builder canopyCylinderHeightPct(double val) {
                canopyCylinderHeightPct = val;
                return this;
            }

            @Nonnull
            public Builder ageFactorExp(double val) {
                ageFactorExp = val;
                return this;
            }

            @Nonnull
            public Builder canopyCylinderRadiusFactor(double val) {
                canopyCylinderRadiusFactor = val;
                return this;
            }

            @Nonnull
            public Builder canopyCylinderTaper(double val) {
                canopyCylinderTaper = val;
                return this;
            }

            @Nonnull
            public Builder attractorClumping(double val) {
                attractorClumping = val;
                return this;
            }

            @Nonnull
            public Builder lightDirection(@Nonnull Vec3 val) {
                lightDirection = Vec3.of(val.x, 1.0f, val.z);
                return this;
            }

            @Nonnull
            public Builder addOverrides(@Nonnull List<Overrides> overrides) {
                this.overrides.addAll(overrides);
                return this;
            }

            @Nonnull
            public final Builder addOverrides(Overrides...overrides) {
                if (overrides.length >= 1) {
                    this.overrides.addAll(List.of(overrides));
                }
                return this;
            }

            @Nonnull
            public Builder setOverrides(@Nonnull Set<Overrides> overrides) {
                this.overrides = overrides;
                return this;
            }

            @Nonnull
            public Builder addOverride(@Nonnull Overrides override) {
                this.overrides.add(override);
                return this;
            }

            @Nonnull
            public Builder overridenGravity(@Nonnull Vec3 val) {
                overridenGravity = val;
                return this;
            }

            @Nonnull
            public Builder gravitropism(float val) {
                gravitropism = val;
                return this;
            }

            @Nonnull
            public Builder coniferousBudRadius(float val) {
                coniferousBudRadius = val;
                return this;
            }

            @Nonnull
            public Builder coniferousMaxVerticalTolerance(double val) {
                coniferousMaxVerticalTolerance = val;
                return this;
            }

            @Nonnull
            public Builder phototropism(float val) {
                phototropism = val;
                return this;
            }

            @Nonnull
            public Builder lateralAngleDegrees(float val) {
                lateralAngleDegrees = val;
                return this;
            }

            @Nonnull
            public Builder initialVigor(float val) {
                initialVigor = val;
                return this;
            }

            @Nonnull
            public Builder baseBudLight(float val) {
                baseBudLight = val;
                return this;
            }

            @Nonnull
            public Builder straightness(float val) {
                straightness = val;
                return this;
            }

            @Nonnull
            public Builder flexibility(float val) {
                flexibility = val;
                return this;
            }

            @Nonnull
            public Builder minAgeBeforeShed(int val) {
                minAgeBeforeShed = val;
                return this;
            }

            @Nonnull
            public Builder coniferousBranchRange(int min, int max) {
                if (min == max) {
                    max += 1;
                }
                else if (min > max) {
                    var m = max;
                    max = min;
                    min = m;
                }
                this.coniferousBranchMin = min;
                this.coniferousBranchMax = max;
                return this;
            }

            @Nonnull
            public Builder influenceRadius(double val) {
                influenceRadius = val;
                return this;
            }

            @Nonnull
            public Builder vigorRadius(double val) {
                vigorRadius = val;
                return this;
            }

            @Nonnull
            public Builder killRadius(double val) {
                killRadius = val;
                return this;
            }

            @Nonnull
            public Builder trunkGrowthMaxAge(int val) {
                trunkGrowthMaxAge = val;
                return this;
            }

            @Nonnull
            public Builder budProbability(double val) {
                budProbability = val;
                return this;
            }

            @Nonnull
            public Builder vigorDecay(float val) {
                vigorDecay = val;
                return this;
            }

            @Nonnull
            public Builder maxKids(int val) {
                maxKids = val;
                return this;
            }

            @Nonnull
            public Builder maxDepth(int val) {
                maxDepth = val;
                return this;
            }

            @Nonnull
            public Builder pruningHeight(float val) {
                pruningHeight = val;
                return this;
            }

            @Nonnull
            public Builder forTrunk(@Nonnull TimeCurve val) {
                forTrunk = val;
                return this;
            }

            @Nonnull
            public Builder minRadius(float val) {
                minRadius = val;
                return this;
            }

            @Nonnull
            public Builder rootAgeK(float val) {
                rootAgeK = val;
                return this;
            }

            @Nonnull
            public Builder rootBaseMultiplier(float val) {
                rootBaseMultiplier = val;
                return this;
            }

            @Nonnull
            public Builder rootHeightScale(float val) {
                rootHeightScale = val;
                return this;
            }

            @Nonnull
            public Builder rootMaxMultiplier(float val) {
                rootMaxMultiplier = val;
                return this;
            }

            @Nonnull
            public Builder distanceBetweenChildren(float val) {
                distanceBetweenChildren = val;
                return this;
            }

            @Nonnull
            public Builder minSplittingAge(int val) {
                minSplittingAge = val;
                return this;
            }

            @Nonnull
            public Builder senescenceStartPercentage(double val) {
                senescenceStartPercentage = val;
                return this;
            }

            @Nonnull
            public Builder senescenceDecayRate(double val) {
                senescenceDecayRate = val;
                return this;
            }

            @Nonnull
            public Builder senescenceVigorPenalty(double val) {
                senescenceVigorPenalty = val;
                return this;
            }

            @Nonnull
            public Builder senescenceGravBias(double val) {
                senescenceGravBias = val;
                return this;
            }

            @Nonnull
            public Builder senescenceBudPenalty(double val) {
                senescenceBudPenalty = val;
                return this;
            }

            @Nonnull
            public Builder senescenceChildAttenuation(double val) {
                senescenceChildAttenuation = val;
                return this;
            }

            @Nonnull
            public Builder senescenceAffectsChildren(boolean val) {
                senescenceAffectsChildren = val;
                return this;
            }

            @Nonnull
            public GrowthData build() {
                return new GrowthData(this);
            }

            @Nonnull
            public Builder spread(float i) {
                this.spread = i;
                return this;
            }

            public Builder yReduction(double v) {
                this.yReduction = v;
                return this;
            }

            public Builder coilResolution(int resolution) {
                this.coilResolution = resolution;
                return this;
            }

            public Builder coilDepth(int coilDepth) {
                this.coilDepth = coilDepth;
                return this;
            }
        }
    }

    private static class DoSomethingArrayList<T> extends ArrayList<T> {

        private final Runnable action;

        public DoSomethingArrayList(Runnable action) {
            this.action = action;
        }

        @Override
        public boolean add(T t) {
            action.run();
            return super.add(t);
        }

        @Override
        public void add(int i, T t) {
            action.run();
            super.add(i, t);
        }

        @Override
        public T remove(int i) {
            action.run();
            return super.remove(i);
        }
    }

    public static class TreeNode {
        public final TreeNode parent;
        public final List<TreeNode> children = new ArrayList<>();
        private final List<Vec3> controlPoints =
                new DoSomethingArrayList<>(this::invalidateCache);
        private final List<Vec3> extraPoints = new ArrayList<>();
        public Vec3 startPos;
        public Vec3 direction;
        public float vigor = 1.0f;
        public double maxLength = Double.POSITIVE_INFINITY;
        public NodeStatus nodeStatus;
        public long id;
        public int order;
        public int childOrder;
        public int currentParentOrder = 0;
        public int createdAt = 0;
        public float baseRadius; // This is basically max radius
        public Vec3 localUp = Vec3.of(0, 1, 0);
        public boolean canGrowTaller = true;
        public boolean isChild = false;
        public int baseLengthInPoints = 1;

        private Double cachedLength;
        private Vec3 cachedTip = null;
        private int cacheVersion = 0;

        public TreeNode(TreeNode parent, long id, Vec3 startPos, Vec3 direction, float radius) {
            this.parent = parent;
            this.id = id;
            this.startPos = startPos;
            this.direction = direction != null ? direction.normalize() : new Vec3(0, 1, 0);
            this.nodeStatus = NodeStatus.BUD;
            this.controlPoints.add(startPos);
            this.baseRadius = radius;
            this.order = (parent != null) ? parent.order + 1 : 0;
        }

        public Vec3 tip() {
            if (cachedTip == null || controlPoints.isEmpty()) {
                cachedTip = controlPoints.isEmpty() ? startPos : controlPoints.getLast();
            }
            return cachedTip;
        }

        public Vec3 firstPoint() {
            return controlPoints.getFirst();
        }

        public List<Vec3> getControlPoints() {
            return Collections.unmodifiableList(controlPoints);
        }

        public List<Vec3> getAllPoints() {
            List<Vec3> combined = new ArrayList<>(controlPoints);
            combined.addAll(extraPoints);
            return combined;
        }

        public List<TreeNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public double length() {
            if (cachedLength != null) return cachedLength;

            double sum = 0;
            for (int i = 1; i < controlPoints.size(); i++) {
                sum += controlPoints.get(i).distance(controlPoints.get(i - 1));
            }
            cachedLength = sum;
            return sum;
        }

        // Call this when adding points
        public void invalidateCache() {
            cachedTip = null;
            cachedLength = null;
            cacheVersion++;
        }

        @Override
        public String toString() {
            return String.format("{ createdAt: age %s, length: %s, start %s, children: %s }", createdAt, this.length(), startPos, children.size());
        }
    }
    
    /**
     Memory-efficient TreeNode replacement using pool / SoA layout.
    
     - TreeNodePool stores node fields in primitive arrays (int/float/long) to reduce per-node object overhead.
     - ControlPointPool stores control points as a linked list per-node but keeps points in primitive arrays.
     - Vec3Pool is a lightweight primitive pool for positions used by control points.

     Usage:
       TreeNodePool pool = new TreeNodePool(4096);
       int rootIdx = pool.createNode(-1, 1L, startX, startY, startZ, dirX, dirY, dirZ, 0.5f);
       pool.addControlPoint(rootIdx, startX, startY, startZ);
       // later get tip:
       Vec3 tip = pool.tip(rootIdx);

     This code intentionally avoids creating small per-node collections and Vec3 objects except when the API returns them.
    */
    public final class TreeNodePool {
        // Node arrays (SoA)
        private int[] parent;
        private int[] firstChild;   // index of first child
        private int[] nextSibling;  // sibling linked list

        private int[] firstCP;      // index into control point pool (first)
        private int[] lastCP;       // last control point index
        private int[] cpCount;      // number of control points

        private double[] startX, startY, startZ;
        private double[] dirX, dirY, dirZ;
        private float[] vigor;
        private byte[] nodeStatus;  // store ordinal of enum to save space
        private long[] id;
        private int[] order;
        private int[] createdAt;
        private float[] baseRadius;
        private double[] upX, upY, upZ;
        private int[] flags;        // bitflags (bit0 = canGrowTaller)

        private int size = 0;

        // Pools used by this node pool
        private final ControlPointPool cpPool;

        private static final int FLAG_CAN_GROW_TALLER = 1 << 0;

        public TreeNodePool(int initialCapacity) {
            ensureCapacity(initialCapacity);
            cpPool = new ControlPointPool(Math.max(1024, initialCapacity * 4));
        }

        private void ensureCapacity(int cap) {
            int newCap = Math.max(16, cap);
            parent = ensure(parent, newCap, -1);
            firstChild = ensure(firstChild, newCap, -1);
            nextSibling = ensure(nextSibling, newCap, -1);
            firstCP = ensure(firstCP, newCap, -1);
            lastCP = ensure(lastCP, newCap, -1);
            cpCount = ensure(cpCount, newCap, 0);
            startX = ensure(startX, newCap, 0.0);
            startY = ensure(startY, newCap, 0.0);
            startZ = ensure(startZ, newCap, 0.0);
            dirX = ensure(dirX, newCap, 0.0);
            dirY = ensure(dirY, newCap, 0.0);
            dirZ = ensure(dirZ, newCap, 1.0);
            vigor = ensure(vigor, newCap, 1.0f);
            nodeStatus = ensure(nodeStatus, newCap, (byte)0);
            id = ensure(id, newCap, 0L);
            order = ensure(order, newCap, 0);
            createdAt = ensure(createdAt, newCap, 0);
            baseRadius = ensure(baseRadius, newCap, 0.0f);
            upX = ensure(upX, newCap, 0.0);
            upY = ensure(upY, newCap, 1.0);
            upZ = ensure(upZ, newCap, 0.0);
            flags = ensure(flags, newCap, 0);
        }

        // generic ensure helpers
        private static int[] ensure(int[] a, int cap, int def) {
            if (a == null) return fill(new int[cap], def);
            if (a.length >= cap) return a;
            int[] n = new int[cap];
            System.arraycopy(a, 0, n, 0, a.length);
            if (a.length < cap) java.util.Arrays.fill(n, a.length, cap, def);
            return n;
        }
        private static int[] fill(int[] a, int v) { java.util.Arrays.fill(a, v); return a; }

        private static double[] ensure(double[] a, int cap, double def) {
            if (a == null) return fill(new double[cap], def);
            if (a.length >= cap) return a;
            double[] n = new double[cap];
            System.arraycopy(a, 0, n, 0, a.length);
            if (a.length < cap) java.util.Arrays.fill(n, a.length, cap, def);
            return n;
        }
        private static double[] fill(double[] a, double v) { java.util.Arrays.fill(a, v); return a; }

        private static float[] ensure(float[] a, int cap, float def) {
            if (a == null) return fill(new float[cap], def);
            if (a.length >= cap) return a;
            float[] n = new float[cap];
            System.arraycopy(a, 0, n, 0, a.length);
            if (a.length < cap) java.util.Arrays.fill(n, a.length, cap, def);
            return n;
        }
        private static float[] fill(float[] a, float v) { java.util.Arrays.fill(a, v); return a; }

        private static byte[] ensure(byte[] a, int cap, byte def) {
            if (a == null) return fill(new byte[cap], def);
            if (a.length >= cap) return a;
            byte[] n = new byte[cap];
            System.arraycopy(a, 0, n, 0, a.length);
            if (a.length < cap) java.util.Arrays.fill(n, a.length, cap, def);
            return n;
        }
        private static byte[] fill(byte[] a, byte v) { java.util.Arrays.fill(a, v); return a; }

        private static long[] ensure(long[] a, int cap, long def) {
            if (a == null) return fill(new long[cap], def);
            if (a.length >= cap) return a;
            long[] n = new long[cap];
            System.arraycopy(a, 0, n, 0, a.length);
            if (a.length < cap) java.util.Arrays.fill(n, a.length, cap, def);
            return n;
        }
        private static long[] fill(long[] a, long v) { java.util.Arrays.fill(a, v); return a; }

        private static int[] ensure(int[] a, int cap) { return ensure(a, cap, -1); }

        private void growIfNeeded() {
            if (size + 1 >= parent.length) {
                ensureCapacity(parent.length * 2);
            }
        }

        // Creates a new node and returns its index
        public int createNode(int parentIndex, long nodeId, double sx, double sy, double sz,
                              double dx, double dy, double dz, float radius) {
            growIfNeeded();
            int idx = size++;
            parent[idx] = parentIndex;
            firstChild[idx] = -1;
            nextSibling[idx] = -1;
            firstCP[idx] = -1;
            lastCP[idx] = -1;
            cpCount[idx] = 0;
            startX[idx] = sx; startY[idx] = sy; startZ[idx] = sz;
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len == 0) { dx = 0; dy = 1; dz = 0; len = 1; }
            dirX[idx] = dx / len; dirY[idx] = dy / len; dirZ[idx] = dz / len;
            vigor[idx] = 1.0f;
            nodeStatus[idx] = 0; // NodeStatus.BUD ordinal
            id[idx] = nodeId;
            order[idx] = (parentIndex >= 0) ? order[parentIndex] + 1 : 0;
            createdAt[idx] = 0;
            baseRadius[idx] = radius;
            upX[idx] = 0.0; upY[idx] = 1.0; upZ[idx] = 0.0;
            flags[idx] = FLAG_CAN_GROW_TALLER;

            // link into parent's child list (push at head for O(1))
            if (parentIndex >= 0) {
                nextSibling[idx] = firstChild[parentIndex];
                firstChild[parentIndex] = idx;
            }

            return idx;
        }

        // Adds a child explicitly (when you create child separately)
        public void addChild(int parentIndex, int childIndex) {
            if (parentIndex < 0 || childIndex < 0) return;
            parent[childIndex] = parentIndex;
            nextSibling[childIndex] = firstChild[parentIndex];
            firstChild[parentIndex] = childIndex;
            order[childIndex] = order[parentIndex] + 1;
        }

        // Control point helpers
        public int addControlPoint(int nodeIndex, double x, double y, double z) {
            int cpIdx = cpPool.add(x, y, z);
            if (firstCP[nodeIndex] == -1) {
                firstCP[nodeIndex] = cpIdx;
                lastCP[nodeIndex] = cpIdx;
            } else {
                cpPool.setNext(lastCP[nodeIndex], cpIdx);
                lastCP[nodeIndex] = cpIdx;
            }
            cpCount[nodeIndex]++;
            return cpIdx;
        }

        public Vec3 tip(int nodeIndex) {
            int last = lastCP[nodeIndex];
            if (last == -1) return Vec3.of(startX[nodeIndex], startY[nodeIndex], startZ[nodeIndex]);
            return cpPool.getVec3(last);
        }

        public Vec3 firstPoint(int nodeIndex) {
            int first = firstCP[nodeIndex];
            if (first == -1) return Vec3.of(startX[nodeIndex], startY[nodeIndex], startZ[nodeIndex]);
            return cpPool.getVec3(first);
        }

        // Iterate child indices (returns simple int array copy to avoid allocations per iteration)
        public int[] childrenOf(int nodeIndex) {
            int count = 0;
            for (int i = firstChild[nodeIndex]; i != -1; i = nextSibling[i]) count++;
            if (count == 0) return new int[0];
            int[] out = new int[count];
            int p = 0;
            for (int i = firstChild[nodeIndex]; i != -1; i = nextSibling[i]) out[p++] = i;
            return out;
        }

        public int nodeCount() { return size; }

        // Example small accessor setters/getters
        public void setVigor(int idx, float v) { vigor[idx] = v; }
        public float getVigor(int idx) { return vigor[idx]; }
        public void setCanGrowTaller(int idx, boolean v) { flags[idx] = v ? (flags[idx] | FLAG_CAN_GROW_TALLER) : (flags[idx] & ~FLAG_CAN_GROW_TALLER); }
        public boolean canGrowTaller(int idx) { return (flags[idx] & FLAG_CAN_GROW_TALLER) != 0; }

        // Other helpers follow your original API shape
        public double length(int idx) {
            Vec3 a = firstPoint(idx);
            Vec3 b = tip(idx);
            double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }

        // Convert control points into a list of Vec3 (creates objects for API convenience)
        public java.util.List<Vec3> getControlPointsAsList(int nodeIndex) {
            java.util.ArrayList<Vec3> out = new java.util.ArrayList<>(Math.max(2, cpCount[nodeIndex]));
            for (int i = firstCP[nodeIndex]; i != -1; i = cpPool.getNext(i)) out.add(cpPool.getVec3(i));
            return out;
        }

        // Quick toString-like representation
        public String nodeToString(int idx) {
            return String.format("{ age:%d, length:%.3f, start: %s, children: %d }",
                    createdAt[idx], length(idx), Vec3.of(startX[idx], startY[idx], startZ[idx]),
                    childrenOf(idx).length);
        }

        // --- inner control point pool ---
        private static final class ControlPointPool {
            private double[] x, y, z;
            private int[] next;
            private int size = 0;

            ControlPointPool(int cap) {
                x = new double[cap]; y = new double[cap]; z = new double[cap]; next = new int[cap]; java.util.Arrays.fill(next, -1);
            }

            private void ensure(int cap) {
                if (size + 1 < x.length) return;
                int ncap = x.length * 2;
                double[] nx = new double[ncap]; double[] ny = new double[ncap]; double[] nz = new double[ncap];
                int[] nn = new int[ncap];
                System.arraycopy(x, 0, nx, 0, x.length);
                System.arraycopy(y, 0, ny, 0, y.length);
                System.arraycopy(z, 0, nz, 0, z.length);
                System.arraycopy(next, 0, nn, 0, next.length);
                java.util.Arrays.fill(nn, next.length, nn.length, -1);
                x = nx; y = ny; z = nz; next = nn;
            }

            int add(double xx, double yy, double zz) {
                ensure(size + 1);
                int idx = size++;
                x[idx] = xx; y[idx] = yy; z[idx] = zz; next[idx] = -1;
                return idx;
            }

            void setNext(int index, int nextIndex) { next[index] = nextIndex; }
            int getNext(int index) { return next[index]; }
            Vec3 getVec3(int index) { return Vec3.of(x[index], y[index], z[index]); }
        }
    }

    // small math structs
    public static class Vec2 {
        public float x, y;

        public Vec2(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}