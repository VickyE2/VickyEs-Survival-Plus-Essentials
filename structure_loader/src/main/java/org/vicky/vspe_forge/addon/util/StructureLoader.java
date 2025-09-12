package org.vicky.vspe_forge.addon.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.vicky.utilities.ANSIColor;

public class StructureLoader {
   private final Map<String, List<Class<? extends BaseStructure>>> loadedClasses = new HashMap<>();
   private final List<String> includedPackages = new ArrayList<>();
   private Logger logger;

   public StructureLoader(Logger logger) {
      this.logger = logger;
   }

   public void scanPackageInJar(File jarFile, String packageName) {
      try (JarFile jar = new JarFile(jarFile)) {
         String packagePath = packageName.replace('.', '/');

         try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader())) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
               JarEntry entry = entries.nextElement();
               String entryName = entry.getName();
               if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                  String className = entryName.replace('/', '.').replace(".class", "");

                  try {
                     Class<?> loadedClass = classLoader.loadClass(className);
                     if (BaseStructure.class.isAssignableFrom(loadedClass)) {
                        try {
                           Constructor<? extends BaseStructure> constructor = (Constructor<? extends BaseStructure>)loadedClass.getDeclaredConstructor();
                           constructor.setAccessible(true);
                           BaseStructure structure = constructor.newInstance();
                           this.loadedClasses
                              .computeIfAbsent(structure.getGeneratorKey(), k -> new ArrayList<>())
                              .add((Class<? extends BaseStructure>)loadedClass);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException var15) {
                           throw new RuntimeException(var15);
                        }
                     }
                  } catch (ClassNotFoundException var16) {
                     this.logger.error("Could not load class: " + className);
                     var16.printStackTrace();
                  }
               }
            }
         }
      } catch (Exception var19) {
         this.logger.error("Error scanning JAR: " + var19.getMessage());
         var19.printStackTrace();
      }
   }

   public void scanPackagesInJar(File jarFile, List<String> packageNames) {
      this.logger.info("Scanning JAR file: " + jarFile.getAbsolutePath());
      if (!jarFile.exists()) {
         this.logger.error("Jar file does not exist: " + jarFile.getAbsolutePath());
      } else {
         this.logger.info("Packages to scan: " + packageNames);
         if (packageNames.isEmpty()) {
            this.logger.warn("No packages specified for scanning.");
         } else {
            try {
               label97: {
                  try (
                     JarFile jar = new JarFile(jarFile);
                     URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());
                  ) {
                     Enumeration<JarEntry> entries = jar.entries();
                     if (entries.hasMoreElements()) {
                        while (entries.hasMoreElements()) {
                           JarEntry entry = entries.nextElement();
                           String entryName = entry.getName();

                           for (String packageName : packageNames) {
                              String packagePath = packageName.replace('.', '/');
                              if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                                 if (this.includedPackages.stream().noneMatch(k -> k.equals(packageName))) {
                                    this.logger.info(ANSIColor.colorize("purple[Scanning package entry: " + packageName + "]"));
                                    this.includedPackages.add(packageName);
                                 }

                                 String className = entryName.replace('/', '.').replace(".class", "");
                                 this.logger.info(ANSIColor.colorize("yellow[Found class entry: " + className.replace(packageName + ".", "") + "]"));

                                 try {
                                    Class<?> loadedClass = classLoader.loadClass(className);
                                    if (BaseStructure.class.isAssignableFrom(loadedClass)) {
                                       this.logger
                                          .info(ANSIColor.colorize("green[Loaded class: " + loadedClass.getName().replace(packageName + ".", "") + "]"));

                                       try {
                                          Constructor<? extends BaseStructure> constructor = (Constructor<? extends BaseStructure>)loadedClass.getDeclaredConstructor(
                                             
                                          );
                                          constructor.setAccessible(true);
                                          BaseStructure structure = constructor.newInstance();
                                          this.loadedClasses
                                             .computeIfAbsent(structure.getGeneratorKey(), k -> new ArrayList<>())
                                             .add((Class<? extends BaseStructure>)loadedClass);
                                       } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException var17) {
                                          throw new RuntimeException(var17);
                                       }
                                    }
                                 } catch (ClassNotFoundException var18) {
                                    this.logger.error("Could not load class: " + className, var18);
                                 } catch (Exception var19) {
                                    this.logger.error("Unexpected error while loading class: " + className, var19);
                                 }
                              }
                           }
                        }
                        break label97;
                     }

                     this.logger.warn("No entries found in JAR file.");
                  }

                  return;
               }
            } catch (Exception var22) {
               this.logger.error("Error scanning JAR: " + var22.getMessage(), var22);
            }
         }
      }
   }

   public Map<String, List<Class<? extends BaseStructure>>> getLoadedClasses() {
      return this.loadedClasses;
   }
}
