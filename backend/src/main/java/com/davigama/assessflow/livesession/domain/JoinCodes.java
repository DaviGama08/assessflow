package com.davigama.assessflow.livesession.domain;

import java.security.SecureRandom;
import java.util.Locale;

public final class JoinCodes {
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodes() {}

    public static String generate() {
        char[] chars = new char[6];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new String(chars);
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean valid(String code) {
        return normalize(code).matches("[" + ALPHABET + "]{6}");
    }
}
