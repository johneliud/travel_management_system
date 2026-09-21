package com.travelmanagementsystem.shared;

import java.security.SecureRandom;

public final class TestData {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@$!%*?&#";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;

    private TestData() {
    }

    public static String generateValidPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);

        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        for (int i = 4; i < 16; i++) {
            sb.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }
        return sb.toString();
    }
}
