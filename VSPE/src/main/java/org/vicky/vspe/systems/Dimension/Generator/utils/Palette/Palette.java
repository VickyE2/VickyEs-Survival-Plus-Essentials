package org.vicky.vspe.systems.Dimension.Generator.utils.Palette;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class Palette implements BasePalette, Ymlable {
   public final String id;
   private final Map<Object, Integer> layers;
   private NoiseSampler sampler;

   public Palette(String id) {
      this.id = Utilities.getCleanedID(id);
      this.layers = new HashMap<>();
      this.sampler = null;
   }

   public void addLayer(Map<Material, Integer> materials, int layerThickness) {
      Map<NamespacedKey, Integer> instance = new HashMap<>();

      for (Entry<Material, Integer> material : materials.entrySet()) {
         if (material.getKey().isBlock()) {
            instance.put(material.getKey().getKey(), material.getValue());
         }
      }

      this.layers.put(instance, layerThickness);
   }

   public void addLayer(LayerClass layerClass) {
      this.layers.put(layerClass, 0);
   }

   public void setSampler(NoiseSampler sampler) {
      this.sampler = sampler;
   }

   public String getId() {
      return this.id;
   }

   @Override
   public StringBuilder getYml() {
      StringBuilder builder = new StringBuilder();
      builder.append("id: ").append(this.id).append("\n");
      builder.append("type: PALETTE").append("\n");
      if (!this.layers.isEmpty()) {
         builder.append("layers: ").append("\n");

         for (Entry<Object, Integer> entry : this.layers.entrySet()) {
            Object var6 = entry.getKey();
            if (var6 instanceof Map) {
               Map<?, ?> key = (Map<?, ?>)var6;
               builder.append("  - materials: ").append("\n");

               for (Entry<?, ?> contextMaterial : key.entrySet()) {
                  if (contextMaterial.getKey() instanceof NamespacedKey contextKey) {
                     builder.append("      - ").append(contextKey.asString()).append(": ").append(contextMaterial.getValue()).append("\n");
                  }
               }

               builder.append("    layers: ").append(entry.getValue()).append("\n");
            } else if (entry.getKey() instanceof LayerClass layerClass) {
               builder.append((CharSequence)layerClass.getYml());
            }
         }
      }

      if (this.sampler != null) {
         builder.append("sampler: ").append("\n");
         builder.append(Utilities.getIndentedBlock(this.sampler.getYml().toString(), "  "));
      }

      return builder;
   }
}
