package ir.hamgit.ahh.PvZ.model.minigame;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class IZombieGame implements MinigameSession {

    private static final int STARTING_SUN = 150;
    private static final int PRODUCER_INTERVAL_TICKS = 100;
    private static final int PRODUCER_BASE_AMOUNT = 10;
    private static final int RED_LINE_COLUMN = Board.COLUMNS - 2;
    private static final ZombieType PRODUCER_TYPE = ZombieType.SUN_PRODUCER;

    private final Board board;
    private final List<ZombieType> availableZombies;
    private final Map<ZombieType, Integer> costs;
    private final List<Zombie> producers = new ArrayList<>();
    private final boolean[] brainAvailable;
    private int sunAmount = STARTING_SUN;
    private int producerTicks;
    private int brainsEaten;
    private boolean won;

    public IZombieGame(List<ZombieType> availableZombies, Map<ZombieType, Integer> costs) {
        this.board = new Board(ChapterType.MINIGAME, 1, 3, null,
            new MinigameLevelHandler());
        this.board.enableBrainMode();
        this.availableZombies = availableZombies == null ? List.of()
            : availableZombies.stream().filter(type -> type != null && type != PRODUCER_TYPE)
            .distinct().toList();
        this.costs = copyCosts(costs);
        this.brainAvailable = new boolean[board.getRows()];
        Arrays.fill(brainAvailable, true);
        placeRandomDefenders();
        placeProducers();
    }

    private Map<ZombieType, Integer> copyCosts(Map<ZombieType, Integer> source) {
        Map<ZombieType, Integer> result = new EnumMap<>(ZombieType.class);
        if (source != null) {
            source.forEach((type, cost) -> {
                if (type != null && type != PRODUCER_TYPE && cost != null) {
                    result.put(type, Math.max(0, cost));
                }
            });
        }
        return Map.copyOf(result);
    }

    private void placeRandomDefenders() {
        List<PlantType> pool = List.of(PlantType.WALL_NUT, PlantType.PEASHOOTER,
            PlantType.SNOW_PEA, PlantType.TALL_NUT);
        for (int r = 0; r < board.getRows(); r++) {
            placeDefendersInRow(r, pool);
        }
    }

    private void placeDefendersInRow(int row, List<PlantType> pool) {
        int defenderCount = 2 + (int) (Math.random() * 2);
        for (int i = 0; i < defenderCount; i++) {
            PlantType type = pool.get((int) (Math.random() * pool.size()));
            int col = 1 + (int) (Math.random() * (board.getColumns() - 2));
            board.plantForFree(type, col, row);
        }
    }

    private void placeProducers() {
        for (int r = 0; r < board.getRows(); r++) {
            board.spawnZombieAt(PRODUCER_TYPE, r, board.getColumns() - 1);
            producers.add(board.getZombies().get(board.getZombies().size() - 1));
            producers.get(producers.size() - 1).setStationary(true);
        }
    }

    public boolean placeZombie(ZombieType type, int x, int lane) {
        if (type == null || !availableZombies.contains(type)) {
            System.out.println("That zombie isn't available to you.");
            return false;
        }
        boolean outsidePlacementZone = x <= RED_LINE_COLUMN || x > board.getColumns()
            || lane < 0 || lane >= board.getRows();
        if (outsidePlacementZone) {
            System.out.println("Place zombies to the right of the red line, at column "
                + (RED_LINE_COLUMN + 1) + " or " + board.getColumns() + ".");
            return false;
        }
        int cost = costs.getOrDefault(type, Integer.MAX_VALUE);
        if (cost > sunAmount) {
            System.out.println("Not enough sun.");
            return false;
        }
        sunAmount -= cost;
        board.spawnZombieAt(type, lane, x);
        return true;
    }

    public void tick(int ticks) {
        if (ticks <= 0) {
            return;
        }
        for (int i = 0; i < ticks && !isOver(); i++) {
            advanceOneTick();
        }
    }

    private void advanceOneTick() {
        tickProducers();
        board.advanceTime(1);
        detectNewlyEatenBrains();
        checkWinLoss();
    }

    private void tickProducers() {
        producerTicks++;
        if (producerTicks < PRODUCER_INTERVAL_TICKS) {
            return;
        }
        producerTicks = 0;
        int amount = PRODUCER_BASE_AMOUNT + board.getTickCount() / (PRODUCER_INTERVAL_TICKS * 5);
        for (Zombie producer : producers) {
            if (producer.isAlive()) {
                sunAmount = safeAdd(sunAmount, amount);
            }
        }
    }

    private int safeAdd(int current, int amount) {
        return (int) Math.min(Integer.MAX_VALUE, (long) current + Math.max(0, amount));
    }

    private void detectNewlyEatenBrains() {
        boolean[] current = board.getLawnMowerAvailability();
        for (int r = 0; r < current.length; r++) {
            if (brainAvailable[r] && !current[r]) {
                brainAvailable[r] = false;
                brainsEaten++;
                System.out.println("Brain in row " + r + " eaten! (" + brainsEaten + "/" + current.length + ")");
            }
        }
    }

    private void checkWinLoss() {
        if (brainsEaten >= board.getRows()) {
            won = true;
        }
    }

    public boolean isLost() {
        boolean outOfSun = availableZombies.stream()
            .allMatch(t -> costs.getOrDefault(t, Integer.MAX_VALUE) > sunAmount);
        boolean noZombiesLeft = board.getZombies().stream().noneMatch(Zombie::isAlive);
        return !won && outOfSun && noZombiesLeft;
    }

    public boolean isOver() {
        return won || isLost();
    }

    public boolean isWon() {
        return won;
    }

    public Board getBoard() {
        return board;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public List<ZombieType> getAvailableZombies() {
        return List.copyOf(availableZombies);
    }

    public int getCost(ZombieType type) {
        return costs.getOrDefault(type, Integer.MAX_VALUE);
    }

    public int getFirstPlacementColumn() {
        return RED_LINE_COLUMN + 1;
    }

    public boolean[] getBrainAvailability() {
        return brainAvailable.clone();
    }
}
