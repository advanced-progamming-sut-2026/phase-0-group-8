package ir.hamgit.ahh.PvZ.controller;

import java.util.HashMap;
import java.util.Map;

public class CommandParser {

    public static Map<String, String> parse(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String trimmed = raw.trim();
        String[] tokens = trimmed.split("\\s+");
        StringBuilder command = new StringBuilder();
        int i = 0;
        while (i < tokens.length && !tokens[i].startsWith("-")) {
            if (command.length() > 0) {
                command.append(" ");
            }
            command.append(tokens[i]);
            i++;
        }
        result.put("command", command.toString().trim());
        while (i < tokens.length) {
            String token = tokens[i];
            if (token.startsWith("-") && i + 1 < tokens.length && !tokens[i + 1].startsWith("-")) {
                String key = token.substring(1);
                StringBuilder value = new StringBuilder(tokens[i + 1]);
                i += 2;
                while (i < tokens.length && !tokens[i].startsWith("-")) {
                    value.append(" ").append(tokens[i]);
                    i++;
                }
                result.put(key, value.toString().trim());
                result.put(token, value.toString().trim());
            } else if (token.startsWith("-")) {
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
        String val = getFlag(flags, key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim().split("\\s+", 2)[0]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String normalizeKey(String key) {
        return key != null && key.startsWith("-") ? key.substring(1) : key;
    }

    public static String getCommand(Map<String, String> flags) {
        return flags.getOrDefault("command", "");
    }
}
