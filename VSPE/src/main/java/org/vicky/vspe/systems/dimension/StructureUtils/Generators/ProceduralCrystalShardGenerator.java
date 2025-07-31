package org.vicky.vspe.systems.dimension.StructureUtils.Generators;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.bukkit.Material;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.dimension.StructureUtils.ProceduralStructureGenerator;
import org.vicky.vspe.systems.dimension.StructureUtils.StructureCacheUtils;
import org.vicky.vspe.utilities.Math.Vector3;

import java.util.*;

import static java.lang.Math.*;

/**
 * Generates crystal shards in different states (NORMAL, SLIGHTLY_BROKEN, CRACKED, CLUSTERED),
 * with full control over palette distribution (including spirals) and realistic break simulation.
 */
public class ProceduralCrystalShardGenerator extends ProceduralStructureGenerator {
    public enum State { NORMAL, SLIGHTLY_BROKEN, CRACKED, CLUSTERED }

    // Configuration
    private final State state;
    private final int height, width;
    private final boolean hollow;
    private final boolean glow;
    private final boolean glowUw;
    private final double glowLightLevel;
    private final double yaw, pitch, tipFrac;
    private final int maxChildren;
    private final double minShrink, maxShrink;
    private final List<BlockState> palette;
    private final DistributionFunction distributionFunction;
    private final Random rnd = new Random();

    // Slightly_broken parameters
    private final float breakHeightMin, breakHeightMax;
    private final double breakYawMin, breakYawMax;
    private final double breakPitchMin, breakPitchMax;
    private final double breakDistanceMin, breakDistanceMax;
    private final double crackFrac, crackThickness, crackChance;

    public ProceduralCrystalShardGenerator(Builder b) {
        this.state = b.state;
        this.height = b.height;
        this.width = b.width;
        this.hollow = b.hollow;
        this.glow = b.glow;
        this.glowUw = b.underwater;
        this.glowLightLevel = b.glowLightLevel;
        this.yaw = b.yaw;
        this.pitch = b.pitch;
        this.maxChildren = b.maxChildren;
        this.minShrink = b.minShrink;
        this.maxShrink = b.maxShrink;
        this.palette = List.copyOf(b.palette);
        this.platform = b.platform;
        this.tipFrac = b.tipFrac;
        this.distributionFunction = b.distributionFunction;
        this.breakHeightMin = b.breakHeightMin;
        this.breakHeightMax = b.breakHeightMax;
        this.breakYawMin = b.breakYawMin;
        this.breakYawMax = b.breakYawMax;
        this.breakPitchMin = b.breakPitchMin;
        this.breakPitchMax = b.breakPitchMax;
        this.breakDistanceMin = b.breakDistanceMin;
        this.breakDistanceMax = b.breakDistanceMax;
        this.crackChance = b.crackChance;
        this.crackFrac = b.crackFrac;
        this.crackThickness = b.crackThickness;
    }

    @Override
    public void generate(Random rnd, WritableWorld world, Vector3Int origin, Platform platform) {
        prepareFlush(world, platform);
        List<Vector3Int> placed = new ArrayList<>();
        generateShard(rnd, origin, height, width, placed);

        // Clustered children
        if(state == State.CLUSTERED) {
            int count = rnd.nextInt(1, maxChildren + 1);
            for(int i=0;i<count;i++) {
                double shrink = minShrink + rnd.nextDouble()*(maxShrink-minShrink);
                int h2 = (int)(height * shrink);
                int w2 = max(1, (int)(width * shrink));
                double angle = rnd.nextDouble()*2*PI;
                int dx = (int)((width + w2) * cos(angle));
                int dz = (int)((width + w2) * sin(angle));
                Vector3Int childOrigin = origin.mutable().add(dx, 0, dz);
                generateShard(rnd, childOrigin, h2, w2, new ArrayList<>());
            }
        }

        // Breakage simulation for SLIGHTLY_BROKEN
        if(state == State.SLIGHTLY_BROKEN) {
            simulateSlightBreak(origin, placed, world);
        }
        else if(state == State.CRACKED) {
            simulateCrack(origin, placed, world, crackFrac, crackThickness, crackChance);
        }

        flush.run();
    }

