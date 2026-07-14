package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import java.util.List;

public class MintFamilyBoostBehavior implements PlantBehavior {
    @Override
    public void onPlanted(Plant self, BoardContext ctx) {
        List<Plant> familyMembers = ctx.getPlantsOfCategory(self.getCategory());

        for (Plant plant : familyMembers) {
            plant.setLastActionTick(0);
        }

        ctx.removePlant(self);
    }

    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {}
}
