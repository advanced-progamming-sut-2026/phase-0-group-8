package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;
import ir.hamgit.ahh.PvZ.model.enums.TileType;



import java.util.ArrayList;
import java.util.List;

public class SaveOurSeedsLevel extends SpecialLevelHandler {

    private final int protectedCount;
    private final List<Plant> protectedPlants = new ArrayList<>();
    private boolean failed;

    public SaveOurSeedsLevel(int protectedCount) {
        this.protectedCount = protectedCount;
    }

    @Override
    public void onLevelStart(Board board) {
        List<PlantType> pool = new ArrayList<>(PlantRegistry.getAll().stream()
            .filter(this::isSuitableProtectedPlant).map(PlantDef::getType).toList());
        List<int[]> positions = safePositions(board);
        int count = Math.min(protectedCount, positions.size());
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            int positionIndex = (int) (Math.random() * positions.size());
            placeProtectedPlant(board, pool, positions.remove(positionIndex));
        }
    }

    private List<int[]> safePositions(Board board) {
        List<int[]> positions = new ArrayList<>();
        int safeColumns = Math.max(1, board.getColumns() / 2);
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < safeColumns; x++) {
                positions.add(new int[] {x, lane});
            }
        }
        return positions;
    }

    private void placeProtectedPlant(Board board, List<PlantType> pool, int[] position) {
        PlantType type = pool.get((int) (Math.random() * pool.size()));
        int x = position[0];
        int lane = position[1];
        boolean planted = board.plantForFree(type, x, lane);
        if (!planted) {
            board.getTileAt(x, lane).setType(TileType.NORMAL);
            planted = board.plantForFree(type, x, lane);
        }
        if (planted) {
            protectedPlants.add(board.getTileAt(x, lane).getPlant());
            System.out.printf("A seed to protect: %s at (%d, %d)%n", type, x, lane);
        }
    }

    @Override
    public void onPlantLost(Board board, Plant plant) {
        if (protectedPlants.contains(plant)) {
            failed = true;
        }
    }

    @Override
    public boolean canPluckPlant(Plant plant) {
        return !protectedPlants.contains(plant);
    }

    private boolean isSuitableProtectedPlant(PlantDef def) {
        return !def.hasBehavior(BehaviorType.DISAPPEAR_AFTER_ACTION)
            && !def.hasBehavior(BehaviorType.REMOVE_GRAVE)
            && !def.hasBehavior(BehaviorType.MELT_ICE)
            && !def.hasBehavior(BehaviorType.CONTACT_EXPLOSION)
            && !def.hasBehavior(BehaviorType.ADJACENT_SMASH)
            && !def.hasBehavior(BehaviorType.CONTACT_FREEZE)
            && !def.hasBehavior(BehaviorType.AQUATIC_INSTANT_KILL)
            && !def.hasBehavior(BehaviorType.LIMITED_LIFESPAN)
            && !def.hasBehavior(BehaviorType.COPY_PLANT)
            && !def.hasTag(Tag.TRAP)
            && !def.isCanPlantOnWater();
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return failed;
    }
}
