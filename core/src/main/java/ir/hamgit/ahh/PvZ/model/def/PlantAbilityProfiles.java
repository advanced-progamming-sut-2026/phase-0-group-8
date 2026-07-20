package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

public final class PlantAbilityProfiles {

    private static final Map<PlantType, PlantAbilityProfile> PROFILES = new EnumMap<>(PlantType.class);
    private static final Map<PlantType, PlantFamily> FAMILIES = new EnumMap<>(PlantType.class);

    static {
        registerProfiles();
        registerFamilies();
    }

    private PlantAbilityProfiles() {
    }

    public static PlantAbilityProfile get(PlantType type) {
        return PROFILES.getOrDefault(type, PlantAbilityProfile.builder().build());
    }

    public static PlantFamily getFamily(PlantType type) {
        return FAMILIES.get(type);
    }

    private static void registerProfiles() {
        interval(15, PlantType.PEASHOOTER, PlantType.REPEATER, PlantType.THREEPEATER,
            PlantType.SNOW_PEA, PlantType.ROTOBAGA, PlantType.PEA_POD, PlantType.SPLIT_PEA,
            PlantType.CACTUS, PlantType.FIRE_PEASHOOTER, PlantType.STARFRUIT,
            PlantType.GOO_PEASHOOTER, PlantType.MEGA_GATLING_PEA, PlantType.CAT_TAIL);
        profile(PlantType.REPEATER, builder(15).shots(2));
        profile(PlantType.MEGA_GATLING_PEA, builder(15).shots(4));
        profile(PlantType.CITRON, builder(90));
        profile(PlantType.CAULIPOWER, builder(120));
        profile(PlantType.ELECTRIC_BLUEBERRY, builder(120));
        profile(PlantType.BOWLING_BULB, builder(20).damageCycle(40, 120, 180)
            .intervalCycle(20, 50, 100));
        profile(PlantType.CACTUS, builder(15).pierce(3));
        profile(PlantType.SEA_SHROOM, builder(15).range(3).lifespan(600));
        profile(PlantType.PUFF_SHROOM, builder(15).range(3).lifespan(600));
        profile(PlantType.FUME_SHROOM, builder(15).range(5).pierce(9));
        profile(PlantType.MAGNET_SHROOM, builder(100).range(9));
        registerLobbers();
        registerTrapsAndMelee();
    }

    private static void registerLobbers() {
        interval(29, PlantType.CABBAGE_PULT, PlantType.KERNEL_PULT, PlantType.MELON_PULT,
            PlantType.WINTER_MELON, PlantType.PEPPER_PULT);
        profile(PlantType.KERNEL_PULT, builder(29).damageCycle(20, 40));
        profile(PlantType.MELON_PULT, builder(29).splash(1));
        profile(PlantType.WINTER_MELON, builder(29).splash(1));
        profile(PlantType.PEPPER_PULT, builder(29).splash(1));
    }

    private static void registerTrapsAndMelee() {
        profile(PlantType.POTATO_MINE, builder(0).armDelay(150).splash(0));
        profile(PlantType.PRIMAL_POTATO_MINE, builder(0).armDelay(50).splash(1));
        profile(PlantType.CHERRY_BOMB, builder(0).splash(1));
        profile(PlantType.GRAPESHOT, builder(0).splash(1));
        profile(PlantType.BONK_CHOY, builder(3).range(1));
        profile(PlantType.PHAT_BEET, builder(20).range(1).splash(1));
        profile(PlantType.CHOMPER, builder(400).range(1));
        profile(PlantType.WASABI_WHIP, builder(20).range(1));
        profile(PlantType.KIWIBEAST, builder(20).range(1).splash(1).damageCycle(15, 30, 45));
    }

    private static PlantAbilityProfile.Builder builder(int interval) {
        return PlantAbilityProfile.builder().interval(interval);
    }

    private static void profile(PlantType type, PlantAbilityProfile.Builder builder) {
        PROFILES.put(type, builder.build());
    }

    private static void interval(int ticks, PlantType... types) {
        for (PlantType type : types) {
            profile(type, builder(ticks));
        }
    }

    private static void registerFamilies() {
        family(PlantFamily.ENLIGHTEN, PlantType.SUNFLOWER, PlantType.TWIN_SUNFLOWER,
            PlantType.SUN_SHROOM, PlantType.PRIMAL_SUNFLOWER, PlantType.GOLD_BLOOM,
            PlantType.ENLIGHTEN_MINT);
        family(PlantFamily.APPEASE, PlantType.PEASHOOTER, PlantType.REPEATER, PlantType.THREEPEATER,
            PlantType.SNOW_PEA, PlantType.ROTOBAGA, PlantType.PEA_POD, PlantType.SPLIT_PEA,
            PlantType.CITRON, PlantType.BOWLING_BULB, PlantType.FIRE_PEASHOOTER,
            PlantType.STARFRUIT, PlantType.GOO_PEASHOOTER, PlantType.MEGA_GATLING_PEA,
            PlantType.SEA_SHROOM, PlantType.PUFF_SHROOM, PlantType.APPEASE_MINT);
        family(PlantFamily.ARMA, PlantType.CABBAGE_PULT, PlantType.KERNEL_PULT, PlantType.MELON_PULT,
            PlantType.WINTER_MELON, PlantType.PEPPER_PULT, PlantType.ARMA_MINT);
        family(PlantFamily.BOMBARD, PlantType.POTATO_MINE, PlantType.PRIMAL_POTATO_MINE,
            PlantType.CHERRY_BOMB, PlantType.SQUASH, PlantType.GRAPESHOT, PlantType.JALAPENO,
            PlantType.DOOM_SHROOM, PlantType.TANGLE_KELP, PlantType.ICEBERG_LETTUCE,
            PlantType.ICE_SHROOM, PlantType.HOT_POTATO, PlantType.GRAVE_BUSTER,
            PlantType.BOMBARD_MINT);
        registerRemainingFamilies();
    }

    private static void registerRemainingFamilies() {
        family(PlantFamily.ENFORCE, PlantType.BONK_CHOY, PlantType.PHAT_BEET, PlantType.CHOMPER,
            PlantType.WASABI_WHIP, PlantType.KIWIBEAST, PlantType.ENFORCE_MINT);
        family(PlantFamily.REINFORCE, PlantType.WALL_NUT, PlantType.TALL_NUT, PlantType.ENDURIAN,
            PlantType.GARLIC, PlantType.SWEET_POTATO, PlantType.EXPLODE_O_NUT, PlantType.PUMPKIN,
            PlantType.SUN_BEAN, PlantType.REINFORCE_MINT);
        family(PlantFamily.ENCHANT, PlantType.TORCHWOOD, PlantType.HYPNO_SHROOM, PlantType.IMITATER,
            PlantType.LILY_PAD, PlantType.ENCHANT_MINT);
        family(PlantFamily.PIERCE, PlantType.CACTUS, PlantType.FUME_SHROOM, PlantType.PIERCE_MINT);
        family(PlantFamily.CATTAIL, PlantType.CAULIPOWER, PlantType.ELECTRIC_BLUEBERRY,
            PlantType.MAGNET_SHROOM, PlantType.CAT_TAIL, PlantType.CATTAIL_MINT);
    }

    private static void family(PlantFamily family, PlantType... types) {
        for (PlantType type : types) {
            FAMILIES.put(type, family);
        }
    }
}
