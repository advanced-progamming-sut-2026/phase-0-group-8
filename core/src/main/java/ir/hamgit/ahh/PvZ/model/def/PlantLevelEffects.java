package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

/** Exact level-two through level-four effects transcribed from plants.csv. */
public final class PlantLevelEffects {

    private static final Map<PlantType, String[]> EFFECTS = new EnumMap<>(PlantType.class);

    static {
        registerSunAndPeaPlants();
        registerSpecialShooters();
        registerLobbersAndTraps();
        registerDefenseAndUtility();
        registerMints();
    }

    private PlantLevelEffects() {
    }

    private static void registerSunAndPeaPlants() {
        register(PlantType.SUNFLOWER, "Prod. Time -2s", "HP +150", "Double Sun Chance");
        register(PlantType.TWIN_SUNFLOWER, "Prod. Time -2s", "HP +150", "Cost -25");
        register(PlantType.SUN_SHROOM, "Grow Time -5s", "HP +150", "Double Sun Chance");
        register(PlantType.PRIMAL_SUNFLOWER, "Prod. Time -2s", "HP +150", "Cost -25");
        register(PlantType.GOLD_BLOOM, "Cooldown -5s", "Sun +50", "Cost -25");
        register(PlantType.PEASHOOTER, "Dmg +10", "HP +150", "Cost -25");
        register(PlantType.REPEATER, "Dmg +10", "HP +200", "Cost -25");
        register(PlantType.THREEPEATER, "Cost -25", "Dmg +10", "HP +200");
        register(PlantType.SNOW_PEA, "Dmg +10", "Chill Time +2s", "Cost -25");
        register(PlantType.ROTOBAGA, "Dmg +10", "HP +150", "Cost -25");
        register(PlantType.PEA_POD, "Dmg +10", "HP +200", "Cost -25");
        register(PlantType.SPLIT_PEA, "Dmg +10", "HP +200", "Cost -25");
    }

    private static void registerSpecialShooters() {
        register(PlantType.CITRON, "Charge Time -1s", "Dmg +150", "Cost -50");
        register(PlantType.CAULIPOWER, "Cooldown -2s", "HP +150", "Cost -50");
        register(PlantType.ELECTRIC_BLUEBERRY, "Cooldown -2s", "Target Priority Up", "Cost -25");
        register(PlantType.BOWLING_BULB, "Regen -1s", "Dmg +15", "Cost -25");
        register(PlantType.CACTUS, "Pierce +1", "Dmg +10", "Cost -25");
        register(PlantType.FIRE_PEASHOOTER, "Dmg +10", "HP +200", "Cost -25");
        register(PlantType.STARFRUIT, "Atk Speed +10%", "Dmg +10", "Cost -25");
        register(PlantType.GOO_PEASHOOTER, "Dmg/Tick +5", "HP +150", "Cost -25");
        register(PlantType.MEGA_GATLING_PEA, "Dmg +10", "Plant Food Chance +5%", "Cost -50");
        register(PlantType.SEA_SHROOM, "Range +1 Tile", "Dmg +5", "Lifespan +10s");
        register(PlantType.PUFF_SHROOM, "Lifespan +10s", "Dmg +10", "Range +1 Tile");
        register(PlantType.FUME_SHROOM, "Range +1 Tile", "Dmg +10", "Cost -25");
    }

    private static void registerLobbersAndTraps() {
        register(PlantType.CABBAGE_PULT, "Dmg +10", "Atk Speed +15%", "HP +150");
        register(PlantType.KERNEL_PULT, "Butter +5%", "Dmg +10", "HP +150");
        register(PlantType.MELON_PULT, "Cost -25", "AoE Dmg +15", "Dmg +30");
        register(PlantType.WINTER_MELON, "Cost -50", "AoE Dmg +15", "Cost -25");
        register(PlantType.PEPPER_PULT, "Dmg +15", "Warmth Radius +1", "Cost -25");
        register(PlantType.POTATO_MINE, "Arm Time -3s", "Cooldown -5s", "Dmg +600");
        register(PlantType.PRIMAL_POTATO_MINE, "Arm Time -1s", "Cooldown -3s", "Dmg +400");
        register(PlantType.CHERRY_BOMB, "Cooldown -5s", "Dmg +600", "Cost -25");
        register(PlantType.SQUASH, "Cooldown -3s", "Dmg +600", "Can crush 2x");
        register(PlantType.GRAPESHOT, "Dmg +600", "Bounces +1", "Cost -25");
        register(PlantType.JALAPENO, "Cooldown -5s", "Dmg +600", "Cost -25");
        register(PlantType.DOOM_SHROOM, "Cooldown -5s", "Dmg +800", "Cost -50");
        register(PlantType.TANGLE_KELP, "Cooldown -5s", "Targets +1", "Cost -25");
        register(PlantType.ICEBERG_LETTUCE, "Cooldown -2s", "Freeze Time +2s", "Cost -0");
    }

