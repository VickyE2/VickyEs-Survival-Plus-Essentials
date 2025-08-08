package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Structures;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.vicky.platform.utils.Vec3;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.addon.util.BaseStructure;
import org.vicky.vspe.platform.systems.dimension.StructureUtils.Generators.ProceduralCrystalShardGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.TerraPlatformWorld;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SeededRandomSource;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import java.util.List;
import java.util.Random;

public class HugeCoralGenerator extends BaseStructure {

    public HugeCoralGenerator() {
        super("HUGE_CORAL_TREE", "CHROMATIC_UNDERWATER");
    }

    // --- SPIRAL CORAL ---
    private static void generateSpiralCoral(Random random, TerraPlatformWorld world, Vector3Int origin, int height, int span, PlatformBlockState<String> blueCoral, PlatformBlockState<String> coralFan) {
        double angleStep = 2 * Math.PI / (height + 1);
        boolean opposite = random.nextBoolean();

        for (int y = 0; y < height; y++) {
            double t = (double) y / (height - 1);
            double radius = span * (0.3 + 0.7 * t); // gradually expand
            double angle = y * angleStep;
            int x1 = (int) (Math.cos(angle) * radius);
            int z1 = (int) (Math.sin(angle) * radius);
            int x2 = (int) (Math.cos(opposite ? -angle : angle) * radius);
            int z2 = (int) (Math.sin(opposite ? -angle : angle) * radius);

            int thickness = Math.max(1, (int) (radius * 0.1));
            placeThickCoralPath(world, origin, x1, y, z1, thickness, blueCoral);
            placeThickCoralPath(world, origin, x2, y, z2, thickness, blueCoral);

            if (random.nextInt(4) == 0) {
                world.setPlatformBlockState(new Vec3(origin.getX() + x1 + 1, origin.getY() + y, origin.getZ() + z1), coralFan);
                world.setPlatformBlockState(new Vec3(origin.getX() + x2 - 1, origin.getY() + y, origin.getZ() + z2), coralFan);
            }
        }
    }

