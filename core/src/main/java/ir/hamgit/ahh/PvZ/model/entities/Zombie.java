package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.ZombieSpecialBehaviors;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.enums.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Zombie {

    private static final double GLOW_CHANCE = 0.05;
    private static final int ALL_STAR_SPRINT_MULTIPLIER = 3;
    private static final int ENRAGED_MULTIPLIER = 3;
    private static final int BIG_OBSTACLE_HP_THRESHOLD = 1000;
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
    private int stunnedTicks;
    private int chilledTicks;
    private int poisonTicks;
    private int poisonDamage;
    private int poisonTimer;
    private double chillFactor = 1.0;
    private boolean hypnotized;
    private int hypnotizedDamagePercent = 100;
    private boolean reversed;
    private boolean dynamiteActive;
    private boolean hadBarrel;
    private boolean impsSpawned;
    private boolean submerged;
    private boolean sprintOver;
    private final double difficultyMultiplier;
    private boolean stationary;
    private final ZombieSpecialBehaviors specialBehaviors = new ZombieSpecialBehaviors();

    public Zombie(ZombieDef def, int lane, double startX) {
        this(def, lane, startX, 1.0);
    }

    public Zombie(ZombieDef def, int lane, double startX, double difficultyMultiplier) {
        this.def = def;
        this.difficultyMultiplier = difficultyMultiplier;
        this.currentHp = Math.max(1, (int) Math.round(def.getMaxHp() * difficultyMultiplier));
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

    public void tick(Board board) {
        if (!alive) {
            return;
        }
        tickStatusEffects();
        if (!alive) {
            return;
        }
        if (frozenTicks > 0) {
            frozenTicks--;
            return;
        }
        if (stunnedTicks > 0) {
            stunnedTicks--;
            return;
        }
        if (!isImmobile() && !stationary) {
            tryMove(board);
        }
        runSpecialAbility(board);
    }

    private boolean isImmobile() {
        return def.getType() == ZombieType.FISHERMAN || def.getType() == ZombieType.KING;
    }

    private void tryMove(Board board) {
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

    private Plant plantBlockingMe(Board board) {
        Plant p = board.getPlantInFrontOf(this);
        if (p != null && def.hasBehavior(BehaviorType.FLY_OVER_OBSTACLES) && canFlyOver(p)) {
            return null;
        }
        return p;
    }

    private boolean canFlyOver(Plant p) {
        if (p.getDef().hasBehavior(BehaviorType.PREVENT_JUMP)) {
            return false;
        }
        boolean bigObstacle = p.getDef().getMaxHp() >= BIG_OBSTACLE_HP_THRESHOLD;
        boolean hazard = p.getDef().hasTag(Tag.TRAP) || p.getDef().hasTag(Tag.MOVE_ZOMBIES);
        return !bigObstacle && !hazard;
    }

    private void attackPlant(Board board, Plant target) {
        if (def.getType() == ZombieType.SNORKEL) {
            submerged = false;
        }
        if (target.getDef().hasBehavior(BehaviorType.HYPNOTIZE_WHEN_EATEN)) {
            target.onEaten(board, this);
            return;
        }
        if (instantKillsPlants()) {
            board.destroyPlantInstantly(target);
            markAllStarSprintOver();
            if (def.getType() == ZombieType.ZOMBOTANY_SQUASH) {
                forceKill();
            }
            return;
        }
        
        
        int baseDamage = damagePerTick(def.getDamage(), difficultyMultiplier);
        int dmg = isEnraged() ? baseDamage * ENRAGED_MULTIPLIER : baseDamage;
        target.takeDamage(dmg);
        target.onBitten(board, this);
        if (!target.isAlive()) {
            board.handlePlantDestroyed(target);
        }
    }

    public void redirectToAdjacentLane(int rows) {
        int delta = Math.random() < 0.5 ? -1 : 1;
        int candidate = lane + delta;
        lane = (candidate < 0 || candidate >= rows) ? lane - delta : candidate;
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

    private void moveAsHypnotized(Board board) {
        Zombie enemy = board.getNearestEnemyZombieInFront(this);
        if (enemy != null) {
            int damage = damagePerTick(def.getDamage(), difficultyMultiplier);
            enemy.takeDamage(damage * hypnotizedDamagePercent / 100, false, false);
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

    private double effectiveSpeed(Board board) {
        double base = 0.1 * def.getSpeed() * board.getDifficultySpeedMultiplier() * chillFactor;
        if (def.getType() == ZombieType.ALL_STAR && !sprintOver) {
            base *= ALL_STAR_SPRINT_MULTIPLIER;
        }
        if (isSpinning()) {
            base *= 2;
        }
        if (isEnraged()) {
            base *= ENRAGED_MULTIPLIER;
        }
        return base;
    }

    private int damagePerTick(int damagePerSecond, double multiplier) {
        return Math.max(1, (int) Math.round(
            damagePerSecond * multiplier / Board.TICKS_PER_SECOND));
    }

    private boolean isSpinning() {
        return activeEffects.getOrDefault("spinCooldown", 0) > 0;
    }

    private void applyTileEffects(Board board) {
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

    private void runSpecialAbility(Board board) {
        specialBehaviors.run(this, board);
    }

    private void tickStatusEffects() {
        if (chilledTicks > 0 && --chilledTicks == 0) {
            chillFactor = 1.0;
        }
        if (poisonTicks > 0) {
            poisonTicks--;
            poisonTimer--;
            if (poisonTimer <= 0) {
                takeDamage(poisonDamage, true);
                poisonTimer = Board.TICKS_PER_SECOND;
            }
        }
    }

    public void takeDamage(int amount, boolean isPoisonDamage) {
        takeDamage(amount, isPoisonDamage, false);
    }

    public void takeDamage(int amount, boolean isPoisonDamage, boolean fromLobber) {
        if (!alive || (submerged && !fromLobber)) {
            return;
        }
        int remaining = amount;
        if (!isPoisonDamage) {
            for (Armor armor : armors) {
                if (!armor.isDestroyed()) {
                    remaining = armor.absorbDamage(remaining);
                    if (remaining <= 0) {
                        return;
                    }
                }
            }
        }
        currentHp -= remaining;
        if (currentHp <= 0) {
            alive = false;
        }
    }

    public void forceKill() {
        alive = false;
    }

    public void die(Board board) {
        deathHandled = true;
        System.out.printf("Zombie of type %s is dead at (%.0f, %d)%n", def.getType(), x, lane);
        returnStolenSunOnDeath(board);
        if (def.getType() == ZombieType.WIZARD) {
            board.revertCatsCastBy(this);
        }
        if (glowing) {
            board.dropPlantFood(x, lane);
        }
        board.maybeDropCurrency();
        board.onZombieKilled(this);
    }

    private void returnStolenSunOnDeath(Board board) {
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
        chilledTicks = Math.max(chilledTicks, ticks);
    }

    public void applyStun(int ticks) {
        stunnedTicks = Math.max(stunnedTicks, ticks);
    }

    public void applyPoison(int damage, int ticks) {
        poisonDamage = Math.max(poisonDamage, damage);
        poisonTicks = Math.max(poisonTicks, ticks);
        poisonTimer = 1;
    }

    public void freeze(int ticks) {
        frozenTicks = Math.max(frozenTicks, ticks);
    }

    public void warm() {
        frozenTicks = 0;
        chilledTicks = 0;
        chillFactor = 1.0;
    }

    public void triggerSpin(int ticks) {
        if (def.getType() == ZombieType.JESTER) {
            activeEffects.put("spinCooldown", ticks);
        }
    }

    public void hypnotize() {
        this.hypnotized = true;
    }

    public void hypnotize(boolean healthBuff, boolean damageBuff) {
        hypnotize();
        if (healthBuff) {
            currentHp += Math.max(1, def.getMaxHp() / 2);
        }
        if (damageBuff) {
            hypnotizedDamagePercent = 150;
        }
    }

    public boolean isHypnotized() {
        return hypnotized;
    }

    public void knockBack(int tiles) {
        x += Math.max(0, tiles);
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

    public void setLane(int lane) {
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

    public void setStationary(boolean stationary) {
        this.stationary = stationary;
    }

    public Map<String, Integer> getActiveEffects() {
        return activeEffects;
    }

    public void addStolenSun(int amount) {
        stolenSun += amount;
    }

    public boolean hasThrownImp() {
        return hasThrownImp;
    }

    public void setHasThrownImp(boolean value) {
        this.hasThrownImp = value;
    }

    public boolean isDynamiteActive() {
        return dynamiteActive;
    }

    public void setDynamiteActive(boolean value) {
        this.dynamiteActive = value;
    }

    public void setReversed(boolean value) {
        this.reversed = value;
    }

    public boolean isHadBarrel() {
        return hadBarrel;
    }

    public void setHadBarrel(boolean value) {
        this.hadBarrel = value;
    }

    public boolean isImpsSpawned() { return impsSpawned; }

    public void setImpsSpawned(boolean value) {
        this.impsSpawned = value;
    }
}
