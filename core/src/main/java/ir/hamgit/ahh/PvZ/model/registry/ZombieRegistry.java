package ir.hamgit.ahh.PvZ.model.registry;


import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Static in-memory catalogue of every {@link ZombieDef}, populated once in
 * the static initializer (per the class reference).
 *
 * <p>Armor HP values (cone=370, bucket=1100, helmet=shoulder=1600, block=2200)
 * come directly from the spec. Base zombie HP/speed/damage/waveCost numbers
 * are <b>not</b> given exact values anywhere in the spec (only armor HP is),
 * so the figures below are reasonable placeholders for balancing/demo
 * purposes - tune them freely, nothing else in the engine depends on the
 * exact numbers. The <i>mechanics</i> (which behaviors each zombie has) are
 * taken directly from the "زامبی‌ها" section of the spec.</p>
 */
public final class ZombieRegistry {

    private static final Map<ZombieType, ZombieDef> ALL = new EnumMap<>(ZombieType.class);
    private static final String ALL_CHAPTERS = "all";
    private static final String EGYPT = "ANCIENT_EGYPT";
    private static final String FROSTBITE = "FROSTBITE_CAVES";
    private static final String BEACH = "BIG_WAVE_BEACH";
    private static final String DARK_AGES = "DARK_AGES";

    static {
        registerCommonZombies();
        registerEgyptZombies();
        registerFrostbiteZombies();
        registerBeachZombies();
        registerDarkAgesZombies();
    }

    private ZombieRegistry() {
    }

    public static ZombieDef get(ZombieType type) {
        return ALL.get(type);
    }

    public static Map<ZombieType, ZombieDef> getAll() {
        return ALL;
    }

    public static List<ZombieDef> getForChapter(String chapter) {
        List<ZombieDef> result = new ArrayList<>();
        for (ZombieDef def : ALL.values()) {
            if (ALL_CHAPTERS.equals(def.getChapter()) || def.getChapter().equalsIgnoreCase(chapter)) {
                result.add(def);
            }
        }
        return result;
    }

    private static void registerCommonZombies() {
        add(ZombieType.NORMAL, "Zombie", 270, 1.0, 20, 100, armors(), behaviors(), ALL_CHAPTERS);
        add(ZombieType.CONEHEAD, "Conehead Zombie", 270, 1.0, 20, 150,
            armors(ArmorType.CONE), behaviors(), ALL_CHAPTERS);
        add(ZombieType.BUCKETHEAD, "Buckethead Zombie", 270, 1.0, 20, 250,
            armors(ArmorType.BUCKET), behaviors(), ALL_CHAPTERS);
        add(ZombieType.KNIGHT, "Knight Zombie", 400, 0.9, 30, 400,
            armors(ArmorType.HELMET, ArmorType.SHOULDER), behaviors(), ALL_CHAPTERS);
        add(ZombieType.BLOCKHEAD, "Blockhead Zombie", 400, 0.8, 30, 500,
            armors(ArmorType.BLOCK), behaviors(), ALL_CHAPTERS);
        add(ZombieType.GARGANTUAR, "Gargantuar", 3000, 0.5, 200, 1000,
            armors(), behaviors(BehaviorType.THROW_IMP, BehaviorType.INSTANT_KILL_PLANT), ALL_CHAPTERS);
        add(ZombieType.IMP, "Imp", 90, 1.6, 30, 50, armors(), behaviors(), ALL_CHAPTERS);
        add(ZombieType.ALL_STAR, "All-star Zombie", 500, 2.2, 40, 300,
            armors(), behaviors(BehaviorType.INSTANT_KILL_PLANT), ALL_CHAPTERS);
        add(ZombieType.ARCADE, "Arcade Zombie", 270, 1.1, 20, 250,
            armors(ArmorType.ARCADE_MACHINE), behaviors(BehaviorType.INSTANT_KILL_PLANT, BehaviorType.PUSH_OBJECT),
            ALL_CHAPTERS);
        add(ZombieType.PARASOL, "Parasol Zombie", 270, 1.0, 20, 200,
            armors(), behaviors(BehaviorType.REFLECT_PROJECTILES), ALL_CHAPTERS);
        add(ZombieType.TURQUOISE, "Turquoise Zombie", 270, 1.0, 20, 350,
            armors(), behaviors(BehaviorType.STEAL_SUN, BehaviorType.LASER_DESTROY), ALL_CHAPTERS);
        add(ZombieType.PROSPECTOR, "Prospector Zombie", 270, 1.0, 20, 300,
            armors(), behaviors(BehaviorType.DYNAMITE_EXPLOSION, BehaviorType.MOVE_REVERSE), ALL_CHAPTERS);
        add(ZombieType.PIANIST, "Pianist Zombie", 270, 0.9, 20, 300,
            armors(ArmorType.PIANO), behaviors(BehaviorType.ROW_SHUFFLE), ALL_CHAPTERS);
        add(ZombieType.NEWSPAPER, "Newspaper Zombie", 270, 1.0, 20, 250,
            armors(ArmorType.NEWSPAPER), behaviors(BehaviorType.NEWSPAPER_RAGE), ALL_CHAPTERS);
        add(ZombieType.BARREL_ROLLER, "Barrel Roller Zombie", 270, 1.1, 20, 350,
            armors(ArmorType.BARREL), behaviors(BehaviorType.SPAWN_IMP_FROM_BARREL, BehaviorType.PUSH_OBJECT),
            ALL_CHAPTERS);
    }

