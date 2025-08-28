package org.vicky.vspe.utilities;

import java.security.SecureRandom;

public class RandomStringGenerator {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^*()-_=+?/";
    private static final RandomStringGenerator INSTANCE = new RandomStringGenerator();
    private final SecureRandom random = new SecureRandom();

    private RandomStringGenerator() {
    }

    public static RandomStringGenerator getInstance() {
        return INSTANCE;
    }

    public String generate(int length, boolean includeUpper, boolean includeLower, boolean includeDigits, boolean includeSymbols) {
        StringBuilder pool = new StringBuilder();
        if (includeUpper) {
            pool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        if (includeLower) {
            pool.append("abcdefghijklmnopqrstuvwxyz");
        }

        if (includeDigits) {
            pool.append("0123456789");
        }

        if (includeSymbols) {
            pool.append("!@#$%^*()-_=+?/");
        }

        if (pool.length() == 0) {
            throw new IllegalArgumentException("At least one character type must be included.");
        } else {
            StringBuilder result = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                int index = this.random.nextInt(pool.length());
                result.append(pool.charAt(index));
            }

            return result.toString();
        }
    }

    public String generatePassword(int length) {
        return this.generate(length, true, true, true, true);
    }
}
