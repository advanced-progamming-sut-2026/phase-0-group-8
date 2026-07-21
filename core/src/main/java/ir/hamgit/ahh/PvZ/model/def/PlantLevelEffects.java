package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

public final class PlantLevelEffects {

    private PlantLevelEffects() {
    }

    public static String getEffectAtLevel(PlantType type, int level) {
        PlantDef definition = PlantRegistry.get(type);
        return definition == null ? "" : definition.getLevelEffect(level);
    }

    public static int sum(PlantType type, int level, String prefix) {
        int result = 0;
        for (int current = 2; current <= Math.min(4, level); current++) {
            String effect = getEffectAtLevel(type, current);
            if (effect.startsWith(prefix)) {
                result += numberAfter(effect, prefix);
            }
        }
        return result;
    }

    public static boolean has(PlantType type, int level, String effectName) {
        for (int current = 2; current <= Math.min(4, level); current++) {
            if (getEffectAtLevel(type, current).equalsIgnoreCase(effectName)) {
                return true;
            }
        }
        return false;
    }

    public static int registeredCount() {
        return (int) PlantRegistry.getAll().stream()
            .filter(definition -> definition.getLevelEffects().size() == 3).count();
    }

    private static int numberAfter(String effect, String prefix) {
        String remainder = effect.substring(prefix.length()).trim();
        int end = 0;
        while (end < remainder.length() && Character.isDigit(remainder.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(remainder.substring(0, end));
    }
}
