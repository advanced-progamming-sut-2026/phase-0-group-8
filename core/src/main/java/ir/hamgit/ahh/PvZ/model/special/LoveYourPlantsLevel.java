package ir.hamgit.ahh.PvZ.model.special;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public class LoveYourPlantsLevel extends SpecialLevelHandler {

    private final int maxPlantLosses;
    private int lossCount;

    public LoveYourPlantsLevel(int maxPlantLosses) {
        this.maxPlantLosses = maxPlantLosses;
    }

    @Override
    public void onPlantLost(Board board, Plant plant) {
        lossCount++;
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return lossCount >= maxPlantLosses;
    }

    public int getLossCount() {
        return lossCount;
    }
}
