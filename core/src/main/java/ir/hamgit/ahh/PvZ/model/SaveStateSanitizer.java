package ir.hamgit.ahh.PvZ.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

 
final class SaveStateSanitizer {

    private SaveStateSanitizer() {
    }

    static <T> List<T> list(List<T> source) {
        List<T> result = new ArrayList<>();
        if (source != null) {
            source.stream().filter(java.util.Objects::nonNull).forEach(result::add);
        }
        return result;
    }

    static <T> Set<T> set(Set<T> source) {
        Set<T> result = new HashSet<>();
        if (source != null) {
            source.stream().filter(java.util.Objects::nonNull).forEach(result::add);
        }
        return result;
    }

    static <K> Map<K, Integer> nonNegativeMap(Map<K, Integer> source) {
        return boundedMap(source, 0, Integer.MAX_VALUE);
    }

    static <K> Map<K, Integer> boundedMap(Map<K, Integer> source, int minimum, int maximum) {
        Map<K, Integer> result = new HashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<K, Integer> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey(), Math.max(minimum, Math.min(maximum, entry.getValue())));
            }
        }
        return result;
    }

    static <K> Map<K, Boolean> booleanMap(Map<K, Boolean> source) {
        Map<K, Boolean> result = new HashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    result.put(key, value);
                }
            });
        }
        return result;
    }
}
