package org.vicky.vspe.systems.Dimension.Generator.utils;

import com.sk89q.worldedit.util.io.ResourceLoader;
import org.vicky.vspe.VSPE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class Utilities {
    public static String getCleanedID(String id) {
        return id.toUpperCase().replaceAll("[^A-Z]", "_");
    }

    public static File getResourceAsFile(String resourcePath) throws IOException, URISyntaxException {
        // Get the resource as a URL
        URL resourceUrl = ResourceLoader.class.getClassLoader().getResource(resourcePath);

        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }

        // Convert URL to File
        File resourceFile = Paths.get(resourceUrl.toURI()).toFile();

        // Check if the file exists
        if (!resourceFile.exists()) {
            throw new FileNotFoundException("Resource file does not exist: " + resourceFile.getPath());
        }

        return resourceFile;
    }

    public static int generateRandomNumber() {
        Random random = new Random();

        // First digit (1-9)
        int firstDigit = random.nextInt(9) + 1;

        // Remaining 3 digits (000-999)
        int remainingDigits = random.nextInt(1000); // Generates a number between 0 and 999

        // Combine the first digit with the remaining digits
        return firstDigit * 1000 + remainingDigits;
    }

    public static File createTemporaryZipFile() throws IOException {
        // Create a temporary file with a .zip extension
        File tempFile = File.createTempFile("tempZip", ".zip", VSPE.getPlugin().getDataFolder());

        // Ensure the temporary file is deleted when the JVM exits
        tempFile.deleteOnExit();
        return tempFile;
    }

    public static String getIndentedBlock(String block, String currentIndentation) {
        StringBuilder sb = new StringBuilder();
        String[] lines = block.split("\n");
        for (String line : lines) {
            sb.append(currentIndentation).append(line).append("\n");
        }
        return sb.toString();
    }

    public static String generateCommonName(List<String> names) {
        return names.get(0) + "_GROUP";
    }
}
