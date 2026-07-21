package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * STUB. The user/auth teammate owns the real {@code User} class (see the
 * class reference: username, passwordHash, nickname, email, coins,
 * diamonds, unlockedPlants, seenZombies, quest progress, etc).
 *
 * <p>This trimmed-down version only has the fields/methods that
 * {@code GameController} and {@code Board} need to call into for currency
 * and unlock bookkeeping while playing a level, so the combat/currency code
 * compiles and is independently testable. When the real User class lands,
 * delete this file and everything else should keep compiling unchanged as
 * long as the real class keeps these same method signatures (or update the
 * few call sites in GameController - they're all in one place).</p>
 */
public class User {

    private int coins;
    private int diamonds;
    private int difficulty = 3;
    private int bestScoreMode;
    private final Set<PlantType> unlockedPlants = EnumSet.noneOf(PlantType.class);
    private final Set<ZombieType> seenZombies = new HashSet<>();
    private final Map<PlantType, Integer> plantLevels = new EnumMap<>(PlantType.class);
    private final Map<PlantType, Boolean> plantBoosts = new EnumMap<>(PlantType.class);
    private int greenhousePots;

    public int getCoins() {
        return coins;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void addCoins(int amount) {
        coins += amount;
    }

    public void addDiamonds(int amount) {
        diamonds += amount;
    }

    public boolean spendCoins(int amount) {
        if (coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    public boolean spendDiamonds(int amount) {
        if (diamonds < amount) {
            return false;
        }
        diamonds -= amount;
        return true;
    }

    public void unlockPlant(PlantType type) {
        unlockedPlants.add(type);
    }

    public Set<PlantType> getUnlockedPlants() {
        return unlockedPlants;
    }

    public void seeZombie(ZombieType type) {
        seenZombies.add(type);
    }

    public Set<ZombieType> getSeenZombies() {
        return seenZombies;
    }

    public int getPlantLevel(PlantType type) {
        return plantLevels.getOrDefault(type, 1);
    }

    public boolean consumeBoostIfAvailable(PlantType type) {
        Boolean has = plantBoosts.get(type);
        if (Boolean.TRUE.equals(has)) {
            plantBoosts.put(type, false);
            return true;
        }
        return false;
    }

    public void storeBoost(PlantType type) {
        plantBoosts.putIfAbsent(type, true);
    }

    public void addGreenhousePot() {
        greenhousePots++;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getBestScoreMode() {
        return bestScoreMode;
    }

    public void setBestScoreMode(int bestScoreMode) {
        this.bestScoreMode = Math.max(this.bestScoreMode, bestScoreMode);
    }
}
