package ir.hamgit.ahh.PvZ.model.registry;

import ir.hamgit.ahh.PvZ.model.def.ZombieDef;

import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ZombieRegistry {

    private static final String DEFAULT_RESOURCE = "data/game-definitions.json";
    private static final String ALL_CHAPTERS = "all";
    private static final Map<ZombieType, ZombieDef> ALL = new EnumMap<>(ZombieType.class);

    static {
        loadFromJson(DEFAULT_RESOURCE);
    }

    private ZombieRegistry() {
    }

    public static ZombieDef get(ZombieType type) {
        return ALL.get(type);
    }

    public static Map<ZombieType, ZombieDef> getAll() {
        return Collections.unmodifiableMap(ALL);
    }

    public static List<ZombieDef> getForChapter(String chapter) {
        List<ZombieDef> result = new ArrayList<>();
        for (ZombieDef definition : ALL.values()) {
            boolean available = ALL_CHAPTERS.equals(definition.getChapter())
                || definition.getChapter().equalsIgnoreCase(chapter);
            if (available) {
                result.add(definition);
            }
        }
        return result;
    }

    public static synchronized void loadFromJson(String location) {
        Map<ZombieType, ZombieDef> loaded = new EnumMap<>(ZombieType.class);
        for (Map<String, Object> row : JsonDefinitionLoader.readObjects(location, "zombies")) {
            ZombieDef definition = createDefinition(row);
            if (loaded.put(definition.getType(), definition) != null) {
                throw new IllegalArgumentException("Duplicate zombie type: " + definition.getType());
            }
        }
        validateComplete(loaded, location);
        ALL.clear();
        ALL.putAll(loaded);
    }

    private static ZombieDef createDefinition(Map<String, Object> row) {
        ZombieType type = ZombieType.valueOf(JsonDefinitionLoader.text(row, "type"));
        List<ArmorType> armor = JsonDefinitionLoader.enums(row, "armorLayers", ArmorType.class);
        List<BehaviorType> behaviors = JsonDefinitionLoader.enums(row, "behaviors", BehaviorType.class);
        return new ZombieDef(type, JsonDefinitionLoader.text(row, "displayName"),
            JsonDefinitionLoader.integer(row, "maxHp"), JsonDefinitionLoader.decimal(row, "speed"),
            JsonDefinitionLoader.integer(row, "damage"), JsonDefinitionLoader.integer(row, "waveCost"),
            armor, behaviors, JsonDefinitionLoader.text(row, "chapter"));
    }

    private static void validateComplete(Map<ZombieType, ZombieDef> loaded, String location) {
        for (ZombieType type : ZombieType.values()) {
            if (!loaded.containsKey(type)) {
                throw new IllegalStateException(location + " is missing zombie " + type);
            }
        }
        if (loaded.size() != ZombieType.values().length) {
            throw new IllegalStateException(location + " contains unknown zombie definitions");
        }
    }
}
