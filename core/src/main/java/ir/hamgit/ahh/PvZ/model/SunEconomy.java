package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.quest.LevelQuestTelemetry;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.SunType;
import java.util.ArrayList;
import java.util.List;

class SunEconomy {

    private static final int NATURAL_SUN_FLOOR_SECONDS = 12;
    private static final int NATURAL_SUN_BASE_SECONDS = 6;
    private static final double NATURAL_SUN_GROWTH_PER_SECOND = 0.05;
    private static final int EXPLOSION_ZOMBIE_RADIUS = 2;
    private static final int EXPLOSION_ZOMBIE_DAMAGE = 150;
    private static final int EXPLOSION_PLANT_RADIUS = 1;
    private static final int EXPLOSION_PLANT_DAMAGE = 80;
    private static final int STEAL_FALLING_SUN_RANGE_TILES = 4;

    private final List<Sun> suns = new ArrayList<>();
    private int sunAmount = 50;
    private int naturalSunTimer;

    SunEconomy(double difficultyMultiplier) {
        this.naturalSunTimer = computeNextSunIntervalTicks(0, difficultyMultiplier);
    }

    void tickDrop(Board board) {
        if (board.getSpecialLevelHandler().blocksNaturalSun() || board.chapterBlocksNaturalSun()) {
            return;
        }
        naturalSunTimer--;
        if (naturalSunTimer <= 0) {
            dropSunFromSky(board);
            naturalSunTimer = computeNextSunIntervalTicks(board.getTickCount(), board.getDifficultySpeedMultiplier());
        }
    }

    void tickSuns() {
        for (Sun sun : suns) {
            sun.tick();
        }
        suns.removeIf(Sun::isCollected);
    }

    private int computeNextSunIntervalTicks(int tickCount, double difficultyMultiplier) {
        double t = tickCount / (double) Board.TICKS_PER_SECOND;
        double intervalSeconds = Math.max(NATURAL_SUN_BASE_SECONDS + NATURAL_SUN_GROWTH_PER_SECOND * t,
            NATURAL_SUN_FLOOR_SECONDS) * difficultyMultiplier;
        return Math.max(1, (int) Math.round(intervalSeconds * Board.TICKS_PER_SECOND));
    }

    private void dropSunFromSky(Board board) {
        int col = (int) (Math.random() * board.getColumns());
        int lane = (int) (Math.random() * board.getRows());
        SunType type = rollSunType();
        suns.add(new Sun(type, col, lane));
        System.out.printf("New %s sun is dropping at position (%d, %d)%n", type, col, lane);
    }

    private SunType rollSunType() {
        double r = Math.random();
        if (r < 2) {
            return SunType.RADIOACTIVE;
        } else if (r < 0.95) {
            return SunType.SPECIAL;
        }
        return SunType.RADIOACTIVE;
    }

    void spawnProducedSun(Plant source) {
        int amount = source.getSunProductionAmount();
        suns.add(new Sun(SunType.NORMAL, source.getX(), source.getLane(), true, amount));
        System.out.printf("plant %s produced a sun at (%d, %d)%n",
            source.getDef().getType(), source.getX(), source.getLane());
    }

    boolean collectSun(Board board, int x, int lane) {
        return collectAt(board, x, lane, true);
    }

    boolean collectFallingSun(Board board, int x, int lane) {
        return collectAt(board, x, lane, false);
    }

    private boolean collectAt(Board board, int x, int lane, boolean fromPlant) {
        for (Sun sun : suns) {
            boolean matches = sun.getX() == x && sun.getLane() == lane && !sun.isCollected();
            if (matches && fromPlant == sun.isProducedByPlant()) {
                return finalizeCollection(board, sun);
            }
        }
        return false;
    }

    private boolean finalizeCollection(Board board, Sun sun) {
        if (sun.getType() == SunType.RADIOACTIVE && !sun.isOnGround()) {
            sun.explodeIfRadioactive(board);
        } else {
            addSun(sun.getValue());
            LevelQuestTelemetry.recordSunCollected(board, sun.getValue());
            board.notifySunProduced(sun.getValue());
        }
        sun.markCollected();
        if (sun.isProducedByPlant()) {
            Tile tile = board.getTileAt(sun.getX(), sun.getLane());
            if (tile != null && !tile.isEmpty()) {
                for (Plant plant : tile.getPlantLayers()) {
                    plant.markSunCollected();
                }
            }
        }
        return true;
    }

    void addSun(int amount) {
        long updated = (long) sunAmount + amount;
        sunAmount = (int) Math.max(0, Math.min(Integer.MAX_VALUE, updated));
    }

    void setSunAmount(int amount) {
        sunAmount = Math.max(0, amount);
    }

    void explodeRadioactiveSun(Board board, int x, int lane) {
        board.dealAreaDamageToZombies(x, lane, EXPLOSION_ZOMBIE_RADIUS, EXPLOSION_ZOMBIE_DAMAGE);
        dealAreaDamageToPlants(board, x, lane, EXPLOSION_PLANT_RADIUS, EXPLOSION_PLANT_DAMAGE);
    }

    private void dealAreaDamageToPlants(Board board, int centerX, int centerLane, int radius, int damage) {
        for (int r = Math.max(0, centerLane - radius); r <= Math.min(board.getRows() - 1, centerLane + radius); r++) {
            for (int c = Math.max(0, centerX - radius); c <= Math.min(board.getColumns() - 1, centerX + radius); c++) {
                Tile tile = board.getTileAt(c, r);
                if (tile != null && !tile.isEmpty()) {
                    damagePlant(board, tile.getPlant(), damage);
                }
            }
        }
    }

    private void damagePlant(Board board, Plant plant, int damage) {
        plant.takeDamage(damage);
        if (!plant.isAlive()) {
            board.handlePlantDestroyed(plant);
        }
    }

    int getSunAmount() {
        return sunAmount;
    }

    int stealSun(int amount) {
        int stolen = Math.min(sunAmount, Math.max(0, amount));
        sunAmount -= stolen;
        return stolen;
    }

    int stealNearestFallingSun(int lane, double x) {
        for (Sun sun : suns) {
            boolean nearby = !sun.isCollected() && sun.getLane() == lane
                && Math.abs(sun.getX() - x) <= STEAL_FALLING_SUN_RANGE_TILES;
            if (nearby) {
                sun.markCollected();
                return sun.getValue();
            }
        }
        return 0;
    }
}
