package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils;

import org.vicky.vspe.platform.VSPEPlatformPlugin;

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
        URL resourceUrl = VSPEPlatformPlugin.get().getClass().getClassLoader().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        } else {
            File resourceFile = Paths.get(resourceUrl.toURI()).toFile();
            if (!resourceFile.exists()) {
                throw new FileNotFoundException("Resource file does not exist: " + resourceFile.getPath());
            } else {
                return resourceFile;
            }
        }
    }

    public static int generateRandomNumber() {
        Random random = new Random();
        int firstDigit = random.nextInt(9) + 1;
        int remainingDigits = random.nextInt(1000);
        return firstDigit * 1000 + remainingDigits;
    }

    public static String generateRandomSeed() {
        Random random = new Random();
        int firstDigit = random.nextInt(1, 9);
        int firstSet = random.nextInt(1999999999);
        int secondSet = random.nextInt(1999999999);
        return "" + firstDigit + firstSet + secondSet;
    }

    public static String generateRandomFourLetterString() {
        Random random = new Random();
        StringBuilder result = new StringBuilder(4);

        for (int i = 0; i < 4; i++) {
            char randomLetter = (char) (65 + random.nextInt(26));
            result.append(randomLetter);
        }

        return result.toString();
    }

    public static File createTemporaryZipFile() throws IOException {
        File tempFile = File.createTempFile("tempZip", ".zip", new File("./plugins/VickyEs_Survival_Plus_Essentials/"));
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
        if (stackTrace.length > 3) {
            String callerClassName = stackTrace[3].getClassName();

            try {
                Class<?> callerClass = Class.forName(callerClassName);
                return targetClass.isAssignableFrom(callerClass) && !targetClass.equals(callerClass);
            } catch (ClassNotFoundException var4) {
                var4.printStackTrace();
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
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1), Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
