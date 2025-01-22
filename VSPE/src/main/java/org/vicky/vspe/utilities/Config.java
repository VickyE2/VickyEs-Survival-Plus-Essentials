package org.vicky.vspe.utilities;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import org.vicky.vspe.utilities.global.GlobalResources;

public class Config {
   public static final Map<String, Object> configs = new HashMap<>();
   private final JavaPlugin plugin;

   public Config(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public void registerConfigs() {
      for (String key : configs.keySet()) {
         if (!GlobalResources.configManager.doesPathExist(key)) {
            GlobalResources.configManager.setBracedConfigValue(key, configs.getOrDefault(key, ""), "");
         }

         GlobalResources.configManager.saveConfig();
      }
   }

   static {
      configs.put("Feature.AdvancementPlus.enable", true);
   }
}
