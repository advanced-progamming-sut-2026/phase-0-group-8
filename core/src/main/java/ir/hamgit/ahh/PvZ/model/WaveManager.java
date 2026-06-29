package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.def.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    /**
     * Builds a list of random zombies whose total combined waveCost equals
     * or is just under the calculated wave budget.
     * * @param waveCost The total budget for this wave (from getWaveCost)
     * @return A list of Zombie definitions ready to be spawned
     */
    public List<ZombieDef> buildWave(int waveCost) {
        List<ZombieDef> waveZombies = new ArrayList<>();
        List<ZombieType> availableTypes = getZombiesForChapter();

        List<ZombieDef> availableDefs = new ArrayList<>();
        int minCost = Integer.MAX_VALUE;

        for (ZombieType type : availableTypes) {
            ZombieDef def = ZombieRegistry.get(type);
            if (def != null) {
                availableDefs.add(def);
                if (def.getWaveCost() < minCost) {
                    minCost = def.getWaveCost();
                }
            }
        }

        if (availableDefs.isEmpty()) {
            return waveZombies;
        }

        int currentCost = 0;
        Random rand = new Random();

        while (currentCost + minCost <= waveCost) {
            ZombieDef randomZombie = availableDefs.get(rand.nextInt(availableDefs.size()));

            if (currentCost + randomZombie.getWaveCost() <= waveCost) {
                waveZombies.add(randomZombie);
                currentCost += randomZombie.getWaveCost();
            }
        }

        return waveZombies;
    }

    /**
     * Filters and returns only the zombie types that are allowed to spawn in this chapter.
     * * @return A list of valid ZombieTypes for the current chapter
     */
    private List<ZombieType> getZombiesForChapter() {
        List<ZombieType> validZombies = new ArrayList<>();

        List<ZombieDef> chapterZombies = ZombieRegistry.getForChapter(chapter.name());
        if (chapterZombies != null) {
            for (ZombieDef def : chapterZombies) {
                validZombies.add(def.getType());
            }
        }

        List<ZombieDef> commonZombies = ZombieRegistry.getForChapter("ALL");
        if (commonZombies != null) {
            for (ZombieDef def : commonZombies) {
                if (!validZombies.contains(def.getType())) {
                    validZombies.add(def.getType());
                }
            }
        }

        return validZombies;
    }
}
