package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;



import java.util.Collections;
import java.util.Set;

public abstract class SpecialLevelHandler {

    public void onLevelStart(Board board) {
    }

    public void onTick(Board board) {
    }

    public void onWaveStart(Board board, int waveNumber) {
    }

    public void onZombieKilled(Board board, Zombie zombie) {
    }

    public void onPlantLost(Board board, Plant plant) {
    }

    public boolean blocksNaturalSun() {
        return false;
    }

    public boolean checkCustomLoss(Board board) {
        return false;
    }

    public boolean checkCustomWin(Board board) {
        return false;
    }

    public Set<PlantType> getLockedPlantTypes() {
        return Collections.emptySet();
    }

    public boolean isSelectablePlant(PlantType type) {
        return true;
    }

    public boolean canPluckPlant(Plant plant) {
        return true;
    }

    public boolean waitsForManualWaveStart() {
        return false;
    }

    public boolean blocksWaveSpawning() {
        return false;
    }

    public int getInitialSun() {
        return -1;
    }
}