    /**
     * Builds a single shard, recording each placed block position.
     */
    private void generateShard(Random rnd, Vector3Int origin, int h, int w, List<Vector3Int> outPlaced) {
        // Precompute inner/out offsets once
        short[] innerBase = StructureCacheUtils.getPolygonOffsets(w, 6);
        short[] outerBase = StructureCacheUtils.getPolygonOffsets(w + 1, 6);
        Set<Long> innerSet = new HashSet<>();
        for (int i = 0; i < innerBase.length; i += 2) {
            innerSet.add(((long)innerBase[i] << 32) | (innerBase[i+1] & 0xFFFFFFFFL));
        }
        // Build a quick lookup for the outer ring too
        Set<Long> outerSet = new HashSet<>();
        for (int i = 0; i < outerBase.length; i += 2) {
            outerSet.add(((long)outerBase[i] << 32) | (outerBase[i+1] & 0xFFFFFFFFL));
        }

        double tipHeight = tipFrac * h;  // as before

        // 1) fill the shard
        for (int y = 0; y <= h; y++) {
            double t = (double)y / h;
            // piecewise radius for tiny tips + full body
            double radiusF;
            if (y > h - tipHeight) {
                radiusF = w * ((h - y) / tipHeight);
            } else {
                radiusF = w;
            }

            int layerR = Math.max(0, (int)Math.floor(radiusF));
            short[] layerOffsets = StructureCacheUtils.getPolygonOffsets(layerR, 6);

            for (int i = 0; i < layerOffsets.length; i += 2) {
                int dx = layerOffsets[i], dz = layerOffsets[i+1];

                Vector3 local = Vector3.at(dx, y, dz);

                // 2) define the pivot in *that* same local space
                Vector3 pivot = Vector3.at(0, 0, 0); // base is y=0

                // 3) move point *relative* to pivot
                Vector3 relative = local.subtract(pivot);

                // 4) apply your yaw & pitch to that relative vector
                Vector3 rotated = rotateAroundY(relative, yaw);
                rotated = rotateAroundAxis(rotated, Vector3.at(1,0,0), pitch);

                // 5) move back by the pivot
                Vector3 afterPivot = rotated.add(pivot);

                // 6) translate into world space
                int x  = origin.getX() + (int) Math.round(afterPivot.getX());
                int yy = origin.getY() + (int) Math.round(afterPivot.getY());
                int z  = origin.getZ() + (int) Math.round(afterPivot.getZ());

                // optional hollow filter
                if (hollow && layerR > 0) {
                    int innerR = layerR - 1;
                    double bound = innerR * Math.cos(Math.PI/6)
                            / Math.cos((Math.atan2(dz,dx) % (Math.PI/3)) - (Math.PI/6));
                    if (Math.hypot(dx,dz) < bound) continue;
                }

                // place the shard block
                double theta = Math.atan2(dz, dx);
                BlockState block = distributionFunction.pick(palette, t, theta, rnd);
                guardAndStore(x, yy, z, block, false);
                outPlaced.add(Vector3Int.of(x, yy, z));

                // ---- new: glow adjacent to every outer-shell block ----
                if (glow) {
                    // detect if this (dx,dz) is on the outer edge of the current layer
                    long key = ((long)dx << 32) | (dz & 0xFFFFFFFFL);
                    if (outerSet.contains(key) && !innerSet.contains(key)) {
                        // compute the outward neighbor in local coords
                        int sx = Integer.signum(dx);
                        int sz = Integer.signum(dz);
                        Vector3 glowLocal = Vector3.at(dx + sx, y, dz + sz);
                        // rotate same as the shard block
                        glowLocal = rotateAroundY(glowLocal, yaw);
                        glowLocal = rotateAroundAxis(glowLocal, Vector3.at(1,0,0), pitch);

                        int gx = origin.getX() + (int)Math.round(glowLocal.getX());
                        int gy = origin.getY() + (int)Math.round(glowLocal.getY());
                        int gz = origin.getZ() + (int)Math.round(glowLocal.getZ());

                        BlockState glowState = platform.getWorldHandle()
                                .createBlockState(
                                        Material.LIGHT
                                                .createBlockData(
                                                        "[level=" + (int)(glowLightLevel*15)
                                                                + ",waterlogged=" + glowUw + "]")
                                                .getAsString()
                                );
                        guardAndStore(gx, gy, gz, glowState, false);
                    }
                }
            }
        }
    }


