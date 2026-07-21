package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

final class ChapterMechanics {

    private static final int FIRE_MELT_DAMAGE_PER_TICK = 6;
    private final ChapterType chapter;
    private int waterStart = 6;

    ChapterMechanics(ChapterType chapter) {
        this.chapter = chapter;
    }

    void tick(Board board) {
        if (chapter == ChapterType.FROSTBITE_CAVES) {
            meltIceNearFirePlants(board);
        }
    }

    void onWaveStart(Board board, int waveNumber) {
        switch (chapter) {
            case FROSTBITE_CAVES -> blowFreezingWind(board);
            case BIG_WAVE_BEACH -> changeTide(board);
            case DARK_AGES -> runDarkAgesWave(board);
            default -> { }
        }
    }

    int zombieSpawnColumn(Board board, boolean finalWave) {
        if (chapter == ChapterType.ANCIENT_EGYPT && finalWave) {
            return board.getColumns() - 1 - (int) (Math.random() * 4);
        }
        if (chapter == ChapterType.BIG_WAVE_BEACH && Math.random() < 0.25) {
            return Math.max(1, waterStart + (int) (Math.random() * (board.getColumns() - waterStart)));
        }
        return board.getColumns();
    }

    boolean blocksNaturalSun() {
        return chapter == ChapterType.DARK_AGES;
    }

    void onGraveDestroyed(Board board, Tile tile) {
        int reward = tile.takeGraveReward();
        if (reward == Tile.GRAVE_REWARD_SUN) {
            board.addSun(50);
            System.out.println("The grave contained 50 sun.");
        } else if (reward == Tile.GRAVE_REWARD_PLANT_FOOD) {
            board.incrementPlantFoodCount();
            System.out.println("The grave contained Plant Food.");
        }
    }

    private void meltIceNearFirePlants(Board board) {
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                Plant plant = board.getTileAt(x, lane).getPlant();
                if (plant != null && plant.isFrozen() && hasAdjacentFirePlant(board, x, lane)) {
                    plant.damageIce(FIRE_MELT_DAMAGE_PER_TICK);
                }
            }
        }
    }

    private boolean hasAdjacentFirePlant(Board board, int x, int lane) {
        for (int row = Math.max(0, lane - 1); row <= Math.min(board.getRows() - 1, lane + 1); row++) {
            for (int col = Math.max(0, x - 1); col <= Math.min(board.getColumns() - 1, x + 1); col++) {
                Plant neighbor = board.getTileAt(col, row).getPlant();
                if (neighbor != null && neighbor.getDef().hasTag(Tag.FIRE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void blowFreezingWind(Board board) {
        System.out.println("A freezing wind blows across the lawn.");
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                Plant plant = board.getTileAt(x, lane).getPlant();
                if (plant != null && Math.random() < 0.55) {
                    plant.applyIceLayer();
                }
            }
        }
    }

    private void changeTide(Board board) {
        waterStart = 4 + (int) (Math.random() * 4);
        System.out.println("The tide changed; water now begins at column " + waterStart + ".");
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                Tile tile = board.getTileAt(x, lane);
                tile.setType(x >= waterStart ? TileType.WATER : TileType.NORMAL);
                drownUnsupportedPlants(board, tile);
            }
        }
    }

    private void drownUnsupportedPlants(Board board, Tile tile) {
        if (tile.getType() != TileType.WATER || tile.isEmpty()) {
            return;
        }
        boolean supported = tile.getPlantLayers().stream().anyMatch(plant ->
            plant.getDef().isCanPlantOnWater()
                || plant.getDef().hasBehavior(BehaviorType.WATER_PLATFORM));
        if (!supported) {
            for (Plant plant : tile.getPlantLayers()) {
                board.destroyPlantInstantly(plant);
            }
        }
    }

    private void runDarkAgesWave(Board board) {
        board.spawnRandomGraves(1 + (int) (Math.random() * 3));
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                Tile tile = board.getTileAt(x, lane);
                boolean safeSpawn = x >= ChapterTerrainFactory.NECROMANCY_MIN_COLUMN;
                if (safeSpawn && tile.hasNecromancy() && Math.random() < 0.35) {
                    board.spawnZombieAt(ZombieType.NORMAL, lane, x);
                    System.out.println("Necromancy raised a zombie at (" + x + ", " + lane + ").");
                }
            }
        }
    }
}
