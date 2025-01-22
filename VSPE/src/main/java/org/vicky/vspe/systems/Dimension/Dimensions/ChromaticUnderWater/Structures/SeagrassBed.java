package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import java.util.Random;
import org.bukkit.Material;
import org.vicky.vspe.addon.util.BaseStructure;

public class SeagrassBed extends BaseStructure {
   public SeagrassBed() {
      super("SEAGRASS_BED", "SEAGRASS_BED");
   }

   @Override
   protected void generateSteps(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
      BlockState seagrass = this.platform.getWorldHandle().createBlockState("minecraft:seagrass[age=2]");
      BlockState mossBlock = this.platform.getWorldHandle().createBlockState(Material.MOSS_BLOCK.getKey().asString());
      if (writableWorld.getBlockState(0, -1, 0).matches(mossBlock)) {
         writableWorld.setBlockState(vector3Int, seagrass);
      } else if (random.nextInt(0, 15) == 4) {
         writableWorld.setBlockState(vector3Int, seagrass);
      }
   }
}
