package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.vicky.vspe.addon.util.BaseStructure;

import java.util.Random;

public class GlowBerriesVines extends BaseStructure {
    private final int maxHeight;

    public GlowBerriesVines(int maxHeight) {
        super("GLOW_BERRIES_VINES", "CHROMATIC_UNDERWATER");
        this.maxHeight = maxHeight;
    }

    public GlowBerriesVines() {
        super("GLOW_BERRIES_VINES", "CHROMATIC_UNDERWATER");
        this.maxHeight = 13;
    }

    @Override
    protected void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
        BlockState stage_1_vine = this.platform.getWorldHandle().createBlockState("minecraft:cave_vines[age=20, berries=" + random.nextBoolean() + "]");
        BlockState stage_2_vine = this.platform.getWorldHandle().createBlockState("minecraft:cave_vines[age=13, berries=" + random.nextBoolean() + "]");
        BlockState stage_3_vine = this.platform.getWorldHandle().createBlockState("minecraft:cave_vines[age=2, berries=" + random.nextBoolean() + "]");
        int height = random.nextInt(maxHeight - 3, maxHeight + 1);
        if (random.nextInt(2, 10) == 3) {
            for (int y = 0; y < height; y++) {
                Vector3Int vineLocation = Vector3Int.of(vector3Int, 0, -y, 0);
                if (writableWorld.getBlockState(Vector3Int.of(vector3Int, 0, -1, 0)).isAir())
                    if ((double) y <= 0.3 * (double) height) {
                        writableWorld.setBlockState(vineLocation, stage_1_vine);
                    } else if ((double) y <= 0.7 * (double) height) {
                        writableWorld.setBlockState(vineLocation, stage_2_vine);
                    } else {
                        writableWorld.setBlockState(vineLocation, stage_3_vine);
                    }
                else {
                    writableWorld.setBlockState(vineLocation, stage_3_vine);
                    break;
                }
            }
        }
    }
}
