package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.vicky.vspe.addon.util.BaseStructure;

import java.util.Random;

public class SeagrassBed extends BaseStructure {
    public SeagrassBed() {
        super("SEAGRASS_BED", "SEAGRASS_BED");
    }

    @Override
    protected void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
        BlockState seagrass = this.platform.getWorldHandle().createBlockState("minecraft:seagrass");
        BlockState tallSeagrassB = this.platform.getWorldHandle().createBlockState("minecraft:tall_seagrass[half=lower]");
        BlockState tallSeagrassT = this.platform.getWorldHandle().createBlockState("minecraft:tall_seagrass[half=upper]");

        Vector3Int seagrassLocation = Vector3Int.of(vector3Int, 0, 0, 0);
        Vector3Int seagrassLocationTop = Vector3Int.of(vector3Int, 0, 1, 0);

        if (random.nextInt(1, 15) == 8) {
            writableWorld.setBlockState(seagrassLocation, seagrass);
        } else {
            writableWorld.setBlockState(seagrassLocation, tallSeagrassB);
            writableWorld.setBlockState(seagrassLocationTop, tallSeagrassT);
        }
    }
}
