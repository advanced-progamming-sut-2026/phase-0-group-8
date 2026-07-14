package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import java.util.ArrayList;
import java.util.List;

public final class ZombieBehaviorFactory {
    private ZombieBehaviorFactory() {}

    public static List<ZombieBehavior> createSpecialBehaviors(String objClass) {
        List<ZombieBehavior> specials = new ArrayList<>();
        if (objClass == null) return specials;

        switch (objClass) {
            case "ZombieIceAgeHunterProps":
                specials.add(new ZombieIceAgeHunterBehavior());
                break;
            case "ZombieCrystalSkullProps":
                specials.add(new ZombieCrystalSkullBehavior());
                break;
            case "ZombieGargantuarProps":
                specials.add(new GargantuarSmashBehavior());
                break;
            case "ZombieProspectorProps":
                specials.add(new ZombieProspectorBehavior(10.0));
                break;
            case "ZombieIceAgeDodoProps":
                specials.add(new ZombieIceAgeDodoBehavior());
                break;
            default:
                break;
        }
        return specials;
    }
}
