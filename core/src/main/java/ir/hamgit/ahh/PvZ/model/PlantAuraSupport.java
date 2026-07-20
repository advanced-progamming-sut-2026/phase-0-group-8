package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.Tag;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

final class PlantAuraSupport {

    private static final int ATTRACTION_RANGE = 2;
    private final PlantDef def;

    PlantAuraSupport(PlantDef def) {
        this.def = def;
    }

    void boostFamily(Plant mint, Board board) {
        PlantFamily family = def.getFamily();
        int duration = PlantLevelRuntime.mintDuration(mint, 100);
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                for (Plant target : board.getTileAt(x, lane).getPlantLayers()) {
                    if (target != mint && target.getDef().getFamily() == family) {
                        target.applyPlantFood(board, duration);
                    }
                }
            }
        }
    }

    void freezeAll(Plant plant, Board board, int baseTicks) {
        for (Zombie zombie : board.getZombies()) {
            zombie.freeze(PlantLevelRuntime.freezeTicks(plant, baseTicks));
        }
    }

    void attractZombies(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            boolean adjacentLane = Math.abs(zombie.getLane() - plant.getLane()) == 1;
            boolean near = Math.abs(zombie.getX() - plant.getX()) <= ATTRACTION_RANGE;
            if (zombie.isAlive() && adjacentLane && near) {
                zombie.setLane(plant.getLane());
            }
        }
    }

    void warmSurroundings(Plant plant, Board board) {
        if (!def.hasTag(Tag.FIRE)) {
            return;
        }
        int radius = PlantLevelRuntime.warmthRadius(plant);
        for (int lane = Math.max(0, plant.getLane() - radius);
             lane <= Math.min(board.getRows() - 1, plant.getLane() + radius); lane++) {
            for (int x = Math.max(0, plant.getX() - radius);
                 x <= Math.min(board.getColumns() - 1, plant.getX() + radius); x++) {
                warmTile(board.getTileAt(x, lane));
            }
        }
    }

    private void warmTile(Tile tile) {
        if (tile.getType() == TileType.ICY_GROUND) {
            tile.hitIce(6);
        }
        for (Plant plant : tile.getPlantLayers()) {
            plant.meltIce();
        }
    }
}
