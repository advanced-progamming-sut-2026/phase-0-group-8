package ir.hamgit.ahh.PvZ.model.special;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;

public class LoveYourPlantsLevel extends SpecialLevelHandler {

    private final int maxPlantLosses;
    private int lossCount;

    public LoveYourPlantsLevel(int maxPlantLosses) {
        this.maxPlantLosses = maxPlantLosses;
    }

    @Override
    public void onPlantLost(Board board, Plant plant) {
        boolean intentional = plant.getDef().hasBehavior(BehaviorType.DISAPPEAR_AFTER_ACTION)
            || plant.getDef().hasBehavior(BehaviorType.LIMITED_LIFESPAN);
        if (!intentional) {
            lossCount++;
        }
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return lossCount >= maxPlantLosses;
    }

    public int getMaxPlantLosses() {
        return maxPlantLosses;
    }

    public int getLossCount() {
        return lossCount;
    }
}
