package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.def.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

/**
 * Planting rules (tile eligibility, stacking, water/lily-pad handling),
 * plucking, and the plant-food inventory. Split out of {@link model.Board} purely
 * to keep Board under the project's class-length Checkstyle/PMD guideline;
 * reaches board state through Board's own public API plus a couple of
 * narrow package-private mutators ({@code Board.spendSun},
 * {@code Board.incrementPlantFoodCount}, {@code Board.consumePlantFoodIfAvailable})
 * added specifically so this class doesn't need direct field access.
 */
class PlantingOps {

    boolean plantPlant(model.Board board, PlantType type, int x, int lane) {
        PlantDef def = PlantRegistry.get(type);
        if (def == null || !board.isInBounds(x, lane) || board.getSunAmount() < def.getSunCost()) {
            return false;
        }
        if (!place(board, def, x, lane)) {
            return false;
        }
        board.spendSun(def.getSunCost());
        return true;
    }

    boolean plantForFree(model.Board board, PlantType type, int x, int lane) {
        PlantDef def = PlantRegistry.get(type);
        return def != null && board.isInBounds(x, lane) && place(board, def, x, lane);
    }

    private boolean place(model.Board board, PlantDef def, int x, int lane) {
        Tile tile = board.getTileAt(x, lane);
        if (tile == null || !canPlaceOn(tile, def)) {
            return false;
        }
        return tile.plantHere(new Plant(def, x, lane));
    }

    private boolean canPlaceOn(Tile tile, PlantDef def) {
        if (tile.getType() == TileType.WATER) {
            boolean somethingFloatingHere = !tile.isEmpty() && tile.getPlant().getDef().isCanPlantOnWater();
            return def.isCanPlantOnWater() || somethingFloatingHere;
        }
        if (!tile.isPlantable()) {
            return false;
        }
        if (tile.isEmpty()) {
            return true;
        }
        return def.isCanStackOn() || tile.getPlant().getDef().isCanStackOn();
    }

    boolean pluckPlant(model.Board board, int x, int lane) {
        Tile tile = board.getTileAt(x, lane);
        if (tile == null || tile.isEmpty()) {
            return false;
        }
        tile.removePlant();
        return true;
    }

    boolean feedPlant(model.Board board, int x, int lane) {
        Tile tile = board.getTileAt(x, lane);
        if (tile == null || tile.isEmpty() || !board.consumePlantFoodIfAvailable()) {
            return false;
        }
        tile.getPlant().applyPlantFood(board);
        return true;
    }

    void grantPlantFood(model.Board board) {
        board.incrementPlantFoodCount();
        System.out.printf("The glowing zombie dropeed a plant food; you have %d plant foods now.%n",
            board.getPlantFoodCount());
    }

    void cheatAddPlantFood(model.Board board) {
        board.incrementPlantFoodCount();
    }
}