    private static void registerDefenseAndUtility() {
        registerMeleeAndDefense();
        registerSupportPlants();
    }

    private static void registerMeleeAndDefense() {
        register(PlantType.BONK_CHOY, "Dmg +5", "Atk Speed +10%", "HP +200");
        register(PlantType.PHAT_BEET, "Dmg +10", "Atk Speed +10%", "HP +200");
        register(PlantType.CHOMPER, "Digest -2s", "HP +200", "Digest -3s");
        register(PlantType.WASABI_WHIP, "Dmg +10", "Range +1 Tile", "HP +200");
        register(PlantType.KIWIBEAST, "HP +200", "Dmg +15", "Max Size +1");
        register(PlantType.WALL_NUT, "HP +1000", "Cooldown -5s", "HP +1500");
        register(PlantType.TALL_NUT, "HP +2000", "Cooldown -5s", "HP +3000");
        register(PlantType.ENDURIAN, "Reflect Dmg +5", "HP +1000", "Cost -25");
        register(PlantType.GARLIC, "HP +150", "Cooldown -3s", "HP +250");
        register(PlantType.SWEET_POTATO, "HP +1000", "Cooldown -5s", "HP +1500");
        register(PlantType.EXPLODE_O_NUT, "HP +1000", "Explode Dmg +200", "Cost -25");
        register(PlantType.PUMPKIN, "HP +1000", "Cooldown -5s", "HP +1500");
        register(PlantType.SUN_BEAN, "Sun Drop +5", "HP +150", "Cost -25");
    }

    private static void registerSupportPlants() {
        register(PlantType.TORCHWOOD, "HP +300", "AoE on Death", "Cost -25");
        register(PlantType.MAGNET_SHROOM, "Range +1 Tile", "Cooldown -5s", "HP +200");
        register(PlantType.HYPNO_SHROOM, "Cost -25", "Zombie HP Buff", "Zombie Dmg Buff");
        register(PlantType.CAT_TAIL, "Dmg +10", "HP +200", "Cost -25");
        register(PlantType.IMITATER, "Cooldown -2s", "Cost -25", "plant food on enterance");
        register(PlantType.ICE_SHROOM, "Freeze Time +2s", "Cooldown -5s", "Dmg +50");
        register(PlantType.LILY_PAD, "Cost -25", "HP +200", "Cooldown -2s");
        register(PlantType.HOT_POTATO, "Cooldown -2s", "Melt Area 3x3", "Explode on Finish");
        register(PlantType.GRAVE_BUSTER, "Eat Time -1s", "Cooldown -2s", "Explode on Finish");
    }

    private static void registerMints() {
        registerMint(PlantType.ENLIGHTEN_MINT);
        registerMint(PlantType.APPEASE_MINT);
        registerMint(PlantType.ARMA_MINT);
        registerMint(PlantType.BOMBARD_MINT);
        registerMint(PlantType.ENFORCE_MINT);
        registerMint(PlantType.REINFORCE_MINT);
        registerMint(PlantType.ENCHANT_MINT);
        registerMint(PlantType.PIERCE_MINT);
        registerMint(PlantType.CATTAIL_MINT);
    }

    private static void registerMint(PlantType type) {
        register(type, "Duration +1s", "Cooldown -5s", "reset family cooldowns");
    }

    private static void register(PlantType type, String levelTwo, String levelThree, String levelFour) {
        EFFECTS.put(type, new String[] {levelTwo, levelThree, levelFour});
    }

    public static String getEffectAtLevel(PlantType type, int level) {
        String[] values = EFFECTS.get(type);
        return values == null || level < 2 || level > 4 ? "" : values[level - 2];
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
        return EFFECTS.size();
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
