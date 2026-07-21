package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.quest.LevelQuestTelemetry;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class BoardLevelOps {

    private final Set<ZombieType> encountered = EnumSet.noneOf(ZombieType.class);
    private boolean brainMode;
    private boolean gameOver;
    private boolean playerWon;
    private int zombiesKilled;

    void spawn(Board board, ZombieDef def, int lane, int x, int wave, double difficultyMultiplier) {
        if (def == null || lane < 0 || lane >= board.getRows()) {
            return;
        }
        Zombie zombie = new Zombie(def, lane, x, difficultyMultiplier);
        zombie.setWaveNumber(wave);
        board.getZombies().add(zombie);
        encountered.add(def.getType());
    }

    void triggerMower(Board board, int lane, Zombie triggeringZombie) {
        boolean[] mowers = board.getLawnMowerAvailability();
        if (brainMode) {
            mowers[lane] = false;
            triggeringZombie.forceKill();
            return;
        }
        if (!mowers[lane]) {
            lose("The zombie ate your brain; LOSER!!!");
            return;
        }
        mowers[lane] = false;
        List<String> killed = killLaneZombies(board, lane);
        System.out.println("The lawn mower in row " + lane + " killed these zombies:");
        killed.forEach(System.out::println);
    }

    private List<String> killLaneZombies(Board board, int lane) {
        List<String> killed = new ArrayList<>();
        for (Zombie zombie : board.getZombies()) {
            boolean vulnerable = zombie.getLane() == lane && zombie.isAlive()
                && zombie.getDef().getType() != ZombieType.GARGANTUAR;
            if (vulnerable) {
                killed.add(zombie.getDef().getType().toString());
                LevelQuestTelemetry.recordMowerKill(board);
                zombie.forceKill();
            }
        }
        return killed;
    }

    void checkTerminalState(Board board, int currentWave, int totalWaves, boolean finalWaveStarted) {
        if (gameOver) {
            return;
        }
        if (board.getSpecialLevelHandler().checkCustomLoss(board)) {
            lose("Level failed.");
        } else if (board.getSpecialLevelHandler().checkCustomWin(board)) {
            win();
        } else if (currentWave >= totalWaves && finalWaveStarted && noZombiesRemain(board)) {
            win();
        }
    }

    private boolean noZombiesRemain(Board board) {
        return board.getZombies().stream().noneMatch(Zombie::isAlive);
    }

    private void win() {
        gameOver = true;
        playerWon = true;
        System.out.println("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    }

    private void lose(String message) {
        gameOver = true;
        playerWon = false;
        System.out.println(message);
    }

    int plantsRemaining(Board board) {
        int result = 0;
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                result += board.getTileAt(x, lane).getPlantLayers().size();
            }
        }
        return result;
    }

    int mowersRemaining(boolean[] mowers) {
        int result = 0;
        for (boolean available : mowers) {
            result += available ? 1 : 0;
        }
        return result;
    }

    void enableBrainMode() {
        brainMode = true;
    }

    void recordKill() {
        zombiesKilled++;
    }

    int getZombiesKilled() {
        return zombiesKilled;
    }

    Set<ZombieType> getEncountered() {
        return Set.copyOf(encountered);
    }

    boolean isGameOver() {
        return gameOver;
    }

    boolean isPlayerWon() {
        return playerWon;
    }
}
