package org.vicky.vspe.utilities.Math;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.regex.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String text = new String(Files.readAllBytes(Paths.get("C:/Users/Java/Downloads/VickyEs-Survival-Plus-Essentials/VSPE/src/main/java/org/vicky/vspe/utilities/Math/logs.txt")));


        long count = Pattern.compile("^.*Placing batch of blocks\\.\\.\\.\\s*\\d+\\s*/\\s*\\d+.*$", Pattern.MULTILINE)
                .matcher(text)
                .results()
                .count();

        System.out.println("Matched lines count: " + count);
    }
}
