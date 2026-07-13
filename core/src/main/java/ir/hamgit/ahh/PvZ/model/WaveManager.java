package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.def.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns wave timing and composition for a level: per-wave zombie budgets
 * (each wave 25% harder than the last, final "flag" wave doubled per spec),
 * random wave composition, and the 75%-cleared trigger for the next wave.
 * Split out of {@link Board} to keep Board under the project's class-length
 * Checkstyle/PMD guideline - every method that needs board state (rows/
 * columns, zombies, the special-level handler) reaches it through Board's
 * own public API, exactly like {@link ZombieAbilitySupport}.
 */
public class WaveManager {

    private static final double WAVE_GROWTH = 1.25;
    private static final double FINAL_WAVE_MULTIPLIER = 2.0;
    private static final double WAVE_ADVANCE_HP_FRACTION = 0.25;
    private static final int BUILD_WAVE_GUARD = 200;

    private final ChapterType chapter;
    private final int difficulty;
    private final int baseWaveCost;
    private final int totalWaves;
    private final boolean[] waveStarted;
    private int currentWave;
    private int currentWaveTotalHp;

    public WaveManager(ChapterType chapter, int difficulty, int totalWaves) {
        this(chapter, difficulty, 100, totalWaves);
    }

    public WaveManager(ChapterType chapter, int difficulty, int baseWaveCost, int totalWaves) {
        this.chapter = chapter;
        this.difficulty = difficulty;
        this.baseWaveCost = baseWaveCost;
        this.totalWaves = Math.max(1, totalWaves);
        this.waveStarted = new boolean[this.totalWaves];
    }

    /**
     * @param waveNumber 1-based wave index within the level
     * @param totalWavesInLevel total number of waves in this level (used to detect the final/flag wave)
     */
    public int getWaveCost(int waveNumber, int totalWavesInLevel) {
        double cost = baseWaveCost * Math.pow(WAVE_GROWTH, waveNumber - 1);
        if (waveNumber == totalWavesInLevel) {
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
        while (remaining > 0 && !pool.isEmpty() && guard < BUILD_WAVE_GUARD) {
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

    // ------------------------------------------------------------------
    // Wave timing (moved from Board)
    // ------------------------------------------------------------------

    void checkAdvance(Board board) {
        boolean skip = board.isGameOver() || currentWave >= totalWaves
            || board.getSpecialLevelHandler().waitsForManualWaveStart()
            || board.getSpecialLevelHandler().blocksWaveSpawning();
        if (skip) {
            return;
        }
        if (!waveStarted[currentWave]) {
            spawnWave(board, currentWave);
        } else if (remainingHpOfCurrentWave(board) <= currentWaveTotalHp * WAVE_ADVANCE_HP_FRACTION) {
            advanceToNextWave(board);
        }
    }

    private void advanceToNextWave(Board board) {
        currentWave++;
        if (currentWave < totalWaves) {
            spawnWave(board, currentWave);
        }
    }

    private void spawnWave(Board board, int waveIndex) {
        waveStarted[waveIndex] = true;
        boolean finalWave = waveIndex == totalWaves - 1;
        System.out.println(finalWave ? "The final wave has come." : "Wave " + (waveIndex + 1) + " started.");
        int cost = getWaveCost(waveIndex + 1, totalWaves);
        List<ZombieDef> wave = buildWave(cost);
        currentWaveTotalHp = 0;
        spawnWaveZombies(board, wave, waveIndex);
        board.getSpecialLevelHandler().onWaveStart(board, waveIndex + 1);
    }

    private void spawnWaveZombies(Board board, List<ZombieDef> wave, int waveIndex) {
        for (ZombieDef def : wave) {
            int lane = (int) (Math.random() * board.getRows());
            board.spawnZombieAt(def.getType(), lane, board.getColumns());
            currentWaveTotalHp += def.getMaxHp();
            System.out.printf("Zombie %s spawned at wave %d in lane %d which costed %d.%n",
                def.getType(), waveIndex + 1, lane, def.getWaveCost());
        }
    }

    private int remainingHpOfCurrentWave(Board board) {
        int remaining = 0;
        for (Zombie z : board.getZombies()) {
            if (z.getWaveNumber() == currentWave) {
                remaining += Math.max(0, z.getCurrentHp());
            }
        }
        return remaining;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return totalWaves;
    }

    public boolean hasFinalWaveStarted() {
        return waveStarted[totalWaves - 1];
    }
}
