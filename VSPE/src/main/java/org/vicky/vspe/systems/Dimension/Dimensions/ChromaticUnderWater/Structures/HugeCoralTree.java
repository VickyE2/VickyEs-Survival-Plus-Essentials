package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.bukkit.Material;
import org.vicky.vspe.addon.util.BaseStructure;

import java.util.Random;

public class HugeCoralTree extends BaseStructure {

    public HugeCoralTree() {
        super("HUGE_CORAL_TREE", "CHROMATIC_UNDERWATER");
    }

    // --- SPIRAL CORAL ---
    private static void generateSpiralCoral(Random random, WritableWorld world, Vector3Int origin, int height, int span, BlockState blueCoral, BlockState coralFan) {
        double angleStep = Math.PI / 10;
        boolean opposite = random.nextInt(9) == 5;
        int thickness = Math.max(1, (int) (span * 0.2));

        for (int y = 0; y < height; y++) {
            double angle = y * angleStep;
            int x1 = (int) (Math.cos(angle) * span / 4);
            int z1 = (int) (Math.sin(angle) * span / 4);
            int x2 = (int) (Math.cos(opposite ? -angle : angle) * span / 4);
            int z2 = (int) (Math.sin(opposite ? -angle : angle) * span / 4);

            placeThickCoralPath(world, origin, x1, y, z1, thickness, blueCoral);
            placeThickCoralPath(world, origin, x2, y, z2, thickness, blueCoral);

            if (random.nextInt(3) == 0) {
                world.setBlockState(Vector3Int.of(origin, x1 + 1, y, z1), coralFan);
                world.setBlockState(Vector3Int.of(origin, x2 - 1, y, z2), coralFan);
            }
        }
    }

    // --- TREE-LIKE CORAL ---
    private static void generateTreeCoral(Random random, WritableWorld world, Vector3Int origin, int height, int span, BlockState blueCoral, BlockState coralFan) {
        int trunkHeight = height / 2 + random.nextInt(height / 4); // Trunk reaches halfway
        int trunkThickness = random.nextInt(2, 4); // Trunk base size

        // Create thick central trunk (solid base)
        for (int dx = -trunkThickness; dx <= trunkThickness; dx++) {
            for (int dz = -trunkThickness; dz <= trunkThickness; dz++) {
                for (int y = 0; y < trunkHeight; y++) {
                    world.setBlockState(Vector3Int.of(origin, dx, y, dz), blueCoral);
                }
            }
        }

        int branchCount = random.nextInt(5, 10);
        for (int i = 0; i < branchCount; i++) {
            int branchStartY = trunkHeight + random.nextInt(height - trunkHeight); // Start higher
            int branchLength = random.nextInt(10, 17); // Long branches
            double angle = random.nextDouble() * Math.PI * 2; // Random circular direction
            double upwardCurve = 0.2 + random.nextDouble() * 0.3; // Controls upward curve

            int x = 0, z = 0;
            for (int j = 0; j < branchLength; j++) {
                // Slowly curve upwards instead of being straight
                int y = branchStartY + (int) (j * upwardCurve);

                // Branch expands outward as it rises
                x += (int) (Math.cos(angle) * (j / 3.0));
                z += (int) (Math.sin(angle) * (j / 3.0));

                // Make branch thicker at the top
                int branchThickness = Math.max(1, (int) (j * 0.15));

                placeThickCoralPath(world, origin, x, y, z, branchThickness, blueCoral);

                // Add coral fans for decoration
                if (random.nextBoolean()) {
                    world.setBlockState(Vector3Int.of(origin, x, y + 1, z), coralFan);
                }
            }
        }
    }

    // --- COLUMN CORAL ---
    private static void generateColumnCoral(Random random, WritableWorld world, Vector3Int origin, int height, int maxSpan, BlockState blueCoral, BlockState coralFan) {
        int baseThickness = 2; // Start small
        int topThickness = maxSpan / 2; // Expands at the top

        for (int y = 0; y < height; y++) {
            // Gradually increase thickness towards the top
            int thickness = (int) (baseThickness + ((double) y / height) * (topThickness - baseThickness));

            // Place the main column
            placeThickCoralPath(world, origin, 0, y, 0, thickness, blueCoral);

            // Start branching when the column is past 1/3 of its height
            if (y > height / 3 && random.nextInt(3) == 0) {
                int branchCount = random.nextInt(1, 3); // 1-2 branches per level
                for (int b = 0; b < branchCount; b++) {
                    int branchStartY = y + random.nextInt(2); // Random offset
                    int branchLength = random.nextInt(3, 7);
                    int branchX = random.nextInt(-thickness, thickness);
                    int branchZ = random.nextInt(-thickness, thickness);

                    for (int j = 0; j < branchLength; j++) {
                        // Branch expands outward naturally
                        placeThickCoralPath(world, origin, branchX, branchStartY + j, branchZ, Math.max(1, (int) (branchLength * 0.2)), blueCoral);

                        // Branch gradually curves
                        branchX += random.nextInt(-1, 2);
                        branchZ += random.nextInt(-1, 2);
                    }

                    // Place coral fan at branch tip
                    if (random.nextBoolean()) {
                        world.setBlockState(Vector3Int.of(origin, branchX, branchStartY + branchLength, branchZ), coralFan);
                    }
                }
            }
        }
    }

