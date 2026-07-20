package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;


import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

class PlantingOps {

    private PlantType lastPlantType;

    boolean plantPlant(Board board, PlantType type, int x, int lane) {
        PlantDef def = PlantRegistry.get(type);
        return def != null && plantPlant(board, type, x, lane, def.getSunCost(), 1);
    }

    boolean plantPlant(Board board, PlantType type, int x, int lane, int adjustedCost, int level) {
        PlantDef requestedDef = PlantRegistry.get(type);
        PlantDef placedDef = resolveDefinition(type);
        if (requestedDef == null || placedDef == null || !board.isInBounds(x, lane)
            || board.getSunAmount() < adjustedCost) {
            return false;
        }
        if (!place(board, placedDef, x, lane, level)) {
            return false;
        }
        board.spendSun(adjustedCost);
        rememberPlant(type, placedDef);
        return true;
    }

    boolean plantForFree(Board board, PlantType type, int x, int lane) {
        PlantDef def = resolveDefinition(type);
        boolean planted = def != null && board.isInBounds(x, lane) && place(board, def, x, lane, 1);
        if (planted) {
            rememberPlant(type, def);
        }
        return planted;
    }

    private PlantDef resolveDefinition(PlantType type) {
        PlantDef requested = PlantRegistry.get(type);
        if (requested == null || !requested.hasBehavior(BehaviorType.COPY_PLANT)) {
            return requested;
        }
        return lastPlantType == null ? null : PlantRegistry.get(lastPlantType);
    }

    private void rememberPlant(PlantType requestedType, PlantDef placedDef) {
        if (requestedType != PlantType.IMITATER) {
            lastPlantType = placedDef.getType();
        }
    }

    private boolean place(Board board, PlantDef def, int x, int lane, int level) {
        Tile tile = board.getTileAt(x, lane);
        if (isTerrainOnly(def)) {
            return handleTerrainAbility(board, tile, def, x, lane, level);
        }
        if (tile == null || !canPlaceOn(tile, def)) {
            return false;
        }
        if (canAddStackedHead(tile, def)) {
            return tile.getPlant().addStackedHead();
        }
        return tile.plantHere(new Plant(def, x, lane));
    }

    private boolean isTerrainOnly(PlantDef def) {
        return def.hasBehavior(BehaviorType.REMOVE_GRAVE)
            || (def.hasBehavior(BehaviorType.MELT_ICE)
            && !def.hasBehavior(BehaviorType.LANE_EXPLOSION));
    }

    private boolean handleTerrainAbility(Board board, Tile tile, PlantDef def,
                                         int x, int lane, int level) {
        if (tile == null) {
            return false;
        }
        if (def.hasBehavior(BehaviorType.REMOVE_GRAVE) && tile.getType() == TileType.GRAVE) {
            tile.setType(TileType.NORMAL);
            explodeTerrainAbility(board, def, x, lane, level);
            return true;
        }
        if (def.hasBehavior(BehaviorType.MELT_ICE) && isIceTile(tile)) {
            meltIce(board, x, lane, level >= 3 ? 1 : 0);
            explodeTerrainAbility(board, def, x, lane, level);
            return true;
        }
        return false;
    }

    private void meltIce(Board board, int centerX, int centerLane, int radius) {
        for (int lane = Math.max(0, centerLane - radius);
             lane <= Math.min(board.getRows() - 1, centerLane + radius); lane++) {
            for (int x = Math.max(0, centerX - radius);
                 x <= Math.min(board.getColumns() - 1, centerX + radius); x++) {
                Tile target = board.getTileAt(x, lane);
                if (isIceTile(target)) {
                    target.setType(TileType.NORMAL);
                }
                if (target.getPlant() != null) {
                    target.getPlant().meltIce();
                }
            }
        }
    }

    private void explodeTerrainAbility(Board board, PlantDef def, int x, int lane, int level) {
        if (level >= 4) {
            board.dealAreaDamageToZombies(x, lane, 1, Math.max(600, def.getDamage()));
        }
    }

    private boolean isIceTile(Tile tile) {
        return tile.getType() == TileType.ICY_GROUND || tile.getType() == TileType.SLIPPERY_UP
            || tile.getType() == TileType.SLIPPERY_DOWN;
    }

    private boolean canAddStackedHead(Tile tile, PlantDef def) {
        return !tile.isEmpty() && def.hasBehavior(BehaviorType.STACKABLE_HEADS)
            && tile.getPlant().getDef().getType() == def.getType();
    }

    private boolean canPlaceOn(Tile tile, PlantDef def) {
        if (tile.getType() == TileType.WATER) {
            boolean platform = !tile.isEmpty()
                && tile.getPlant().getDef().hasBehavior(BehaviorType.WATER_PLATFORM);
            return def.isCanPlantOnWater() || platform;
        }
        if (!tile.isPlantable()) {
            return false;
        }
        if (tile.isEmpty()) {
            return true;
        }
        return def.isCanStackOn() || def.hasBehavior(BehaviorType.PROTECTIVE_SHELL)
            || tile.getPlant().getDef().isCanStackOn()
            || tile.getPlant().getDef().hasBehavior(BehaviorType.WATER_PLATFORM);
    }

    boolean pluckPlant(Board board, int x, int lane) {
        Tile tile = board.getTileAt(x, lane);
        if (tile == null || tile.isEmpty()) {
            return false;
        }
        if (!board.getSpecialLevelHandler().canPluckPlant(tile.getPlant())) {
            return false;
        }
        tile.removePlant();
        return true;
    }

    boolean feedPlant(Board board, int x, int lane) {
        Tile tile = board.getTileAt(x, lane);
        if (tile == null || tile.isEmpty() || !board.consumePlantFoodIfAvailable()) {
            return false;
        }
        tile.getPlant().applyPlantFood(board);
        return true;
    }

    void grantPlantFood(Board board) {
        board.incrementPlantFoodCount();
        System.out.printf("The glowing zombie dropeed a plant food; you have %d plant foods now.%n",
            board.getPlantFoodCount());
    }

    void cheatAddPlantFood(Board board) {
        board.incrementPlantFoodCount();
    }
}
