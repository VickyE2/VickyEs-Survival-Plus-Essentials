package org.vicky.vspe.utilities;

import org.vicky.vspe.VSPE;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private final ZipOutputStream zos;

    public ZipUtils(ZipOutputStream zos) {
        this.zos = zos;
    }

    public void writeStringToZip(String stringContent, String directoryInsideZip, String fileName) throws IOException {
        String entryName = directoryInsideZip + fileName;
        this.zos.putNextEntry(new ZipEntry(entryName));
        byte[] contentBytes = stringContent.getBytes();
        this.zos.write(contentBytes, 0, contentBytes.length);
        this.zos.closeEntry();
    }

    public boolean writeFileToZip(File fileToWrite, String directoryInsideZip) throws IOException {
        if (!fileToWrite.exists()) {
            VSPE.getInstancedLogger().severe("The specified file does not exist: " + fileToWrite.getAbsolutePath());
            return false;
        } else {
            String entryName = directoryInsideZip + fileToWrite.getName();
            this.zos.putNextEntry(new ZipEntry(entryName));

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToWrite))) {
                byte[] buffer = new byte[1024];

                int length;
                while ((length = bis.read(buffer)) > 0) {
                    this.zos.write(buffer, 0, length);
                }
            }

            this.zos.closeEntry();
            return true;
        }
    }

    public void writeResourceToZip(String resourceFilePath, String zipEntryName) throws IOException {
        String normalizedPath = resourceFilePath.replace("\\", "/");

        try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
            if (resourceStream == null) {
                VSPE.getInstancedLogger().severe("Resource not found: " + normalizedPath);
                return;
            }

            zipEntryName = zipEntryName.replaceFirst("^/", "");
            this.zos.putNextEntry(new ZipEntry(zipEntryName));
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                this.zos.write(buffer, 0, bytesRead);
            }

            this.zos.closeEntry();
        }
    }

    public void addResourceDirectoryToZip(String resourceDirectoryPath, String baseFolder, String lastTrailingFolder) throws IOException {
        if (!resourceDirectoryPath.endsWith("/")) {
            resourceDirectoryPath = resourceDirectoryPath + "/";
        }

        Enumeration<URL> resources = this.getClass().getClassLoader().getResources(resourceDirectoryPath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File resourceFile = null;

                try {
                    resourceFile = new File(resource.toURI());
                } catch (URISyntaxException var13) {
                    throw new RuntimeException(var13);
                }

                if (resourceFile.isDirectory()) {
                    File[] files = resourceFile.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            this.addResourceDirectoryToZip(resourceDirectoryPath + file.getName(), baseFolder, lastTrailingFolder);
                        }
                    }
                } else {
                    this.addResourceToZip(resourceDirectoryPath + resourceFile.getName(), baseFolder, lastTrailingFolder);
                }
            } else if (resource.getProtocol().equals("jar")) {
                String resourcePathInJar = resource.getPath();
                String jarPath = resourcePathInJar.substring(5, resourcePathInJar.indexOf("!"));

                try (JarFile jarFile = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(resourceDirectoryPath) && !entry.isDirectory()) {
                            this.addResourceToZip(entry.getName(), baseFolder, lastTrailingFolder);
                        }
                    }
                }
            }
        }
    }

    public void addResourceToZip(String resourcePath, String baseFolder, String lastTrailingFolder) throws IOException {
        String normalizedPath = resourcePath.replace("\\", "/");
        normalizedPath = normalizedPath.replaceFirst("^file:/", "");
        normalizedPath = normalizedPath.replaceFirst("^/", "");

        try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
            if (resourceStream == null) {
                VSPE.getInstancedLogger().severe("Resource not found: " + normalizedPath);
                return;
            }

            String entryName = normalizedPath.substring(normalizedPath.indexOf(lastTrailingFolder) + lastTrailingFolder.length());
            entryName = baseFolder + entryName.replaceFirst("^/", "");
            this.zos.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                this.zos.write(buffer, 0, bytesRead);
            }

            this.zos.closeEntry();
        }
    }

    public void addResourceToZip(String resourcePath, String baseFolder) throws IOException {
        String normalizedPath = resourcePath.replace("\\", "/");

        try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
            if (resourceStream == null) {
                VSPE.getInstancedLogger().severe("Resource not found: " + normalizedPath);
                return;
            }

            String entryName = normalizedPath.substring(normalizedPath.indexOf("dimension/") + "dimension/".length());
            entryName = baseFolder + entryName;
            this.zos.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                this.zos.write(buffer, 0, bytesRead);
            }

            this.zos.closeEntry();
        }
    }

    public boolean writeStringToBaseDirectory(String stringContent, String fileName) throws IOException {
        if (stringContent != null && fileName != null && !fileName.isEmpty()) {
            this.zos.putNextEntry(new ZipEntry(fileName));
            byte[] contentBytes = stringContent.getBytes();
            this.zos.write(contentBytes, 0, contentBytes.length);
            this.zos.closeEntry();
            return true;
        } else {
            throw new IllegalArgumentException("String content and file name must not be null or empty.");
        }
    }

    public void close() throws IOException {
        this.zos.close();
    }
}
