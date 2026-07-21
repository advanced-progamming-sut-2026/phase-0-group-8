package ir.hamgit.ahh.PvZ.model.registry;



import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static in-memory catalogue of every {@link PlantDef}.
 *
 * <p><b>Important:</b> the project spec keeps the authoritative plant table in
 * {@code phase1/assets/Data/plants.csv} (49 plants, owned by whoever wires up
 * the collection/shop menus). That file was not available while this module
 * was written, so {@link #registerDefaults()} seeds the registry with a
 * representative subset - one or two examples per plant category described
 * in the doc (producer, shooter, lobber, explosive, wall-nut, trap, ...) -
 * so the board/combat code can be built, compiled and demoed end to end.</p>
 *
 * <p>Once the team's real CSV exists, call {@link #loadFromCsv(String)} to
 * replace these defaults - nothing else in the combat/board code needs to
 * change, since it all reads plant behaviour generically off {@link PlantDef}
 * tags/behaviors rather than switching on plant name.</p>
 */
public final class PlantRegistry {

    private static final Map<PlantType, PlantDef> ALL = new EnumMap<>(PlantType.class);
    private static final int TICKS_PER_SECOND = 10;

    static {
        registerDefaults();
    }

    private PlantRegistry() {
    }

    public static PlantDef get(PlantType type) {
        return ALL.get(type);
    }

    public static Collection<PlantDef> getAll() {
        return ALL.values();
    }

    /**
     * Loads plant definitions from a CSV file, replacing whatever is
     * currently registered. Expected header (order does not matter):
     * {@code type,displayName,sunCost,maxHp,rechargeSeconds,damage,range,tags,
     * behaviors,seedPacketsToUpgrade,coinsToUpgrade,canStackOn,
     * canPlantOnWater,sunProductionAmount,sunProductionIntervalTicks,aoeRadius}.
     * {@code tags} and {@code behaviors} are ';'-separated enum names.
     */
    public static void loadFromCsv(String path) {
        List<String> lines = readLines(path);
        if (lines.isEmpty()) {
            return;
        }
        ALL.clear();
        String[] header = lines.get(0).split(",", -1);
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                registerRow(header, lines.get(i).split(",", -1));
            }
        }
    }

    private static List<String> readLines(String path) {
        try {
            return Files.readAllLines(Path.of(path));
        } catch (IOException e) {
            System.err.println("Could not read plants CSV at " + path + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static void registerRow(String[] header, String[] row) {
        Map<String, String> col = new HashMap<>();
        for (int i = 0; i < header.length && i < row.length; i++) {
            col.put(header[i].trim(), row[i].trim());
        }
        PlantType type = PlantType.valueOf(col.get("type"));
        PlantDef def = new PlantDef(type, col.get("displayName"), parseInt(col, "sunCost"),
            parseInt(col, "maxHp"), parseInt(col, "rechargeSeconds"), parseInt(col, "damage"),
            parseInt(col, "range"), parseTags(col.get("tags")), parseBehaviors(col.get("behaviors")),
            parseInt(col, "seedPacketsToUpgrade"), parseInt(col, "coinsToUpgrade"),
            Boolean.parseBoolean(col.get("canStackOn")), Boolean.parseBoolean(col.get("canPlantOnWater")),
            parseInt(col, "sunProductionAmount"), parseInt(col, "sunProductionIntervalTicks"),
            parseInt(col, "aoeRadius"));
        ALL.put(type, def);
    }

    private static int parseInt(Map<String, String> col, String key) {
        String value = col.get(key);
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static Set<Tag> parseTags(String raw) {
        Set<Tag> set = EnumSet.noneOf(Tag.class);
        if (raw != null && !raw.isBlank()) {
            for (String s : raw.split(";")) {
                set.add(Tag.valueOf(s.trim()));
            }
        }
        return set;
    }

    private static List<BehaviorType> parseBehaviors(String raw) {
        List<BehaviorType> list = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String s : raw.split(";")) {
                list.add(BehaviorType.valueOf(s.trim()));
            }
        }
        return list;
    }

    private static void registerDefaults() {
        addProducers();
        addShooters();
        addLobbers();
        addExplosivesAndTraps();
        addWallNutsAndWater();
        addSupportPlants();
        addRemainingUtilityPlants();
    }

    private static void addProducers() {
        add(PlantType.SUNFLOWER, "Sunflower", 50, 300, 5, 0, 0,
            tags(Tag.DAY, Tag.SUN), behaviors(BehaviorType.PRODUCE_SUN), 5, 25, false, false,
            25, 24 * TICKS_PER_SECOND, 0);
        add(PlantType.TWIN_SUNFLOWER, "Twin Sunflower", 150, 350, 5, 0, 0,
            tags(Tag.DAY, Tag.SUN), behaviors(BehaviorType.PRODUCE_SUN), 8, 40, false, false,
            50, 24 * TICKS_PER_SECOND, 0);
        add(PlantType.SUN_SHROOM, "Sun-shroom", 25, 300, 5, 0, 0,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.RAMP_UP, Tag.SUN), behaviors(BehaviorType.PRODUCE_SUN),
            5, 25, false, false, 15, 24 * TICKS_PER_SECOND, 0);
        add(PlantType.MARIGOLD, "Marigold", 0, 300, 5, 0, 0,
            tags(Tag.SUN), behaviors(), 0, 0, false, false, 0, 0, 0);
    }

    private static void addShooters() {
        add(PlantType.PEASHOOTER, "Peashooter", 100, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD), 5, 25, false, false, 0, 0, 0);
        add(PlantType.REPEATER, "Repeater", 200, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD), 8, 50, false, false, 0, 0, 0);
        add(PlantType.SNOW_PEA, "Snow Pea", 175, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.PEA, Tag.ICE), behaviors(BehaviorType.SHOOT_ICE), 6, 30, false, false, 0, 0, 0);
        add(PlantType.SPLIT_PEA, "Split Pea", 125, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD), 6, 30, false, false, 0, 0, 0);
        add(PlantType.THREEPEATER, "Threepeater", 325, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD), 10, 60, false, false, 0, 0, 0);
        add(PlantType.GATLING_PEA, "Gatling Pea", 250, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD), 10, 60, false, false, 0, 0, 0);
        add(PlantType.PUFF_SHROOM, "Puff-shroom", 0, 300, 5, 20, 5,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD),
            3, 15, false, false, 0, 0, 0);
        add(PlantType.SCAREDY_SHROOM, "Scaredy-shroom", 25, 300, 5, 20, 9,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.PEA), behaviors(BehaviorType.SHOOT_FORWARD),
            4, 20, false, false, 0, 0, 0);
        add(PlantType.FUME_SHROOM, "Fume-shroom", 75, 300, 5, 20, 9,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.POISON), behaviors(BehaviorType.SHOOT_POISON),
            6, 30, false, false, 0, 0, 0);
        add(PlantType.GLOOM_SHROOM, "Gloom-shroom", 150, 300, 5, 30, 3,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.AOE), behaviors(BehaviorType.SHOOT_FORWARD),
            10, 60, false, false, 0, 0, 0);
        add(PlantType.STARFRUIT, "Starfruit", 300, 300, 5, 25, 9,
            tags(Tag.DAY), behaviors(BehaviorType.SHOOT_FORWARD), 10, 60, false, false, 0, 0, 0);
        add(PlantType.CACTUS, "Cactus", 125, 300, 5, 20, 9,
            tags(Tag.DAY), behaviors(BehaviorType.SHOOT_FORWARD), 6, 30, false, false, 0, 0, 0);
        add(PlantType.CATPAIL, "Catpail", 300, 300, 5, 20, 9,
            tags(Tag.DAY, Tag.ICE), behaviors(BehaviorType.SHOOT_ICE), 10, 60, false, false, 0, 0, 0);
    }

    private static void addLobbers() {
        add(PlantType.CABBAGE_PULT, "Cabbage-pult", 100, 300, 5, 40, 9,
            tags(Tag.DAY), behaviors(BehaviorType.SHOOT_ARC), 6, 30, false, false, 0, 0, 0);
        add(PlantType.KERNEL_PULT, "Kernel-pult", 100, 300, 5, 20, 9,
            tags(Tag.DAY), behaviors(BehaviorType.SHOOT_ARC), 6, 30, false, false, 0, 0, 0);
        add(PlantType.MELON_PULT, "Melon-pult", 300, 300, 5, 80, 9,
            tags(Tag.DAY, Tag.AOE), behaviors(BehaviorType.SHOOT_ARC), 10, 60, false, false, 0, 0, 1);
        add(PlantType.WINTER_MELON, "Winter Melon", 500, 300, 5, 80, 9,
            tags(Tag.DAY, Tag.AOE, Tag.ICE), behaviors(BehaviorType.SHOOT_ARC, BehaviorType.SHOOT_ICE),
            12, 70, false, false, 0, 0, 1);
        add(PlantType.COB_CANNON, "Cob Cannon", 500, 300, 30, 300, 9,
            tags(Tag.DAY, Tag.AOE), behaviors(BehaviorType.SHOOT_ARC), 15, 90, false, false, 0, 0, 2);
    }

    private static void addExplosivesAndTraps() {
        add(PlantType.CHERRY_BOMB, "Cherry Bomb", 150, 300, 50, 1800, 0,
            tags(Tag.EXPLOSIVE), behaviors(BehaviorType.EXPLODE_ON_PLANT), 6, 30, false, false, 0, 0, 1);
        add(PlantType.POTATO_MINE, "Potato Mine", 25, 300, 30, 1800, 0,
            tags(Tag.TRAP, Tag.EXPLOSIVE), behaviors(BehaviorType.EXPLODE_ON_PLANT),
            4, 20, false, false, 0, 0, 1);
        add(PlantType.JALAPENO, "Jalapeno", 125, 300, 50, 1800, 0,
            tags(Tag.FIRE, Tag.EXPLOSIVE, Tag.AOE), behaviors(BehaviorType.EXPLODE_AREA),
            6, 30, false, false, 0, 0, 9);
        add(PlantType.DOOMSHROOM, "Doom-shroom", 125, 300, 50, 1800, 0,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.EXPLOSIVE, Tag.AOE), behaviors(BehaviorType.EXPLODE_ON_PLANT),
            6, 30, false, false, 0, 0, 2);
        add(PlantType.SPIKEWEED, "Spikeweed", 100, 300, 15, 20, 0,
            tags(Tag.TRAP), behaviors(BehaviorType.EXPLODE_ON_PLANT), 5, 25, false, false, 0, 0, 0);
        add(PlantType.SPIKEROCK, "Spikerock", 175, 300, 15, 40, 0,
            tags(Tag.TRAP), behaviors(BehaviorType.EXPLODE_ON_PLANT), 7, 35, false, false, 0, 0, 0);
        add(PlantType.ICE_SHROOM, "Ice-shroom", 75, 300, 50, 0, 0,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.ICE, Tag.AOE), behaviors(BehaviorType.EXPLODE_ON_PLANT),
            6, 30, false, false, 0, 0, 9);
        add(PlantType.SQUASH, "Squash", 50, 300, 8, 1800, 1,
            tags(Tag.DAY, Tag.TRAP), behaviors(BehaviorType.EXPLODE_ON_PLANT), 5, 25, false, false, 0, 0, 0);
        add(PlantType.TANGLE_KELP, "Tangle Kelp", 25, 300, 15, 1800, 0,
            tags(Tag.WATER, Tag.TRAP), behaviors(BehaviorType.EXPLODE_ON_PLANT),
            5, 25, false, true, 0, 0, 0);
    }

    private static void addWallNutsAndWater() {
        add(PlantType.WALLNUT, "Wall-nut", 50, 4000, 30, 0, 0,
            tags(Tag.DAY), behaviors(), 6, 30, false, false, 0, 0, 0);
        add(PlantType.TALL_NUT, "Tall-nut", 125, 8000, 30, 0, 0,
            tags(Tag.DAY), behaviors(), 8, 40, false, false, 0, 0, 0);
        add(PlantType.PUMPKIN, "Pumpkin", 125, 4000, 5, 0, 0,
            tags(Tag.DAY), behaviors(), 6, 30, true, false, 0, 0, 0);
        add(PlantType.LILY_PAD, "Lily Pad", 25, 300, 5, 0, 0,
            tags(Tag.WATER), behaviors(), 4, 20, true, true, 0, 0, 0);
        add(PlantType.SEA_SHROOM, "Sea-shroom", 0, 300, 5, 0, 0,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.WATER), behaviors(), 4, 20, false, true, 0, 0, 0);
        add(PlantType.CHOMPER, "Chomper", 150, 300, 8, 1800, 1,
            tags(Tag.DAY), behaviors(BehaviorType.INSTANT_KILL_PLANT), 6, 30, false, false, 0, 0, 0);
    }

    private static void addSupportPlants() {
        add(PlantType.TORCHWOOD, "Torchwood", 175, 300, 5, 0, 0,
            tags(Tag.FIRE), behaviors(), 6, 30, false, false, 0, 0, 0);
        add(PlantType.GARLIC, "Garlic", 50, 300, 10, 0, 0,
            tags(Tag.MOVE_ZOMBIES), behaviors(BehaviorType.PUSH_OBJECT), 5, 25, false, false, 0, 0, 0);
        add(PlantType.HYPNO_SHROOM, "Hypno-shroom", 75, 300, 30, 0, 0,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.MAGIC), behaviors(BehaviorType.HYPNOTIZE),
            6, 30, false, false, 0, 0, 0);
        add(PlantType.UMBRELLA_LEAF, "Umbrella Leaf", 100, 300, 5, 0, 0,
            tags(Tag.WATER), behaviors(BehaviorType.REFLECT_PROJECTILES), 6, 30, true, true, 0, 0, 0);
        add(PlantType.MAGNETSHROOM, "Magnet-shroom", 100, 300, 10, 0, 3,
            tags(Tag.NIGHT, Tag.SHROOM, Tag.MAGIC), behaviors(BehaviorType.STEAL_ARMOR),
            6, 30, false, false, 0, 0, 0);
    }

    /** These have real, distinctive mechanics in the spec (grave removal, fog clearing, coin
     *  magnetism, plant duplication) that don't map onto any existing BehaviorType - they're
     *  registered with correct stats/tags so the data table is complete, but the special-case
     *  logic itself is a TODO (see README "known gaps"). */
    private static void addRemainingUtilityPlants() {
        add(PlantType.COFFEE_BEAN, "Coffee Bean", 75, 300, 5, 0, 0,
            tags(Tag.NIGHT), behaviors(), 4, 20, false, false, 0, 0, 0);
        add(PlantType.BLOVER, "Blover", 25, 300, 5, 0, 0,
            tags(), behaviors(), 4, 20, false, false, 0, 0, 0);
        add(PlantType.GRAVE_BUSTER, "Grave Buster", 75, 300, 5, 0, 0,
            tags(Tag.NIGHT), behaviors(), 4, 20, false, false, 0, 0, 0);
        add(PlantType.PLANTERN, "Plantern", 25, 300, 5, 0, 0,
            tags(Tag.NIGHT), behaviors(), 4, 20, false, false, 0, 0, 0);
        add(PlantType.GOLD_MAGNET, "Gold Magnet", 50, 300, 5, 0, 0,
            tags(), behaviors(), 4, 20, false, false, 0, 0, 0);
        add(PlantType.FLOWER_POT, "Flower Pot", 25, 300, 5, 0, 0,
            tags(), behaviors(), 4, 20, true, false, 0, 0, 0);
        add(PlantType.IMITATER, "Imitater", 0, 300, 5, 0, 0,
            tags(), behaviors(), 0, 0, false, false, 0, 0, 0);
    }

    private static void add(PlantType type, String name, int sunCost, int maxHp, int recharge, int damage,
                            int range, Set<Tag> tags, List<BehaviorType> behaviors, int seedPackets, int coins,
                            boolean stack, boolean water, int sunAmount, int sunInterval, int aoeRadius) {
        ALL.put(type, new PlantDef(type, name, sunCost, maxHp, recharge, damage, range, tags, behaviors,
            seedPackets, coins, stack, water, sunAmount, sunInterval, aoeRadius));
    }

    private static Set<Tag> tags(Tag... values) {
        Set<Tag> set = EnumSet.noneOf(Tag.class);
        for (Tag t : values) {
            set.add(t);
        }
        return set;
    }

    private static List<BehaviorType> behaviors(BehaviorType... values) {
        List<BehaviorType> list = new ArrayList<>();
        for (BehaviorType b : values) {
            list.add(b);
        }
        return list;
    }
}
