package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

/**
 * A live plant on the board. Behaviour is driven generically off
 * {@link PlantDef#getBehaviors()} / {@link PlantDef#getTags()} rather than
 * switching on plant name, so it scales to the full 49-plant table once the
 * real CSV is loaded (see {@code model.def.PlantRegistry}).
 */
public class Plant {

    private static final int ARM_TICKS_FOR_TRAPS = 20;
    private static final int FUSE_TICKS_FOR_INSTANT_EXPLOSIVES = 10;
    private static final int DEFAULT_FIRE_RATE_TICKS = 20;
    private static final int PLANT_FOOD_BOOST_TICKS = 100;
    private static final int MAX_ICE_LAYERS = 3;

    private final PlantDef def;
    private int currentHp;
    private final int x;
    private final int lane;
    private int level = 1;
    private boolean boosted;
    private int boostTicksRemaining;
    private int sunProductionTimer;
    private boolean hasUncollectedSun;
    private boolean frozen;
    private int iceLayers;
    private boolean cat;
    private Plant underPlant;
    private int shootCooldownTicks;
    private int armTicksRemaining;
    private int fuseTicksRemaining;
    private boolean armed;
    private boolean triggeredExplosion;

    public Plant(PlantDef def, int x, int lane) {
        this.def = def;
        this.currentHp = def.getMaxHp();
        this.x = x;
        this.lane = lane;
        if (def.hasTag(Tag.TRAP)) {
            this.armTicksRemaining = ARM_TICKS_FOR_TRAPS;
        }
        if (isFuseExplosive()) {
            this.fuseTicksRemaining = FUSE_TICKS_FOR_INSTANT_EXPLOSIVES;
        }
    }

    private boolean isFuseExplosive() {
        boolean explosive = def.hasBehavior(BehaviorType.EXPLODE_ON_PLANT)
            || def.hasBehavior(BehaviorType.EXPLODE_AREA);
        return explosive && !def.hasTag(Tag.TRAP);
    }

    private static final int MAGNET_INTERVAL_TICKS = 20;
    private int magnetCooldownTicks;

    public void tick(model.Board board) {
        if (!isAlive()) {
            return;
        }
        frozen = iceLayers >= MAX_ICE_LAYERS;
        if (frozen || cat) {
            return;
        }
        tickTimers();
        tickExplosives(board);
        tickShooting(board);
        tickSunProduction(board);
        tickMagnetism(board);
    }

    private void tickMagnetism(model.Board board) {
        if (!def.hasBehavior(BehaviorType.STEAL_ARMOR)) {
            return;
        }
        if (magnetCooldownTicks > 0) {
            magnetCooldownTicks--;
            return;
        }
        if (board.stealMetalArmorNear(x, lane, Math.max(1, def.getRange()))) {
            magnetCooldownTicks = MAGNET_INTERVAL_TICKS;
        }
    }

    private void tickTimers() {
        if (shootCooldownTicks > 0) {
            shootCooldownTicks--;
        }
        if (boostTicksRemaining > 0 && --boostTicksRemaining == 0) {
            boosted = false;
        }
        if (armTicksRemaining > 0 && --armTicksRemaining == 0) {
            armed = true;
        }
    }

    private void tickExplosives(model.Board board) {
        if (triggeredExplosion) {
            return;
        }
        if (def.hasTag(Tag.TRAP) && (def.hasBehavior(BehaviorType.EXPLODE_ON_PLANT) || armed)) {
            tickTrapExplosive(board);
        } else if (isFuseExplosive()) {
            tickFuseExplosive(board);
        }
    }

    private void tickTrapExplosive(model.Board board) {
        if (armed && board.hasAdjacentZombie(x, lane)) {
            explode(board);
        }
    }

    private void tickFuseExplosive(model.Board board) {
        if (fuseTicksRemaining > 0) {
            fuseTicksRemaining--;
            if (fuseTicksRemaining <= 0) {
                explode(board);
            }
        }
    }

