package ir.hamgit.ahh.PvZ.model.registry;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;

import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlantRegistry {

    private static final String DEFAULT_RESOURCE = "data/game-definitions.json";
    private static final Map<PlantType, PlantDef> ALL = new EnumMap<>(PlantType.class);

    static {
        loadFromJson(DEFAULT_RESOURCE);
    }

    private PlantRegistry() {
    }

    public static PlantDef get(PlantType type) {
        return ALL.get(type);
    }

    public static Collection<PlantDef> getAll() {
        return Collections.unmodifiableCollection(ALL.values());
    }

     
    public static synchronized void loadFromJson(String location) {
        Map<PlantType, PlantDef> loaded = new EnumMap<>(PlantType.class);
        for (Map<String, Object> row : JsonDefinitionLoader.readObjects(location, "plants")) {
            PlantDef definition = createDefinition(row);
            if (loaded.put(definition.getType(), definition) != null) {
                throw new IllegalArgumentException("Duplicate plant type: " + definition.getType());
            }
        }
        validateComplete(loaded, location);
        ALL.clear();
        ALL.putAll(loaded);
    }

    private static PlantDef createDefinition(Map<String, Object> row) {
        PlantType type = PlantType.valueOf(JsonDefinitionLoader.text(row, "type"));
        Set<Tag> tags = EnumSet.noneOf(Tag.class);
        tags.addAll(JsonDefinitionLoader.enums(row, "tags", Tag.class));
        List<BehaviorType> behaviors = JsonDefinitionLoader.enums(row, "behaviors", BehaviorType.class);
        return new PlantDef(type, JsonDefinitionLoader.text(row, "displayName"),
            JsonDefinitionLoader.integer(row, "sunCost"), JsonDefinitionLoader.integer(row, "maxHp"),
            JsonDefinitionLoader.integer(row, "rechargeSeconds"),
            JsonDefinitionLoader.integer(row, "damage"), JsonDefinitionLoader.integer(row, "range"),
            tags, behaviors, JsonDefinitionLoader.strings(row, "levelEffects"),
            JsonDefinitionLoader.integer(row, "seedPacketsToUpgrade"),
            JsonDefinitionLoader.integer(row, "coinsToUpgrade"),
            JsonDefinitionLoader.bool(row, "canStackOn"),
            JsonDefinitionLoader.bool(row, "canPlantOnWater"),
            JsonDefinitionLoader.integer(row, "sunProductionAmount"),
            JsonDefinitionLoader.integer(row, "sunProductionIntervalTicks"),
            JsonDefinitionLoader.integer(row, "aoeRadius"));
    }

    private static void validateComplete(Map<PlantType, PlantDef> loaded, String location) {
        for (PlantType type : PlantType.values()) {
            PlantDef definition = loaded.get(type);
            if (definition == null) {
                throw new IllegalStateException(location + " is missing plant " + type);
            }
            if (definition.getBehaviors().isEmpty()) {
                throw new IllegalStateException(type + " must define at least one behavior");
            }
            int expectedEffects = type == PlantType.MARIGOLD ? 0 : 3;
            if (definition.getLevelEffects().size() != expectedEffects) {
                throw new IllegalStateException(type + " must define " + expectedEffects
                    + " level effects");
            }
        }
        if (loaded.size() != PlantType.values().length) {
            throw new IllegalStateException(location + " contains unknown plant definitions");
        }
    }
}
