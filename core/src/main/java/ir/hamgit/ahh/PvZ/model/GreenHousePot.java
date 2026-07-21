package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class GreenHousePot implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean isLocked;
    private PlantType plantType;
    private long plantedAt;
    private boolean isMarigold;
    private double growthHours;

    public GreenHousePot(boolean isLocked) {
        this.isLocked = isLocked;
    }

    public boolean plantRandom(List<PlantType> unlockedPlants) {
        return plantRandom(unlockedPlants, ThreadLocalRandom.current());
    }

    boolean plantRandom(List<PlantType> unlockedPlants, RandomGenerator random) {
        if (isLocked || !isEmpty() || unlockedPlants == null) {
            return false;
        }
        isMarigold = random.nextDouble() < 0.5 || unlockedPlants.isEmpty();
        if (isMarigold) {
            plantType = PlantType.MARIGOLD;
            growthHours = 2;
        } else {
            plantType = unlockedPlants.get(random.nextInt(unlockedPlants.size()));
            growthHours = 8;
        }
        plantedAt = System.currentTimeMillis();
        return true;
    }

    public Harvest harvest() {
        if (!isReadyToCollect()) {
            return null;
        }
        Harvest result = new Harvest(plantType, isMarigold);
        plantType = null;
        plantedAt = 0;
        isMarigold = false;
        growthHours = 0;
        return result;
    }

    public int getAccelerationCost() {
        if (isEmpty() || isReadyToCollect()) {
            return 0;
        }
        return (int) Math.ceil(hoursRemaining());
    }

    public boolean finishGrowth() {
        if (isEmpty() || isReadyToCollect()) {
            return false;
        }
        plantedAt = System.currentTimeMillis() - (long) (growthHours * 3_600_000L);
        return true;
    }

    public boolean isReadyToCollect() {
        return !isEmpty() && hoursElapsed() >= growthHours;
    }

    private double hoursElapsed() {
        return Math.max(0, (System.currentTimeMillis() - plantedAt) / 3_600_000.0);
    }

    public double hoursRemaining() {
        return isEmpty() ? 0 : Math.max(0, growthHours - hoursElapsed());
    }

    public void unlock() {
        isLocked = false;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public boolean isEmpty() {
        return plantType == null;
    }

    /** Result of harvesting; the service applies its reward to the user. */
    public record Harvest(PlantType plantType, boolean marigold) {
    }
}
