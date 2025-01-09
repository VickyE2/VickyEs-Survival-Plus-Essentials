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


    public static String generateRandomFourLetterString() {
        Random random = new Random();
        StringBuilder result = new StringBuilder(4);

        for (int i = 0; i < 4; i++) {
            char randomLetter = (char) ('A' + random.nextInt(26)); // Random letter from 'A' to 'Z'
            result.append(randomLetter);
        }

        return result.toString();
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

    public static boolean isCallerSubclassOf(Class<?> targetClass) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length > 3) { // Ensure there's a caller in the stack
            String callerClassName = stackTrace[3].getClassName(); // Calling class
            try {
                Class<?> callerClass = Class.forName(callerClassName);
                return targetClass.isAssignableFrom(callerClass) && !targetClass.equals(callerClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public static int getLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