    // --- TREE-LIKE CORAL ---
    private static void generateTreeCoral(Random random, TerraPlatformWorld world, Vector3Int origin, int heighto, int span, PlatformBlockState<String> blueCoral, PlatformBlockState<String> coralFan, Platform platform) {
        final var origin2 = origin.mutable().add(0, -10, 0);
        new ProceduralCrystalShardGenerator.Builder<WritableWorld>()
            .palette(List.of(
                    SimpleBlockState.Companion.from("minecraft:green_stained_glass", (it) -> it),
                    SimpleBlockState.Companion.from("minecraft:yellow_stained_glass", (it) -> it),
                    SimpleBlockState.Companion.from("minecraft:orange_stained_glass", (it) -> it),
                    SimpleBlockState.Companion.from("minecraft:red_stained_glass", (it) -> it)
            ))
            .size(
                    heighto,
                    7
            )
            .glow(true, true, 0.9f)
            .hollow(false)
            .tipPercentage(0.3)
            .state(ProceduralCrystalShardGenerator.State.SLIGHTLY_BROKEN)
            .breakHeightRange(0.4f, 0.7f)         // very low break, easy to see
            .breakAngles(30, 60, 30, 60)     // big tilt
            .breakDistance(2.0, 8.0)
            .distribution((palette, yNorm, theta, rnd) -> {
                double t = (theta / (2 * Math.PI) + yNorm) % 1.0;
                double shaped = 0.5 * (1 - Math.cos(t * Math.PI));
                int idx = (int)(shaped * (palette.size() - 1));
                return palette.get(idx);
            })
            .build(world)
            .generate(new SeededRandomSource(random.nextLong() + 5149898959L), new Vec3(origin2.getX(), origin2.getY(), origin2.getZ()));
        /*
        new ProceduralTubeGenerator.Builder()
            .heightRange(20, heighto)       // vertical between 30 and 40
            .tubeRadius(5)               // each tube is 3 blocks thick
            .slope(0.07)              // positive = outward flare; tweak for more/less curvature
            .flareStart(0.6f)
            .reservoir(true, 7)  // a 5-block radius water basin beneath
            .rimCap(false)
            .materials(blueCoral, platform.getWorldHandle().createBlockState(SimpleBlockState.Companion.from("minecraft:water"))
            .build()
            .generate(random, world, origin);
        */
        /*
        new ProceduralBranchedTreeGenerator.Builder()
            .woodMaterial(blueCoral)
            .height(heighto)
            .width(span)
            .trunkThickness(3)
            .randomness(random.nextFloat(0.6f))
            .twistiness(0.1f)
            .branchThickness(2)
            .branchLengthReduction(0.6f)
            .branchShrinkPerLevel(0.7f)
            .branchPitchRange(0f, 10f)
            .branchStart(1.0f)
            .branchDepth(random.nextInt(7) + 3)
            .maxBranchShrink(0.004f)
            .cheapMode(true)
            .qualityFactor(1f)
            .addDecoration(
                0.6f,
                (wWorld, loc) -> wWorld.setBlockState(loc, coralFan),
                ProceduralBranchedTreeGenerator.Orientation.TOP
            )
            .build()
            .generate(random, world, origin);
         */
        /*
        new ProceduralMushroomGenerator.Builder()
            .capEq((xNorm, yNorm, height, rnd) -> {
                // A) Normalize the raw height into [0…1]:
                double hNorm = (height - 10)
                        / (double)(18 - 10);

                // B) Compute a smooth exponent between maxExp (sharp) & minExp (flat)
                double minExp = 1.0, maxExp = 2.0;  // tweak to taste
                double exp    = minExp + (maxExp - minExp) * (1 - hNorm);

                // C) Vertical profile with this exponent:
                double yProf = Math.pow(1 - yNorm, exp);

                // D) Your tip‐bend mask (unchanged):
                double a = 1.2, b = 1.0, B = 0.5, p = 4.0;
                double base   = Math.pow(a * xNorm * yNorm, 2.8) * (b * yNorm);
                double bend   = 1 - B * Math.pow(xNorm, p);
                double tipMask= Math.max(0, base * bend);

                // E) Combine them—use yProf at center, otherwise carve away tipMask%
                return xNorm < 1e-6
                        ? yProf
                        : yProf * (1 - tipMask);
            })
            .capHeightRange(15, 21)
            .capRadiusRange(22, 34)
            .stemHeightRange(47, 72, 5)
            .stemTaperPct(0.4f)
            .stemTilt(0.24f, 15.0)
            .ridgeMat(platform.getWorldHandle().createBlockState(SimpleBlockState.Companion.from("minecraft:quartz_block"))
            .spots(
                    platform.getWorldHandle().createBlockState(SimpleBlockState.Companion.from("minecraft:red_mushroom_block"),
                    0.25f
            )
            .materials(
                    platform.getWorldHandle().createBlockState(SimpleBlockState.Companion.from("minecraft:mushroom_stem"),
                    platform.getWorldHandle().createBlockState(SimpleBlockState.Companion.from("minecraft:white_concrete")
            )
            .hollowCap()
            .build()
            .generate(random, world, origin);
        */
        /*
        int trunkHeight = height / 2 + random.nextInt(height / 4);
        int trunkThickness = 2 + random.nextInt(3);

        // central trunk
        for (int dx = -trunkThickness; dx <= trunkThickness; dx++) {
            for (int dz = -trunkThickness; dz <= trunkThickness; dz++) {
                for (int y = 0; y < trunkHeight; y++) {
                    world.setBlockState(new Vec3(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz), blueCoral);
                }
            }
        }

        int branchCount = 5 + random.nextInt(6);
        for (int i = 0; i < branchCount; i++) {
            int startY = trunkHeight + random.nextInt(height - trunkHeight);
            double angle = random.nextDouble() * 2 * Math.PI;
            double branchLen = 8 + random.nextDouble() * (span * 0.2);
            double curl = 0.1 + random.nextDouble() * 0.2;

            double cx = 0, cz = 0;
            for (int j = 0; j < (int) branchLen; j++) {
                int y = startY + j;
                cx += Math.cos(angle);
                cz += Math.sin(angle);
                angle += (random.nextGaussian() * 0.05);
                angle *= (1 - curl);

                int bx = (int) Math.round(cx);
                int bz = (int) Math.round(cz);
                int thickness = Math.max(1, (int) (1 + j * 0.08));
                placeThickCoralPath(world, origin, bx, y, bz, thickness, blueCoral);
                if (random.nextFloat() < 0.3) {
                    world.setBlockState(new Vec3(origin.getX() + bx, origin.getY() + y + 1, origin.getZ() + bz), coralFan);
                }
            }
        }
         */
    }

