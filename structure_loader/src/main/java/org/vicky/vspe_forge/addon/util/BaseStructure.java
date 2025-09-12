package org.vicky.vspe_forge.addon.util;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.registry.key.Keyed;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.structure.Structure;
import com.dfsek.terra.api.util.Rotation;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.world.WritableWorld;
import java.util.Random;

public abstract class BaseStructure implements Structure, Keyed<BaseStructure> {
   protected Platform platform;
   private final String id;
   private final String generatorKey;

   public BaseStructure() {
      this.id = null;
      this.generatorKey = null;
   }

   public BaseStructure(String id, String generatorKey) {
      this.id = id;
      this.generatorKey = generatorKey;
   }

   public String getGeneratorKey() {
      return this.generatorKey;
   }

   public void setPlatform(Platform platform) {
      this.platform = platform;
   }

   public String getID() {
      return this.id;
   }

   public String getId() {
      return this.id;
   }

   protected abstract void generateSteps(Vector3Int var1, WritableWorld var2, Random var3, Rotation var4);

   public boolean generate(Vector3Int vector3Int, WritableWorld writableWorld, Random random, Rotation rotation) {
      this.generateSteps(vector3Int, writableWorld, random, rotation);
      return true;
   }

   public RegistryKey getRegistryKey() {
      return RegistryKey.of("vspe", this.id);
   }
}
