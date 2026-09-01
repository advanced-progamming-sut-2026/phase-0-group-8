package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.quest.LevelQuestTelemetry;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;


import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.registry.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;


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
    private final Random random;
    private int currentWave;
    private int currentWaveTotalHp;

    public WaveManager(ChapterType chapter, int difficulty, int totalWaves) {
        this(chapter, difficulty, 250, totalWaves);
    }

    public WaveManager(ChapterType chapter, int difficulty, int baseWaveCost, int totalWaves) {
        this.chapter = chapter;
        this.difficulty = difficulty;
        this.baseWaveCost = baseWaveCost;
        this.totalWaves = Math.max(1, totalWaves);
        this.waveStarted = new boolean[this.totalWaves];
        long dailySeed = LocalDate.now().toEpochDay() * 31L + chapter.ordinal() * 17L + totalWaves;
        this.random = new Random(dailySeed);
    }

     



    public int getWaveCost(int waveNumber, int totalWavesInLevel) {
        double cost = baseWaveCost * Math.pow(WAVE_GROWTH, waveNumber - 1);
        if (waveNumber == totalWavesInLevel) {
            cost *= FINAL_WAVE_MULTIPLIER;
        }
        int unit = difficultyCostUnit() * 5;
        return Math.max(unit, (int) Math.round(cost / unit) * unit);
    }

    public List<ZombieDef> buildWave(int waveCost) {
        List<ZombieDef> pool = getZombiesForChapter();
        List<ZombieDef> wave = new ArrayList<>();
        int remaining = waveCost;
        int guard = 0;
        while (remaining > 0 && !pool.isEmpty() && guard < BUILD_WAVE_GUARD) {
            int availableBudget = remaining;
            List<ZombieDef> affordable = pool.stream()
                .filter(def -> effectiveCost(def) <= availableBudget)
                .filter(def -> canFillBudget(availableBudget - effectiveCost(def), pool)).toList();
            if (affordable.isEmpty()) {
                
                
                
                affordable = pool.stream()
                    .filter(def -> effectiveCost(def) <= availableBudget)
                    .toList();
                if (affordable.isEmpty()) break;
            }
            ZombieDef pick = affordable.get(random.nextInt(affordable.size()));
            wave.add(pick);
            remaining -= effectiveCost(pick);
            guard++;
        }
        return wave;
    }

    private int effectiveCost(ZombieDef def) {
        return Math.max(1, def.getWaveCost() / 10) * difficultyCostUnit();
    }

    private boolean canFillBudget(int budget, List<ZombieDef> pool) {
        boolean[] reachable = new boolean[budget + 1];
        reachable[0] = true;
        for (int value = 1; value <= budget; value++) {
            for (ZombieDef def : pool) {
                int cost = effectiveCost(def);
                if (value >= cost && reachable[value - cost]) {
                    reachable[value] = true;
                    break;
                }
            }
        }
        return reachable[budget];
    }

    private int difficultyCostUnit() {
        return Math.max(1, (int) Math.round(30.0 / Math.max(1, difficulty)));
    }

    private List<ZombieDef> getZombiesForChapter() {
        return ZombieRegistry.getForChapter(chapter.name());
    }


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
        board.onWaveStart(waveIndex + 1);
        LevelQuestTelemetry.recordWaveStart(board, waveIndex + 1);
        board.getSpecialLevelHandler().onWaveStart(board, waveIndex + 1);
    }

    private void spawnWaveZombies(Board board, List<ZombieDef> wave, int waveIndex) {
        for (ZombieDef def : wave) {
            int lane = random.nextInt(board.getRows());
            boolean finalWave = waveIndex == totalWaves - 1;
            board.spawnZombieAt(def.getType(), lane, board.getZombieSpawnColumn(finalWave));
            currentWaveTotalHp += (int) Math.round(def.getMaxHp() * board.getDifficultySpeedMultiplier());
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

     




    float getContinuousProgress(Board board) {
        if (currentWave >= totalWaves) {
            return 1f;
        }
        float insideWave = 0f;
        if (waveStarted[currentWave] && currentWaveTotalHp > 0) {
            float remainingFraction = remainingHpOfCurrentWave(board) / (float) currentWaveTotalHp;
            insideWave = Math.min(1f, Math.max(0f,
                (1f - remainingFraction) / (1f - (float) WAVE_ADVANCE_HP_FRACTION)));
        }
        return Math.min(1f, (currentWave + insideWave) / Math.max(1f, totalWaves));
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
