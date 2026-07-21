package ir.hamgit.ahh.PvZ.model.minigame;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.special.BeghouledLevelHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class BeghouledGame implements MinigameSession {

    private static final List<PlantType> PALETTE = List.of(
        PlantType.PEASHOOTER, PlantType.WALL_NUT, PlantType.PUFF_SHROOM,
        PlantType.CABBAGE_PULT, PlantType.SUNFLOWER);
    private static final int BASE_MATCH_REWARD = 50;
    private static final int CASCADE_BONUS = 50;
    private static final int TOTAL_WAVES = 9999;
    private static final int MIN_MATCH_LENGTH = 3;

    private final Board board;
    private final PlantType[][] grid;
    private final boolean[][] crater;
    private final Map<PlantType, PlantType> upgrades = buildUpgradeTable();
    private final Map<PlantType, Integer> upgradeCost = buildUpgradeCosts();
    private int sunAmount;
    private int matchesMade;
    private final int targetMatches;
    private boolean won;

    public BeghouledGame(int targetMatches, int difficulty) {
        this.targetMatches = targetMatches;
        BeghouledLevelHandler handler = new BeghouledLevelHandler();
        this.board = new Board(ChapterType.MINIGAME, TOTAL_WAVES, difficulty, null, handler);
        handler.setGame(this);
        this.grid = new PlantType[board.getRows()][board.getColumns()];
        this.crater = new boolean[board.getRows()][board.getColumns()];
        fillEntireBoardRandomly();
    }

    private static Map<PlantType, PlantType> buildUpgradeTable() {
        Map<PlantType, PlantType> map = new EnumMap<>(PlantType.class);
        map.put(PlantType.PEASHOOTER, PlantType.REPEATER);
        map.put(PlantType.REPEATER, PlantType.MEGA_GATLING_PEA);
        map.put(PlantType.WALL_NUT, PlantType.TALL_NUT);
        map.put(PlantType.PUFF_SHROOM, PlantType.FUME_SHROOM);
        map.put(PlantType.CABBAGE_PULT, PlantType.MELON_PULT);
        map.put(PlantType.MELON_PULT, PlantType.WINTER_MELON);
        return map;
    }

    private static Map<PlantType, Integer> buildUpgradeCosts() {
        Map<PlantType, Integer> map = new EnumMap<>(PlantType.class);
        map.put(PlantType.PEASHOOTER, 500);
        map.put(PlantType.REPEATER, 1500);
        map.put(PlantType.WALL_NUT, 500);
        map.put(PlantType.PUFF_SHROOM, 250);
        map.put(PlantType.CABBAGE_PULT, 1000);
        map.put(PlantType.MELON_PULT, 750);
        return map;
    }

    // ------------------------------------------------------------------
    // Board setup
    // ------------------------------------------------------------------

    private void fillEntireBoardRandomly() {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getColumns(); c++) {
                if (!crater[r][c]) {
                    placeRandomPlantAt(r, c);
                }
            }
        }
    }

    private void placeRandomPlantAt(int row, int col) {
        board.pluckPlant(col, row);
        PlantType type = PALETTE.get((int) (Math.random() * PALETTE.size()));
        board.plantForFree(type, col, row);
        grid[row][col] = type;
    }

    // ------------------------------------------------------------------
    // Swapping / matching
    // ------------------------------------------------------------------

    public boolean swapPlants(int x1, int y1, int x2, int y2) {
        if (!isValidSwapTarget(x1, y1, x2, y2)) {
            System.out.println("Can't swap those tiles.");
            return false;
        }
        swapGridEntries(x1, y1, x2, y2);
        if (findMatches().isEmpty()) {
            swapGridEntries(x1, y1, x2, y2);
            System.out.println("That swap doesn't make a match.");
            return false;
        }
        resolveCascadesUntilStable();
        return true;
    }

    private boolean isValidSwapTarget(int x1, int y1, int x2, int y2) {
        boolean adjacent = Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
        return adjacent && inBounds(x1, y1) && inBounds(x2, y2) && !crater[y1][x1] && !crater[y2][x2];
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < board.getColumns() && y >= 0 && y < board.getRows();
    }

    private void swapGridEntries(int x1, int y1, int x2, int y2) {
        PlantType tmp = grid[y1][x1];
        placeAt(y1, x1, grid[y2][x2]);
        placeAt(y2, x2, tmp);
    }

    private void placeAt(int row, int col, PlantType type) {
        board.pluckPlant(col, row);
        if (type != null) {
            board.plantForFree(type, col, row);
        }
        grid[row][col] = type;
    }

    private void resolveCascadesUntilStable() {
        boolean cascade = false;
        List<List<int[]>> matches = findMatches();
        while (!matches.isEmpty()) {
            rewardMatches(matches, cascade);
            removeMatches(matches);
            dropAndRefill();
            cascade = true;
            matches = findMatches();
        }
        if (matchesMade >= targetMatches) {
            triggerWin();
        } else if (hasNoPossibleMove()) {
            System.out.println("No moves left - the board resets!");
            fillEntireBoardRandomly();
        }
    }

    private void rewardMatches(List<List<int[]>> matches, boolean cascade) {
        for (List<int[]> group : matches) {
            int reward = BASE_MATCH_REWARD * (group.size() - MIN_MATCH_LENGTH + 1) + (cascade ? CASCADE_BONUS : 0);
            sunAmount += reward;
            matchesMade++;
        }
    }

    private void removeMatches(List<List<int[]>> matches) {
        for (List<int[]> group : matches) {
            for (int[] cell : group) {
                placeAt(cell[0], cell[1], null);
            }
        }
    }

    private List<List<int[]>> findMatches() {
        List<List<int[]>> matches = new ArrayList<>();
        findRowMatches(matches);
        findColumnMatches(matches);
        return matches;
    }

    private void findRowMatches(List<List<int[]>> matches) {
        for (int r = 0; r < board.getRows(); r++) {
            int runStart = 0;
            for (int c = 1; c <= board.getColumns(); c++) {
                if (c == board.getColumns() || !sameType(grid[r][c], grid[r][runStart])) {
                    addRunIfLongEnough(matches, r, runStart, c - 1, true);
                    runStart = c;
                }
            }
        }
    }

    private void findColumnMatches(List<List<int[]>> matches) {
        for (int c = 0; c < board.getColumns(); c++) {
            int runStart = 0;
            for (int r = 1; r <= board.getRows(); r++) {
                if (r == board.getRows() || !sameType(grid[r][c], grid[runStart][c])) {
                    addRunIfLongEnough(matches, c, runStart, r - 1, false);
                    runStart = r;
                }
            }
        }
    }

    private boolean sameType(PlantType a, PlantType b) {
        return a != null && a == b;
    }

    private void addRunIfLongEnough(List<List<int[]>> matches, int fixed, int start, int end, boolean isRow) {
        if (end - start + 1 < MIN_MATCH_LENGTH) {
            return;
        }
        List<int[]> group = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            group.add(isRow ? new int[] {fixed, i} : new int[] {i, fixed});
        }
        matches.add(group);
    }

    // ------------------------------------------------------------------
    // Gravity / refill / crater tracking
    // ------------------------------------------------------------------

    private void dropAndRefill() {
        for (int c = 0; c < board.getColumns(); c++) {
            dropColumn(c);
        }
    }

    private void dropColumn(int col) {
        int writeRow = board.getRows() - 1;
        for (int r = board.getRows() - 1; r >= 0; r--) {
            if (crater[r][col]) {
                continue;
            }
            if (grid[r][col] != null) {
                moveIfNeeded(r, col, writeRow);
                writeRow--;
            }
        }
        fillAboveWithNewPlants(writeRow, col);
    }

    private void moveIfNeeded(int fromRow, int col, int toRow) {
        if (fromRow == toRow) {
            return;
        }
        PlantType type = grid[fromRow][col];
        placeAt(fromRow, col, null);
        placeAt(toRow, col, type);
    }

    private void fillAboveWithNewPlants(int topWriteRow, int col) {
        for (int r = topWriteRow; r >= 0; r--) {
            if (!crater[r][col]) {
                placeRandomPlantAt(r, col);
            }
        }
    }

    private boolean hasNoPossibleMove() {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getColumns(); c++) {
                boolean rightMatch = c + 1 < board.getColumns()
                    && swapWouldMatch(r, c, r, c + 1);
                boolean downMatch = r + 1 < board.getRows()
                    && swapWouldMatch(r, c, r + 1, c);
                if (rightMatch || downMatch) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean swapWouldMatch(int y1, int x1, int y2, int x2) {
        if (y1 == y2 && x1 == x2 || crater[y1][x1] || crater[y2][x2]) {
            return false;
        }
        swapGridEntries(x1, y1, x2, y2);
        boolean matched = !findMatches().isEmpty();
        swapGridEntries(x1, y1, x2, y2);
        return matched;
    }

    /** Called by {@link BeghouledLevelHandler} whenever a zombie eats a plant on this grid. */
    public void onPlantEatenByZombie(int x, int lane) {
        crater[lane][x] = true;
        grid[lane][x] = null;
    }

    // ------------------------------------------------------------------
    // Upgrades / win / commands
    // ------------------------------------------------------------------

    public boolean upgrade(PlantType from, PlantType to) {
        Integer cost = upgradeCost.get(from);
        if (cost == null || to == null || to != upgrades.get(from) || cost > sunAmount) {
            System.out.println("Can't upgrade that.");
            return false;
        }
        sunAmount -= cost;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getColumns(); c++) {
                if (grid[r][c] == from) {
                    placeAt(r, c, to);
                }
            }
        }
        return true;
    }

    private void triggerWin() {
        won = true;
        board.cheatReleaseNuke();
    }

    public void tick(int ticks) {
        if (ticks > 0) {
            board.advanceTime(ticks);
        }
    }

    public boolean isOver() {
        return won || board.isGameOver();
    }

    public boolean isWon() {
        return won;
    }

    public Board getBoard() {
        return board;
    }

}
