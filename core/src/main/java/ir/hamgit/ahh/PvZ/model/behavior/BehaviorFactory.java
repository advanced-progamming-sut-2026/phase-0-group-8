package ir.hamgit.ahh.PvZ.model.behavior;

import ir.hamgit.ahh.PvZ.model.behavior.plant.PlantBehavior;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import java.util.ArrayList;
import java.util.List;

public final class BehaviorFactory {
    private BehaviorFactory() {}

    public static List<PlantBehavior> createBehaviors(List<BehaviorType> types) {
        List<PlantBehavior> behaviors = new ArrayList<>();
        if (types == null) return behaviors;

        for (BehaviorType type : types) {
            switch (type) {

            }
        }
        return behaviors;
    }
}
