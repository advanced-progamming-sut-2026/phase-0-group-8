package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.def.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes per-wave zombie budgets and builds a random wave that sums to
 * that budget. Per spec: each wave is 25% harder than the last, and the
 * final ("flag") wave is double the previous wave's cost.
 */
public class WaveManager {

    private static final double WAVE_GROWTH = 1.25;
    private static final double FINAL_WAVE_MULTIPLIER = 2.0;

    private final ChapterType chapter;
    private final int difficulty;
    private final int baseWaveCost;

    public WaveManager(ChapterType chapter, int difficulty) {
        this(chapter, difficulty, 100);
    }

    public WaveManager(ChapterType chapter, int difficulty, int baseWaveCost) {
        this.chapter = chapter;
        this.difficulty = difficulty;
        this.baseWaveCost = baseWaveCost;
    }

    /**
     * @param waveNumber 1-based wave index within the level
     * @param totalWaves total number of waves in this level (used to detect the final/flag wave)
     */
    public int getWaveCost(int waveNumber, int totalWaves) {
        double cost = baseWaveCost * Math.pow(WAVE_GROWTH, waveNumber - 1);
        if (waveNumber == totalWaves) {
            cost *= FINAL_WAVE_MULTIPLIER;
        }
        double difficultyMultiplier = 3.0 / Math.max(1, difficulty);
        return (int) Math.round(cost * difficultyMultiplier);
    }

    public List<ZombieDef> buildWave(int waveCost) {
        List<ZombieDef> pool = getZombiesForChapter();
        List<ZombieDef> wave = new ArrayList<>();
        int remaining = waveCost;
        int guard = 0;
        while (remaining > 0 && !pool.isEmpty() && guard < 200) {
            ZombieDef pick = pool.get((int) (Math.random() * pool.size()));
            wave.add(pick);
            remaining -= Math.max(1, pick.getWaveCost());
            guard++;
        }
        return wave;
    }

    private List<ZombieDef> getZombiesForChapter() {
        return ZombieRegistry.getForChapter(chapter.name());
    }
}
