package ir.hamgit.ahh.PvZ.model.enums;

/** Tags describing what a plant/zombie mechanically does; used to drive
 *  generic, data-based dispatch in {@code Plant} and {@code Zombie} instead
 *  of hard-coding per-species logic everywhere. */
public enum BehaviorType {
    SHOOT_FORWARD, SHOOT_ARC, SHOOT_ICE, SHOOT_FIRE, SHOOT_POISON, PRODUCE_SUN,
    EXPLODE_ON_PLANT, EXPLODE_AREA, INSTANT_KILL_PLANT, THROW_IMP, REFLECT_PROJECTILES,
    STEAL_SUN, TORCH_BURN, FREEZE_PLANT, LASER_DESTROY, HOOK_PLANT, NEWSPAPER_RAGE,
    SPAWN_IMP_FROM_BARREL, THROW_TOMBSTONE, DYNAMITE_EXPLOSION, TRANSFORM_PLANT_CAT,
    UPGRADE_ZOMBIES, ROW_SHUFFLE, PUSH_OBJECT, FLY_OVER_OBSTACLES, MOVE_REVERSE,
    SWIM_UNDERWATER, HYPNOTIZE, IMMUNE_TO_ICE, IMMUNE_TO_FIRE, IMMUNE_TO_LAWNMOWER,
    STEAL_ARMOR
}
