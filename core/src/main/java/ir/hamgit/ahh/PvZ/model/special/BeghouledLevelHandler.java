package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.minigame.BeghouledGame;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

/**
 * Handler for the Beghouled minigame: unlike every other minigame, waves
 * spawn and never stop (per spec), so {@link #blocksWaveSpawning()} stays
 * false. Sun only comes from matches here, so sky suns are still switched
 * off. Forwards plant-loss events to the game so it can mark that tile a
 * permanent crater.
 */
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
