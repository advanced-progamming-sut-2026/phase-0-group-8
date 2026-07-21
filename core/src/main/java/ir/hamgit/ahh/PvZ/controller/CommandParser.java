package ir.hamgit.ahh.PvZ.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern COORDINATES = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");

    public static Map<String, String> parse(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String trimmed = raw.trim();
        String[] tokens = trimmed.split("\\s+");
        StringBuilder command = new StringBuilder();
        int i = 0;
        while (i < tokens.length && !isFlagToken(tokens[i])) {
            if (command.length() > 0) {
                command.append(" ");
            }
            command.append(tokens[i]);
            i++;
        }
        result.put("command", command.toString().trim());
        while (i < tokens.length) {
            String token = tokens[i];
            if (isFlagToken(token) && i + 1 < tokens.length && !isFlagToken(tokens[i + 1])) {
                String key = token.substring(1);
                StringBuilder value = new StringBuilder(tokens[i + 1]);
                i += 2;
                while (i < tokens.length && !isFlagToken(tokens[i])) {
                    value.append(" ").append(tokens[i]);
                    i++;
                }
                result.put(key, value.toString().trim());
                result.put(token, value.toString().trim());
            } else if (isFlagToken(token)) {
                result.put(token.substring(1), "true");
                result.put(token, "true");
                i++;
            } else {
                i++;
            }
        }
        return result;
    }

    public static String getFlag(Map<String, String> flags, String key) {
        return flags.get(normalizeKey(key));
    }

    public static int getIntFlag(Map<String, String> flags, String key, int defaultValue) {
        Integer value = getIntegerFlag(flags, key);
        return value == null ? defaultValue : value;
    }

    public static Integer getIntegerFlag(Map<String, String> flags, String key) {
        return parseInteger(getFlag(flags, key));
    }

    public static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim().split("\\s+", 2)[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int[] parseCoordinates(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = COORDINATES.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        Integer x = parseInteger(matcher.group(1));
        Integer y = parseInteger(matcher.group(2));
        return x == null || y == null ? null : new int[] {x, y};
    }

    private static String normalizeKey(String key) {
        return key != null && key.startsWith("-") ? key.substring(1) : key;
    }

    private static boolean isFlagToken(String token) {
        return token.startsWith("-") && !token.matches("-\\d+");
    }

    public static String getCommand(Map<String, String> flags) {
        return flags.getOrDefault("command", "");
    }
}
