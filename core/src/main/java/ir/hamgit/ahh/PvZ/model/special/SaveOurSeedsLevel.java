package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;



import java.util.ArrayList;
import java.util.List;

/**
 * "محافظ دانه‌ها" - a handful of plants are pre-placed on the board at level
 * start; losing any one of them immediately ends the level. Real level
 * content (exact positions/species) would come from level-design data the
 * team defines later; here we place {@code protectedCount} random plants at
 * random empty tiles so the mechanic is fully functional and testable.
 */
public class SaveOurSeedsLevel extends SpecialLevelHandler {

    private final int protectedCount;
    private final List<int[]> protectedPositions = new ArrayList<>();
    private boolean failed;

    public SaveOurSeedsLevel(int protectedCount) {
        this.protectedCount = protectedCount;
    }

    private static final int MAX_ATTEMPTS_PER_PLANT = 30;

    @Override
    public void onLevelStart(Board board) {
        List<PlantType> pool = new ArrayList<>(PlantRegistry.getAll().stream()
                .map(PlantDef::getType).toList());
        for (int i = 0; i < protectedCount && !pool.isEmpty(); i++) {
            placeOneProtectedPlant(board, pool);
        }
    }

    private void placeOneProtectedPlant(Board board, List<PlantType> pool) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_PLANT; attempt++) {
            PlantType type = pool.get((int) (Math.random() * pool.size()));
            int x = (int) (Math.random() * board.getColumns());
            int lane = (int) (Math.random() * board.getRows());
            if (board.plantForFree(type, x, lane)) {
                protectedPositions.add(new int[] {x, lane});
                System.out.printf("A seed to protect: %s at (%d, %d)%n", type, x, lane);
                return;
            }
        }
    }

    @Override
    public void onPlantLost(Board board, Plant plant) {
        for (int[] pos : protectedPositions) {
            if (pos[0] == plant.getX() && pos[1] == plant.getLane()) {
                failed = true;
            }
        }
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return failed;
    }
}