    /**
     * Simulates a chunk of the shard breaking off and rotating away.
     */
    private void simulateSlightBreak(Vector3Int origin,
                                     List<Vector3Int> placed,
                                     WritableWorld world) {
        VSPE.getInstancedLogger().info("🔨 simulateSlightBreak called! placed.size()=" + placed.size());
        int breakY = (int) (rnd.nextFloat(breakHeightMin, breakHeightMax + 1) * height);
        VSPE.getInstancedLogger().info("   → breakY = " + breakY);
        double yawOff   = Math.toRadians(breakYawMin + rnd.nextDouble() * (breakYawMax - breakYawMin));
        double pitchOff = Math.toRadians(breakPitchMin + rnd.nextDouble() * (breakPitchMax - breakPitchMin));
        double maxShift = Math.max(width, height) * 0.5;
        double dist = Math.min(
                maxShift,
                breakDistanceMin + rnd.nextDouble()*(breakDistanceMax - breakDistanceMin)
        );
        VSPE.getInstancedLogger().info(
                String.format("   → yaw=%1$.1f°, pitch=%2$.1f°, dist=%.2f",
                        Math.toDegrees(yawOff), Math.toDegrees(pitchOff), dist)
        );

        // 2) compute the plane normal & crack axis
        Vector3 normal     = Vector3.at(
                Math.cos(pitchOff) * Math.sin(yawOff),
                Math.sin(pitchOff),
                Math.cos(pitchOff) * Math.cos(yawOff)
        ).normalize();
        Vector3 crackAxis  = normal.cross(Vector3.at(0, 1, 0)).normalize();

        // Prepare lists for chunk above the break
        List<Vector3Int> removedPos   = new ArrayList<>();
        List<BlockState> removedState = new ArrayList<>();

        // 3) split your placed blocks into “base” vs. “to move”
        for (Vector3Int pos : new ArrayList<>(placed)) {
            int relY = pos.getY() - origin.getY();
            if (relY >= breakY) {
                // capture its state (fall back to air if null)
                BlockState bs = getQueuedState(pos.getX(), pos.getY(), pos.getZ());
                removedPos.add(pos);
                removedState.add(bs);
                removeAt(pos.getX(), pos.getY(), pos.getZ());
                placed.remove(pos);  // so placed now only contains the base
            }
        }

        // 4) re-place the moving chunk, rotated & translated
        for (int i = 0; i < removedPos.size(); i++) {
            Vector3Int old    = removedPos .get(i);
            BlockState state  = removedState.get(i);

            // a) local vector from the break plane
            Vector3 rel = Vector3.at(
                    old.getX() - origin.getX(),
                    old.getY() - (origin.getY() + breakY),
                    old.getZ() - origin.getZ()
            );

            // b) rotate around the crack axis & then yaw
            rel = rotateAroundAxis(rel, crackAxis, pitchOff);
            rel = rotateAroundY(rel, yawOff);

            // c) translate outward
            rel = rel.add(normal.multiply(dist));

            // d) back to world coords (pivoted at origin+breakY)
            Vector3 dest = rel.add(Vector3.at(
                    origin.getX(),
                    origin.getY() + breakY,
                    origin.getZ()
            ));
            Vector3Int np = Vector3Int.of(
                    (int) Math.round(dest.getX()),
                    (int) Math.round(dest.getY()),
                    (int) Math.round(dest.getZ())
            );

            // e) place the original blockstate
            guardAndStore(np.getX(), np.getY(), np.getZ(), state /*platform.getWorldHandle().createBlockState("minecraft:gold_block")*/, false);
        }

        // 5) in `placed` we still have the base blocks (pos.getY()< breakY),
        //    so if you need to re-flush or process them further, they are intact.
    }