    private static void generateArchedCoral(Random random, WritableWorld world, Vector3Int origin, int radius, BlockState blueCoral, BlockState coralFan) {
        int arches = 4 + random.nextInt(2); // 4-5 arches
        double angleStep = Math.PI * 2 / arches; // Angle spacing for arches

        for (int i = 0; i < arches; i++) {
            double baseAngle = i * angleStep; // Distribute arches in a circular pattern

            // Generate an arch that curves inward, forming a dome-like structure
            for (int y = -radius / 2; y <= radius; y++) { // Dome-like structure, stopping at half-radius
                double archRadius = Math.sqrt(radius * radius - y * y); // Sphere equation

                // Rotate around the toroidal shape
                for (double theta = 0; theta < Math.PI * 2; theta += Math.PI / 6) { // Circular iteration for full effect
                    int x = (int) (Math.cos(baseAngle) * archRadius * Math.cos(theta));
                    int z = (int) (Math.sin(baseAngle) * archRadius * Math.cos(theta));

                    int thickness = Math.max(1, (int) (archRadius * 0.15)); // 15% thickness for a smooth structure

                    placeThickCoralPath(world, origin, x, y, z, thickness, blueCoral);

                    // Randomly add coral fans at intersections
                    if (random.nextInt(3) == 0) {
                        world.setBlockState(Vector3Int.of(origin, x, y + 1, z), coralFan);
                    }
                }
            }
        }
    }

    private static void placeThickCoralPath(WritableWorld world, Vector3Int origin, int centerX, int centerY, int centerZ, int thickness, BlockState blueCoral) {
        for (int dx = -thickness / 2; dx <= thickness / 2; dx++) {
            for (int dz = -thickness / 2; dz <= thickness / 2; dz++) {
                if (dx * dx + dz * dz <= (thickness / 2) * (thickness / 2)) {
                    world.setBlockState(Vector3Int.of(origin, centerX + dx, centerY, centerZ + dz), blueCoral);
                }
            }
        }
    }

    @Override
    protected void generateSteps(Vector3Int location, WritableWorld world, Random random, Rotation var4) {
        int height = random.nextInt(30, 41);
        int span = random.nextInt(20, 51);

        BlockState coral = null;
        BlockState coralFan = null;

        int coralType = random.nextInt(4);
        int coralMaterial = random.nextInt(5);

        switch (coralMaterial) {
            case 0 -> {
                coral = this.platform.getWorldHandle().createBlockState(Material.TUBE_CORAL_BLOCK.getKey().asString());
                coralFan = this.platform.getWorldHandle().createBlockState(Material.TUBE_CORAL_FAN.getKey().asString());
            }
            case 1 -> {
                coral = this.platform.getWorldHandle().createBlockState(Material.BRAIN_CORAL_BLOCK.getKey().asString());
                coralFan = this.platform.getWorldHandle().createBlockState(Material.BRAIN_CORAL_FAN.getKey().asString());
            }
            case 2 -> {
                coral = this.platform.getWorldHandle().createBlockState(Material.BUBBLE_CORAL_BLOCK.getKey().asString());
                coralFan = this.platform.getWorldHandle().createBlockState(Material.BUBBLE_CORAL_FAN.getKey().asString());
            }
            case 3 -> {
                coral = this.platform.getWorldHandle().createBlockState(Material.FIRE_CORAL_BLOCK.getKey().asString());
                coralFan = this.platform.getWorldHandle().createBlockState(Material.FIRE_CORAL_FAN.getKey().asString());
            }
            case 4 -> {
                coral = this.platform.getWorldHandle().createBlockState(Material.HORN_CORAL_BLOCK.getKey().asString());
                coralFan = this.platform.getWorldHandle().createBlockState(Material.HORN_CORAL_FAN.getKey().asString());
            }
        }

        switch (coralType) {
            case 0 -> generateSpiralCoral(random, world, location, height, span, coral, coralFan);
            case 1 -> generateTreeCoral(random, world, location, height, span, coral, coralFan);
            case 2 -> generateColumnCoral(random, world, location, height, span, coral, coralFan);
            case 3 -> generateArchedCoral(random, world, location, height, coral, coralFan);
        }
    }
}
