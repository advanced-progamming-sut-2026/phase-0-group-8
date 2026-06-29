package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ZombieRegistry {
    private static ZombieRegistry instance;
    private static final Map<ZombieType, ZombieDef> registry = new HashMap<>();

    private ZombieRegistry() {}

    public static ZombieRegistry getInstance() {
        if (instance == null) {
            instance = new ZombieRegistry();
        }

        return instance;
    }

    public void registerDefinition(ZombieDef def) {
        if (def == null || def.getType() == null) {
            return;
        }

        registry.put(def.getType(), def);
    }

    public void loadDefinitions(Collection<ZombieDef> templates) {
        if (templates == null) {
            return;
        }

        for (ZombieDef def : templates) {
            registerDefinition(def);
        }
    }

    public ZombieDef getDefinition(ZombieType type) {
        return registry.get(type);
    }

    public void clear() {
        registry.clear();
    }
}
