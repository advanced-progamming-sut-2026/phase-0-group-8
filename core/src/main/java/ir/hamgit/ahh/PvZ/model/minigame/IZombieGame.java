package ir.hamgit.ahh.PvZ.model.minigame;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * "من زامبی" (I, Zombie). Player controls zombies instead of plants against
 * a handful of pre-placed defending plants.
 *
 * <p>Reuses {@link Board}/{@link Zombie} entirely unmodified: a "brain" at the
 * left edge of a row is mechanically identical to a lawn mower (first zombie
 * to reach it "eats" it), so instead of reimplementing that, this class just
 * watches {@link Board#getLawnMowerAvailability()} flip from available to
 * used and counts that as a brain eaten. The one pre-existing zombie per row
 * that generates the player's zombie-currency is a repurposed Bucketheaded
 * zombie (matches the spec's "same HP as a bucket zombie" note) - real
 * level content (which 5 zombies + costs are offered, exact defender
 * layout) is level-design data the team can pass in or hardcode later.</p>
 */
public class IZombieGame implements MinigameSession {

    private static final int STARTING_SUN = 150;
    private static final int PRODUCER_INTERVAL_TICKS = 100;
    private static final int PRODUCER_BASE_AMOUNT = 10;
    private static final ZombieType PRODUCER_TYPE = ZombieType.BUCKETHEAD;

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
        this.board = new Board(ChapterType.MINIGAME, 1, 3, null, new MinigameLevelHandler());
        this.board.enableBrainMode();
        this.availableZombies = availableZombies;
        this.costs = costs;
        this.brainAvailable = new boolean[board.getRows()];
        Arrays.fill(brainAvailable, true);
        placeRandomDefenders();
        placeProducers();
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
        if (type == PRODUCER_TYPE || !availableZombies.contains(type)) {
            System.out.println("That zombie isn't available to you.");
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
        for (int i = 0; i < ticks; i++) {
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
                sunAmount += amount;
            }
        }
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
}
