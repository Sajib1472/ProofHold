package com.proofhold.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class AnswerHasher {

    private AnswerHasher() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalize(raw).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
