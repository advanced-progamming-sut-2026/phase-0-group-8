package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.minigame.BeghouledGame;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public class BeghouledLevelHandler extends SpecialLevelHandler {

    private BeghouledGame game;

    public void setGame(BeghouledGame game) {
        this.game = game;
    }

    @Override
    public boolean blocksNaturalSun() {
        return true;
    }

    @Override
    public void onPlantLost(Board board, Plant plant) {
        if (game != null) {
            game.onPlantEatenByZombie(plant.getX(), plant.getLane());
        }
    }
}