    /**
     * Simulates a crack by randomly removing blocks in a horizontal band.
     * @param crackFrac      fraction up the shard where the crack occurs (0–1)
     * @param crackThickness fraction of total height to use as crack band
     * @param crackChance    probability [0–1] to remove each block in that band
     */
    private void simulateCrack(Vector3Int origin,
                               List<Vector3Int> placed,
                               WritableWorld world,
                               double crackFrac,
                               double crackThickness,
                               double crackChance) {
        int h = height;  // your shard total height
        int centerY = origin.getY() + (int) Math.floor(crackFrac * h);
        int halfBand = (int) Math.ceil((crackThickness * h) / 2.0);

        // Remove a horizontal slab around centerY
        for (Vector3Int pos : new ArrayList<>(placed)) {
            int dy = pos.getY() - centerY;
            if (Math.abs(dy) <= halfBand && rnd.nextDouble() < crackChance) {
                world.setBlockState(pos, null);
                placed.remove(pos);
            }
        }
    }

    /**
     * Functional interface for distributing blocks.  Allows spirals by inspecting theta.
     */
    @FunctionalInterface
    public interface DistributionFunction {
        BlockState pick(List<BlockState> palette, double yNorm, double theta, Random rnd);
    }

    public static class Builder {
        private State state = State.NORMAL;
        private int height=8, width=2;
        private boolean hollow=false, glow=false, underwater=false;
        private double glowLightLevel=1.0, tipFrac = 0.4;;
        private double yaw=0, pitch=0;
        private int maxChildren=3;
        private double minShrink=0.5, maxShrink=0.8;
        private List<BlockState> palette = new ArrayList<>();
        private DistributionFunction distributionFunction;
        private Platform platform;
        // break params
        private float breakHeightMin=0.4f, breakHeightMax=0.6f;
        private double breakYawMin=0, breakYawMax=180;
        private double breakPitchMin=10, breakPitchMax=45;
        private double breakDistanceMin=1, breakDistanceMax=3;
        private double crackFrac = 0.5, crackThickness = 0.1, crackChance = 0.3;

        public Builder state(State s){ this.state=s;return this; }
        public Builder size(int h,int w){ this.height=h;this.width=w;return this; }
        public Builder tipPercentage(double tipFraq) { this.tipFrac = tipFraq; return this; }
        public Builder hollow(boolean b){ this.hollow=b;return this; }
        public Builder glow(boolean b,boolean uw,double lvl,Platform p){ this.glow=b;this.underwater=uw;this.glowLightLevel=lvl;this.platform=p;return this; }
        public Builder rotation(double y,double p){ this.yaw=y;this.pitch=p;return this; }
        public Builder cluster(int max,double min,double max2){ this.maxChildren=max;this.minShrink=min;this.maxShrink=max2;return this; }
        public Builder palette(List<BlockState> pal){ this.palette=pal;return this; }
        public Builder distribution(DistributionFunction f){ this.distributionFunction=f;return this; }
        public Builder breakHeightRange(float min,float max){ this.breakHeightMin=min;this.breakHeightMax=max;return this; }
        public Builder breakAngles(double yawMin,double yawMax,double pitchMin,double pitchMax){ this.breakYawMin=yawMin;this.breakYawMax=yawMax;this.breakPitchMin=pitchMin;this.breakPitchMax=pitchMax;return this; }
        public Builder crackValues(double crackChance,double crackFrac,double crackThickness){ this.crackChance = crackChance; this.crackFrac = crackFrac; this.crackThickness = crackThickness; return this; }
        public Builder breakDistance(double min,double max){ this.breakDistanceMin=min;this.breakDistanceMax=max;return this; }
        public ProceduralCrystalShardGenerator build(){
            Objects.requireNonNull(palette,"Palette required");
            Objects.requireNonNull(distributionFunction,"DistributionFunction required");
            return new ProceduralCrystalShardGenerator(this);
        }
    }
}
