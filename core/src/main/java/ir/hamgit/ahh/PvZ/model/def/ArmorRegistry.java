package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import java.util.*;

public class ArmorRegistry {
    private static ArmorRegistry instance;
    private final Map<ArmorType, ArmorDef> registry = new HashMap<>();

    private ArmorRegistry() {}

    public static ArmorRegistry getInstance() {
        if (instance == null) {
            instance = new ArmorRegistry();
        }

        return instance;
    }

    public void registerDefinition(ArmorDef def) {
        if (def == null || def.getArmorType() == null) {
            return;
        }

        registry.put(def.getArmorType(), def);
    }

    public void loadDefinitions(Collection<ArmorDef> templates) {
        if (templates == null) {
            return;
        }

        for (ArmorDef def : templates) {
            registerDefinition(def);
        }
    }

    public ArmorDef getDefinition(ArmorType type) {
        return registry.get(type);
    }

    public void clear() {
        registry.clear();
    }


}
