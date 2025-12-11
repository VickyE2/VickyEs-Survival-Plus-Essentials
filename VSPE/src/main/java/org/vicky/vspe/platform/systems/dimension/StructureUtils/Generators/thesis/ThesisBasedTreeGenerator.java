package org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.thesis;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vicky.platform.utils.Vec3;
import org.vicky.utilities.ContextLogger.ContextLogger;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.CurveFunctions;
import org.vicky.vspe.platform.systems.dimension.TimeCurve;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.clamp;

/**
 * World-agnostic, logic-focused tree generator.
 * Replace DefaultEnvironment with your own GrowthEnvironment for custom bounds/directions/seed.
 */
public class ThesisBasedTreeGenerator {
    private static final int MAX_TOTAL_NODES = 2000;
    private static final double OWNERSHIP_CAPACITY_BASE = 5.0;   // base capacity multiplier per (1.0 vigor)
    private static final double STEAL_THRESHOLD = 1.25;          // challenger must have >= 1.25 * minScore to steal
    private static final double DISTANCE_DECAY = 2.1;            // distance decay exponent (higher -> favors closer tips)
    private static final double SEN_EPS = 1e-6;
    private static final Logger log = LoggerFactory.getLogger(ThesisBasedTreeGenerator.class);
    @NotNull
    protected final GrowthData growthData;
    private final Map<Long, TreeNode> cachedBranches = new HashMap<>();
    private final Vec3 global_gravity;
    private final boolean debug = true;
    private final ContextLogger logger;
    int send = 0;
    private long lastNodeId = 0;
    private int age = 0;
    private long seed = 0;
    private float totalEnergy = 0.0f;
    private List<Vec3> attractorPool = new ArrayList<>();
    private double lastCanopyTopY = Double.NEGATIVE_INFINITY;
    private int ticksSinceAttractorRefresh = 0;
    private TreeNode root = null;
    private float maxRadius;
    private double influenceRadiusAdaptive;
    private double vigorRadiusAdaptive;
    private double killRadiusAdaptive;

    // === Public API ===
    private double targetAge;


    public ThesisBasedTreeGenerator(@NotNull GrowthData growthData, long seed, ContextLogger logger) {
        this.growthData = growthData;
        this.seed = seed;
        this.logger = logger;
        if (growthData.overrides.contains(Overrides.GlobalOverrides.OVERRIDE_GRAVITY)) {
            this.global_gravity = growthData.overriden_gravity;
        } else {
            this.global_gravity = Vec3.of(0.0, -1.0, 0.0);
        }
    }

    public static Vec3 computeTreeCentroid(Collection<TreeNode> nodes) {
        if (nodes.isEmpty()) return new Vec3(0, 0, 0);

        double x = 0, y = 0, z = 0;
        for (TreeNode node : nodes) {
            x += node.startPos.x;
            y += node.startPos.y;
            z += node.startPos.z;
        }

        int n = nodes.size();
        return new Vec3(x / n, y / n, z / n);
    }

    public static List<List<TreeNode>> sortByLevels(TreeNode root) {
        List<List<TreeNode>> levels = new ArrayList<>();
        if (root == null) return levels;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            int size = queue.size();
            List<TreeNode> currentLevel = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                currentLevel.add(node);

                if (node != null) {
                    queue.addAll(node.children);
                }
            }