    private static void registerEgyptZombies() {
        add(ZombieType.RA_ZOMBIE, "Ra Zombie", 340, 1.0, 20, 300,
            armors(), behaviors(BehaviorType.STEAL_SUN), EGYPT);
        add(ZombieType.EXPLORER, "Explorer Zombie", 300, 1.0, 20, 300,
            armors(), behaviors(BehaviorType.TORCH_BURN), EGYPT);
        add(ZombieType.TOMBRAISER, "Tombraiser", 320, 0.9, 20, 350,
            armors(), behaviors(BehaviorType.THROW_TOMBSTONE), EGYPT);
    }

    private static void registerFrostbiteZombies() {
        add(ZombieType.DODO_RIDER, "Dodo Rider Zombie", 270, 1.3, 20, 300,
            armors(), behaviors(BehaviorType.FLY_OVER_OBSTACLES), FROSTBITE);
        add(ZombieType.HUNTER, "Hunter Zombie", 300, 1.0, 20, 350,
            armors(), behaviors(BehaviorType.FREEZE_PLANT), FROSTBITE);
        add(ZombieType.TROGLOBITE, "Troglobite", 320, 1.0, 20, 350,
            armors(), behaviors(BehaviorType.PUSH_OBJECT, BehaviorType.INSTANT_KILL_PLANT), FROSTBITE);
    }

    private static void registerBeachZombies() {
        add(ZombieType.FISHERMAN, "Fisherman Zombie", 270, 0.0, 20, 300,
            armors(), behaviors(BehaviorType.HOOK_PLANT), BEACH);
        add(ZombieType.SNORKEL, "Snorkel Zombie", 270, 1.0, 20, 300,
            armors(), behaviors(BehaviorType.SWIM_UNDERWATER), BEACH);
        add(ZombieType.OCTOPUS, "Octopus Zombie", 300, 1.0, 20, 350,
            armors(), behaviors(BehaviorType.FREEZE_PLANT), BEACH);
    }

    private static void registerDarkAgesZombies() {
        add(ZombieType.JESTER, "Jester Zombie", 270, 1.0, 20, 300,
            armors(), behaviors(BehaviorType.REFLECT_PROJECTILES), DARK_AGES);
        add(ZombieType.WIZARD, "Wizard Zombie", 300, 1.0, 20, 350,
            armors(), behaviors(BehaviorType.TRANSFORM_PLANT_CAT), DARK_AGES);
        add(ZombieType.KING, "King", 500, 0.0, 0, 500,
            armors(), behaviors(BehaviorType.UPGRADE_ZOMBIES), DARK_AGES);
        add(ZombieType.DRAGON_IMP, "Imp Dragon", 90, 1.6, 30, 60,
            armors(), behaviors(BehaviorType.IMMUNE_TO_FIRE), DARK_AGES);
    }

    private static void add(ZombieType type, String name, int maxHp, double speed, int damage, int waveCost,
                            List<ArmorType> armors, List<BehaviorType> behaviors, String chapter) {
        ALL.put(type, new ZombieDef(type, name, maxHp, speed, damage, waveCost, armors, behaviors, chapter));
    }

    private static List<ArmorType> armors(ArmorType... values) {
        List<ArmorType> list = new ArrayList<>();
        for (ArmorType a : values) {
            list.add(a);
        }
        return list;
    }

    private static List<BehaviorType> behaviors(BehaviorType... values) {
        List<BehaviorType> list = new ArrayList<>();
        for (BehaviorType b : values) {
            list.add(b);
        }
        return list;
    }
}
