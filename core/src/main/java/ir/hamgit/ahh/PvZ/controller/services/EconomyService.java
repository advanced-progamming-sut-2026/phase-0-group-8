package ir.hamgit.ahh.PvZ.controller.services;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Sun;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.SunType;

import java.util.Random;

public class EconomyService {

    private static EconomyService instance;
    private final Random random;

    private int ticksSinceLastDrop = 0;

    private EconomyService() {
        this.random = new Random();
    }

    public static EconomyService getInstance() {
        if (instance == null) {
            instance = new EconomyService();
        }
        return instance;
    }

    /**
     * Called every tick to handle passive sun generation from the sky.
     */
    public void spawnFallingSuns(Board board, int totalGameTicks) {

    }

    /**
     * Determines the sun type based on the probabilities in the Persian documentation.
     */
    private SunType determineSunType() {
        return null;
    }

    /**
     * Handles the player typing the "collect sun -l (x, y)" command.
     */
    public void collectSun(Board board, int x, int y) {

    }

    /**
     * Processes the explosion for the Radioactive sun.
     * 150 damage to zombies in 5x5, 80 damage to plants in 3x3.
     */
    private void explodeRadioactiveSun(Board board, int centerX, int centerY) {

    }
}
