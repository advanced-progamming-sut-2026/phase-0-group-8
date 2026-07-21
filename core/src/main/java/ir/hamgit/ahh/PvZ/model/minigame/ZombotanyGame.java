package ir.hamgit.ahh.PvZ.model.minigame;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

public class ZombotanyGame implements MinigameSession {

    private fina

    public ZombotanyGame(int level) {
        board = new Board(ChapterType.MINIGAME, 1, Math.min(5, level + 2), null,
            new MinigameLevelHandler());
        setupPlants();
        spawnHybrids(level);
    }

    private void setupPlants() {
        for (int lane = 0; lane < board.getRows(); lane++) {
            board.plantForFree(PlantType.SUNFLOWER, 1, lane);
            board.plantForFree(PlantType.PEASHOOTER, 3, lane);
        }
        board.setSunAmount(500);
    }

    private void spawnHybrids(int level) {
        ZombieType[] types = {ZombieType.ZOMBOTANY_PEA, ZombieType.ZOMBOTANY_WALLNUT,
            ZombieType.ZOMBOTANY_JALAPENO, ZombieType.ZOMBOTANY_SQUASH};
        int count = 3 + level * 2;
        for (int i = 0; i < count; i++) {
            board.spawnZombieAt(types[i % types.length], i % board.getRows(), board.getColumns());
        }
    }

    public void tick(int ticks) {
        if (ticks > 0) {
            board.advanceTime(ticks);
        }
    }

    public boolean plant(PlantType type, int x, int lane) {
        return board.plantPlant(type, x, lane);
    }

    public void collectSun(int x, int lane) {
        board.collectSun(x, lane);
        board.collectFallingSun(x, lane);
    }

    public boolean isOver() {
        return board.isGameOver() || board.getZombies().stream().noneMatch(zombie -> zombie.isAlive());
    }

    public boolean isWon() {
        return !board.isGameOver() && board.getZombies().stream().noneMatch(zombie -> zombie.isAlive());
    }

    public Board getBoard() {
        return board;
    }
}
