package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlantRegistry {
    private static PlantRegistry instance;
    private static final Map<PlantType, PlantDef> registry = new HashMap<>();

    private PlantRegistry() {}

    public static PlantRegistry getInstance() {
        if (instance == null) {
            instance = new PlantRegistry();
        }

        return instance;
    }

    public void registerDefinition(PlantDef def) {
        if (def == null || def.getType() == null) {
            return;
        }

        registry.put(def.getType(), def);
    }

    public void loadDefinitions(Collection<PlantDef> templates) {
        if (templates == null) {
            return;
        }

        for (PlantDef def : templates) {
            registerDefinition(def);
        }
    }

    public PlantDef getDefinition(PlantType type) {
        PlantDef def = registry.get(type);
        return def;
    }

    public void clear() {
        registry.clear();
    }
}
