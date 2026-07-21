package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

import ir.hamgit.ahh.PvZ.model.def.PlantAbilityProfile;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

final class PlantProjectileLauncher {

    private static final int BUTTER_STUN_TICKS = 30;

    private final PlantDef def;
    private final PlantAbilityProfile profile;

    PlantProjectileLauncher(PlantDef def) {
        this.def = def;
        profile = def.getAbilityProfile();
    }

    void fire(Plant plant, Board board, int damage) {
        if (def.hasBehavior(BehaviorType.SHOOT_ADJACENT_LANES)) {
            launchLaneFan(plant, board, damage);
        } else if (def.hasBehavior(BehaviorType.SHOOT_DIAGONAL)) {
            launchDiagonal(plant, board, damage);
        } else if (def.hasBehavior(BehaviorType.SHOOT_FIVE_DIRECTIONS)) {
            launchStar(plant, board, damage);
        } else if (def.hasBehavior(BehaviorType.SHOOT_BACKWARD)) {
            launchSplit(plant, board, damage);
        } else {
            launchRepeated(plant, board, damage);
        }
    }

    void launchGrapes(Plant plant, Board board, int damage) {
        for (int lane = 0; lane < board.getRows(); lane++) {
            launch(plant, board, damage, lane, lane % 2 == 0 ? 1 : -1, true);
        }
    }

    void launchGiantPeas(Plant plant, Board board, int damage, int count) {
        for (int i = 0; i < count; i++) {
            launch(plant, board, damage, plant.getLane(), 1, false);
        }
    }

    void launchFoodBulbs(Plant plant, Board board, int damage, int count) {
        for (int i = 0; i < count; i++) {
            ProjectileSpec spec = baseSpec(plant, damage * 5, plant.getLane(), 1)
                .ricochet(true).pierce(PlantLevelRuntime.bounceCount(plant, 5))
                .splash(1).build();
            board.getProjectiles().add(new Projectile(spec));
        }
    }

    void launchInfinitePiercing(Plant plant, Board board, int damage) {
        ProjectileSpec spec = baseSpec(plant, damage, plant.getLane(), 1)
            .pierce(Integer.MAX_VALUE).range(board.getColumns()).build();
        board.getProjectiles().add(new Projectile(spec));
    }

    private void launchLaneFan(Plant plant, Board board, int damage) {
        for (int lane = plant.getLane() - 1; lane <= plant.getLane() + 1; lane++) {
            if (lane >= 0 && lane < board.getRows()) {
                launch(plant, board, damage, lane, 1, false);
            }
        }
    }

    private void launchDiagonal(Plant plant, Board board, int damage) {
        int[] lanes = {plant.getLane() - 1, plant.getLane() + 1};
        for (int lane : lanes) {
            if (lane >= 0 && lane < board.getRows()) {
                launch(plant, board, damage, lane, 1, false);
                launch(plant, board, damage, lane, -1, false);
            }
        }
    }

    private void launchStar(Plant plant, Board board, int damage) {
        launch(plant, board, damage, plant.getLane(), 1, false);
        launch(plant, board, damage, plant.getLane(), -1, false);
        int firstAdjacent = plant.getLane() > 0 ? plant.getLane() - 1 : plant.getLane() + 1;
        int secondAdjacent = plant.getLane() < board.getRows() - 1
            ? plant.getLane() + 1 : plant.getLane() - 1;
        launch(plant, board, damage, firstAdjacent, 1, false);
        if (secondAdjacent != firstAdjacent) {
            launch(plant, board, damage, secondAdjacent, 1, false);
        }
        launch(plant, board, damage, firstAdjacent, -1, false);
    }

    private void launchSplit(Plant plant, Board board, int damage) {
        launch(plant, board, damage, plant.getLane(), 1, false);
        launch(plant, board, damage, plant.getLane(), -1, false);
        launch(plant, board, damage, plant.getLane(), -1, false);
    }

    private void launchRepeated(Plant plant, Board board, int damage) {
        int count = def.hasBehavior(BehaviorType.MULTI_SHOT) ? profile.getShotCount() : 1;
        if (def.hasBehavior(BehaviorType.STACKABLE_HEADS)) {
            count = plant.getStackCount();
        }
        if (plant.isBoosted()) {
            count *= 5;
        }
        for (int i = 0; i < count; i++) {
            launch(plant, board, damage, plant.getLane(), 1, false);
        }
    }

    private void launch(Plant plant, Board board, int damage, int lane, int direction, boolean grape) {
        ProjectileSpec.Builder builder = baseSpec(plant, damage, lane, direction);
        if (def.hasBehavior(BehaviorType.RICOCHET_PROJECTILE) || grape) {
            builder.ricochet(true).pierce(PlantLevelRuntime.bounceCount(plant, 3));
        }
        int butterBonus = PlantLevelRuntime.value(plant, "Butter +");
        if (def.hasBehavior(BehaviorType.BUTTER_STUN)
            && (damage >= 40 || Math.random() * 100 < butterBonus)) {
            builder.stun(BUTTER_STUN_TICKS);
        }
        board.getProjectiles().add(new Projectile(builder.build()));
    }

    private ProjectileSpec.Builder baseSpec(Plant plant, int damage, int lane, int direction) {
        return ProjectileSpec.builder(def.getType(), damage, plant.getX(), lane)
            .direction(direction)
            .range(PlantLevelRuntime.range(plant, profile.getRangeTiles()))
            .fire(def.hasBehavior(BehaviorType.SHOOT_FIRE))
            .ice(def.hasBehavior(BehaviorType.SHOOT_ICE))
            .poison(def.hasBehavior(BehaviorType.SHOOT_POISON))
            .lobber(def.hasBehavior(BehaviorType.SHOOT_ARC))
            .homing(def.hasBehavior(BehaviorType.HOMING_SHOT))
            .ignoreArmor(def.hasBehavior(BehaviorType.IGNORE_ARMOR))
            .ignitable(def.hasTag(Tag.PEA))
            .splash(def.hasBehavior(BehaviorType.SPLASH_DAMAGE) ? profile.getSplashRadius() : 0)
            .chillTicks(PlantLevelRuntime.freezeTicks(plant, 30))
            .poisonDamage(PlantLevelRuntime.poisonDamage(plant, Math.max(1, damage / 4)))
            .pierce(def.hasBehavior(BehaviorType.PIERCE_PROJECTILE)
                ? PlantLevelRuntime.pierceCount(plant, profile.getPierceCount()) : 1);
    }
}
