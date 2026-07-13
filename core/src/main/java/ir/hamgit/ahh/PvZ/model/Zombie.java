package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A live zombie on the board. Generic movement/attack/armor handling lives
 * here for every zombie; each species' signature ability (per the "زامبی‌ها"
 * section of the spec) is a small private method dispatched from
 * {@link #runSpecialAbility(model.Board)}, using {@link #activeEffects} as a
 * generic countdown-timer bag so we don't need one bespoke field per zombie.
 */
public class Zombie {

    private static final double GLOW_CHANCE = 0.05;
    private static final int ALL_STAR_SPRINT_MULTIPLIER = 3;
    private static final int ENRAGED_MULTIPLIER = 3;
    private static final int BIG_OBSTACLE_HP_THRESHOLD = 1000;
    /** Must match {@link ZombieSpecialBehaviors}'s copy - only the constructor's initial seed needs it here. */
    private static final int DYNAMITE_FUSE_TICKS = 100;

    private final ZombieDef def;
    private int currentHp;
    private double x;
    private int lane;
    private int waveNumber;
    private final List<Armor> armors = new ArrayList<>();
    private boolean alive = true;
    private boolean deathHandled;
    private final boolean glowing;
    private boolean hasThrownImp;
    private int stolenSun;
    private boolean torchLit = true;
    private final Map<String, Integer> activeEffects = new HashMap<>();
    private int frozenTicks;
    private double chillFactor = 1.0;
    private boolean hypnotized;
    private boolean reversed;
    private boolean dynamiteActive;
    private boolean hadBarrel;
    private boolean impsSpawned;
    private boolean submerged;
    private boolean sprintOver;
    private final ZombieSpecialBehaviors specialBehaviors = new ZombieSpecialBehaviors();

    public Zombie(ZombieDef def, int lane, double startX) {
        this.def = def;
        this.currentHp = def.getMaxHp();
        this.lane = lane;
        this.x = startX;
        this.glowing = Math.random() < GLOW_CHANCE;
        for (ArmorType type : def.getArmorLayers()) {
            armors.add(new Armor(type));
        }
        if (def.getType() == ZombieType.PROSPECTOR) {
            this.dynamiteActive = true;
            this.activeEffects.put("dynamiteFuse", DYNAMITE_FUSE_TICKS);
        }
        if (def.getType() == ZombieType.BARREL_ROLLER) {
            this.hadBarrel = true;
        }
    }

    public void tick(model.Board board) {
        if (!alive) {
            return;
        }
        if (frozenTicks > 0) {
            frozenTicks--;
            return;
        }
        if (!isImmobile()) {
            tryMove(board);
        }
        runSpecialAbility(board);
    }

    private boolean isImmobile() {
        return def.getType() == ZombieType.FISHERMAN || def.getType() == ZombieType.KING;
    }

    private void tryMove(model.Board board) {
        if (hypnotized) {
            moveAsHypnotized(board);
            return;
        }
        Plant target = plantBlockingMe(board);
        if (target != null) {
            attackPlant(board, target);
            return;
        }
        double step = reversed ? -effectiveSpeed(board) : effectiveSpeed(board);
        x -= step;
        applyTileEffects(board);
        if (x <= 0) {
            board.triggerLawnMower(lane, this);
        } else if (x >= board.getColumns()) {
            alive = false;
        }
    }

    private Plant plantBlockingMe(model.Board board) {
        Plant p = board.getPlantInFrontOf(this);
        if (p != null && def.hasBehavior(BehaviorType.FLY_OVER_OBSTACLES) && canFlyOver(p)) {
            return null;
        }
        return p;
    }

    private boolean canFlyOver(Plant p) {
        boolean isTallNut = p.getDef().getType() == PlantType.TALL_NUT;
        if (isTallNut) {
            return false;
        }
        boolean bigObstacle = p.getDef().getMaxHp() >= BIG_OBSTACLE_HP_THRESHOLD;
        boolean hazard = p.getDef().hasTag(Tag.TRAP) || p.getDef().hasTag(Tag.MOVE_ZOMBIES);
        return !bigObstacle && !hazard;
    }

    private void attackPlant(model.Board board, Plant target) {
        if (instantKillsPlants()) {
            board.destroyPlantInstantly(target);
            markAllStarSprintOver();
            return;
        }
        if (target.getDef().hasBehavior(BehaviorType.HYPNOTIZE)) {
            board.destroyPlantInstantly(target);
            this.hypnotize();
            return;
        }
        if (target.getDef().hasTag(Tag.MOVE_ZOMBIES)) {
            board.destroyPlantInstantly(target);
            redirectToAdjacentLane(board);
            return;
        }
        int dmg = isEnraged() ? def.getDamage() * ENRAGED_MULTIPLIER : def.getDamage();
        target.takeDamage(dmg);
        if (!target.isAlive()) {
            board.handlePlantDestroyed(target);
        }
    }

    /** Garlic: eating it doesn't kill the zombie, it gets shoved into a neighboring lane instead. */
    private void redirectToAdjacentLane(model.Board board) {
        int delta = Math.random() < 0.5 ? -1 : 1;
        int candidate = lane + delta;
        lane = (candidate < 0 || candidate >= board.getRows()) ? lane - delta : candidate;
    }

    private void markAllStarSprintOver() {
        if (def.getType() == ZombieType.ALL_STAR) {
            sprintOver = true;
        }
    }

    private boolean instantKillsPlants() {
        boolean torchActive = def.hasBehavior(BehaviorType.TORCH_BURN) && torchLit;
        boolean generic = def.hasBehavior(BehaviorType.INSTANT_KILL_PLANT);
        return torchActive || generic;
    }

    private boolean isEnraged() {
        return def.getType() == ZombieType.NEWSPAPER && !armors.isEmpty()
            && armors.stream().allMatch(Armor::isDestroyed);
    }

    private void moveAsHypnotized(model.Board board) {
        Zombie enemy = board.getNearestEnemyZombieInFront(this);
        if (enemy != null) {
            enemy.takeDamage(def.getDamage(), false, false);
            if (!enemy.isAlive()) {
                board.markDeathHandledIfNeeded(enemy);
            }
            return;
        }
        x += effectiveSpeed(board);
        applyTileEffects(board);
        if (x >= board.getColumns()) {
            alive = false;
        }
    }

    private double effectiveSpeed(model.Board board) {
        double base = 0.1 * def.getSpeed() * board.getDifficultySpeedMultiplier() * chillFactor;
        if (def.getType() == ZombieType.ALL_STAR && !sprintOver) {
            base *= ALL_STAR_SPRINT_MULTIPLIER;
        }
        if (isSpinning()) {
            base *= 2;
        }
        return base;
    }

    private boolean isSpinning() {
        return activeEffects.getOrDefault("spinCooldown", 0) > 0;
    }

    private void applyTileEffects(model.Board board) {
        if (def.hasBehavior(BehaviorType.FLY_OVER_OBSTACLES)) {
            return;
        }
        Tile tile = board.getTileAt((int) Math.round(x), lane);
        if (tile == null) {
            return;
        }
        if (tile.getType() == TileType.SLIPPERY_UP && lane > 0) {
            lane--;
        } else if (tile.getType() == TileType.SLIPPERY_DOWN && lane < board.getRows() - 1) {
            lane++;
        }
        submerged = tile.getType() == TileType.WATER && def.getType() == ZombieType.SNORKEL;
    }

    private void runSpecialAbility(model.Board board) {
        specialBehaviors.run(this, board);
    }

    public void takeDamage(int amount, boolean isPoisonDamage) {
        takeDamage(amount, isPoisonDamage, false);
    }

    public void takeDamage(int amount, boolean isPoisonDamage, boolean fromLobber) {
        if (!alive || (submerged && !fromLobber)) {
            return;
        }
        if (!isPoisonDamage) {
            for (Armor armor : armors) {
                if (!armor.isDestroyed()) {
                    armor.takeDamage(amount);
                    return;
                }
            }
        }
        currentHp -= amount;
        if (currentHp <= 0) {
            alive = false;
        }
    }

    public void forceKill() {
        alive = false;
    }

    void die(model.Board board) {
        deathHandled = true;
        System.out.printf("Zombie of type %s is dead at (%.0f, %d)%n", def.getType(), x, lane);
        returnStolenSunOnDeath(board);
        if (def.getType() == ZombieType.WIZARD) {
            board.revertCatsCastBy(this);
        }
        if (glowing) {
            board.grantPlantFood();
        }
        board.maybeDropCurrency();
        board.onZombieKilled(this);
    }

    private void returnStolenSunOnDeath(model.Board board) {
        if (def.getType() == ZombieType.RA_ZOMBIE && stolenSun > 0) {
            board.addSun(stolenSun);
        } else if (def.getType() == ZombieType.TURQUOISE && stolenSun > 0) {
            board.addSun(stolenSun / 2);
        }
    }

    public void applyEffect(String name, int ticks) {
        activeEffects.put(name, ticks);
    }

    public void applyChill(double factor, int ticks) {
        this.chillFactor = factor;
        activeEffects.put("chilled", ticks);
    }

    public void triggerSpin(int ticks) {
        if (def.getType() == ZombieType.JESTER) {
            activeEffects.put("spinCooldown", ticks);
        }
    }

    public void hypnotize() {
        this.hypnotized = true;
    }

    public void extinguishDynamite() {
        dynamiteActive = false;
        activeEffects.remove("dynamiteFuse");
    }

    public void setTorchLit(boolean lit) {
        this.torchLit = lit;
    }

    public void stripArmor(ArmorType type) {
        armors.removeIf(a -> a.getType() == type);
    }

    /** Used by e.g. King ("upgrades" a nearby normal zombie into a knight). */
    public void addArmor(ArmorType type) {
        armors.add(new Armor(type));
    }

    public boolean isReflecting() {
        return def.getType() == ZombieType.PARASOL || (def.getType() == ZombieType.JESTER && isSpinning());
    }

    public boolean isSlowed() {
        return chillFactor < 1.0;
    }

    public boolean isFrozen() {
        return frozenTicks > 0;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isDeathHandled() {
        return deathHandled;
    }

    public double getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    void setLane(int lane) {
        this.lane = lane;
    }

    public ZombieDef getDef() {
        return def;
    }

    public List<Armor> getArmors() {
        return armors;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    public boolean isGlowing() {
        return glowing;
    }

    // ------------------------------------------------------------------
    // Package-private accessors for ZombieSpecialBehaviors only - not part
    // of Zombie's public API, just the seam for that split (see its javadoc).
    // ------------------------------------------------------------------

    Map<String, Integer> getActiveEffects() {
        return activeEffects;
    }

    void addStolenSun(int amount) {
        stolenSun += amount;
    }

    boolean hasThrownImp() {
        return hasThrownImp;
    }

    void setHasThrownImp(boolean value) {
        this.hasThrownImp = value;
    }

    boolean isDynamiteActive() {
        return dynamiteActive;
    }

    void setDynamiteActive(boolean value) {
        this.dynamiteActive = value;
    }

    void setReversed(boolean value) {
        this.reversed = value;
    }

    boolean isHadBarrel() {
        return hadBarrel;
    }

    void setHadBarrel(boolean value) {
        this.hadBarrel = value;
    }

    boolean isImpsSpawned() {
        return impsSpawned;
    }

    void setImpsSpawned(boolean value) {
        this.impsSpawned = value;
    }
}
