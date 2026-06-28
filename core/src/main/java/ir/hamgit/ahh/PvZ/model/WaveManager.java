package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.ChapterType;

public class WaveManager {
    private final ChapterType chapter;
    private final int difficulty;
    private final int baseWaveCost;

    public WaveManager(ChapterType chapter, int difficulty, int baseWaveCost) {
        this.chapter = chapter;
        this.difficulty = difficulty;
        this.baseWaveCost = baseWaveCost;
    }
}
