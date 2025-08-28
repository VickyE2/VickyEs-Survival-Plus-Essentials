package org.vicky.vspe.utilities;

public class NumberToWords {
    private static final String[] units = {
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
    };

    private static final String[] teens = {
            "eleven", "twelve", "thirteen", "fourteen", "fifteen",
            "sixteen", "seventeen", "eighteen", "nineteen"
    };

    private static final String[] tens = {
            "", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    };

    public static String convert(int number) {
        if (number < 0 || number > 99) {
            throw new IllegalArgumentException("Number out of range (0-99)");
        }

        if (number < 10) {
            return (units[number]).toUpperCase();
        } else if (number < 20) {
            return (teens[number - 11]).toUpperCase();
        } else {
            int unit = number % 10;
            int ten = number / 10;
            return (tens[ten] + (unit != 0 ? "_" + units[unit] : "")).toUpperCase();
        }
    }
}

