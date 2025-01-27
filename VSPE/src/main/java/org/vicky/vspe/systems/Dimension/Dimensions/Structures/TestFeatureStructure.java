package org.vicky.vspe.systems.Dimension.Dimensions.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.GeometryUtil;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import org.vicky.vspe.addon.util.BaseStructure;

import java.util.Random;

public class TestFeatureStructure extends BaseStructure {
    public TestFeatureStructure() {
        super("TEST_STRUCTURE", "TEST_GENERATOR");
    }

    @Override
    protected void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
        BlockState goldBlock = this.platform.getWorldHandle().createBlockState("minecraft:gold_ore");
        BlockState goldBlockDeepslate = this.platform.getWorldHandle().createBlockState("minecraft:deepslate_gold_ore");
        int size = random.nextInt(2, 5);
        boolean randomOreType = random.nextBoolean();
        GeometryUtil.cube(vector3Int, size, action -> writableWorld.setBlockState(action, randomOreType ? goldBlock : goldBlockDeepslate));
    }
}
