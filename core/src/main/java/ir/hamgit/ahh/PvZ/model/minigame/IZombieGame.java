package ir.hamgit.ahh.PvZ.model.minigame;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class IZombieGame implements MinigameSession {

    private static final int SOLO_STARTING_ZOMBIE_SUN = 150;
    private static final int COUCH_STARTING_PLANT_SUN = 350;
    private static final int COUCH_STARTING_ZOMBIE_SUN = 400;
    public static final int COUCH_DURATION_TICKS = 120 * Board.TICKS_PER_SECOND;
    private static final int COUCH_PLANT_INCOME_INTERVAL_TICKS = 40;
    private static final int COUCH_ZOMBIE_INCOME_INTERVAL_TICKS = 50;
    private static final int COUCH_PLANT_INCOME = 25;
    private static final int COUCH_ZOMBIE_INCOME = 50;
    private static final int PRODUCER_INTERVAL_TICKS = 100;
    private static final int PRODUCER_BASE_AMOUNT = 10;
    private static final int SOLO_RED_LINE_COLUMN = Board.COLUMNS - 2;
    private static final int COUCH_RED_LINE_COLUMN = Board.COLUMNS - 3;
    private static final int FIRST_PLANT_COLUMN = 1;
    private static final int LAST_PLANT_COLUMN = COUCH_RED_LINE_COLUMN;
    private static final ZombieType PRODUCER_TYPE = ZombieType.SUN_PRODUCER;
    private static final int PLACEMENT_COOLDOWN_TICKS = 30;
    private static final List<PlantType> COUCH_PLANTS = List.of(
        PlantType.PEASHOOTER, PlantType.SUNFLOWER, PlantType.WALL_NUT, PlantType.SNOW_PEA);

    private final Board board;
    private final boolean couchPlay;
    private final List<ZombieType> availableZombies;
    private final Map<ZombieType, Integer> costs;
    private final Map<ZombieType, Integer> zombieCooldowns = new EnumMap<>(ZombieType.class);
    private final Map<PlantType, Integer> plantCooldowns = new EnumMap<>(PlantType.class);
    private final List<Zombie> producers = new ArrayList<>();
    private final boolean[] brainAvailable;
    private int sunAmount;
    private int producerTicks;
    private int elapsedTicks;
    private int brainsEaten;
    private boolean won;
    private String couchWinnerRole;

    public IZombieGame(List<ZombieType> availableZombies, Map<ZombieType, Integer> costs) {
        this(availableZombies, costs, false);
    }

     




    public IZombieGame(List<ZombieType> availableZombies, Map<ZombieType, Integer> costs,
                       boolean couchPlay) {
        this.board = new Board(ChapterType.MINIGAME, 1, 3, null, new MinigameLevelHandler());
        this.board.enableBrainMode();
        this.couchPlay = couchPlay;
        this.availableZombies = availableZombies == null ? List.of()
            : availableZombies.stream().filter(type -> type != null && type != PRODUCER_TYPE)
                .distinct().toList();
        this.costs = copyCosts(costs);
        this.brainAvailable = new boolean[board.getRows()];
        Arrays.fill(brainAvailable, true);
        if (couchPlay) {
            sunAmount = COUCH_STARTING_ZOMBIE_SUN;
            board.setSunAmount(COUCH_STARTING_PLANT_SUN);
        } else {
            sunAmount = SOLO_STARTING_ZOMBIE_SUN;
            placeRandomDefenders();
            placeProducers();
        }
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
        int firstColumn = getFirstPlacementColumn();
        boolean outsidePlacementZone = x < firstColumn || x >= board.getColumns()
            || lane < 0 || lane >= board.getRows();
        if (outsidePlacementZone) {
            System.out.println("Place zombies to the right of the red line, at column "
                + firstColumn + " or " + (board.getColumns() - 1) + ".");
            return false;
        }
        if (zombieCooldowns.getOrDefault(type, 0) > 0) {
            System.out.println("That zombie is still recharging.");
            return false;
        }
        int cost = costs.getOrDefault(type, Integer.MAX_VALUE);
        if (cost > sunAmount) {
            System.out.println("Not enough sun.");
            return false;
        }
        sunAmount -= cost;
        board.spawnZombieAt(type, lane, x);
        zombieCooldowns.put(type, PLACEMENT_COOLDOWN_TICKS);
        return true;
    }

     
    public boolean placePlant(PlantType type, int x, int lane) {
        if (!couchPlay || type == null || !COUCH_PLANTS.contains(type)
            || x < FIRST_PLANT_COLUMN || x > LAST_PLANT_COLUMN
            || lane < 0 || lane >= board.getRows()
            || plantCooldowns.getOrDefault(type, 0) > 0) {
            return false;
        }
        PlantDef definition = PlantRegistry.get(type);
        if (definition == null || board.getSunAmount() < definition.getSunCost()
            || !board.plantPlant(type, x, lane)) {
            return false;
        }
        plantCooldowns.put(type, Math.max(1,
            definition.getRechargeSeconds() * Board.TICKS_PER_SECOND));
        return true;
    }

     
    public boolean collectPlantSun(int x, int lane) {
        return couchPlay && (board.collectSun(x, lane) | board.collectFallingSun(x, lane));
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
        tickZombieCooldowns();
        if (couchPlay) {
            tickPlantCooldowns();
            elapsedTicks++;
            tickCouchIncome();
        } else {
            tickProducers();
        }
        board.advanceTime(1);
        detectNewlyEatenBrains();
        checkWinLoss();
    }

    private void tickZombieCooldowns() {
        for (Map.Entry<ZombieType, Integer> entry : new EnumMap<>(zombieCooldowns).entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) zombieCooldowns.remove(entry.getKey());
            else zombieCooldowns.put(entry.getKey(), next);
        }
    }

    private void tickPlantCooldowns() {
        for (Map.Entry<PlantType, Integer> entry : new EnumMap<>(plantCooldowns).entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) plantCooldowns.remove(entry.getKey());
            else plantCooldowns.put(entry.getKey(), next);
        }
    }

    private void tickCouchIncome() {
        if (elapsedTicks % COUCH_PLANT_INCOME_INTERVAL_TICKS == 0) {
            board.addSun(COUCH_PLANT_INCOME);
        }
        if (elapsedTicks % COUCH_ZOMBIE_INCOME_INTERVAL_TICKS == 0) {
            sunAmount = safeAdd(sunAmount, COUCH_ZOMBIE_INCOME);
        }
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
        if (couchPlay) {
            if (brainsEaten >= board.getRows()) {
                couchWinnerRole = "ZOMBIES";
            } else if (elapsedTicks >= COUCH_DURATION_TICKS) {
                couchWinnerRole = "PLANTS";
            }
        } else if (brainsEaten >= board.getRows()) {
            won = true;
        }
    }

    public boolean isLost() {
        if (couchPlay) {
            return "ZOMBIES".equals(couchWinnerRole);
        }
        boolean outOfSun = availableZombies.stream()
            .allMatch(t -> costs.getOrDefault(t, Integer.MAX_VALUE) > sunAmount);
        boolean noZombiesLeft = board.getZombies().stream().noneMatch(Zombie::isAlive);
        return !won && outOfSun && noZombiesLeft;
    }

    public boolean isOver() {
        return couchPlay ? couchWinnerRole != null : won || isLost();
    }

    public boolean isWon() {
        return couchPlay ? "PLANTS".equals(couchWinnerRole) : won;
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

    public int getCooldownTicks(ZombieType type) {
        return zombieCooldowns.getOrDefault(type, 0);
    }

    public int getFirstPlacementColumn() {
        return (couchPlay ? COUCH_RED_LINE_COLUMN : SOLO_RED_LINE_COLUMN) + 1;
    }

    public boolean[] getBrainAvailability() {
        return brainAvailable.clone();
    }

    public boolean isCouchPlay() {
        return couchPlay;
    }

    public List<PlantType> getAvailablePlants() {
        return couchPlay ? COUCH_PLANTS : List.of();
    }

    public int getPlantSunAmount() {
        return couchPlay ? board.getSunAmount() : 0;
    }

    public int getPlantCost(PlantType type) {
        PlantDef definition = PlantRegistry.get(type);
        return definition == null ? Integer.MAX_VALUE : definition.getSunCost();
    }

    public int getPlantCooldownTicks(PlantType type) {
        return plantCooldowns.getOrDefault(type, 0);
    }

    public int getFirstPlantColumn() {
        return FIRST_PLANT_COLUMN;
    }

    public int getLastPlantColumn() {
        return LAST_PLANT_COLUMN;
    }

    public int getRemainingSeconds() {
        if (!couchPlay) return 0;
        int ticksLeft = Math.max(0, COUCH_DURATION_TICKS - elapsedTicks);
        return (ticksLeft + Board.TICKS_PER_SECOND - 1) / Board.TICKS_PER_SECOND;
    }

    public String getCouchWinnerRole() {
        return couchWinnerRole;
    }

    public String getCouchResultText() {
        if ("PLANTS".equals(couchWinnerRole)) {
            return "Player 1 (Plants) survived for two minutes.";
        }
        if ("ZOMBIES".equals(couchWinnerRole)) {
            return "Player 2 (Zombies) ate all five brains.";
        }
        return "Couch Play is still in progress.";
    }
}
