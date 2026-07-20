package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

public class GreenHousePot implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int x;
    private final int y;
    private boolean isLocked;
    private PlantType plantType;
    private long plantedAt;
    private boolean isMarigold;
    private double growthHours;

    public GreenHousePot(int x, int y, boolean isLocked) {
        this.x = x;
        this.y = y;
        this.isLocked = isLocked;
        this.plantType = null;
        this.plantedAt = 0;
        this.isMarigold = false;
        this.growthHours = 0;
    }

    public void plantRandom(java.util.List<PlantType> unlockedPlants) {
        this.isMarigold = Math.random() < 0.5 || unlockedPlants.isEmpty();
        if (isMarigold) {
            this.plantType = PlantType.MARIGOLD;
            this.growthHours = 2;
        } else {
            this.plantType = unlockedPlants.get((int) (Math.random() * unlockedPlants.size()));
            this.growthHours = 8;
        }
        this.plantedAt = System.currentTimeMillis();
    }

    public void restore(PlantType restoredType, long restoredPlantedAt,
                        boolean restoredMarigold, double restoredGrowthHours) {
        plantType = restoredType;
        plantedAt = restoredPlantedAt;
        isMarigold = restoredMarigold;
        growthHours = restoredGrowthHours;
    }

    public boolean isReadyToCollect() {
        if (plantType == null) {
            return false;
        }
        return hoursElapsed() >= growthHours;
    }

    public double hoursElapsed() {
        return (System.currentTimeMillis() - plantedAt) / 3600000.0;
    }

    public double hoursRemaining() {
        double remaining = growthHours - hoursElapsed();
        return Math.max(0, remaining);
    }

    public int collect(User user) {
        if (!isReadyToCollect()) {
            return -1;
        }
        int reward;
        if (isMarigold) {
            user.addCoins(500);
            reward = 500;
        } else {
            user.setPlantBoost(plantType, true);
            reward = 0;
        }
        plantType = null;
        isMarigold = false;
        return reward;
    }

    public boolean speedGrow(User user) {
        if (isReadyToCollect() || plantType == null) {
            return false;
        }
        int cost = (int) Math.ceil(hoursRemaining());
        if (!user.spendDiamonds(cost)) {
            return false;
        }
        plantedAt = System.currentTimeMillis() - (long) (growthHours * 3600000L);
        return true;
    }

    public void unlock() {
        this.isLocked = false;
    }

    public long getPlantedAt() {
        return plantedAt;
    }

    public double getGrowthHours() {
        return growthHours;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public boolean isMarigold() {
        return isMarigold;
    }

    public boolean isEmpty() {
        return plantType == null;
    }
}
