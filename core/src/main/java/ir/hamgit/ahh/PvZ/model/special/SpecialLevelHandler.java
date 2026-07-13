package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Plant;
import ir.hamgit.ahh.PvZ.model.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;



import java.util.Collections;
import java.util.Set;

/**
 * Hook set every level goes through, special or not. A normal level uses
 * {@link NormalLevelHandler} (all defaults, i.e. no special behaviour).
 * Each of the eight special level types from the spec gets its own small
 * subclass in this package; {@code Board} and {@code GameController} call
 * into these hooks at the right points instead of switching on
 * {@code SpecialLevelType} everywhere.
 */
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

    /** Plant What You Get: zombies don't spawn until "start zombie waves" is issued. */
    public boolean waitsForManualWaveStart() {
        return false;
    }

    /** Minigames: they spawn zombies/plants on their own schedule via Board's public API
     *  instead of Board's normal wave system, but still want its per-tick combat loop. */
    public boolean blocksWaveSpawning() {
        return false;
    }

    /** Plant What You Get: fixed starting sun pool instead of the normal 50. */
    public int getInitialSun() {
        return -1;
    }
}
