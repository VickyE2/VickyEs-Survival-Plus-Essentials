package org.vicky.vspe.systems.Dimension.Dimensions.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.vicky.vspe.addon.util.BaseStructure;

import java.util.Random;

public class TestExtrusionStructure extends BaseStructure {
    public TestExtrusionStructure() {
        super("TEST_VINES", "TEST_GENERATOR");
    }

    @Override
    protected void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
        boolean hasBerries = random.nextBoolean();
        BlockState stage_1_vine = this.platform.getWorldHandle().createBlockState("minecraft:cave_vines[age=20, berries=" + hasBerries + "]");
        BlockState stage_2_vine = this.platform.getWorldHandle().createBlockState("minecraft:cave_vines[age=13, berries=" + hasBerries + "]");
        BlockState stage_3_vine = this.platform.getWorldHandle().createBlockState("minecraft:cave_vines[age=2, berries=" + hasBerries + "]");
        int height = random.nextInt(3, 5);
        if (random.nextInt(2, 10) == 3) {
            for (int y = 0; y < height; y++) {
                Vector3Int vineLocation = Vector3Int.of(vector3Int, 0, -y, 0);
                if ((double) y <= 0.3 * (double) height) {
                    writableWorld.setBlockState(vineLocation, stage_1_vine);
                } else if ((double) y <= 0.7 * (double) height) {
                    writableWorld.setBlockState(vineLocation, stage_2_vine);
                } else {
                    writableWorld.setBlockState(vineLocation, stage_3_vine);
                }
            }
        }
    }
}