            levels.add(currentLevel);
        }

        return levels;
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
        totalEnergy = root.vigor;
        cachedBranches.put(root.id, root);
        if (debug) {
            log("initRoot -> " + nodeShort(root) + " startPos=" + startPos);
        }
        log("Tree exhibits: " + String.join(", ",
                growthData.overrides.stream().map(Objects::toString).toArray(String[]::new)), true);
    }

    public TreeNode getRoot() {
        return root;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int newAge) {
        this.age = newAge;
    }

    /**
     * Simulate from current age up to targetAge (inclusive of each tick).
     * Each tick runs: accumulateLight, distributeVigor, addShoots, recalc child counts, shed.
     */
    public void simulateToAge(int targetAge) {
        if (root == null) throw new IllegalStateException("Root not initialized");
        if (targetAge <= age) return;
        this.targetAge = targetAge;
        for (int t = age + 1; t <= targetAge; t++) {
            age = t;
            simulateTick();
        }
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
        for (int t = age + 1; t <= targetAge; t++) {
            age = t;
            simulateTick();
        }
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
        if (root == null) return;
        float totalVigor = accumulateSubtreeVigor(root);
        root.baseRadius = Math.min(
                maxRadius,
                growthData.minRadius + (float) Math.log1p(totalVigor) * 0.5f
        );

        double currentHeight = Math.max(8.0, root.tip().distance(root.firstPoint()) * 0.5);

        influenceRadiusAdaptive = growthData.influenceRadius + currentHeight * 0.25;
        vigorRadiusAdaptive = growthData.vigorRadius + currentHeight * 0.16;
        killRadiusAdaptive = growthData.killRadius + currentHeight * 0.10;

        ensureattractorPoolUpToDate();

        for (TreeNode n : cachedBranches.values()) {
            n.vigor *= growthData.vigorDecay; // 2% decay per tick
        }

        log("=== simulateTick START age=" + age + " ===");

        if (root.vigor <= 0.4f) {
            root.vigor = growthData.initialVigor * 0.43f;
            // log("⚠️ Root had zero vigor; assigning initialVigor=" + growthData.initialVigor);
        } else {
            log("🌱 Root vigor=" + root.vigor);
        }

        // logStage("Growth / Shoots");
        addShoots(root);
        // logTreeStructure(root, "> ");

        // logStage("Shedding");
        shedBranches(root);
        // logTreeStructure(root, ">> ");

        logTickSummary();
        dumpSummary();
        log("=== simulateTick END ===\n");
    }

    private void ensureattractorPoolUpToDate() {
        Vec3 top = computeHighestBranch(cachedBranches.values());
        double topY = top.y;
        double canopyMoved = Math.abs(topY - lastCanopyTopY);

        // regenerate if tree lifted a lot, or every N ticks
        if (attractorPool.isEmpty() || canopyMoved > growthData.baseLength * 2 || ticksSinceAttractorRefresh > 40) {
            attractorPool = produceAttractorPointsAdaptive();
            lastCanopyTopY = topY;
            ticksSinceAttractorRefresh = 0;
        } else {
            ticksSinceAttractorRefresh++;
            // small partial replenishment if pool shrinks
            if (attractorPool.size() < 30) {
                attractorPool.addAll(produceAttractorPointsAdaptiveSmallBatch());
            }
        }
    }

    private List<Vec3> produceAttractorPointsAdaptiveSmallBatch() {
        // call the full generator but with fewer points/clusters or copy some with jitter
        List<Vec3> small = produceAttractorPointsAdaptive();
        return small.subList(0, Math.min(20, small.size()));
    }

    private void shedBranches(TreeNode root) {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            var current = queue.poll();
            log("Shreading -> " + current.id);

            if (current.nodeStatus == NodeStatus.DEAD) {
                log("Shreaded -> " + current.id);
                current.parent.children.remove(current);
                current.children.forEach(it -> it.nodeStatus = NodeStatus.DEAD);
            }

            queue.addAll(current.children);
        }
    }

    private void addShoots(TreeNode root) {
        if (age > 10 && send < 1) {
            log("attractorPool: " + attractorPool);
            send++;
        }
        pruneConsumedattractorPool(root, attractorPool);
        if (age > 10 && send < 2) {
            log("attractorPoolAfter: " + attractorPool);
            send++;
        }
        Map<TreeNode, List<Vec3>> owned = assignAttractorsToTips(attractorPool);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            var current = queue.poll();
            current.vigor = Math.min(1.0f, current.vigor);
            current.baseRadius = Math.min(maxRadius, current.baseRadius);

            double allowedKillDist;
            if (current.order == 0) {
                allowedKillDist = killRadiusAdaptive * 0.6;
            }
            else {
                allowedKillDist = killRadiusAdaptive;
            }

            log("Polling -> " + current.id);
            logTreeStructure(current, ">> ");
            Random rnd = new Random(seed ^ (current.id * 31L) ^ (this.age * 7919L));
            cachedBranches.putIfAbsent(current.id, current);
            if (current.order == 0) {
                Vec3 last = current.tip();
                current.nodeStatus = NodeStatus.ALIVE;
                Vec3 lightBias = growthData.lightDirection.normalize();

                if (age == growthData.multiTrunkismAge
                        && growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM)
                        && current == root) {
                    log("MultiTrunkism Enabled for Root Node");
                    current.canGrowTaller = false;
                    Vec3 tip = current.getControlPoints().getLast();

                    // choose number of trunks (tweak: use config or baseRadius)
                    Random mRnd = new Random(seed ^ (current.id * 97L) ^ (this.age * 7919L));
                    int defaultN = 3; // fallback
                    int n = growthData.multiTrunkismMaxAmount > 1 ? mRnd.nextInt(1, growthData.multiTrunkismMaxAmount + 1)
                            : Math.max(defaultN, Math.min(6, Math.round(current.baseRadius))); // clamp 2..6

                    // area-conserve thickness: sum(pi r_i^2) = pi R^2  -> r_i = R / sqrt(n) for equal children
                    double parentRadius = current.baseRadius;
                    double parentArea = Math.PI * parentRadius * parentRadius;
                    double childArea = parentArea / (double) n;
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
                        Vec3 budOrigin = tip;

                        TreeNode trunk = new TreeNode(current, lastNodeId++, budOrigin, dir, childRadius);
                        trunk.vigor = current.vigor * (0.9f * (float) Math.sqrt(growthData.vigorDecay)); // still healthy but a little less
                        trunk.createdAt = age;

                        current.children.add(trunk);
                        cachedBranches.putIfAbsent(trunk.id, trunk);
                    }

                    // Optionally reduce root vigor and radius so mass is conserved and root doesn't keep outcompeting
                    current.vigor *= 0.5f;
                    current.baseRadius *= 0.6f; // root becomes a stubbier base after splitting

                    // prevent the root from creating more children in the same tick (it now only acts as base)
                    current.nodeStatus = NodeStatus.ALIVE; // keep alive for thickness contribution if needed
                    queue.addAll(current.children);

                }

                if (current.canGrowTaller) {
                    Vec3 tropismTarget = lightBias.multiply(growthData.phototropism)
                            .add(global_gravity.multiply(growthData.gravitropism))
                            .normalize();

                    double angle = current.direction.angleTo(tropismTarget);

                    double bendStrength = Math.pow(angle / Math.PI, 1.5) * 0.25; // smaller = subtler bend
                    bendStrength *= growthData.flexibility; // optional species-level parameter

                    // rotate direction slightly toward tropismTarget
                    Vec3 newDir = current.direction.lerp(tropismTarget, bendStrength).normalize();

                    // add slight random noise for natural asymmetry
                    Vec3 noise = Vec3.randomUnit(rnd).multiply(0.05 * (1.0 - growthData.straightness));
                    newDir = newDir.add(noise).normalize();

                    // now extend
                    Vec3 nextPoint = last
                            .add(newDir.multiply(growthData.baseLength * Math.max(0.02, current.vigor)));
                    log("CurrPoint: " + last + ", New Point: " + nextPoint);
                    log("Length: " + growthData.baseLength);
                    current.controlPoints.add(nextPoint);
                    current.direction = newDir;
                }

                var localPool = owned.getOrDefault(current, new ArrayList<>());
                long nearbyattractorPool = localPool.size();
                boolean rndChance = rnd.nextDouble() < growthData.budProbability;
                log(Arrays.toString(current.children.toArray()));
                boolean childrenNotClose = current.children.isEmpty() || current.children.getLast().firstPoint()
                        .distance(current.getControlPoints().getLast()) > growthData.distanceBetweenChildren;
                logf("attractorPool: %s %s, ChildSize: %s, RndChance: %s, Distance: %s%n", nearbyattractorPool, nearbyattractorPool > 3, current.children.size() < growthData.maxKids, rndChance, childrenNotClose);
                if (nearbyattractorPool > 3 && rndChance && childrenNotClose && age > (
                        growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM) ?
                                growthData.multiTrunkismAge + growthData.minSplittingAge : growthData.minSplittingAge)
                        && current.children.size() < growthData.maxKids && current.canGrowTaller) { // 20% chance per growth cycle
                    Vec3 budOrigin = current.getControlPoints().getLast();
                    Vec3 attractionDir = new Vec3(0, 0, 0);
                    int count = 0;
                    for (Vec3 attractor : localPool) {
                        double dist = attractor.distance(budOrigin);
                        if (dist < influenceRadiusAdaptive * 1.5) {
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
                    var dividor = CurveFunctions.radius(current.baseRadius, 0.0, 0.0, 1.0, growthData.forTrunk);
                    TreeNode branch = new TreeNode(current, lastNodeId++, budOrigin, attractionDir, dividor.apply(0.90).floatValue());
                    branch.vigor = current.vigor * (growthData.vigorDecay * 0.8f);
                    branch.createdAt = age;
                    current.children.add(branch);
                    if (growthData.overrides.contains(Overrides.BranchOverrides.MIRROR_BRANCHES)) {
                        TreeNode mirrored_branch = new TreeNode(current, lastNodeId++, budOrigin, attractionDir.multiply(-1), dividor.apply(0.90).floatValue());
                        mirrored_branch.vigor = current.vigor * (growthData.vigorDecay * 0.8f);
                        mirrored_branch.createdAt = age;
                        current.children.add(mirrored_branch);
                    }
                    current.vigor *= (growthData.vigorDecay * 0.8f);
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

                queue.addAll(current.children);
            }
            else {
                if (Math.abs(current.firstPoint().distance(current.parent.tip())) /
                        Math.abs(current.parent.tip().distance(current.parent.firstPoint())) > growthData.pruningHeight
                        && current.parent.children.size() > 3) current.nodeStatus = NodeStatus.DEAD;
                if (current.nodeStatus == NodeStatus.DEAD) continue;


                Vec3 tip = current.getControlPoints().getLast();
                current.nodeStatus = NodeStatus.ALIVE;

                // --- Get attractorPool affecting this node ---
                List<Vec3> localattractorPool = owned.getOrDefault(current, Collections.emptyList());

                // use localAttractors for vigor updates, direction, and bud logic
                // also compute close attractors from localAttractors:
                List<Vec3> closeattractorPool = localattractorPool.stream()
                        .filter(a -> a.distance(current.tip()) < vigorRadiusAdaptive)
                        .toList();

                // --- Determine attraction direction ---
                // ---------- Improved attraction + individuality + turn-limiting ----------
                Vec3 attractionTarget;
                double sen;
                if (current.order > 1) sen = senescenceFactor(current.parent);
                else sen = senescenceFactor(current);
                sen = Math.min(1.0, sen);

                if (!localattractorPool.isEmpty()) {
                    // Distance-weighted (inverse-square) averaging toward attractor cloud
                    Vec3 sum = new Vec3(0, 0, 0);
                    double totalWeight = 0.0;
                    for (Vec3 a : localattractorPool) {
                        Vec3 toA = a.subtract(tip);
                        double dist = Math.max(0.0001, toA.length());
                        double weight = 1.0 / (dist * dist); // inverse-square falloff (tweak if needed)
                        sum = sum.add(toA.multiply(weight)); // preserve magnitude so closer ones dominate
                        totalWeight += weight;
                    }
                    Vec3 weightedAvg = totalWeight > 0 ? sum.divide(totalWeight).normalize() : current.direction.normalize();

                    // Strength scaling with senescence (you want attractors weaker when sen large)
                    double attractorStrength = Math.max(0.0, 1.0 - Math.pow(sen, 2)); // keep but tweak curve if needed
                    // Make the attraction a gentle blend (smaller than before) to avoid fast convergence
                    double attractorBlend = 0.15 + 0.35 * attractorStrength; // 0.15..0.5 depending on sen
                    attractorBlend = Math.min(0.85, Math.max(0.05, attractorBlend));

                    // Compose target as a blend between inertia (current.direction) and weightedAvg
                    Vec3 baseAttraction = current.direction.normalize().lerp(weightedAvg, attractorBlend).normalize();

                    // small per-node deterministic bias to break symmetry (stable across runs)
                    Random perNode = new Random(current.id * 31L + 17L); // deterministic per-node seed
                    Vec3 nodeBias = Vec3.randomUnit(perNode).multiply(0.03 * (1.0 - growthData.straightness));
                    // Slightly reduce vertical bias if senescent (keeps droop behavior)

                    attractionTarget = new Vec3(baseAttraction.x, baseAttraction.y * Math.sqrt(1.0 - sen), baseAttraction.z).normalize().add(nodeBias).normalize();

                    // ---- vigor and close attractor logic (unchanged mostly but keep distance weighting) ----
                    double baseGain = 0.01 * localattractorPool.size();
                    double effectiveGain = baseGain * (1.0 - sen * growthData.senescenceVigorPenalty);
                    effectiveGain = Math.max(0.0, effectiveGain);
                    double closeFactor = 1.0 + 0.5 * closeattractorPool.size();
                    closeFactor = Math.min(closeFactor, 3.0);
                    current.vigor += (float) (effectiveGain * closeFactor);
                    current.vigor = Math.min(current.vigor, 1.0f);
                } else {
                    // No attractors: keep inertia but more droop with senescence
                    attractionTarget = current.direction.normalize();
                    if (sen < 0.32) {
                        double decayMultiplier = 1.0 + sen * growthData.senescenceDecayRate;
                        current.vigor *= (float) (growthData.vigorDecay * decayMultiplier / 2.0f);
                    } else {
                        float decayRate = growthData.vigorDecay;
                        float deltaTime = 0.076f;
                        current.vigor *= (float) Math.exp(-decayRate * deltaTime);
                    }
                }

                Vec3 parentDir = current.parent.direction.normalize();
                Vec3 worldUp = global_gravity;
                Vec3 temp = Math.abs(parentDir.dot(worldUp)) > 0.98 ? Vec3.of(1, 0, 0) : worldUp;
                Vec3 localRight = parentDir.crossProduct(temp);
                if (localRight.length() < 1e-6) localRight = parentDir.crossProduct(Vec3.of(0, 0, 1));
                Vec3 localUp = localRight.crossProduct(parentDir).normalize();
                if (!Double.isFinite(localUp.x) || !Double.isFinite(localUp.y) || !Double.isFinite(localUp.z))
                    localUp = worldUp;
                current.localUp = localUp;

                Vec3 upDir = current.localUp.lerp(worldUp, 0.05).normalize();
                double easedSen = Math.pow(sen, 1.5);
                Vec3 attractionUpBlend = attractionTarget.lerp(upDir, 0.3 * (1.0 - easedSen)).normalize();

                double inertia = Math.pow(1.0 - sen, 2.0);
                double smoothLerp = clamp(0.12 + 0.38 * (1.0 - inertia), 0.06, 0.6); // tuned lower than before
                Vec3 desiredDir = current.direction.normalize().lerp(attractionUpBlend, smoothLerp).normalize();

                double gravLerp = clamp(sen * growthData.senescenceGravBias, 0.0, 1.0);
                Vec3 gravityAdjusted = desiredDir.lerp(worldUp.multiply(-1), gravLerp).normalize();

                double maxTurnDeg = 20.0; // tweak: 10..30 are sensible ranges
                double maxTurn = Math.toRadians(maxTurnDeg);
                double angle = Math.abs(current.direction.angleTo(gravityAdjusted));
                Vec3 limitedDir;
                if (angle > 1e-6 && angle > maxTurn) {
                    double t = maxTurn / angle; // fraction to move toward target (approx slerp)
                    t = clamp(t, 0.01, 1.0);
                    limitedDir = current.direction.lerp(gravityAdjusted, t).normalize();
                } else {
                    limitedDir = gravityAdjusted;
                }

                Vec3 orthNoise = Vec3.randomUnit(rnd)
                        .subtract(limitedDir.multiply(Vec3.randomUnit(rnd).dot(limitedDir)))
                        .normalize()
                        .multiply(0.02 * (1.0 - growthData.straightness)); // smaller than before

                Vec3 finalDir = limitedDir.add(orthNoise).normalize();

                Vec3 nextPoint = tip.add(finalDir.multiply(growthData.baseLength * current.vigor));
                current.direction = finalDir;

                if (current.canGrowTaller) {
                    current.controlPoints.add(nextPoint);
                }


                // --- Branch death logic ---
                if (current.vigor < 0.05f && current.children.size() < 2) {
                    current.nodeStatus = NodeStatus.DEAD;
                    continue;
                }

                // --- Bud creation (branching) ---
                boolean childrenNotClose = current.children.isEmpty() ||
                        current.children.getLast().firstPoint()
                                .distance(current.getControlPoints().getLast()) > (growthData.distanceBetweenChildren * (current.order + 1));

                long nearbyattractorPool = localattractorPool.size();
                double effectiveBudProb = growthData.budProbability * (1.0 - sen * growthData.senescenceBudPenalty);
                effectiveBudProb = Math.max(0.01, effectiveBudProb); // never zero if you want some chance
                boolean rndChance = rnd.nextDouble() < (effectiveBudProb * current.vigor);
                boolean age = this.age - current.createdAt > growthData.minSplittingAge;
                log(String.format("Child info >> attractorPool: %s %s, Age: %s ChildSize: %s, RndChance: %s, Distance: %s, Order: %s, Sen: %s", nearbyattractorPool, nearbyattractorPool > 3, age, current.children.size() < growthData.maxKids, rndChance, childrenNotClose, current.order < growthData.maxDepth, sen));
                if (nearbyattractorPool > 8 && childrenNotClose
                        && rndChance && age
                        && current.children.size() < growthData.maxKids * (1.0 + 0.5 * Math.log1p(1.0 / current.order))
                        && current.order < growthData.maxDepth && current.vigor > 0.75) {

                    Vec3 attractionDir = new Vec3(0, 0, 0);
                    int count = 0;
                    for (Vec3 a : localattractorPool) {
                        double dist = a.distance(nextPoint);
                        if (dist < influenceRadiusAdaptive * 1.5) {
                            double weight = 1.0 / (dist + 0.001);
                            attractionDir = attractionDir.add(a.subtract(nextPoint).normalize().multiply(weight));
                            count++;
                        }
                    }

                    if (count > 0)
                        attractionDir = attractionDir.divide(count).normalize();
                    else
                        attractionDir = current.direction.randomPerturbated(rnd, growthData.lateralAngleDegrees);

                    TreeNode bud = new TreeNode(current, lastNodeId++, nextPoint, attractionDir, current.baseRadius * 0.75f);
                    bud.vigor = Math.min(1.0f, (float) (current.vigor * Math.sqrt(growthData.vigorDecay)));
                    bud.createdAt = this.age;

                    log("Adding new branch toward attractorPool: dir -> " + attractionDir);

                    current.children.add(bud);
                    queue.addAll(current.children);

                    // Consume attractorPool near the bud
                    List<Vec3> consumedByBud = attractorPool.stream()
                            .filter(a -> a.distance(tip) < allowedKillDist)
                            .toList();
                    attractorPool.removeAll(consumedByBud);
                } else {
                    queue.addAll(current.children);
                }
                attractorPool.removeAll(attractorPool.stream()
                        .filter(a -> a.distance(tip) < allowedKillDist)
                        .toList());
            }

            // --- 1) Optional energy transfer between parent/children (unchanged logic) ---
            for (TreeNode child : current.children) {
                if (current.vigor < 0.75 && child.vigor > 0.78) {
                    current.vigor += (child.vigor * 0.08f);
                    child.vigor -= (child.vigor * (0.02f * growthData.vigorDecay));
                }

                if (child.height() / current.height() < 0.35) {
                    child.vigor += (float) Math.sqrt(current.vigor);
                }

                double parentLength = current.firstPoint().distance(current.tip());
                double relativeHeight = parentLength > 0
                        ? child.firstPoint().distance(current.firstPoint()) / parentLength
                        : 0.0;

                double heightFactor = Math.pow(1.0 - relativeHeight, 0.9); // smoother
                if (growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM) &&
                        (current.order == 0 || current == root)) heightFactor = 1.0;
                double vigorBoost = 0.8 + Math.pow(child.vigor, 0.85); // more generous
                float targetChildRadius = (float) (current.baseRadius * heightFactor * vigorBoost * 0.3);

                // Smooth growth instead of clamp
                float lerpRate = 0.25f + 0.1f * (float) Math.sqrt(child.vigor);
                child.baseRadius += (targetChildRadius - child.baseRadius) * lerpRate;

                // Keep within reasonable bounds
                float maxPossible = current.baseRadius * 0.9f;
                child.baseRadius = Math.max(growthData.minRadius,
                        Math.min(child.baseRadius, maxPossible));

                log(String.format("id=%s, hf=%.3f, vb=%.3f, target=%.3f, cbr=%.3f",
                        child.id, heightFactor, vigorBoost, targetChildRadius, child.baseRadius));
            }
        }
    }

    private void pruneConsumedattractorPool(TreeNode root, List<Vec3> attractorPool) {
        // collect all attractorPool that should be removed
        Set<Vec3> toRemove = new HashSet<>();
        collectConsumedattractorPool(root, attractorPool, toRemove);

        // remove in one go for efficiency
        attractorPool.removeAll(toRemove);
    }

    private void collectConsumedattractorPool(TreeNode node, List<Vec3> attractorPool, Set<Vec3> toRemove) {
        if (node.nodeStatus == NodeStatus.DEAD) return;

        Vec3 tip = node.getControlPoints().getLast();

        for (Vec3 a : attractorPool) {
            if (a.distance(tip) < killRadiusAdaptive) {
                toRemove.add(a);
            }
        }

        // recurse into children
        for (TreeNode child : node.children) {
            collectConsumedattractorPool(child, attractorPool, toRemove);
        }
    }

    private List<Vec3> produceAttractorPointsAdaptive() {
        Random rand = new Random(seed ^ age);
        List<Vec3> attractorPool = new ArrayList<>();

        Vec3 top = computeHighestBranch(cachedBranches.values());
        // canopy scale derived from trunk height and max branch reach
        double height = Math.max(4.0, Math.abs(root.tip().distance(root.firstPoint())));
        double canopyRadius = Math.max(8.0, height * 3.8);
        if (growthData.overrides.contains(Overrides.TrunkOverrides.MULTI_TRUNKISM)) {
            canopyRadius *= Math.pow(growthData.multiTrunkismMaxAmount, 0.5);
        }
        double canopyHeight = Math.max(6.0, height * 0.5);

        int numClusters = Math.max(8, (int) Math.round(canopyRadius / 4.0)); // more clusters with bigger canopy
        int pointsPerCluster = Math.max(15, (int) (Math.min(80, canopyRadius)));
        double ageFactor = CurveFunctions.noised(0.0, 1.0,
                        0.0, 1.0, TimeCurve.INVERTED_QUADRATIC, rand)
                .apply((double) age / (targetAge - 1));

        Vec3 lightDir = growthData.lightDirection.normalize();

        for (int c = 0; c < numClusters; c++) {
            double clusterAngle = 2 * Math.PI * rand.nextDouble();
            double innerBias = 1.0 - ageFactor; // 1 at young, 0 at mature
            double clusterRad = canopyRadius * (0.1 + 0.9 * rand.nextDouble() * ageFactor)
                    * (0.5 + 0.5 * (1.0 - innerBias * rand.nextDouble()));
            double cx = Math.cos(clusterAngle) * clusterRad;
            double cz = Math.sin(clusterAngle) * clusterRad;
            double cy = (rand.nextDouble() - 0.2) * canopyHeight * (0.4 + 0.6 * ageFactor);

            Vec3 clusterCenter = top.add(new Vec3(cx, cy, cz));
            clusterCenter = clusterCenter.add(lightDir.multiply(canopyHeight * 0.25 * rand.nextDouble())); // mild tilt

            for (int i = 0; i < pointsPerCluster; i++) {
                // radial jitter inside cluster
                double rr = clusterRad * 0.15 * Math.pow(rand.nextDouble(), 0.9);
                double theta = 2 * Math.PI * rand.nextDouble();
                double phi = Math.acos(2 * rand.nextDouble() - 1);

                double x = rr * Math.sin(phi) * Math.cos(theta);
                double y = rr * Math.cos(phi);
                double z = rr * Math.sin(phi) * Math.sin(theta);

                Vec3 jitterDir = new Vec3(x, y, z).normalize();
                // double biasFactor = 0.15 + 0.6 * Math.pow(rand.nextDouble(), 2);
                double biasFactor = 0.15 + 0.6 * Math.pow(rand.nextDouble(), 2) * ageFactor;
                Vec3 finalDir = jitterDir.lerp(lightDir, biasFactor).add(Vec3.randomUnit(rand).multiply(0.08)).normalize();

                double pointDist = 2 + rand.nextDouble() * canopyHeight * (0.6 + 0.4 * rand.nextDouble());
                Vec3 p = clusterCenter.add(finalDir.multiply(pointDist));
                attractorPool.add(p);
            }
        }
        return attractorPool;
    }

    private boolean attractorVisibleFromTip(Vec3 tip, Vec3 tipDir, Vec3 attractor, double coneDegrees, double maxDist) {
        Vec3 to = attractor.subtract(tip);
        double dist = to.length();
        if (dist > maxDist) return false;
        double cos = tipDir.normalize().dot(to.normalize());
        double cosThresh = Math.cos(Math.toRadians(coneDegrees));
        return cos >= cosThresh;
    }

    private Vec3 computeHighestBranch(Collection<TreeNode> nodes) {
        Vec3 top = new Vec3(0, Double.NEGATIVE_INFINITY, 0);
        for (TreeNode n : nodes) {
            if (n.tip().y > top.y) top = n.tip();
        }
        return top;
    }

    private void logTreeStructure(TreeNode node, String prefix) {
        if (node == null) return;
        log(prefix + nodeShort(node));
        node.children.forEach(it -> logTreeStructure(it, prefix + ">> child "));
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

    private void log(String s) {
        log (s, false);
    }

    private void log(String s, boolean mustPrint) {
        if (debug || mustPrint) logger.print("ThesisTree@" + age + " " + s, ContextLogger.LogType.AMBIENCE);
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
     * Assign each attractor to the best visible tip (TreeNode) according to a score.
     * Returns a map: TreeNode -> list of attractors it owns this tick.
     * Uses a small per-tip capacity with a steal rule to avoid single-tip hogging.
     */
    private Map<TreeNode, List<Vec3>> assignAttractorsToTips(List<Vec3> pool) {
        // Map tip -> currently owned [AttractorScore ...]
        Map<TreeNode, List<AttractorScore>> scoredPerTip = new HashMap<>();

        // build list of candidate tips (skip DEAD)
        List<TreeNode> tips = cachedBranches.values().stream()
                .filter(n -> n.nodeStatus != NodeStatus.DEAD)
                .toList();
        if (tips.isEmpty() || pool.isEmpty()) return Collections.emptyMap();

        // precompute tip positions & directions to avoid repeated calls
        for (Vec3 attractor : pool) {
            // gather candidates that can see this attractor
            List<Candidate> candidates = new ArrayList<>();
            for (TreeNode tip : tips) {
                Vec3 tipPos = tip.tip();
                double dist = tipPos.distance(attractor);
                if (dist > influenceRadiusAdaptive) continue; // out of reach

                // optionally require some cone visibility — widish cone so more competition:
                if (!attractorVisibleFromTip(tipPos, tip.direction, attractor, 140.0, influenceRadiusAdaptive)) {
                    continue;
                }

                double score = computeAttractorScore(tip, attractor, dist);
                candidates.add(new Candidate(tip, score));
            }

            if (candidates.isEmpty()) continue;

            // pick best candidate (highest score)
            candidates.sort((a, b) -> Double.compare(b.score, a.score));
            Candidate best = candidates.getFirst();
            TreeNode bestTip = best.tip;
            double bestScore = best.score;

            // capacity: proportional to vigor (0..1) but always at least 1
            int capacity = Math.max(1, (int) Math.ceil((1.0 + bestTip.vigor) * OWNERSHIP_CAPACITY_BASE));

            // ensure list exists
            scoredPerTip.putIfAbsent(bestTip, new ArrayList<>());
            List<AttractorScore> owned = scoredPerTip.get(bestTip);

            if (owned.size() < capacity) {
                owned.add(new AttractorScore(attractor, bestScore));
                continue;
            }

            // already full — check the weakest currently owned attractor
            int minIndex = 0;
            double minScore = Double.POSITIVE_INFINITY;
            for (int i = 0; i < owned.size(); i++) {
                if (owned.get(i).score < minScore) {
                    minScore = owned.get(i).score;
                    minIndex = i;
                }
            }

            // challenger can steal only if clearly better
            if (bestScore >= minScore * STEAL_THRESHOLD) {
                AttractorScore removed = owned.remove(minIndex);
                owned.add(new AttractorScore(attractor, bestScore));

                // attempt to place removed attractor to the next-best candidate (naive)
                Candidate nextBest = null;
                for (Candidate c : candidates) {
                    if (c.tip != bestTip) {
                        if (nextBest == null || c.score > nextBest.score) nextBest = c;
                    }
                }
                if (nextBest != null) {
                    scoredPerTip.putIfAbsent(nextBest.tip, new ArrayList<>());
                    List<AttractorScore> nextOwned = scoredPerTip.get(nextBest.tip);
                    int nextCap = Math.max(1, (int) Math.ceil((1.0 + nextBest.tip.vigor) * OWNERSHIP_CAPACITY_BASE));
                    if (nextOwned.size() < nextCap) {
                        nextOwned.add(new AttractorScore(removed.attractor, nextBest.score));
                    }
                    // otherwise it's dropped for this tick (will be available next tick)
                }
            }
            // else challenger loses — no change
        }

        // convert to Map<TreeNode, List<Vec3>>
        Map<TreeNode, List<Vec3>> perTip = new HashMap<>();
        for (Map.Entry<TreeNode, List<AttractorScore>> e : scoredPerTip.entrySet()) {
            List<Vec3> list = e.getValue().stream().map(as -> as.attractor).collect(Collectors.toList());
            perTip.put(e.getKey(), list);
        }
        return perTip;
    }

    /**
     * Score function — favors high vigor, closeness, and good alignment.
     * Tunable. Returns positive double where higher is better.
     */
    private double computeAttractorScore(TreeNode tip, Vec3 attractor, double dist) {
        double vigorFactor = 0.01 + tip.vigor; // small floor so tiny vigor still competes
        // distanceFactor: closer → higher (range ~ (0,1] ), use power decay
        double dNorm = Math.max(1e-5, dist / Math.max(1.0, influenceRadiusAdaptive));
        double distanceFactor = Math.pow(1.0 - Math.min(1.0, dNorm), DISTANCE_DECAY); // 1.0 at dist=0, 0 at max

        // angular factor: dot of tip.direction and direction->attractor (0..1)
        Vec3 to = attractor.subtract(tip.tip()).normalize();
        double dot = Math.max(0.0, tip.direction.normalize().dot(to)); // negative alignment considered 0
        double angularFactor = 0.5 + 0.5 * dot; // between 0.5 and 1.0 (so alignment matters but not fully decisive)

        // combine
        double score = vigorFactor * (1.2 * distanceFactor + 0.4 * angularFactor);
        // optionally boost near attractors slightly
        score *= (1.0 + (1.0 - dNorm) * 0.25);
        return score;
    }

    private double senescenceFactor(TreeNode node) {
        if (node == null) return 0.0;
        if (!growthData.senescenceAffectsChildren && node.order > 0) return 0.0;

        double ageSince = Math.max(0, this.age - node.createdAt);
        if (ageSince <= growthData.senescenceStartAge) return 0.0;

        // time since senescence actually started
        double senAge = ageSince - growthData.senescenceStartAge;

        // ramp duration (how long until fully senescent)
        double rampDur = Math.max(1.0, targetAge / 5);

        // normalize into [0..1] range of total senescence timeline
        double t = Math.min(1.0, senAge / rampDur);

        // Exponential ease-in (slow start, rapid late)
        // The 3.5 constant controls curve steepness: 3.5 = quite sharp, 2.0 = smoother
        double shaped = 1.0 - Math.exp(-t * 3.5);

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

    // === Supporting types ===

    public interface Overrides {
        enum BranchOverrides implements Overrides {
            MIRROR_BRANCHES,
        }
        enum TrunkOverrides implements Overrides {
            MULTI_TRUNKISM,
        }
        enum GlobalOverrides implements Overrides {
            OVERRIDE_GRAVITY
        }
    }

    // helper classes for the assignment code (local static-like)
    private record Candidate(TreeNode tip, double score) {
    }

    private static class AttractorScore {
        final Vec3 attractor;
        final double score;

        AttractorScore(Vec3 a, double s) {
            this.attractor = a;
            this.score = s;
        }
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
        public List<Overrides> overrides = new ArrayList<>();
        public float apicalControl = 0.6f;
        public Vec3 lightDirection = new Vec3(0f, 1.0f, 0f);
        public Vec3 overriden_gravity = new Vec3(0f, -1.0f, 0f);
        public float gravitropism = 0.76f;
        public float phototropism = 1.0f;
        public float lateralAngleDegrees = 20.0f;
        public float shedMultiplier = 0.5f;
        public float initialVigor = 1.0f;
        public float baseBudLight = 0.24f;
        public float straightness = 0.37f;
        public float flexibility = 0.95f;
        public int minAgeBeforeShed = 10;
        public double influenceRadius = 25.0;   // how far a branch can “see” light
        public double vigorRadius = 10.5;   // how far a branch can “see” light
        public double killRadius = 3.5;        // distance at which attractor is considered reached
        public double budProbability = 0.86;    // how likely a bud forms when vigor is high
        public float vigorDecay = 0.95f;
        public int maxKids = 15;
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
        public int senescenceStartAge = 5;
        public double senescenceDecayRate = 0.03;
        public double senescenceVigorPenalty = 0.55;
        public double senescenceGravBias = 0.4;
        public double senescenceBudPenalty = 0.6;
        public double senescenceChildAttenuation = 1.0;
        public boolean senescenceAffectsChildren = true;
        public int multiTrunkismAge = 8;
        public int multiTrunkismMaxAmount = 4;

        // constructor for quick creation
        public GrowthData() {
            baseLength = 1.0f;
        }

        public GrowthData(float apicalControl, float baseLength, Vec2 directionWeights) {
            this.apicalControl = apicalControl;
            this.baseLength = baseLength;
        }

        public GrowthData(float apicalControl, float baseLength) {
            this.apicalControl = apicalControl;
            this.baseLength = baseLength;
        }

        private GrowthData(Builder builder) {
            apicalControl = builder.apicalControl;
            baseLength = builder.baseLength;
            lightDirection = builder.lightDirection;
            overriden_gravity = builder.overriden_gravity;
            gravitropism = builder.gravitropism;
            phototropism = builder.phototropism;
            lateralAngleDegrees = builder.lateralAngleDegrees;
            shedMultiplier = builder.shedMultiplier;
            initialVigor = builder.initialVigor;
            baseBudLight = builder.baseBudLight;
            straightness = builder.straightness;
            flexibility = builder.flexibility;
            minAgeBeforeShed = builder.minAgeBeforeShed;
            influenceRadius = builder.influenceRadius;
            vigorRadius = builder.vigorRadius;
            killRadius = builder.killRadius;
            budProbability = builder.budProbability;
            vigorDecay = builder.vigorDecay;
            maxKids = builder.maxKids;
            maxDepth = builder.maxDepth;
            pruningHeight = builder.pruningHeight;
            forTrunk = builder.forTrunk;
            minRadius = builder.minRadius;
            rootAgeK = builder.rootAgeK;
            rootBaseMultiplier = builder.rootBaseMultiplier;
            rootHeightScale = builder.rootHeightScale;
            rootMaxMultiplier = builder.rootMaxMultiplier;
            distanceBetweenChildren = builder.distanceBetweenChildren;
            minSplittingAge = builder.minSplittingAge;
            senescenceStartAge = builder.senescenceStartAge;
            senescenceDecayRate = builder.senescenceDecayRate;
            senescenceVigorPenalty = builder.senescenceVigorPenalty;
            senescenceGravBias = builder.senescenceGravBias;
            senescenceBudPenalty = builder.senescenceBudPenalty;
            senescenceChildAttenuation = builder.senescenceChildAttenuation;
            senescenceAffectsChildren = builder.senescenceAffectsChildren;
            overrides = builder.overrides;
            multiTrunkismMaxAmount = builder.multiTrunkismMaxAmount;
            multiTrunkismAge = builder.multiTrunkismAge;
        }

        public static final class Builder {
            private final float baseLength;
            private List<Overrides> overrides = new ArrayList<>();
            private float apicalControl = 0.6f;
            private Vec3 lightDirection = new Vec3(0f, 1.0f, 0f);
            private Vec3 overriden_gravity = new Vec3(0f, -1.0f, 0f);
            private float gravitropism = 0.76f;
            private float phototropism = 1.0f;
            private float lateralAngleDegrees = 20.0f;
            private float shedMultiplier = 0.5f;
            private float initialVigor = 1.0f;
            private float baseBudLight = 0.24f;
            private float straightness = 0.37f;
            private float flexibility = 0.95f;
            private int minAgeBeforeShed = 10;
            private double influenceRadius = 25.0;   // how far a branch can “see” light
            private double vigorRadius = 10.5;   // how far a branch can “see” light
            private double killRadius = 3.5;        // distance at which attractor is considered reached
            private double budProbability = 0.86;    // how likely a bud forms when vigor is high
            private float vigorDecay = 0.95f;
            private int maxKids = 15;
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
            private int senescenceStartAge = 5;
            private double senescenceDecayRate = 0.03;
            private double senescenceVigorPenalty = 0.55;
            private double senescenceGravBias = 0.4;
            private double senescenceBudPenalty = 0.6;
            private double senescenceChildAttenuation = 1.0;
            private boolean senescenceAffectsChildren = true;
            private int multiTrunkismAge = 8;
            private int multiTrunkismMaxAmount = 4;


            public Builder(float baseLength) {
                this.baseLength = baseLength;
            }

            @Nonnull
            public Builder multiTrunkismAge(int val) {
                multiTrunkismAge = val;
                return this;
            }

            @Nonnull
            public Builder multiTrunkismMaxAmount(int val) {
                multiTrunkismMaxAmount = val;
                return this;
            }

            @Nonnull
            public Builder apicalControl(float val) {
                apicalControl = val;
                return this;
            }

            @Nonnull
            public Builder lightDirection(@Nonnull Vec3 val) {
                lightDirection = val;
                return this;
            }

            @Nonnull
            public Builder addOverrides(@Nonnull List<Overrides> overrides) {
                this.overrides.addAll(overrides);
                return this;
            }

            @Nonnull
            @SafeVarargs
            public final Builder addOverrides(Overrides...overrides) {
                this.overrides.addAll(List.of(overrides));
                return this;
            }

            @Nonnull
            public Builder setOverrides(@Nonnull List<Overrides> overrides) {
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
                overriden_gravity = val;
                return this;
            }

            @Nonnull
            public Builder gravitropism(float val) {
                gravitropism = val;
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
            public Builder shedMultiplier(float val) {
                shedMultiplier = val;
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
            public Builder senescenceStartAge(int val) {
                senescenceStartAge = val;
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
        }
    }

    public static class TreeNode {
        public final TreeNode parent;
        public final List<TreeNode> children = new ArrayList<>();
        private final List<Vec3> controlPoints = new ArrayList<>();
        public Vec3 startPos;
        public Vec3 direction;
        public float vigor = 1.0f;
        public NodeStatus nodeStatus;
        public long id;
        public int order;
        public int createdAt = 0;
        public float baseRadius; // This is basically max radius
        public Vec3 localUp = Vec3.of(0, 1, 0);
        public boolean canGrowTaller = true;

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
            return controlPoints.getLast();
        }

        public Vec3 firstPoint() {
            return controlPoints.getFirst();
        }

        public List<Vec3> getControlPoints() {
            return Collections.unmodifiableList(controlPoints);
        }

        public List<TreeNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public Vec3 endPos() {
            return Vec3.of(0, 0, 0); // startPos.add(direction.multiply(length));
        }

        public double height() {
            return Math.abs(tip().distance(firstPoint()));
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

    // tiny util
    public static class Util {
        public static Vec3 randomPerturbateVector(Vec3 base, float angleDegrees, long localSeed) {
            Random rnd = new Random(localSeed);
            // small perturbation in radians proportional to angleDegrees
            double angleRad = Math.toRadians(angleDegrees);
            // pick a small random vector within [-angleRad/2, angleRad/2] on each axis
            float ax = (float) ((rnd.nextDouble() - 0.5) * angleRad);
            float ay = (float) ((rnd.nextDouble() - 0.5) * angleRad);
            float az = (float) ((rnd.nextDouble() - 0.5) * angleRad);
            Vec3 perturb = new Vec3(ax, ay, az);
            return base.add(perturb).normalize();
        }
    }

}