package com.proofhold.web;

public final class ETags {

    private ETags() {}

    public static String quote(Integer version) {
        int value = version == null ? 0 : version;
        return "\"" + value + "\"";
    }

    public static int parse(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionFailedException("If-Match is required.");
        }
        String trimmed = ifMatch.trim();
        if (trimmed.startsWith("W/")) {
            trimmed = trimmed.substring(2).trim();
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new PreconditionFailedException("If-Match is not a valid item version.");
        }
    }
}
