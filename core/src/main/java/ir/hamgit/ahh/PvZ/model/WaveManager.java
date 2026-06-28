package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.ChapterType;

public class WaveManager {
    private final ChapterType chapter;
    private final int difficulty;
    private final int baseWaveCost;
    private final int totalWaves;

    public WaveManager(ChapterType chapter, int difficulty, int baseWaveCost, int totalWaves) {
        this.chapter = chapter;
        this.difficulty = difficulty;
        this.baseWaveCost = baseWaveCost;
        this.totalWaves = totalWaves;
    }

    /**
     * Calculates the total cost budget allocated for a specific wave.
     * - The cost increases by 25% each wave.
     * - The final wave (flag wave) is double the previous wave's cost.
     * - The difficulty applies a multiplier (dl / 3).
     * * @param waveNumber The current wave index (1-based)
     * @return The calculated cost budget for this wave
     */
    public int getWaveCost(int waveNumber) {
        double cost;

        if (waveNumber == 1) {
            cost = baseWaveCost;
        } else if (waveNumber >= totalWaves) {
            double prevCost = baseWaveCost * Math.pow(1.25, waveNumber - 2);
            cost = prevCost * 2;
        } else {
            cost = baseWaveCost * Math.pow(1.25, waveNumber - 1);
        }

        cost = cost * ((double) difficulty / 3.0);

        return (int) cost;
    }
}
