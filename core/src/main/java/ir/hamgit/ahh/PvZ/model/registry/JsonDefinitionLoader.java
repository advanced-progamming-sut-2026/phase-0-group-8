package ir.hamgit.ahh.PvZ.model.registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonDefinitionLoader {

    private JsonDefinitionLoader() {
    }

    static List<Map<String, Object>> readObjects(String location, String section) {
        String json = readText(location);
        Object root = new Parser(json).parseDocument();
        if (!(root instanceof Map<?, ?> rawRoot)) {
            throw new IllegalArgumentException(location + " must contain a JSON object");
        }
        Object selected = stringKeyMap(rawRoot).get(section);
        if (!(selected instanceof List<?> values)) {
            throw new IllegalArgumentException(location + " must contain array " + section);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> raw)) {
                throw new IllegalArgumentException(location + " contains a non-object entry");
            }
            result.add(stringKeyMap(raw));
        }
        return result;
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("JSON object keys must be strings");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static String readText(String location) {
        ClassLoader loader = JsonDefinitionLoader.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(location)) {
            if (input != null) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read JSON resource " + location, e);
        }
        return readFile(location);
    }

    private static String readFile(String location) {
        Path direct = Path.of(location);
        Path resource = Path.of("src", "main", "resources").resolve(location);
        Path path = Files.isRegularFile(direct) ? direct : resource;
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing JSON definition file: " + location, e);
        }
    }

    static String text(Map<String, Object> row, String key) {
        Object value = required(row, key);
        if (!(value instanceof String result)) {
            throw wrongType(key, "string");
        }
        return result;
    }

    static int integer(Map<String, Object> row, String key) {
        Object value = required(row, key);
        if (!(value instanceof Number number)) {
            throw wrongType(key, "number");
        }
        return number.intValue();
    }

    static double decimal(Map<String, Object> row, String key) {
        Object value = required(row, key);
        if (!(value instanceof Number number)) {
            throw wrongType(key, "number");
        }
        return number.doubleValue();
    }

    static boolean bool(Map<String, Object> row, String key) {
        Object value = required(row, key);
        if (!(value instanceof Boolean result)) {
            throw wrongType(key, "boolean");
        }
        return result;
    }

    static <E extends Enum<E>> List<E> enums(Map<String, Object> row, String key,
                                             Class<E> enumType) {
        Object value = required(row, key);
        if (!(value instanceof List<?> values)) {
            throw wrongType(key, "array");
        }
        List<E> result = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof String name)) {
                throw wrongType(key, "string array");
            }
            result.add(Enum.valueOf(enumType, name));
        }
        return result;
    }

    private static Object required(Map<String, Object> row, String key) {
        if (!row.containsKey(key)) {
            throw new IllegalArgumentException("Missing required JSON field: " + key);
        }
        return row.get(key);
    }

    private static IllegalArgumentException wrongType(String key, String expected) {
        return new IllegalArgumentException("JSON field " + key + " must be a " + expected);
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source;
        }

        private Object parseDocument() {
            Object result = parseValue();
            skipWhitespace();
            if (index != source.length()) {
                fail("Unexpected trailing content");
            }
            return result;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= source.length()) {
                return fail("Unexpected end of JSON");
            }
            return switch (source.charAt(index)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            do {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
            } while (consume(','));
            expect('}');
            return result;
        }

        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            do {
                result.add(parseValue());
                skipWhitespace();
            } while (consume(','));
            expect(']');
            return result;
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char current = source.charAt(index++);
                if (current == '"') {
                    return result.toString();
                }
                result.append(current == '\\' ? parseEscape() : current);
            }
            return fail("Unterminated JSON string");
        }

        private char parseEscape() {
            if (index >= source.length()) {
                return fail("Unterminated JSON escape");
            }
            char escaped = source.charAt(index++);
            return switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> parseUnicode();
                default -> fail("Invalid JSON escape");
            };
        }

        private char parseUnicode() {
            if (index + 4 > source.length()) {
                return fail("Invalid Unicode escape");
            }
            String digits = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(digits, 16);
            } catch (NumberFormatException e) {
                return fail("Invalid Unicode escape");
            }
        }

        private Object parseLiteral(String token, Object value) {
            if (!source.startsWith(token, index)) {
                return fail("Invalid JSON literal");
            }
            index += token.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (consume('-')) {
                start = Math.min(start, index - 1);
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (consume('.')) {
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            String value = source.substring(start, index);
            try {
                return value.contains(".") ? Double.parseDouble(value) : Long.parseLong(value);
            } catch (NumberFormatException e) {
                return fail("Invalid JSON number");
            }
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                fail("Expected '" + expected + "'");
            }
        }

        private <T> T fail(String message) {
            throw new IllegalArgumentException(message + " at JSON offset " + index);
        }
    }
}