    // --- COLUMN CORAL ---
    private static void generateColumnCoral(Random random, TerraPlatformWorld world, Vector3Int origin, int height, int maxSpan, PlatformBlockState<String> blueCoral, PlatformBlockState<String> coralFan) {
        int base = 2;
        int top = maxSpan / 2;
        for (int y = 0; y < height; y++) {
            double t = (double) y / (height - 1);
            int thickness = (int) Math.round(base + t * (top - base));
            placeThickCoralPath(world, origin, 0, y, 0, thickness, blueCoral);

            if (y > height / 3 && random.nextInt(4) == 0) {
                int branches = 1 + random.nextInt(2);
                for (int b = 0; b < branches; b++) {
                    int bx = random.nextInt(-thickness, thickness + 1);
                    int bz = random.nextInt(-thickness, thickness + 1);
                    int bl = 3 + random.nextInt(5);
                    for (int j = 0; j < bl; j++) {
                        int hy = y + j;
                        placeThickCoralPath(world, origin, bx, hy, bz, Math.max(1, bl / 5), blueCoral);
                        bx += random.nextInt(-1, 2);
                        bz += random.nextInt(-1, 2);
                    }
                    if (random.nextBoolean()) {
                        world.setPlatformBlockState(new Vec3(origin.getX() + bx, origin.getY() + y + bl, origin.getZ() + bz), coralFan);
                    }
                }
            }
        }
    }

    // --- ARCHED CORAL ---
    private static void generateArchedCoral(Random random, TerraPlatformWorld world, Vector3Int origin, int radius, PlatformBlockState<String> blueCoral, PlatformBlockState<String> coralFan) {
        int rings = 6 + random.nextInt(3);
        double ringStep = 2 * Math.PI / rings;
        double tubeRadius = radius * 0.3;
        double ringRadius = radius - tubeRadius;
        for (int i = 0; i < rings; i++) {
            double theta = i * ringStep;
            for (int phiStep = 0; phiStep < 360; phiStep += 15) {
                double phi = Math.toRadians(phiStep);
                double g = ringRadius + tubeRadius * Math.cos(phi);
                double x = g * Math.cos(theta);
                double y = tubeRadius * Math.sin(phi) - tubeRadius / 2;
                double z = g * Math.sin(theta);
                int ix = (int) Math.round(x);
                int iy = (int) Math.round(y + (double) radius / 2);
                int iz = (int) Math.round(z);
                int thickness = Math.max(1, (int) (tubeRadius * 0.2));
                placeThickCoralPath(world, origin, ix, iy, iz, thickness, blueCoral);
                if (random.nextInt(5) == 0) {
                    world.setPlatformBlockState(new Vec3(origin.getX() + ix, origin.getY() + iy + 1, origin.getZ() + iz), coralFan);
                }
            }
        }
    }

    private static void placeThickCoralPath(TerraPlatformWorld world, Vector3Int origin, int x, int y, int z, int thickness, PlatformBlockState<String> state) {
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dz = -thickness; dz <= thickness; dz++) {
                if (dx * dx + dz * dz <= thickness * thickness) {
                    world.setPlatformBlockState(new Vec3(x + dx, y, z + dz), state);
                }
            }
        }
    }


    @Override
    protected void generateSteps(Vector3Int location, WritableWorld world, Random random, Rotation var4) {
        int height = random.nextInt(25, 48);
        int span = random.nextInt(20, 35);

        PlatformBlockState<String> coral = null;
        PlatformBlockState<String> coralFan = null;

        int coralType = random.nextInt(4);
        int coralMaterial = random.nextInt(5);

        switch (coralMaterial) {
            case 0 -> {
                coral = SimpleBlockState.Companion.from("minecraft:tube_coral_block", (it) -> it);
                coralFan = SimpleBlockState.Companion.from("minecraft:tube_coral_fan", (it) -> it);
            }
            case 1 -> {
                coral = SimpleBlockState.Companion.from("minecraft:brain_coral_block", (it) -> it);
                coralFan = SimpleBlockState.Companion.from("minecraft:brain_coral_fan", (it) -> it);
            }
            case 2 -> {
                coral = SimpleBlockState.Companion.from("minecraft:bubble_coral_block", (it) -> it);
                coralFan = SimpleBlockState.Companion.from("minecraft:bubble_coral_fan", (it) -> it);
            }
            case 3 -> {
                coral = SimpleBlockState.Companion.from("minecraft:fire_coral_block", (it) -> it);
                coralFan = SimpleBlockState.Companion.from("minecraft:fire_coral_fan", (it) -> it);
            }
            case 4 -> {
                coral = SimpleBlockState.Companion.from("minecraft:horn_coral_block", (it) -> it);
                coralFan = SimpleBlockState.Companion.from("minecraft:horn_coral_fan", (it) -> it);
            }
        }

        final TerraPlatformWorld terraWorld = new TerraPlatformWorld(world);
        switch (/*coralType*/1) {
            case 0 -> generateSpiralCoral(random, terraWorld, location, height, span, coral, coralFan);
            case 1 -> generateTreeCoral(random, terraWorld, location, height, span, coral, coralFan, this.platform);
            case 2 -> generateColumnCoral(random, terraWorld, location, height, span, coral, coralFan);
            case 3 -> generateArchedCoral(random, terraWorld, location, height, coral, coralFan);
        }
    }
}
