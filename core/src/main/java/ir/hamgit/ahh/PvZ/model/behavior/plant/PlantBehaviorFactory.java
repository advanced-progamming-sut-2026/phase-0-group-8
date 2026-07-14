package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.enums.PlantBehaviorType;
import java.util.ArrayList;
import java.util.List;

public final class PlantBehaviorFactory {
    private PlantBehaviorFactory() {}

    public static List<PlantBehavior> createBehaviors(List<PlantBehaviorType> types) {
        List<PlantBehavior> behaviors = new ArrayList<>();
        if (types == null) return behaviors;

        for (PlantBehaviorType type : types) {
            switch (type) {
                case PRODUCE_SUN -> behaviors.add(new SunProduceBehavior());
                case INSTANT_SUN_BURST -> behaviors.add(new InstantSunBurstBehavior());
                case SHOOT_PROJECTILE -> behaviors.add(new ShootProjectileBehavior());
                case DELAYED_EXPLOSIVE -> behaviors.add(new DelayedExplosiveBehavior());
                case INSTANT_EXPLOSIVE -> behaviors.add(new InstantExplosiveBehavior());
                case MELEE_ATTACK -> behaviors.add(new MeleeAttackBehavior());
                case PASSIVE_SHIELD -> behaviors.add(new PassiveShieldBehavior());
                case MODIFIER_UTILITY -> behaviors.add(new ModifierUtilityBehavior());
                case MINT_FAMILY_BOOST -> behaviors.add(new MintFamilyBoostBehavior());
                default -> behaviors.add(new PassiveShieldBehavior());
            }
        }
        return behaviors;
    }
}
