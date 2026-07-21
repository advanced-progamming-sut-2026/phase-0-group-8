package ir.hamgit.ahh.PvZ.model.minigame;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayList;
import java.util.List;


public class VasebreakerGame implements MinigameSession {

    private enum VaseKind { EMPTY, ZOMBIE, SEED_PACKET }

    private static final int SEED_EXPIRY_TICKS = 100;
    private static final double ZOMBIE_CHANCE = 0.28;
    private static final double SEED_CHANCE = 0.22;

    private final Board board;
    private final boolean[][] broken;
    private final VaseKind[][] kind;
    private final boolean[][] gargantuarVase;
    private final PlantType[][] seedType;
    private final int[][] seedTicksRemaining;
    private final List<PlantType> heldSeeds = new ArrayList<>();
    private boolean won;

    public VasebreakerGame(List<int[]> plantVasePositions, List<int[]> gargantuarVasePositions) {
        this.board = new Board(ChapterType.MINIGAME, 1, 3, null, new MinigameLevelHandler());
        int rows = board.getRows();
        int cols = board.getColumns();
        this.broken = new boolean[rows][cols];
        this.kind = new VaseKind[rows][cols];
        this.gargantuarVase = new boolean[rows][cols];
        this.seedType = new PlantType[rows][cols];
        this.seedTicksRemaining = new int[rows][cols];
        randomizeVases();
        forceVaseKind(plantVasePositions, VaseKind.SEED_PACKET);
        forceGargantuarVases(gargantuarVasePositions);
    }

    private void randomizeVases() {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getColumns(); c++) {
                double roll = Math.random();
                kind[r][c] = roll < ZOMBIE_CHANCE ? VaseKind.ZOMBIE
                    : roll < ZOMBIE_CHANCE + SEED_CHANCE ? VaseKind.SEED_PACKET : VaseKind.EMPTY;
            }
        }
    }

    private void forceVaseKind(List<int[]> positions, VaseKind forced) {
        for (int[] pos : positions) {
            kind[pos[1]][pos[0]] = forced;
        }
    }

    private void forceGargantuarVases(List<int[]> positions) {
        for (int[] pos : positions) {
            kind[pos[1]][pos[0]] = VaseKind.ZOMBIE;
            gargantuarVase[pos[1]][pos[0]] = true;
        }
    }

    public void breakVase(int x, int lane) {
        if (!inBounds(x, lane)) {
            System.out.println("That vase position is outside the board.");
            return;
        }
        if (broken[lane][x]) {
            return;
        }
        broken[lane][x] = true;
        System.out.printf("Vase at (%d, %d) breaks open!%n", x, lane);
        switch (kind[lane][x]) {
            case ZOMBIE -> spawnRevealedZombie(x, lane);
            case SEED_PACKET -> dropSeedPacket(x, lane);
            default -> System.out.println("It was empty.");
        }
    }

    private void spawnRevealedZombie(int x, int lane) {
        ZombieType type = gargantuarVase[lane][x] ? ZombieType.GARGANTUAR : ZombieType.NORMAL;
        board.spawnZombieAt(type, lane, x);
    }

    private void dropSeedPacket(int x, int lane) {
        List<PlantType> options = new ArrayList<>(PlantRegistry.getAll().stream()
            .map(PlantDef::getType).toList());
        seedType[lane][x] = options.get((int) (Math.random() * options.size()));
        seedTicksRemaining[lane][x] = SEED_EXPIRY_TICKS;
        System.out.printf("A seed packet for %s appeared at (%d, %d)!%n", seedType[lane][x], x, lane);
    }

    public void collectSeedPacket(int x, int lane) {
        if (!inBounds(x, lane)) {
            System.out.println("That seed position is outside the board.");
            return;
        }
        if (seedType[lane][x] == null) {
            System.out.println("No seed packet there.");
            return;
        }
        heldSeeds.add(seedType[lane][x]);
        seedType[lane][x] = null;
        System.out.println("Picked up a seed packet.");
    }

    public boolean plantHeldSeed(PlantType type, int x, int lane) {
        if (!heldSeeds.remove(type)) {
            System.out.println("You don't have that seed packet.");
            return false;
        }
        boolean placed = board.plantForFree(type, x, lane);
        if (!placed) {
            heldSeeds.add(type);
        }
        return placed;
    }

    public void tick(int ticks) {
        if (ticks <= 0) {
            return;
        }
        for (int i = 0; i < ticks; i++) {
            tickSeedExpiry();
            board.advanceTime(1);
        }
        checkWin();
    }

    private void tickSeedExpiry() {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getColumns(); c++) {
                if (seedType[r][c] != null && --seedTicksRemaining[r][c] <= 0) {
                    System.out.printf("The seed packet at (%d, %d) withered away.%n", c, r);
                    seedType[r][c] = null;
                }
            }
        }
    }

    private void checkWin() {
        for (boolean[] row : broken) {
            for (boolean b : row) {
                if (!b) {
                    return;
                }
            }
        }
        won = board.getZombies().stream().noneMatch(zombie -> zombie.isAlive());
    }

    private boolean inBounds(int x, int lane) {
        return x >= 0 && x < board.getColumns() && lane >= 0 && lane < board.getRows();
    }

    public boolean isOver() {
        return won || board.isGameOver();
    }

    public boolean isWon() {
        return won && !board.isGameOver();
    }

    public Board getBoard() {
        return board;
    }
}
