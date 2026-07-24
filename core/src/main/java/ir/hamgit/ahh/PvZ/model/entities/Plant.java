package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.PlantBehaviorSupport;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.def.PlantLevelEffects;
import ir.hamgit.ahh.PvZ.model.registry.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

public class Plant {

    private static final int PLANT_FOOD_BOOST_TICKS = 100;
    private static final double DOUBLE_SUN_CHANCE = 0.25;
    private static final int MAX_ICE_LAYERS = 3;
    private static final int MAX_STACKED_HEADS = 5;
    private static final int MAX_LEVEL = 4;

    private final PlantDef def;
    private final int x;
    private final int lane;
    private final PlantBehaviorSupport behaviorSupport;
    private int currentHp;
    private int level = 1;
    private int stackCount = 1;
    private int boostTicksRemaining;
    private int iceLayers;
    private int iceShellHp;
    private int metalArmorHp;
    private boolean boosted;
    private boolean frozen;
    private boolean cat;
    private boolean hasUncollectedSun;
    private boolean deathBehaviorHandled;
    private boolean blueFlame;
    private boolean empoweredNextEater;
    private boolean reflectionBoosted;
    private boolean armorExplosionPending;
    private Plant underPlant;

    public Plant(PlantDef def, int x, int lane) {
        this.def = def;
        this.currentHp = Math.max(1, def.getMaxHp());
        this.x = x;
        this.lane = lane;
        this.behaviorSupport = new PlantBehaviorSupport(def);
    }

    public void tick(Board board) {
        tickBoost();
        frozen = iceLayers >= MAX_ICE_LAYERS;
        if (isAlive() && !frozen && !cat) {
            behaviorSupport.tick(this, board);
        }
    }

    private void tickBoost() {
        if (boostTicksRemaining > 0 && --boostTicksRemaining == 0) {
            boosted = false;
        }
    }

    public void applyPlantFood(Board board) {
        applyPlantFood(board, PLANT_FOOD_BOOST_TICKS);
    }

    public void applyPlantFood(Board board, int durationTicks) {
        boosted = true;
        boostTicksRemaining = Math.max(boostTicksRemaining, durationTicks);
        behaviorSupport.applyPlantFood(this, board);
    }

    public void takeDamage(int amount) {
        if (frozen) {
            damageIce(amount);
        } else {
            currentHp -= amount;
            damageMetalArmor(amount);
        }
    }

    private void damageMetalArmor(int amount) {
        if (metalArmorHp <= 0) {
            return;
        }
        int previous = metalArmorHp;
        metalArmorHp = Math.max(0, metalArmorHp - Math.max(0, amount));
        armorExplosionPending = previous > 0 && metalArmorHp == 0
            && def.hasBehavior(BehaviorType.EXPLODE_ON_DEATH);
    }

    public void onBitten(Board board, Zombie attacker) {
        if (def.hasBehavior(BehaviorType.REFLECT_DAMAGE)) {
            int reflected = def.getDamage() + PlantLevelEffects.sum(def.getType(), level, "Reflect Dmg +");
            reflected *= reflectionBoosted ? 2 : 1;
            attacker.takeDamage(reflected, false);
        }
        if (def.hasBehavior(BehaviorType.REDIRECT_ZOMBIE)) {
            attacker.redirectToAdjacentLane(board.getRows());
        }
        if (def.hasBehavior(BehaviorType.SUN_ON_HIT)) {
            board.addSun(5 + PlantLevelEffects.sum(def.getType(), level, "Sun Drop +"));
        }
        if (armorExplosionPending) {
            board.dealAreaDamageToZombies(x, lane, 1, effectiveDamage(def.getDamage()));
            armorExplosionPending = false;
        }
    }

    public void onEaten(Board board, Zombie attacker) {
        board.destroyPlantInstantly(this);
        if (empoweredNextEater) {
            Zombie gargantuar = new Zombie(ZombieRegistry.get(ZombieType.GARGANTUAR),
                attacker.getLane(), attacker.getX());
            gargantuar.hypnotize(true, true);
            board.getZombies().add(gargantuar);
            attacker.forceKill();
            empoweredNextEater = false;
            return;
        }
        attacker.hypnotize(
            PlantLevelEffects.has(def.getType(), level, "Zombie HP Buff"),
            PlantLevelEffects.has(def.getType(), level, "Zombie Dmg Buff"));
    }

