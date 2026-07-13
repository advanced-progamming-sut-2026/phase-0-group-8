package ir.hamgit.ahh.PvZ.controller;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parses simple "-flag value value..." style CLI tokens out of a raw command line. */
public final class CommandParser {

    private CommandParser() {
    }

    public static Map<String, String> parse(String raw) {
        Map<String, String> flags = new LinkedHashMap<>();
        String[] tokens = raw.trim().split("\\s+");
        String currentFlag = null;
        StringBuilder value = new StringBuilder();
        for (String token : tokens) {
            if (isFlagToken(token)) {
                storeFlag(flags, currentFlag, value);
                currentFlag = token;
                value = new StringBuilder();
            } else if (currentFlag != null) {
                appendValue(value, token);
            }
        }
        storeFlag(flags, currentFlag, value);
        return flags;
    }

    private static boolean isFlagToken(String token) {
        return token.startsWith("-") && !token.matches("^-\\d+(\\.\\d+)?$");
    }

    private static void appendValue(StringBuilder value, String token) {
        if (value.length() > 0) {
            value.append(' ');
        }
        value.append(token);
    }

    private static void storeFlag(Map<String, String> flags, String flag, StringBuilder value) {
        if (flag != null) {
            flags.put(flag, value.toString().trim());
        }
    }

    public static String getFlag(Map<String, String> flags, String key) {
        return flags.get(key);
    }

    public static int getIntFlag(Map<String, String> flags, String key, int defaultValue) {
        String v = flags.get(key);
        if (v == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
