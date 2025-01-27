package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.bukkit.Material;
import org.vicky.vspe.addon.util.BaseStructure;

import java.util.Random;

public class SeagrassBed extends BaseStructure {
    public SeagrassBed() {
        super("SEAGRASS_BED", "SEAGRASS_BED");
    }

    @Override
    protected void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
        BlockState seagrass = this.platform.getWorldHandle().createBlockState(Material.SEAGRASS.getKey().asString());
        BlockState mossBlock = this.platform.getWorldHandle().createBlockState(Material.MOSS_BLOCK.getKey().asString());
        Vector3Int seagrassLocation = Vector3Int.of(vector3Int, 0, 0, 0);
        if (writableWorld.getBlockState(Vector3Int.of(vector3Int, 0, -1, 0)).matches(mossBlock)) {
            writableWorld.setBlockState(seagrassLocation, seagrass);
        } else if (random.nextInt(0, 15) == 4) {
            writableWorld.setBlockState(seagrassLocation, seagrass);
        }
    }
}
