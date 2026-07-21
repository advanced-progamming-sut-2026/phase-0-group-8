package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;



import java.util.ArrayList;
import java.util.List;

public class SaveOurSeedsLevel extends SpecialLevelHandler {

    private final int protectedCount;
    private final List<Plant> protectedPlants = new ArrayList<>();
    private boolean failed;

    public SaveOurSeedsLevel(int protectedCount) {
        this.protectedCount = protectedCount;
    }

    private static final int MAX_ATTEMPTS_PER_PLANT = 30;

    @Override
    public void onLevelStart(Board board) {
        List<PlantType> pool = new ArrayList<>(PlantRegistry.getAll().stream()
            .filter(this::isSuitableProtectedPlant).map(PlantDef::getType).toList());
        for (int i = 0; i < protectedCount && !pool.isEmpty(); i++) {
            placeOneProtectedPlant(board, pool);
        }
    }

    private void placeOneProtectedPlant(Board board, List<PlantType> pool) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_PLANT; attempt++) {
            PlantType type = pool.get((int) (Math.random() * pool.size()));
            int safeColumns = Math.max(1, board.getColumns() / 2);
            int x = (int) (Math.random() * safeColumns);
            int lane = (int) (Math.random() * board.getRows());
            if (board.plantForFree(type, x, lane)) {
                protectedPlants.add(board.getTileAt(x, lane).getPlant());
                System.out.printf("A seed to protect: %s at (%d, %d)%n", type, x, lane);
                return;
            }
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
            && !def.hasTag(Tag.TRAP)
            && !def.isCanPlantOnWater();
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return failed;
    }
}