    public void triggerDeathBehavior(Board board) {
        if (deathBehaviorHandled) {
            return;
        }
        deathBehaviorHandled = true;
        if (def.hasBehavior(BehaviorType.EXPLODE_ON_DEATH)) {
            int radius = Math.max(1, def.getAbilityProfile().getSplashRadius());
            int damage = effectiveDamage(def.getDamage())
                + PlantLevelEffects.sum(def.getType(), level, "Explode Dmg +");
            board.dealAreaDamageToZombies(x, lane, radius, damage);
        } else if (PlantLevelEffects.has(def.getType(), level, "AoE on Death")) {
            board.dealAreaDamageToZombies(x, lane, 1, Math.max(100, effectiveDamage(def.getDamage())));
        }
    }

    public void applyIceLayer() {
        iceLayers = Math.min(MAX_ICE_LAYERS, iceLayers + 1);
        if (iceLayers == MAX_ICE_LAYERS) {
            frozen = true;
            iceShellHp = 600;
        }
    }

    public void meltIce() {
        iceLayers = 0;
        frozen = false;
        iceShellHp = 0;
    }

    public void freezeCompletely() {
        iceLayers = MAX_ICE_LAYERS;
        frozen = true;
        iceShellHp = 600;
    }

    public void damageIce(int amount) {
        if (!frozen) {
            return;
        }
        iceShellHp -= Math.max(0, amount);
        if (iceShellHp <= 0) {
            meltIce();
        }
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getIceShellHp() {
        return iceShellHp;
    }

    public void markSunCollected() {
        hasUncollectedSun = false;
    }

    public boolean hasUncollectedSun() {
        return hasUncollectedSun;
    }

    public void setHasUncollectedSun(boolean value) {
        hasUncollectedSun = value;
    }

    public int getSunProductionAmount() {
        int amount = behaviorSupport.getSunAmount()
            + PlantLevelEffects.sum(def.getType(), level, "Sun +");
        if (PlantLevelEffects.has(def.getType(), level, "Double Sun Chance")
            && Math.random() < DOUBLE_SUN_CHANCE) {
            amount *= 2;
        }
        return amount;
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

    public void restoreHealth() {
        currentHp = getEffectiveMaxHp();
    }

    public void upgrade() {
        if (level >= MAX_LEVEL) {
            return;
        }
        level++;
        currentHp += PlantLevelEffects.sum(def.getType(), level, "HP +")
            - PlantLevelEffects.sum(def.getType(), level - 1, "HP +");
        behaviorSupport.applyLevel(this);
    }

    public void setLevel(int targetLevel) {
        int safeLevel = Math.max(1, Math.min(MAX_LEVEL, targetLevel));
        while (level < safeLevel) {
            upgrade();
        }
    }

    public int effectiveDamage(int baseValue) {
        return baseValue + PlantLevelEffects.sum(def.getType(), level, "Dmg +");
    }

    public int getEffectiveMaxHp() {
        return Math.max(1, def.getMaxHp() + PlantLevelEffects.sum(def.getType(), level, "HP +"));
    }

    public void addPermanentArmor(int armorHp) {
        int armor = Math.max(0, armorHp);
        currentHp += armor;
        metalArmorHp += armor;
    }

    public void boostReflection() {
        reflectionBoosted = true;
    }

    public void resetLifespan() {
        behaviorSupport.resetLifespan(this);
    }

    public void growImmediately() {
        behaviorSupport.growImmediately(this);
    }

    public void armImmediately() {
        behaviorSupport.armImmediately();
    }

    public void enableBlueFlame() {
        blueFlame = true;
    }

    public boolean hasBlueFlame() {
        return blueFlame;
    }

    public void empowerNextEater() {
        empoweredNextEater = true;
    }

    public boolean addStackedHead() {
        if (stackCount >= MAX_STACKED_HEADS) {
            return false;
        }
        stackCount++;
        currentHp += getEffectiveMaxHp();
        return true;
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

    public int getStackCount() {
        return stackCount;
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