    private void explode(model.Board board) {
        triggeredExplosion = true;
        boolean wholeRow = def.hasBehavior(BehaviorType.EXPLODE_AREA) && def.getAoeRadius() >= 9;
        int radius = wholeRow ? 9 : Math.max(1, def.getAoeRadius());
        board.dealAreaDamageToZombies(x, lane, radius, def.getDamage());
        board.destroyPlantInstantly(this);
    }

    private void tickShooting(model.Board board) {
        if (!canShoot() || shootCooldownTicks > 0) {
            return;
        }
        if (isRangedShooter() && board.hasZombieInLaneAhead(x, lane, def.getRange())) {
            fireProjectile(board);
        } else if (def.hasBehavior(BehaviorType.INSTANT_KILL_PLANT)) {
            tryMeleeAttack(board);
        }
    }

    private boolean isRangedShooter() {
        return def.hasBehavior(BehaviorType.SHOOT_FORWARD) || def.hasBehavior(BehaviorType.SHOOT_ARC)
            || def.hasBehavior(BehaviorType.SHOOT_ICE) || def.hasBehavior(BehaviorType.SHOOT_FIRE)
            || def.hasBehavior(BehaviorType.SHOOT_POISON);
    }

    private void fireProjectile(model.Board board) {
        int shots = boosted ? 2 : 1;
        for (int i = 0; i < shots; i++) {
            board.spawnProjectile(this);
        }
        shootCooldownTicks = DEFAULT_FIRE_RATE_TICKS;
    }

    private static final int MELEE_DIGEST_TICKS = 150;

    private void tryMeleeAttack(model.Board board) {
        if (board.hasAdjacentZombie(x, lane)) {
            board.dealAreaDamageToZombies(x, lane, 0, Integer.MAX_VALUE / 2);
            shootCooldownTicks = MELEE_DIGEST_TICKS;
        }
    }

    private void tickSunProduction(model.Board board) {
        if (!def.hasBehavior(BehaviorType.PRODUCE_SUN) || hasUncollectedSun) {
            return;
        }
        int interval = boosted ? Math.max(1, def.getSunProductionIntervalTicks() / 2)
            : def.getSunProductionIntervalTicks();
        if (interval <= 0) {
            return;
        }
        sunProductionTimer++;
        if (sunProductionTimer >= interval) {
            sunProductionTimer = 0;
            hasUncollectedSun = true;
            board.spawnProducedSun(this);
        }
    }

    /** Plant Food: boosts for a while and, per spec, makes producers give sun and
     *  detonates fuse-explosives immediately. */
    public void applyPlantFood(model.Board board) {
        boosted = true;
        boostTicksRemaining = PLANT_FOOD_BOOST_TICKS;
        if (def.hasBehavior(BehaviorType.PRODUCE_SUN)) {
            sunProductionTimer = def.getSunProductionIntervalTicks();
        }
        if (isFuseExplosive() && !triggeredExplosion) {
            explode(board);
        }
    }

    public void takeDamage(int amount) {
        currentHp -= frozen ? amount * 2 : amount;
    }

    public void applyIceLayer() {
        iceLayers = Math.min(MAX_ICE_LAYERS, iceLayers + 1);
    }

    public void meltIce() {
        iceLayers = 0;
        frozen = false;
    }

    public void freezeCompletely() {
        iceLayers = MAX_ICE_LAYERS;
        frozen = true;
    }

    public void markSunCollected() {
        hasUncollectedSun = false;
    }

    public void turnIntoCat() {
        cat = true;
    }

    public void turnBackFromCat() {
        cat = false;
    }

    public boolean isCat() {
        return cat;
    }

    public boolean isAlive() {
        return currentHp > 0;
    }

    public boolean canShoot() {
        return !frozen && !cat;
    }

    public void destroy() {
        currentHp = 0;
    }

    public void upgrade() {
        level++;
    }

    public PlantDef getDef() {
        return def;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    public int getLevel() {
        return level;
    }

    public boolean isBoosted() {
        return boosted;
    }

    public int getIceLayers() {
        return iceLayers;
    }

    public Plant getUnderPlant() {
        return underPlant;
    }

    public void setUnderPlant(Plant underPlant) {
        this.underPlant = underPlant;
    }
}
