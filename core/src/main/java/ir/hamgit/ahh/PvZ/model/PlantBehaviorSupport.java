package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.def.PlantAbilityProfile;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlantBehaviorSupport {

    private static final int STAGE_TWO_TICKS = 240;
    private static final int STAGE_THREE_TICKS = 720;
    private static final int BASE_FREEZE_TICKS = 50;
    private static final int GRAPE_DAMAGE_DIVISOR = 6;

    private final PlantDef def;
    private final PlantAbilityProfile profile;
    private final PlantProjectileLauncher projectileLauncher;
    private final PlantAuraSupport auraSupport;
    private int attackCooldown;
    private int sunTimer;
    private int elapsedTicks;
    private int armTicks;
    private int lifespanTicks;
    private int cycleIndex;
    private int stage = 1;
    private boolean instantActionDone;

    public PlantBehaviorSupport(PlantDef def) {
        this.def = def;
        profile = def.getAbilityProfile();
        projectileLauncher = new PlantProjectileLauncher(def);
        auraSupport = new PlantAuraSupport(def);
        armTicks = def.hasBehavior(BehaviorType.ARM_DELAY) ? profile.getArmDelayTicks() : 0;
        lifespanTicks = profile.getLifespanTicks();
    }

    public void tick(Plant plant, Board board) {
        elapsedTicks++;
        decrementTimers();
        updateGrowthStage(plant);
        if (expireIfNeeded(plant, board)) {
            return;
        }
        runInstantAction(plant, board);
        if (!plant.isAlive()) {
            return;
        }
        if (def.hasBehavior(BehaviorType.ATTRACT_ZOMBIES)) {
            auraSupport.attractZombies(plant, board);
        }
        auraSupport.warmSurroundings(plant, board);
        runContactAbility(plant, board);
        runAttack(plant, board);
        produceSun(plant, board);
        runMagnet(plant, board);
    }

    private void decrementTimers() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (armTicks > 0) {
            armTicks--;
        }
        if (lifespanTicks > 0) {
            lifespanTicks--;
        }
    }

    private void updateGrowthStage(Plant plant) {
        if (!def.hasBehavior(BehaviorType.RAMP_UP_SUN)
            && !def.hasBehavior(BehaviorType.RAMP_UP_DAMAGE)) {
            return;
        }
        int finalStageTick = PlantLevelRuntime.growthThreshold(plant, STAGE_THREE_TICKS);
        int secondStageTick = PlantLevelRuntime.growthThreshold(plant, STAGE_TWO_TICKS);
        if (elapsedTicks >= finalStageTick) {
            stage = PlantLevelRuntime.maxGrowthStage(plant);
        } else if (elapsedTicks >= secondStageTick) {
            stage = 2;
        }
    }

    public void applyLevel(Plant plant) {
        armTicks = def.hasBehavior(BehaviorType.ARM_DELAY)
            ? PlantLevelRuntime.armDelay(plant, profile.getArmDelayTicks()) : 0;
        lifespanTicks = PlantLevelRuntime.lifespan(plant, profile.getLifespanTicks());
    }

    private boolean expireIfNeeded(Plant plant, Board board) {
        if (def.hasBehavior(BehaviorType.LIMITED_LIFESPAN) && lifespanTicks <= 0) {
            board.destroyPlantInstantly(plant);
            return true;
        }
        return false;
    }

    private void runInstantAction(Plant plant, Board board) {
        if (instantActionDone) {
            return;
        }
        if (def.hasBehavior(BehaviorType.INSTANT_SUN)) {
            board.addSun(plant.getSunProductionAmount());
            finishInstantAction(plant, board);
        } else if (def.hasBehavior(BehaviorType.FAMILY_BOOST)) {
            auraSupport.boostFamily(plant, board);
            finishInstantAction(plant, board);
        } else if (def.hasBehavior(BehaviorType.FREEZE_ALL)) {
            auraSupport.freezeAll(plant, board, BASE_FREEZE_TICKS);
            damageAllZombies(plant, board);
            finishInstantAction(plant, board);
        } else if (isInstantExplosion()) {
            runExplosion(plant, board);
        }
    }

    private boolean isInstantExplosion() {
        return def.hasBehavior(BehaviorType.DISAPPEAR_AFTER_ACTION)
            && (def.hasBehavior(BehaviorType.AREA_EXPLOSION)
            || def.hasBehavior(BehaviorType.LANE_EXPLOSION)
            || def.hasBehavior(BehaviorType.BOARD_EXPLOSION));
    }

    private void finishInstantAction(Plant plant, Board board) {
        instantActionDone = true;
        if (def.hasBehavior(BehaviorType.DISAPPEAR_AFTER_ACTION)) {
            board.destroyPlantInstantly(plant);
        }
    }

    private void runExplosion(Plant plant, Board board) {
        instantActionDone = true;
        if (def.hasBehavior(BehaviorType.BOARD_EXPLOSION)) {
            damageAllZombies(plant, board);
            leaveCraterIfNeeded(plant, board);
        } else if (def.hasBehavior(BehaviorType.LANE_EXPLOSION)) {
            damageLane(plant, board);
            meltLane(plant, board);
        } else {
            int radius = Math.max(1, profile.getSplashRadius());
            board.dealAreaDamageToZombies(plant.getX(), plant.getLane(), radius,
                plant.effectiveDamage(def.getDamage()) + PlantLevelRuntime.areaDamage(plant, 0));
            spawnGrapesIfNeeded(plant, board);
        }
        board.destroyPlantInstantly(plant);
    }

    private void damageAllZombies(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            zombie.takeDamage(plant.effectiveDamage(def.getDamage()), false);
        }
    }

    private void damageLane(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && zombie.getLane() == plant.getLane()) {
                zombie.takeDamage(plant.effectiveDamage(def.getDamage()), false);
            }
        }
    }

    private void leaveCraterIfNeeded(Plant plant, Board board) {
        if (def.hasBehavior(BehaviorType.LEAVE_CRATER)) {
            board.getTileAt(plant.getX(), plant.getLane()).setType(TileType.CRATER);
        }
    }

    private void spawnGrapesIfNeeded(Plant plant, Board board) {
        if (!def.hasBehavior(BehaviorType.BOUNCING_PROJECTILES)) {
            return;
        }
        int grapeDamage = Math.max(1, plant.effectiveDamage(def.getDamage()) / GRAPE_DAMAGE_DIVISOR);
        projectileLauncher.launchGrapes(plant, board, grapeDamage);
    }

    private void meltLane(Plant plant, Board board) {
        for (int x = 0; x < board.getColumns(); x++) {
            Tile tile = board.getTileAt(x, plant.getLane());
            if (tile.getType() == TileType.ICY_GROUND) {
                tile.setType(TileType.NORMAL);
            }
            if (tile.getPlant() != null) {
                tile.getPlant().meltIce();
            }
        }
    }

    private void runContactAbility(Plant plant, Board board) {
        if (armTicks > 0) {
            return;
        }
        if (def.hasBehavior(BehaviorType.AQUATIC_INSTANT_KILL)) {
            drownAdjacentAquatic(plant, board);
            return;
        }
        Zombie target = nearestZombie(plant, board, 1, false);
        if (target == null) {
            return;
        }
        if (def.hasBehavior(BehaviorType.CONTACT_EXPLOSION)) {
            explodeOnContact(plant, board, target);
        } else if (def.hasBehavior(BehaviorType.ADJACENT_SMASH)) {
            smashTargets(plant, board);
            board.destroyPlantInstantly(plant);
        } else if (def.hasBehavior(BehaviorType.CONTACT_FREEZE)) {
            target.freeze(PlantLevelRuntime.freezeTicks(plant, BASE_FREEZE_TICKS));
            board.destroyPlantInstantly(plant);
        }
    }

    private void drownAdjacentAquatic(Plant plant, Board board) {
        int remaining = PlantLevelRuntime.targetCount(plant, 1);
        for (Zombie zombie : shuffledLivingZombies(board)) {
            if (remaining > 0 && Math.abs(zombie.getX() - plant.getX()) <= 1
                && isAquatic(zombie, board)) {
                zombie.forceKill();
                remaining--;
            }
        }
        if (remaining < PlantLevelRuntime.targetCount(plant, 1)) {
            board.destroyPlantInstantly(plant);
        }
    }

    private void smashTargets(Plant plant, Board board) {
        int remaining = PlantLevelRuntime.crushTargets(plant);
        for (Zombie zombie : shuffledLivingZombies(board)) {
            if (remaining > 0 && zombie.getLane() == plant.getLane()
                && Math.abs(zombie.getX() - plant.getX()) <= 1) {
                zombie.takeDamage(plant.effectiveDamage(def.getDamage()), false);
                remaining--;
            }
        }
    }

    private void explodeOnContact(Plant plant, Board board, Zombie target) {
        int radius = profile.getSplashRadius();
        if (radius > 0) {
            board.dealAreaDamageToZombies(plant.getX(), plant.getLane(), radius,
                plant.effectiveDamage(def.getDamage()) + PlantLevelRuntime.areaDamage(plant, 0));
        } else {
            target.takeDamage(plant.effectiveDamage(def.getDamage()), false);
        }
        board.destroyPlantInstantly(plant);
    }

    private boolean isAquatic(Zombie zombie, Board board) {
        Tile tile = board.getTileAt((int) Math.round(zombie.getX()), zombie.getLane());
        return tile != null && tile.getType() == TileType.WATER;
    }

    private void runAttack(Plant plant, Board board) {
        if (attackCooldown > 0 || !hasAttackBehavior()) {
            return;
        }
        boolean attacked = runDirectAttack(plant, board);
        if (!attacked && hasProjectileBehavior() && hasProjectileTarget(plant, board)) {
            fireProjectiles(plant, board);
            attacked = true;
        }
        if (attacked) {
            resetAttackCooldown(plant);
            triggerPlantFoodChance(plant, board);
        }
    }

    private void triggerPlantFoodChance(Plant plant, Board board) {
        int chancePercent = PlantLevelRuntime.value(plant, "Plant Food Chance +");
        if (chancePercent > 0 && Math.random() * 100 < chancePercent) {
            plant.applyPlantFood(board);
        }
    }

    private boolean hasAttackBehavior() {
        return hasProjectileBehavior() || def.hasBehavior(BehaviorType.FUME_ATTACK)
            || def.hasBehavior(BehaviorType.RANDOM_TARGET)
            || def.hasBehavior(BehaviorType.MELEE_FRONT_BACK)
            || def.hasBehavior(BehaviorType.MELEE_AREA)
            || def.hasBehavior(BehaviorType.SWALLOW_ZOMBIE)
            || def.hasBehavior(BehaviorType.HOMING_SHOT);
    }

    private boolean hasProjectileBehavior() {
        return def.hasBehavior(BehaviorType.SHOOT_FORWARD)
            || def.hasBehavior(BehaviorType.SHOOT_ARC)
            || def.hasBehavior(BehaviorType.SHOOT_DIAGONAL)
            || def.hasBehavior(BehaviorType.SHOOT_FIVE_DIRECTIONS)
            || def.hasBehavior(BehaviorType.HOMING_SHOT);
    }

    private boolean runDirectAttack(Plant plant, Board board) {
        if (def.hasBehavior(BehaviorType.RANDOM_TARGET)) {
            return strikeRandomZombie(plant, board, 1,
                def.hasBehavior(BehaviorType.INSTANT_KILL_ZOMBIE));
        }
        if (def.hasBehavior(BehaviorType.HYPNOTIZE) && def.hasBehavior(BehaviorType.HOMING_SHOT)) {
            return hypnotizeRandomZombie(board, 1);
        }
        if (def.hasBehavior(BehaviorType.FUME_ATTACK)) {
            return damagePiercingLane(plant, board);
        }
        if (def.hasBehavior(BehaviorType.MELEE_FRONT_BACK)) {
            return damageMelee(plant, board, false);
        }
        if (def.hasBehavior(BehaviorType.MELEE_AREA)) {
            return damageMelee(plant, board, true);
        }
        if (def.hasBehavior(BehaviorType.SWALLOW_ZOMBIE)) {
            return swallowZombie(plant, board);
        }
        return false;
    }

    private boolean strikeRandomZombie(Plant plant, Board board, int count, boolean instantKill) {
        List<Zombie> targets = shuffledLivingZombies(board);
        if (PlantLevelRuntime.has(plant, "Target Priority Up")) {
            targets.sort((first, second) -> Integer.compare(second.getCurrentHp(), first.getCurrentHp()));
        }
        int hits = Math.min(count, targets.size());
        for (int i = 0; i < hits; i++) {
            if (instantKill) {
                targets.get(i).forceKill();
            } else {
                targets.get(i).takeDamage(plant.effectiveDamage(def.getDamage()), false);
            }
        }
        return hits > 0;
    }

    private boolean hypnotizeRandomZombie(Board board, int count) {
        List<Zombie> targets = shuffledLivingZombies(board);
        int hits = Math.min(count, targets.size());
        for (int i = 0; i < hits; i++) {
            targets.get(i).hypnotize();
        }
        return hits > 0;
    }

    private List<Zombie> shuffledLivingZombies(Board board) {
        List<Zombie> targets = new ArrayList<>();
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive()) {
                targets.add(zombie);
            }
        }
        Collections.shuffle(targets);
        return targets;
    }

    private boolean damagePiercingLane(Plant plant, Board board) {
        boolean hit = false;
        for (Zombie zombie : board.getZombies()) {
            int range = PlantLevelRuntime.range(plant, profile.getRangeTiles());
            boolean inRange = zombie.getLane() == plant.getLane() && zombie.getX() > plant.getX()
                && zombie.getX() <= plant.getX() + range;
            if (zombie.isAlive() && inRange) {
                zombie.takeDamage(currentDamage(plant), false);
                hit = true;
            }
        }
        return hit;
    }

    private boolean damageMelee(Plant plant, Board board, boolean area) {
        int damage = currentDamage(plant);
        boolean hit = false;
        for (Zombie zombie : board.getZombies()) {
            boolean nearColumn = Math.abs(zombie.getX() - plant.getX()) <= 1;
            boolean nearLane = area ? Math.abs(zombie.getLane() - plant.getLane()) <= 1
                : zombie.getLane() == plant.getLane();
            if (zombie.isAlive() && nearColumn && nearLane) {
                zombie.takeDamage(damage, false);
                hit = true;
            }
        }
        return hit;
    }

    private boolean swallowZombie(Plant plant, Board board) {
        Zombie target = nearestZombie(plant, board,
            PlantLevelRuntime.range(plant, 1), true);
        if (target == null) {
            return false;
        }
        target.forceKill();
        return true;
    }

    private boolean hasProjectileTarget(Plant plant, Board board) {
        if (def.hasBehavior(BehaviorType.HOMING_SHOT)) {
            return !shuffledLivingZombies(board).isEmpty();
        }
        for (Zombie zombie : board.getZombies()) {
            boolean sameOrPatternLane = Math.abs(zombie.getLane() - plant.getLane()) <= 1;
            int range = PlantLevelRuntime.range(plant, profile.getRangeTiles());
            boolean inRange = Math.abs(zombie.getX() - plant.getX()) <= range;
            if (zombie.isAlive() && sameOrPatternLane && inRange) {
                return true;
            }
        }
        return false;
    }

    private void fireProjectiles(Plant plant, Board board) {
        projectileLauncher.fire(plant, board, currentDamage(plant));
    }

    private int currentDamage(Plant plant) {
        int damage;
        if (def.hasBehavior(BehaviorType.RAMP_UP_DAMAGE)) {
            int lastStage = Math.max(1, profile.getCycleLength());
            damage = profile.getDamageAt(Math.min(stage, lastStage) - 1, def.getDamage());
            if (stage > lastStage) {
                damage += (stage - lastStage) * def.getDamage();
            }
        } else {
            damage = profile.getDamageAt(cycleIndex, def.getDamage());
        }
        damage = plant.effectiveDamage(damage);
        return def.hasBehavior(BehaviorType.SPLASH_DAMAGE)
            ? PlantLevelRuntime.areaDamage(plant, damage) : damage;
    }

    private void resetAttackCooldown(Plant plant) {
        int interval = PlantLevelRuntime.attackInterval(plant,
            Math.max(1, profile.getIntervalAt(cycleIndex)));
        attackCooldown = plant.isBoosted() ? Math.max(1, interval / 2) : interval;
        int cycleLength = profile.getCycleLength();
        if (cycleLength > 0) {
            cycleIndex = (cycleIndex + 1) % cycleLength;
        }
    }

    private Zombie nearestZombie(Plant plant, Board board, int range, boolean eitherDirection) {
        Zombie nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : board.getZombies()) {
            double distance = Math.abs(zombie.getX() - plant.getX());
            boolean correctSide = eitherDirection || zombie.getX() >= plant.getX();
            if (zombie.isAlive() && zombie.getLane() == plant.getLane() && correctSide
                && distance <= range && distance < bestDistance) {
                nearest = zombie;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private void produceSun(Plant plant, Board board) {
        if (!def.hasBehavior(BehaviorType.PRODUCE_SUN) || plant.hasUncollectedSun()) {
            return;
        }
        int interval = PlantLevelRuntime.productionInterval(plant,
            def.getSunProductionIntervalTicks());
        if (++sunTimer >= interval && interval > 0) {
            sunTimer = 0;
            plant.setHasUncollectedSun(true);
            board.spawnProducedSun(plant);
        }
    }

    private void runMagnet(Plant plant, Board board) {
        if (def.hasBehavior(BehaviorType.STEAL_ARMOR) && attackCooldown <= 0
            && board.stealMetalArmorNear(plant.getX(), plant.getLane(),
            PlantLevelRuntime.range(plant, profile.getRangeTiles()))) {
            resetAttackCooldown(plant);
        }
    }

    public int getSunAmount() {
        if (!def.hasBehavior(BehaviorType.RAMP_UP_SUN)) {
            return def.getSunProductionAmount();
        }
        return def.getSunProductionAmount() * stage;
    }

    public void applyPlantFood(Plant plant, Board board) {
        attackCooldown = 0;
        armTicks = 0;
        new PlantFoodSupport(def, projectileLauncher).apply(plant, board);
    }

    public void resetLifespan(Plant plant) {
        lifespanTicks = PlantLevelRuntime.lifespan(plant, profile.getLifespanTicks());
    }

    public void growImmediately(Plant plant) {
        stage = PlantLevelRuntime.maxGrowthStage(plant);
        elapsedTicks = STAGE_THREE_TICKS;
    }

    public void armImmediately() { armTicks = 0; }
}
