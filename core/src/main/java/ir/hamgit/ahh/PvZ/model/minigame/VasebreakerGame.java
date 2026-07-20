package ir.hamgit.ahh.PvZ.model.minigame;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.def.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VasebreakerGame {

    private enum VaseKind { EMPTY, ZOMBIE, SEED_PACKET }

    private static final Pattern COORD_PATTERN = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");
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

    public void handle(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("break vase")) {
            handleBreakVase(trimmed);
        } else if (trimmed.startsWith("collect seed packet")) {
            handleCollectSeed(trimmed);
        } else if (trimmed.startsWith("plant plant")) {
            handlePlant(trimmed);
        } else if (trimmed.startsWith("advance time")) {
            Map<String, String> flags = CommandParser.parse(trimmed);
            tick(CommandParser.getIntFlag(flags, "-t", 1));
        } else if (trimmed.startsWith("show map")) {
            board.showMap();
        } else {
            System.out.println("Unknown Vasebreaker command: " + raw);
        }
    }

    public void breakVase(int x, int lane) {
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
        won = true;
    }

    public boolean isOver() {
        return won || board.isGameOver();
    }

    public boolean isWon() {
        return won && !board.isGameOver();
    }

    private void handleBreakVase(String raw) {
        int[] coords = parseCoordinates(raw);
        if (coords != null) {
            breakVase(coords[0], coords[1]);
        }
    }

    private void handleCollectSeed(String raw) {
        int[] coords = parseCoordinates(raw);
        if (coords != null) {
            collectSeedPacket(coords[0], coords[1]);
        }
    }

    private void handlePlant(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int[] coords = parseCoordinates(flags.get("-l"));
        PlantType type = parsePlantType(flags.get("-t"));
        if (coords == null || type == null) {
            return;
        }
        plantHeldSeed(type, coords[0], coords[1]);
    }

    private PlantType parsePlantType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return PlantType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown plant type: " + raw);
            return null;
        }
    }

    private int[] parseCoordinates(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = COORD_PATTERN.matcher(raw);
        return m.find() ? new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))} : null;
    }

    public Board getBoard() {
        return board;
    }
}
