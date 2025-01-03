package org.vicky.vspe.utilities;

import org.vicky.vspe.VSPE;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private final ZipOutputStream zos;

    // Constructor that takes a ZipOutputStream
    public ZipUtils(ZipOutputStream zos) {
        this.zos = zos;
    }

    // Writes a string to the zip file
    public void writeStringToZip(String stringContent, String directoryInsideZip, String fileName) throws IOException {
        String entryName = directoryInsideZip + fileName;
        zos.putNextEntry(new ZipEntry(entryName));

        byte[] contentBytes = stringContent.getBytes();
        zos.write(contentBytes, 0, contentBytes.length);

        zos.closeEntry();
    }

    // Writes a file to the zip file
    public boolean writeFileToZip(File fileToWrite, String directoryInsideZip) throws IOException {
        if (!fileToWrite.exists()) {
            VSPE.getInstancedLogger().severe("The specified file does not exist: " + fileToWrite.getAbsolutePath());
            return false;
        }

        String entryName = directoryInsideZip + fileToWrite.getName();
        zos.putNextEntry(new ZipEntry(entryName));

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToWrite))) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }

        zos.closeEntry();
        return true;
    }

    // Writes resources to the zip file
    public void writeResourceToZip(String resourceFilePath, String zipEntryName) throws IOException {
        String normalizedPath = resourceFilePath.replace("\\", "/");

        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
            if (resourceStream == null) {
                VSPE.getInstancedLogger().severe("Resource not found: " + normalizedPath);
                return;
            }
            zipEntryName = zipEntryName.replaceFirst("^/", "");

            zos.putNextEntry(new ZipEntry(zipEntryName));

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            zos.closeEntry();
        }
    }


    public void addResourceDirectoryToZip(String resourceDirectoryPath, String baseFolder, String lastTrailingFolder) throws IOException {
        // Ensure the path ends with a slash for proper handling
        if (!resourceDirectoryPath.endsWith("/")) {
            resourceDirectoryPath += "/";
        }

        Enumeration<URL> resources = getClass().getClassLoader().getResources(resourceDirectoryPath);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            // Determine if the resource is part of a directory or a JAR
            if (resource.getProtocol().equals("file")) {
                // Handle file system resources
                File resourceFile = null;
                try {
                    resourceFile = new File(resource.toURI());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                if (resourceFile.isDirectory()) {
                    File[] files = resourceFile.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            addResourceDirectoryToZip(resourceDirectoryPath + file.getName(), baseFolder, lastTrailingFolder);
                        }
                    }
                } else {
                    addResourceToZip(resourceDirectoryPath + resourceFile.getName(), baseFolder, lastTrailingFolder);
                }
            } else if (resource.getProtocol().equals("jar")) {
                // Handle JAR resources
                String resourcePathInJar = resource.getPath();
                String jarPath = resourcePathInJar.substring(5, resourcePathInJar.indexOf("!"));
                try (JarFile jarFile = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(resourceDirectoryPath) && !entry.isDirectory()) {
                            addResourceToZip(entry.getName(), baseFolder, lastTrailingFolder);
                        }
                    }
                }
            }
        }
    }

    public void addResourceToZip(String resourcePath, String baseFolder, String lastTrailingFolder) throws IOException {
        // Normalize paths for ZIP compatibility
        String normalizedPath = resourcePath.replace("\\", "/"); // Ensure we use forward slashes for consistency

        // Remove the "file:/" prefix if it exists
        normalizedPath = normalizedPath.replaceFirst("^file:/", "");

        // Remove leading "/" if any
        normalizedPath = normalizedPath.replaceFirst("^/", "");

        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
            if (resourceStream == null) {
                VSPE.getInstancedLogger().severe("Resource not found: " + normalizedPath);
                return;
            }
            String entryName = normalizedPath.substring(normalizedPath.indexOf(lastTrailingFolder) + lastTrailingFolder.length());

            entryName = baseFolder + entryName.replaceFirst("^/", "");

            zos.putNextEntry(new ZipEntry(entryName));

            // Write the content to the ZIP
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            zos.closeEntry();
        }
    }




    public void addResourceToZip(String resourcePath, String baseFolder) throws IOException {
        // Normalize paths for ZIP compatibility
        String normalizedPath = resourcePath.replace("\\", "/");

        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
            if (resourceStream == null) {
                VSPE.getInstancedLogger().severe("Resource not found: " + normalizedPath);
                return;
            }

            // Remove the resource directory part to focus on the nested folder structure
            String entryName = normalizedPath.substring(normalizedPath.indexOf("dimension/") + "dimension/".length());  // Remove the initial path
            entryName = baseFolder + entryName; // Combine it with your base folder

            zos.putNextEntry(new ZipEntry(entryName));

            // Write the content to the ZIP
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            zos.closeEntry();
        }
    }



    public boolean writeStringToBaseDirectory(String stringContent, String fileName) throws IOException {
        if (stringContent == null || fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("String content and file name must not be null or empty.");
        }

        // Create a ZipEntry with just the file name (no directory path)
        zos.putNextEntry(new ZipEntry(fileName));

        // Convert the string content to bytes and write it into the ZIP
        byte[] contentBytes = stringContent.getBytes();
        zos.write(contentBytes, 0, contentBytes.length);

        // Close the entry
        zos.closeEntry();
        return true;
    }

    public void close() throws IOException {
        zos.close();
    }
}

