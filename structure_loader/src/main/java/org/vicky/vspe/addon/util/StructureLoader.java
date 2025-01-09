package org.vicky.vspe.addon.util;

import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class StructureLoader {
    private final List<Class<? extends BaseStructure>> loadedClasses = new ArrayList<>();
    private Logger logger;
    
    public StructureLoader(Logger logger) {
        this.logger = logger;
    }
    public void scanPackageInJar(File jarFile, String packageName) {
        try (JarFile jar = new JarFile(jarFile)) {
            String packagePath = packageName.replace('.', '/');

            // Load the JAR file using a URLClassLoader
            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader())) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Check if the entry belongs to the specified package and is a .class file
                    if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                        String className = entryName
                                .replace('/', '.')
                                .replace(".class", "");

                        try {
                            Class<?> loadedClass = classLoader.loadClass(className);
                            if (BaseStructure.class.isAssignableFrom(loadedClass)) {
                                loadedClasses.add((Class<? extends BaseStructure>) loadedClass);
                                logger.info("Loaded class: " + loadedClass.getName());
                            }
                        } catch (ClassNotFoundException e) {
                            logger.error("Could not load class: " + className);
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void scanPackagesInJar(File jarFile, List<String> packageNames) {
        logger.info("Scanning JAR file: " + jarFile.getAbsolutePath());
        if (!jarFile.exists()) {
            logger.error("Jar file does not exist: " + jarFile.getAbsolutePath());
            return;
        }

        logger.info("Packages to scan: " + packageNames);
        if (packageNames.isEmpty()) {
            logger.warn("No packages specified for scanning.");
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader())) {
                Enumeration<JarEntry> entries = jar.entries();

                if (!entries.hasMoreElements()) {
                    logger.warn("No entries found in JAR file.");
                    return;
                }

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    logger.debug("Scanning entry: " + entryName);

                    for (String packageName : packageNames) {
                        String packagePath = packageName.replace('.', '/');
                        if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                            String className = entryName.replace('/', '.').replace(".class", "");
                            logger.info("Found class entry: " + className);

                            try {
                                Class<?> loadedClass = classLoader.loadClass(className);
                                if (BaseStructure.class.isAssignableFrom(loadedClass)) {
                                    loadedClasses.add((Class<? extends BaseStructure>) loadedClass);
                                    logger.info("Loaded class: " + loadedClass.getName());
                                }
                            } catch (ClassNotFoundException e) {
                                logger.error("Could not load class: " + className, e);
                            } catch (Exception e) {
                                logger.error("Unexpected error while loading class: " + className, e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning JAR: " + e.getMessage(), e);
        }
    }


    public List<Class<? extends BaseStructure>> getLoadedClasses() {
        return loadedClasses;
    }
}
